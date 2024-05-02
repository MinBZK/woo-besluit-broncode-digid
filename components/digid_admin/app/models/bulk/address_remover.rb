
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

# Removes 'cached' BRP (GBA) information stored for a BulkOrder.
module Bulk
  class AddressRemover
    MAX_BRP_CACHE_DAYS = 5

    def initialize(bulk_order)
      @bulk_order = bulk_order
    end

    def can_be_performed?
      @bulk_order.persisted? && @bulk_order.finalized_status?
    end

    def perform!
      @bulk_order.bulk_order_bsns.update_all(gba_data: nil)
      Log.instrument('uc46.bulk_order_brp_details_complete', id: @bulk_order.id, name: @bulk_order.name, type: @bulk_order.bulk_type, bulk_order_id: @bulk_order.id)
      true
    end
  end
end
