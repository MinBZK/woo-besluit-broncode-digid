
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

class PasswordsController < ApplicationController
  include AuthenticationSession

  before_action :find_account
  before_action :find_recovery_account
  before_action :found_account?
  before_action :check_session_time
  before_action :update_session
  before_action :set_name_level
  before_action :skipped?,                only: [:update]
  before_action :change_password_blank?,  only: [:update]
  before_action :check_current_password,  only: [:update]

  def edit
    # redirect the user if he does not have a weak password
    redirect_to my_digid_url unless session[:weak_password].present?
    Log.instrument("379", account_id: @account.id, number: @account.weak_password_skip_count + 1)
  end

  def update

    if @account.password_authenticator.change_password(params[:authenticators_password][:password], params[:authenticators_password][:password_confirmation])
      Log.instrument("384", account_id: @account.id)
      @account.reset_weak_password_skip_count!
      session.delete(:weak_password)
      @redirect_location = handle_after_authentication(session.delete(:weak_password_level), @account)
    else
      if @account.password_authenticator.errors.include?(:current_password) || @account.password_authenticator.errors.include?(:password) || @account.password_authenticator.errors.include?(:password_confirmation)
        if @account.password_authenticator.errors.include?(:password) && @account.password_authenticator.errors.messages[:password].count == 1 && @account.password_authenticator.errors.messages[:password][0] == I18n.t("activerecord.errors.models.authenticators/password.attributes.password.confirmation")
          Log.instrument("387", account_id: @account.id)
        else 
          Log.instrument("385", account_id: @account.id)
        end
      end
      render :edit
    end
  end

  private

  def change_password_blank?
    # check of all fields have data?

    return unless params[:authenticators_password].blank? || params[:authenticators_password][:current_password].blank? || params[:authenticators_password][:password].blank? || params[:authenticators_password][:password_confirmation].blank?

    { current_password: "current_password", password: "new_password", password_confirmation: "repeat_password" }.each do |field, label|
      @account.password_authenticator.errors.add(field, t("activerecord.errors.messages.blank", attribute: t(label))) if params[:authenticators_password].blank? || params[:authenticators_password][field].blank?
    end
    Log.instrument("385", account_id: @account.id)
    render :edit
  end

  def check_current_password
    return if @account.password_authenticator.verify_password(params[:authenticators_password][:current_password])

    @account.blocking_manager.register_failed_attempt!
    @account.password_authenticator.errors.add(:current_password, t("activerecord.errors.models.account.attributes.current_password.incorrect"))
    Log.instrument("67", account_id: @account.id)
    # one more try unless blocked => render a DigiD melding screen... and stop
    render :edit
  end

  def set_name_level
    return unless webservice_present?

    @webdienst        = webservice_name
    @level            = authentication_level

    authentication_webservice = session[:authentication][:webservice_type].constantize.find(session[:authentication][:webservice_id])
    sso_domain                = authentication_webservice.sso_domain if authentication_webservice && session[:authentication][:webservice_type] == "SamlProvider"
    sso_domain_name           = sso_domain.name if sso_domain
    return unless sso_domain_name

    @federation_name = I18n.t("saml.federation.name", webservice_name: @webdienst)
  end

  def skipped?
    return unless params[:commit].eql?("Overslaan")

    if @account.max_weak_password_skip_count_reached?
      render :edit
    else
      Account.where(id: @account.id).update_all("weak_password_skip_count = weak_password_skip_count + 1")
      Log.instrument("383", account_id: @account.id, number: @account.weak_password_skip_count + 1)
      session.delete(:weak_password)
      redirect_to handle_after_authentication(session.delete(:weak_password_level), @account)
    end
  end

  # let's nog proceed if no account has been found
  def found_account?
    raise SessionExpired unless @account
  end
end
