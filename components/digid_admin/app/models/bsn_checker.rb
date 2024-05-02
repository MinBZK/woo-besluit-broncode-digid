
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

# Usage:
#     bsn_check = BsnChecker.new(some_bsn)
#     bsn_check.valid? # => true or false
class BsnChecker
  def initialize(bsn)
    @bsn = bsn
  end

  def valid?
    valid_length? && valid_chars? && eleven_proof?
  end

  def check
    return 'ok' if valid?
    return I18n.t('bulk_order.bsn.error.invalid_length') if !valid_length? && valid_chars?
    return I18n.t('bulk_order.bsn.error.invalid_length_chars') if !valid_length? && !valid_chars?
    return I18n.t('bulk_order.bsn.error.invalid_chars') if valid_length? && !valid_chars?
    return I18n.t('bulk_order.bsn.error.not_eleven_proof') if valid_length? && valid_chars? && !eleven_proof?
  end

  private

  def valid_length?
    @bsn !~ /\A.{9}\z/ ? false : true
  end

  def valid_chars?
    @bsn !~ /\A[+-]?\d+\Z/ ? false : true
  end

  def eleven_proof?
    ((0..7).sum { |i| (9 - i) * @bsn[i].to_i } - @bsn[8].to_i) % 11 == 0
  end
end
