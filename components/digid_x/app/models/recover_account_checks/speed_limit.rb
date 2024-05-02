
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

module RecoverAccountChecks
  # <Controle: Snelheid herstellen per brief>
  class SpeedLimit
    attr_reader :account_id

    def initialize(account_id)
      @account_id = account_id
    end

    def match?
      registrations_for(snelheid_herstelcode.days).count > 0
    end

    def error_message
      I18n.t("messages.notifications.snelheid_herstellen",
             days: "#{snelheid_herstelcode} #{snelheid_herstelcode == 1 ? I18n.t(:day) : I18n.t(:days)}", 
             wait_time:  I18n.l(too_soon_wait_time, format: :date_comma_time_text_tzone_in_brackets)
            )
    end

    def log
      Log.instrument("198", account_id: account_id) # 198
    end

    private

    def registrations_for(period)
      Registration.where(burgerservicenummer: bsn)
                  .where(gba_status: "valid_recovery")
                  .where("created_at > ?", Time.zone.now - period)
                  .requested
    end

    def bsn
      @bsn ||= Account.find(account_id).bsn
    end

    def snelheid_herstelcode
      @snelheid_herstelcode ||= ::Configuration.get_int("snelheid_herstelcode")
    end

    def too_soon_wait_time
      registration = registrations_for(snelheid_herstelcode.days).order(:created_at).take
      registration ? registration.created_at + snelheid_herstelcode.days : 1.minute.from_now
    end
  end
end
