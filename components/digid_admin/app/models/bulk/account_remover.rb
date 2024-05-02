
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
  class AccountRemover
    def initialize(bulk_order)
      @bulk_order = bulk_order
    end

    def can_be_performed?
      @bulk_order.persisted? && @bulk_order.approved_status?
    end

    def perform!
      unless can_be_performed?
        log_not_completed('opdracht niet geaccordeerd')
        return false
      end

      prepare_removal
      remove_accounts
      finalize_removal

      true
    rescue => e
      # We really want to catch everything so we can log it and set a new status
      # Re-raise the exception to make sure we don't break anything
      log_not_completed(e) # FIXME: Use e.message?
      @bulk_order.exceptional_status!
      raise
    end

    private

    def prepare_removal
      @bulk_order.touch(:order_started_at)
      @bulk_order.order_started_status!
      log_removal_started
    end

    def finalize_removal
      # Update status first, needed in CSV
      @bulk_order.touch(:order_finished_at)
      @bulk_order.order_finished_status!
      log_removal_finished
    end

    def remove_accounts
      # FIXME: add dependent: destroy, as in digid_x?
      # We walk thorugh all bulk_order_bsns to see if the status is still valid!
      @bulk_order.bulk_order_bsns.each do |bulk_order_bsn|
        account = bulk_order_bsn.account
        log_account_removed(account, bulk_order_bsn.bsn) if bulk_order_bsn.remove_account!
      end
    end

    def log_removal_started
      Log.instrument('1237', id: @bulk_order.id, name: @bulk_order.name, type: @bulk_order.bulk_type, bulk_order_id: @bulk_order.id)
    end

    def log_removal_finished
      Log.instrument('uc46.bulk_order_delete_accounts_finished', id: @bulk_order.id, name: @bulk_order.name, type: @bulk_order.bulk_type, bulk_order_id: @bulk_order.id)
    end

    def log_account_removed(account, bsn)
      raise(ArgumentError, "Expected Account, got #{account.inspect}!") unless account.is_a?(Account)
      Log.instrument(
        'uc46.bulk_order_account_deleted',
        id: @bulk_order.id,
        name: @bulk_order.name,
        type: @bulk_order.bulk_type,
        bulk_order_id: @bulk_order.id,
        account_id: account.id,
        sector_number: bsn,
        sector_name: 'bsn'
      )
    end

    def log_not_completed(reason)
      reason = reason.to_s.truncate(120)
      Log.instrument('uc46.bulk_order_deleting_accounts_not_completed', reason: reason, id: @bulk_order.id, name: @bulk_order.name, type: @bulk_order.bulk_type, bulk_order_id: @bulk_order.id)
    end
  end
end
