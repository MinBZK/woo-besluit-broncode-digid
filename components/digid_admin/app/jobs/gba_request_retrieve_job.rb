
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

# Does a single GBA call through delayed-job
class GbaRequestRetrieveJob
  include Sidekiq::Worker
  sidekiq_options retry: 5, queue: 'bulk-brp'

  def perform(bsn, bulk_order_id, last=nil)
    bulk_order = BulkOrder.find(bulk_order_id)
    return unless bulk_order
    begin
      Log.instrument('uc46.bulk_order_brn_request_started', bsn: bsn, bulk_order_id: bulk_order_id, sector_number: bsn, sector_name: 'bsn') # log#684
      gba_data = GbaClient.find_bsn(bsn)
      if gba_data
        bulk_order.bulk_order_bsns.where(bsn: bsn).update_all(gba_data: gba_data, gba_timeout: false)
      end
      Log.instrument('uc46.bulk_order_brn_request_finished', bsn: bsn, bulk_order_id: bulk_order_id, sector_number: bsn, sector_name: 'bsn') # log#685
    rescue Net::ReadTimeout
      bulk_order.bulk_order_bsns.where(bsn: bsn).update_all(gba_timeout: true)
      Log.instrument('uc46.bulk_order_brn_request_not_completed', bsn: bsn, bulk_order_id: bulk_order_id, sector_number: bsn, sector_name: 'bsn') # log#686
    end
    return unless last

    # Ideally we want to use here Sidekiq Pro and the batch functionalitity,
    # but now we do poor man's batch: 5 minutes after the last enqueued job finished we schedule the report.
    # The last enqueued job isn't necessarily the last executed job, but 5 minutes give enough room here.
    BulkOrderFinishJob.perform_in(5.minutes, bulk_order_id)
    bulk_order.update_attribute(:brp_last_run_at, Time.zone.now)
  end
end
