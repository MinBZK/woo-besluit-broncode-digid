
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

module Registrations
  class ExistingSmsRequestsController < ApplicationController
    before_action :check_session_time, only: [:show]

    # GET /sms_uitbreiding (existing_sms_request_url) - D21 (also sometimes known as A1E)
    def show
      if display_continue_with_app?
        @confirm = Confirm.new(value: :continue_with_app)
        @confirm_options = [[t("yes_continue_request_with_app"), :continue_with_app], [t("reactivate_app_with_letter"), :continue], [t("reactivate_app_cancel"), false]]
      else
        @confirm = Confirm.new(value: :continue)
        @confirm_options = [[t("reactivate_app_with_letter"), :continue], [t("reactivate_app_cancel"), false]]
      end
      @page_name  = "D21"
      @page_title = t("titles.D21")
      session[:re_request_new_mobile] = true
    end

    # POST /sms_uitbreiding (existing_sms_request_url) - D21
    def create
      @confirm = Confirm.new(confirm_params)

      if @confirm.value == "false"
        Log.instrument("563", account_id: current_account.id)
        return redirect_to(my_digid_url)
      end

      registration = Registration.where(burgerservicenummer: current_account.bsn).order(:id).first
      letter = registration.activation_letters.where(letter_type: "uitbreiding_sms").last if registration

      if letter.present?
        log_code = letter.status == ActivationLetter::Status::SENT ? "166" : "167"
        Log.instrument(log_code, registration_id: current_registration&.id, account_id: current_account&.id)
      end

      if @confirm.value == "continue_with_app"
        current_account.pending_sms_tool&.destroy

        redirect_to my_digid_sms_authenticators_start_url
      elsif @confirm.value == "continue"
        current_account.pending_sms_tool&.destroy

        if current_registration
          redirect_to new_my_digid_request_two_factor_authentications_url
        else
          #do the GBA check
          redirect_to request_sms_url
        end
      elsif @confirm.value == "false"
        Log.instrument("563", account_id: current_account.id)
        redirect_to(my_digid_url)
      else
        redirect_to(my_digid_url)
      end
    end

    # GET /sms_uitbreiding/annuleren (cancel_existing_sms_request_url) - D21
    def cancel
      Log.instrument("645", account_id: current_account.id)
      redirect_to(my_digid_url)
    end

    private

    def display_continue_with_app?
      current_account.app_authenticator_active? && digid_app_enabled? &&
        (current_account.sms_tools.active? || current_account.sms_in_uitbreiding?)
    end

  end
end
