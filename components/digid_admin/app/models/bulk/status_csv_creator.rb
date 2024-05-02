
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

module Bulk
  class StatusCsvCreator
    attr_reader :csv_time

    def initialize(bulk_order)
      @bulk_order = bulk_order
    end

    def available?
      @bulk_order && @bulk_order.bulk_order_bsns.present?
    end

    def filename
      "#{@bulk_order.name} - #{I18n.l(Time.zone.now)}.csv"
    end

    def csv
      @csv_time = Time.zone.now
      CSV.generate(headers: true, return_headers: true) do |csv|
        csv << csv_headers
        # Datum-tijd status opdracht
        #   De datum/tijd van de laatste statuswijziging zoals die ook op het
        #   overzicht (S071) getoond wordt en op S072 per status wordt getoond.
        @bulk_order.bulk_order_bsns.each do |bulk_order_bsn|
          csv << [I18n.l(csv_time), @bulk_order.name, @bulk_order.bulk_type, @bulk_order.human_status, I18n.l(@bulk_order.status_updated_at), bulk_order_bsn.bsn, bulk_order_bsn.human_status]
        end
      end
    end

    private

    def csv_headers
      ['Datum-tijd aanmaak CSV', 'Opdracht', 'Type opdracht', 'Status opdracht', 'Datum-tijd status opdracht', 'BSN', 'Status']
    end
  end
end
