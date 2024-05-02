
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
  # <Controle: Blokkering herstelcode per brief>
  class MaxPerMonth
    attr_reader :account_id

    def initialize(account_id)
      @account_id = account_id
    end

    def match?
      registrations_count = registrations_for(1.month).count
      registrations_count >= herstelcodes_per_maand
    end

    def error_message
      I18n.t("messages.notifications.code_blocked",
             aantal: herstelcodes_per_maand,
             expiry_date_time: I18n.l(too_often_wait_time_codes, format: :sms))
    end

    def log
      Log.instrument("589", account_id: account_id) # 589
    end

    private

    def registrations_for(period)
      Registration.where(burgerservicenummer: bsn)
                  .where(gba_status: "valid_recovery")
                  .where("created_at > ?", Time.zone.now - period)
                  .requested
    end

    def bsn
      @bsn ||= Account.find(account_id).sectorcodes.find_by(sector_id: [Sector.get("bsn"), Sector.get("sofi")]).sectoraalnummer
    end

    def herstelcodes_per_maand
      @herstelcodes_per_maand ||= ::Configuration.get_int("herstelcodes_per_maand")
    end

    def too_often_wait_time_codes
      # return the <herstelcodes_per_maand> last recovery_code created_at date
      codes = RecoveryCode.for_account(account_id).by_letter.last_month.order(created_at: :desc)
                          .limit(herstelcodes_per_maand)
      if codes.count == herstelcodes_per_maand
        codes.last.created_at + 1.month
      else
        1.day.from_now
      end
    end
  end
end
