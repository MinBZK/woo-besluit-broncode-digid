
# Deze broncode is openbaar gemaakt vanwege een Woo-verzoek zodat deze
# gericht is op transparantie en niet op hergebruik. Hergebruik van 
# de broncode is toegestaan onder de EUPL licentie, met uitzondering 
# van broncode waarvoor een andere licentie is aangegeven.
# 
# Het archief waar dit bestand deel van uitmaakt is te vinden op:
#   https://github.com/MinBZK/woo-besluit-broncode-digid
# 
# Eventuele kwetsbaarheden kunnen worden gemeld bij het NCSC via:
#   https://www.ncsc.nl/contact/kwetsbaarheid-melden
# onder vermelding van "Logius, openbaar gemaakte broncode DigiD" 
# 
# Voor overige vragen over dit Woo-besluit kunt u mailen met open@logius.nl
# 
# This code has been disclosed in response to a request under the Dutch
# Open Government Act ("Wet open Overheid"). This implies that publication 
# is primarily driven by the need for transparence, not re-use.
# Re-use is permitted under the EUPL-license, with the exception 
# of source files that contain a different license.
# 
# The archive that this file originates from can be found at:
#   https://github.com/MinBZK/woo-besluit-broncode-digid
# 
# Security vulnerabilities may be responsibly disclosed via the Dutch NCSC:
#   https://www.ncsc.nl/contact/kwetsbaarheid-melden
# using the reference "Logius, publicly disclosed source code DigiD" 
# 
# Other questions regarding this Open Goverment Act decision may be
# directed via email to open@logius.nl

# frozen_string_literal: true

