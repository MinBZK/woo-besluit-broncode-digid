
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

class Subscriber < AccountBase
  belongs_to :subscription
  paginates_per 30

  validates :bsn, length: { is: 9 }, presence: true, uniqueness: { case_sensitive: true, scope: :subscription_id }
  validates :subscription_id, presence: true

  validate :elf_proef, if: -> { bsn.present? }
  validate :not_on_afmeldlijst, if: -> { bsn.present? }

  delegate :name, to: :subscription, prefix: true

  def account
    Account.active.with_bsn(bsn).first || Account.with_bsn(bsn).first
  end

  def elf_proef
    return if ((0..7).sum { |i| (9 - i) * bsn[i].to_i } - bsn[8].to_i) % 11 == 0
    errors.add(:bsn, :elf_proef)
  end

  def not_on_afmeldlijst
    return unless subscription.type == 'PilotGroup'
    return unless Afmeldlijst.bsn_op_afmeldlijst?(bsn)
    errors.add(:bsn, :afmeldlijst)
  end
end
