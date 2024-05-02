
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

module MyDigid
  class RequestTwoFactorAuthenticationsController < BaseController
    include AppSessionConcern

    before_action :render_not_found_if_account_deceased
    before_action :load_registration
    before_action :commit_cancelled?

    def index
      @page_name = "D16"
      @choice_to_proceed = Confirm.new(value: true)
    end

    def new
      Log.instrument("161", account_id: current_account.id)
      @page_name = "D17"
      @sms_tool = Authenticators::SmsTool.new
    end

    def create
      postcode = @registration.activation_letters.last.postcode
      issuer_type = BeveiligdeBezorgingPostcodeCheck.new(postcode).positive? ? IssuerType::LETTER_SECURE_DELIVERY : IssuerType::LETTER
      @sms_tool = build_pending_sms_tool(account: current_account, **sms_tool_params, issuer_type: issuer_type)

      Log.instrument("164", account_id: current_account.id) if @sms_tool.errors.details[:phone_number].any? { |x| x.dig(:error) == t("you_have_reached_the_mobile_numbers_maximum") }

      if @sms_tool.valid?
        from_ask_mobile_to_check_mobile
      else
        Log.instrument("549", account_id: current_account.id) if @sms_tool.errors[:phone_number]
        @page_name = "D17"
        @sms_tool.gesproken_sms = false # reset to false
        render :new
      end
    end

    def dispatch_choice_to_proceed
      @choice_to_proceed = Confirm.new(confirm_params)
      if @choice_to_proceed.yes?
       redirect_to new_my_digid_request_two_factor_authentications_url
      else
        redirect_to cancel_my_digid_request_two_factor_authentications_url
      end
    end

    # aftermath for D17, creates a authentication sms extension
    def confirm # rubocop:disable AbcSize
      Registration.transaction do

        @registration.update_attribute(:status, ::Registration::Status::REQUESTED)
        postcode = @registration.activation_letters.last.postcode
        issuer_type = BeveiligdeBezorgingPostcodeCheck.new(postcode).positive? ? IssuerType::LETTER_SECURE_DELIVERY : IssuerType::LETTER

        if current_account.mobiel_kwijt_in_progress? || session[:change_mobile_while_old_number_not_usable]

          @registration.update_letters_to_finished_and_expiration(geldigheidstermijn_herstel_brief, ActivationLetter::LetterType::RECOVER_SMS)

          build_pending_sms_tool(
            account: current_account,
            activation_code: @registration.activation_codes,
            geldigheidstermijn: geldigheidstermijn_herstel_brief,
            issuer_type: issuer_type,
            phone_number: DigidUtils::PhoneNumber.normalize(session[:sms_options][:new_number]),
            gesproken_sms: session[:sms_options][:gesproken_sms]
          )

          Log.instrument("193", account_id: current_account.id)
        elsif session[:request_new_mobile] == true
          session.delete(:request_new_mobile)

          # make letter final
          @registration.finish_letters(ActivationLetter::LetterType::UITBREIDING)

          build_pending_sms_tool(
            account: current_account,
            activation_code: @registration.activation_codes,
            geldigheidstermijn: geldigheidstermijn_herstel_brief,
            issuer_type: issuer_type,
            phone_number: DigidUtils::PhoneNumber.normalize(session[:sms_options][:new_number]),
            gesproken_sms: session[:sms_options][:gesproken_sms]
          )

          Log.instrument("168", account_id: current_account.id)
        end

        Log.instrument("149", account_id: current_account.id) if session[:sms_options][:gesproken_sms] == "1"

        current_account.destroy_old_email_recovery_codes do |account|
          Log.instrument("889", account_id: account.id, attribute: "telefoonnummer", hidden: true)
        end
      end

      session.delete(:change_mobile_while_old_number_not_usable)
      session.delete(:sms_options)
      redirect_to extension_confirmation_url
    end

    def cancel
      session.delete(:change_mobile_while_old_number_not_usable)
      session.delete(:current_flow)

      Log.instrument("165", account_id: current_account.id)
      redirect_to(my_digid_url)
    end

    private

    # setup sms check with new number
    def from_ask_mobile_to_check_mobile
      session[:sms_options] = {
        cancel_to:      my_digid_url,
        gesproken_sms:  params[:authenticators_sms_tool][:gesproken_sms],
        instant_cancel: true,
        new_number:     params[:authenticators_sms_tool][:phone_number],
        return_to:      confirm_my_digid_request_two_factor_authentications_url,
        step:           :request_sms_code_verification
      }
      redirect_to authenticators_check_mobiel_url
    end

    def my_digid_session
      redirect_to my_digid_url unless session[:change_mobile_while_old_number_not_usable] == true || session[:request_new_mobile] == true
    end

    def load_registration
      @registration = Registration.find_by_id(session[:registration_id]) if session[:registration_id]
      redirect_to my_digid_url if @registration.blank?
    end

    # cancel button from mijn digid, go directly to MijnDigiD homepage
    def commit_cancelled?
      return unless clicked_cancel?

      Log.instrument("165", account_id: current_account.id)
      redirect_to(my_digid_url)
    end

    def sms_tool_params
      params.require(:authenticators_sms_tool).permit(:phone_number, :gesproken_sms).merge(current_phone_number: current_account.active_sms_tool.try(:phone_number)).to_h.symbolize_keys
    end

    def geldigheidstermijn_herstel_brief
      @geldigheidstermijn_herstel_brief ||= ::Configuration.get_int("geldigheidstermijn_herstel_brief")
    end
  end
end
