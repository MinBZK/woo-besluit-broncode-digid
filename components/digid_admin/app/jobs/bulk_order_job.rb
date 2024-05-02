
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

# When a BulkOrder is approved (via BulkOrdersController#approve) we kick off this job.
# It [removes all Accounts / changes account statuses] and (if successful) retrieves addresses.
class BulkOrderJob
  include Sidekiq::Worker
  sidekiq_options queue: 'bulk'

  def perform(bulk_order_id)
    bulk_order = BulkOrder.find(bulk_order_id)

    case bulk_order.bulk_type
    when "verwijderen"
      return unless bulk_order && Bulk::AccountRemover.new(bulk_order).perform!
    when "opschorten"
      return unless bulk_order && Bulk::AccountStatusChanger.new(bulk_order, ::Account::Status::SUSPENDED).perform!
    when "opschorten ongedaan maken"
      return unless bulk_order && Bulk::AccountStatusChanger.new(bulk_order, ::Account::Status::ACTIVE).perform!
    end

    Bulk::AddressRetriever.new(bulk_order).retrieve!
  end
end
