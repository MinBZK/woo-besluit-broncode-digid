
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

# Creates a CSV with address information for each finished BSN in a BulkOrder.
module Bulk
  class AddressListCsvCreator
    attr_reader :csv_time
    include Letter::Salutation
    include Letter::Translation
    include Letter::Address

    def initialize(bulk_order)
      @bulk_order = bulk_order
      @codes_path   = APP_CONFIG["csv_codes_file"]&.path
    end

    def filename
      "Adreslijst - #{@bulk_order.name} - #{I18n.l(@bulk_order.created_at)}.csv"
    end

    def csv
      return '' unless @bulk_order.allow_address_list_download?
      @csv_time = Time.zone.now
      CSV.generate(headers: true, return_headers: true) do |csv|
        csv << csv_headers
        @bulk_order.bulk_order_bsns.finished.each do |bulk_order_bsn|
          status = 'Fout'
          status = 'Time-out' if bulk_order_bsn.gba_timeout?
          if bulk_order_bsn.gba_data.present?
            status = I18n.t(bulk_order_bsn.gba_data.status, scope: 'bulk_order.brp_states', default: 'Onbekende status')
            gba = bulk_order_bsn.gba_data
            csv << [
              I18n.l(csv_time), @bulk_order.name, @bulk_order.bulk_type, bulk_order_bsn.bsn, status,
              naam_aanhef(gba), naam(gba), "#{straatnaam(gba)} #{huisnummer(gba)}",
              "#{bulk_order_bsn.postcode} #{woonplaats(gba, @codes_path)}", '6030'
            ]
          else
            csv << [I18n.l(csv_time), @bulk_order.name, @bulk_order.bulk_type, bulk_order_bsn.bsn, status, '', '', '', '', '6030']
          end
        end
      end
    end

    private

    def csv_headers
      ['Datum-tijd aanmaak CSV', 'Opdracht', 'Type opdracht', 'BSN', 'Status', 'Aanhef', 'Naamregel 1', 'Adresregel 1', 'Adresregel 2', 'Landcode']
    end
  end
end
