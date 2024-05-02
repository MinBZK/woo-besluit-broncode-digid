
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

# Generates a CSV with information about all BulkOrders.
module Bulk
  class OverviewCsvCreator
    def self.available?
      # Do a quick count(*) before retrieving everything
      BulkOrder.any?
    end

    def self.csv
      CSV.generate(headers: true, return_headers: true) do |csv|
        csv << headers
        # `find_each` performs better but doesn't allow DESC
        BulkOrder.order(created_at: :desc).each do |bulk_order|
          csv << create_row(bulk_order)
        end
      end
    end

    def self.filename
      "Overzicht bulkopdrachten #{I18n.l(Time.zone.now)}.csv"
    end

    def self.create_row(bulk_order)
      raise(ArgumentError, "Expected a BulkOrder, got #{bulk_order.inspect}!") unless bulk_order.is_a?(BulkOrder)
      row = []
      row << bulk_order.id
      row << bulk_order.name
      row << bulk_order.bulk_type
      row << I18n.t(bulk_order.status, scope: 'bulk_order.statuses')
      row << I18n.t(bulk_order.account_status_scope, scope: 'bulk_order.account_scopes')

      if bulk_order.invalid_status?
        # Invalid status
        row << I18n.l(bulk_order.created_at)
        row << bulk_order.manager.full_name
        10.times { row << nil } # pad empty columns
      else
        2.times { row << nil } # pad invalid status columns
        row << _localize(bulk_order.created_at) # only when not invalid status?
        row << bulk_order.manager.full_name
        # Approved status
        row << _localize(bulk_order.approved_at)
        row << (bulk_order.approval_manager && bulk_order.approval_manager.full_name)
        # Rejected status
        row << _localize(bulk_order.rejected_at)
        row << (bulk_order.rejection_manager && bulk_order.rejection_manager.full_name)
        # Removal timestamps
        row << _localize(bulk_order.order_started_at)
        row << _localize(bulk_order.order_finished_at)
        # BRP address list retrieval timestamp + finalized timestamp
        row << _localize(bulk_order.brp_started_at)
        row << _localize(bulk_order.finalized_at)
      end

      # Counters
      row << bulk_order.bulk_order_bsns_count               # Totaal aantal BSN's dat in de opdracht is opgenomen
      row << bulk_order.bulk_order_bsns.invalid_bsn.count   # Aantal ongeldige BSN's
      if bulk_order.invalid_status?
        4.times { row << nil }                                  # Hide counters in invalid status (as specified)
      else
        row << bulk_order.no_account_count    # Aantal BSN's zonder account (BSN's met status 'Opgeheven' of 'Afgemeld')
        row << bulk_order.not_found_count     # Aantal niet gevonden BSN's
        row << bulk_order.other_scope_count   # Aantal gevonden accounts in andere status
        row << bulk_order.approved_count      # Aantal aan te passen accounts
      end
      row
    end

    def self.headers
      [
        'Id',
        'Opdracht',
        'Type opdracht',
        'Status opdracht',
        'Accounts aanpassen met status',
        'Ongeldig',
        'Door',
        'Aangemaakt',
        'Door',
        'Geaccordeerd',
        'Door',
        'Afgekeurd',
        'Door',
        'Start aanpassen accounts',
        'Eind aanpassen accounts',
        'Start opvragen adressen',
        'Opdracht uitgevoerd',
        'Aantal BSN\'s',
        'Aantal ongeldige BSN\'s',
        'Aantal BSN niet gevonden',
        'Aantal BSN zonder account',
        'Aantal gevonden accounts in andere status',
        'Aantal aan te passen accounts'
      ]
    end

    # Avoid I18n::ArgumentError for nil values
    # Used in CSV generation
    def self._localize(*args)
      I18n.localize(*args) unless args.first.nil?
    end
  end
end
