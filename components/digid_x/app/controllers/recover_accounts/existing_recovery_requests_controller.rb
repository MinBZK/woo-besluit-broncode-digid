
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

module RecoverAccounts
  class ExistingRecoveryRequestsController < RecoverAccountsController
    # GET /herstellen/lopende_aanvraag/annuleren
    def cancel
      flash.now[:notice] = t("you_will_return_to_homepage_digid_cancel")

      @page_name  = "G2"
      @confirm_to = confirm_cancel_url
      @return_to  = existing_recovery_request_url
      @confirm_log = "222"

      render_partial_or_return_json(".main-content", "shared/cancel", "shared/_cancel")
    end

    # POST /herstellen/lopende_aanvraag
    def create
      if Confirm.new(confirm_params).yes?
        letter = ActivationLetter.where(
          "registrations.burgerservicenummer" => current_account.bsn,
          "activation_letters.letter_type" => "recovery_password"
        ).joins(:registration).last

        letter_sent = letter&.status == ActivationLetter::Status::SENT

        Log.instrument(letter_sent ? "614" : "615", account_id: current_account.id)
        current_account.recovery_codes.by_letter.not_used.not_expired.destroy_all
        redirect_to(recovery_code_gba_check_url)
      else
        Log.instrument("616", account_id: current_account.id)
        redirect_to(APP_CONFIG["urls"]["external"]["digid_home"])
      end
    end

    # GET /herstellen/lopende_aanvraag
    def show
      session[:recover_account_entry_point] = "vervangen_herstellen_wachtwoord"
      @page_name = "E5"
    end
  end
end