# Holds application logic for activating DigiD applications (aanvragen)
#
# This controller activates:
# - an regular (basis) Account (from not active to active)
# - an Account with SMS (midden) (from active basis to active midden)
class ActivationsController < ApplicationController
  include AppSessionConcern
  include LogConcern
  before_action :find_account,              except: [:new]
  before_action :check_session_time,        except: [:new]
  before_action :update_session,            except: [:new]
  before_action :account_deceased?,         only: [:activationcode, :activationcode_post, :confirm_activation]
  before_action :other_buttons?,            only: [:activationcode_post]
  before_action :passed_smscode?,           only: [:activationcode, :confirm_activation]
  before_action :set_current_activatable,   only: [:activationcode, :confirm_activation]
  before_action :passed_activationcode?,    only: [:confirm_activation]
  before_action :check_cookie_blocked?, only: [:new]

  helper_method :activation_code_received_by
  helper_method :show_did_not_receive_letter?

  # activate_url points to here.
  # Starting point for activation
  # redirect to a sign-in page (B1), then come back
  def new
    start_session("activation")
    session[:authentication] = {}
    session.delete("saml.session_key")
    session.delete("saml.provider_id")
    session[:mydigid_logged_in] = false
    session[:smscode_passed] = false
    session[:activationcode_passed] = false
    session[:activatable] = nil

    @activation_options = [[I18n.t("activate_digid"), :account], [I18n.t("activate_sms"), :sms_uitbreiding]]

    @news_items = news_items("Activeringspagina")
    @page_name = "B1"
    @session_ends_label = true
    session.delete(:recover_account_entry_point)

    redirect_to activate_sign_in_url
  end

  # form for submitting activation code(s)
  # if user tried to much, block the account already
  # else check if correct controle code is submitted
  #  when not correct:
  #    count attempts and redirect to either max_reaches or try_again
  #  when correct
  #    redirect to confirmed
  def activationcode
    @page_name = "B3"
    @extend_session_popup = true

    number_of_attempts = current_authenticator.number_of_attempts
    if current_authenticator.reached_attempts_limit?
      block_activation(current_authenticator, number_of_attempts)
    else
      @activationcode = Activationcode.new
    end
  end

  def activationcode_post
    @page_name = "B3"
    @extend_session_popup = true
    @activationcode = Activationcode.new(activation_params.merge(account: @account, authenticator: current_authenticator))

    if @account.app_authenticators.pending.last.try(:activation_code) == activation_params[:activationcode]
      @activationcode.errors.add(:activationcode, :incorrect_due_to_app)
      @activationcode.log_errors
      @activationcode.authenticator.add_one_attempt
      not_correct
    elsif @activationcode.valid?
      # correct!
      session[:activationcode_passed] = true
      redirect_to confirm_activation_url
    else
      not_correct
    end
  end

  def activationcode_cancel
    app_session&.cancel!

    session.delete(:app_session_id)
    redirect_to confirm_cancel_url
  end

  # create the page elements of the final screen
  # and more important, make the account active,
  # unless the (correct) code provided is expired.
  # when ok:
  # - delete all previous attempts
  # - confirm the account
  # - send an email
  def confirm_activation
    Account.transaction do
      current_authenticator.delete_attempts
      if (days = current_authenticator.expired?)
        # expired, do not record this as an attempt
        Log.instrument("90", account_id: @account.id)
        flash[:notice] = t("messages.activation.expired", count: days)
        render_simple_message(ok: APP_CONFIG["urls"]["external"]["digid_home"])
      else
        @page_name = "B4"
        @page_title = t("titles.B4.default")

        @sms_in_uitbreiding = @account.sms_in_uitbreiding?
        # log about it: new accounts are inactive, password is pending, 'uitbreidingen' are already active
        if @account.password_authenticator.state.pending?
          if @account.via_balie?
            FrontDeskActivationJob.perform_async(@account.bsn, @account.password_authenticator.activation_code)
            Log.instrument("93", account_id: @account.id, balie_id: @account.distribution_entity.balie_id, fields: log_fields(@account))
          else
            Log.instrument("93", account_id: @account.id, fields: log_fields(@account))
          end
          @page_header = t("your_digid_is_activated")
          @message_key = "activate_digid_message"
        elsif @account.sms_in_uitbreiding? || @account.mobiel_kwijt_in_progress?
          Log.instrument("391", account_id: @account.id)
          @page_title = t("titles.B4.sms")
          @page_header = t("extra_sms_check_is_activated")
          @message_key = "extra_sms_check_message"
        elsif @account.via_balie? && Rails.application.config.integration_mode
          Log.instrument("93", account_id: @account.id, fields: log_fields(@account))
          @page_header = t("your_digid_is_activated")
          @message_key = "activate_digid_message"
        end

        if @account.mobiel_kwijt_in_progress?
          current_account.with_language { SmsChallengeService.new(account: current_account).send_sms(message: t("sms_message.SMS24")) }
          @account.activate_sms_tool!
          if current_account.email_activated?
            NotificatieMailer.delay(queue: "email").notify_telefoonnummer_wijziging(account_id: current_account.id, recipient: current_account.adres)
          end
          @message_key = "extra_sms_check_message"
        else
          @account.activate!
          @account.activate_sms_tool!
          @account.destroy_other_accounts(sms_in_uitbreiding: @sms_in_uitbreiding)
        end

        reset_session
      end
    end
  end

  private

  def activation_params
    params.require(:activationcode).permit(:activationcode)
  end

  # check to see if the user selected the cancel button
  def other_buttons?
    if clicked_cancel?
      @confirm_to = nil
      cancel_button(return_to: activationcode_url, confirm_to: @confirm_to)
    end
  end

  def not_correct
    number_of_attempts = current_authenticator.number_of_attempts
    if current_authenticator.reached_attempts_limit?
      if @account.mobiel_kwijt_in_progress_activatable?
        @account.sms_tools.pending.destroy_all
      elsif @account.sms_in_uitbreiding_activatable?
        @account.remove_mobiel
        @account.to_basis
      end
      block_activation(current_authenticator, number_of_attempts)
    else
      flash[:notice] ||= I18n.t("activemodel.errors.models.activationcode.attributes.activationcode.incorrect", letter_sent_at: Activationcode.new(account: @account).letter_sent_at)
      render(:activationcode)
    end
  end

  def activation_code_received_by
    if !@account.sms_in_uitbreiding? && @account.midden_aanvraag? && @account.via_balie?
      "front_desk"
    else
      "post"
    end
  end

  def account_activation_session?
    (@account.basis_aanvraag? || @account.midden_aanvraag?) && session[:session].eql?("activation")
  end

  def sms_activation_session?
    (@account.mobiel_kwijt_in_progress_activatable? || @account.sms_in_uitbreiding_activatable?) && session[:session].eql?("activation")
  end

  def account_deceased?
    if @account&.deceased?
      Log.instrument("1479", account_id: @account.id, hidden: true)

      flash.now[:notice] = if @account.status == "active"
                             t("account.deceased.activate_sms")
                           else
                             t("account.deceased.activate")
                           end
      render_message
    end
  end

  def current_authenticator
    @current_authenticator ||= @account.send(session[:activatable])
  end

  # when account is of type 'basis' smscode_passed is already true,
  # otherwise the user had to pass the sms-challenge
  def passed_smscode?
    session[:smscode_passed] = session[:sms_options][:passed?] if session[:sms_options]
    redirect_to activate_url unless session[:smscode_passed]
  end

  def set_current_activatable
    if sms_activation_session?
      session[:activatable] = :pending_sms_tool
    elsif account_activation_session?
      session[:activatable] = :password_authenticator
    else
      redirect_to activate_url
    end
  end

  def passed_activationcode?
    redirect_to(activationcode_url, method: :get) if session[:smscode_passed] && !session[:activationcode_passed]
  end

  def show_did_not_receive_letter?
    !(!session[:session].eql?("activation") || sms_activation_session? || @account.herkomst.present? || @account.issuer_type == IssuerType::DESK)
  end
end
