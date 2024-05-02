
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

# Determines the 'BulkOrder state' of an Account.
#
# Returns following states:
# * finished: 'Account behandeld'
# * approved: 'Account wordt behandeld(verwijderd, aangepast etc..)'
# * suspended: 'Account is opgeschort'
# * other_scope: 'Account heeft andere status dan gekozen'
# * no_account: 'BSN zonder account'
# * not_found: 'BSN niet gevonden'
# * unknown: 'Onbekende status'
# * invalid_bsn: 'Onjuist BSN nummer'
#
class BulkOrderBSN < AccountBase
  belongs_to :bulk_order, counter_cache: true

  serialize :gba_data

  enum status: [:unknown, :approved, :suspended, :other_scope, :no_account, :not_found, :invalid_bsn, :finished]

  before_validation :set_bulk_status

  validates :bulk_order, presence: true
  validates :bsn, presence: true

  delegate :geslachtsnaam, :straatnaam, :postcode, :woonplaats, to: :gba_data # TODO: return correct data needed in AddressListCsvCreator

  def account
    @account ||= Account.with_bsn(bsn).first
  end

  # Returns true if finished. false otherwise.
  def remove_account!
    set_bulk_status # Update status before we're removing
    if account && approved?
      account.destroy!
      finished! # set status
      true
    else
      save!
      false
    end
  end

  def change_account_status!(to:)
    set_bulk_status # Update bulk orders status before we're changing account status
    if account && approved?
      account.update(status: to)
      finished!
      true
    else
      save!
      false
    end
  end

  def human_status
    if suspended_scope && status == "other_scope"
      I18n.translate("other_scope_not_suspended", scope: "bulk_order.bsn_states.#{bulk_type}", default: I18n.translate("bulk_order.bsn_states.#{bulk_type}.unknown"))
    elsif initial_or_requested_scope && status == "other_scope"
      I18n.translate("other_scope_not_initial_or_requested", scope: "bulk_order.bsn_states.#{bulk_type}", default: I18n.translate("bulk_order.bsn_states.#{bulk_type}.unknown"))
    else
      I18n.translate(status, scope: "bulk_order.bsn_states.#{bulk_type}", default: I18n.translate("bulk_order.bsn_states.#{bulk_type}.unknown"))
    end
  end

  def fetch_gba_data!
    Timeout.timeout(5) do # TODO: Make timeout time configurable?!
      gba_data = GbaClient.find_bsn(bsn)
      update(gba_data: gba_data, gba_timeout: false)
    end
  rescue Timeout::Error
    update(gba_timeout: true)
  end

  private

  def set_bulk_status
    @account = nil # FIXME: we reload the account to make sure it is still there
    self.status = determine_bulk_status
  end

  def determine_bulk_status
    return :finished if finished?
    return :invalid_bsn unless BsnChecker.new(bsn).valid?
    return :not_found if account.nil?
    return :no_account if (account.state.removed? || Afmeldlijst.bsn_op_afmeldlijst?(bsn))

    return case
           when suspended_scope
             account.state.suspended? ? :approved : :other_scope
           when initial_or_requested_scope
             (account.state.initial? || account.state.requested?) ? :approved : :other_scope
           when active_scope
             (account.state.active?) ? :approved : :other_scope
          end

    raise 'Unknown flow for account state'
  end

  def bulk_type
    bulk_order.bulk_type
  end

  def active_scope
    bulk_order.active?
  end

  def suspended_scope
    bulk_order.suspended?
  end

  def initial_or_requested_scope
    bulk_order.initial_or_requested?
  end
end
