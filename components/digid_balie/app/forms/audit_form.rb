
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

# frozen_string_literal: true

class AuditForm
  include ActiveModel::Model

  attr_accessor :motivation, :verification_correct, :verification, :user

  validates :verification_correct, inclusion: { in: [false, true] }
  validates :motivation, presence: true, if: :motivation_required?

  def submit
    return false unless valid?

    verification.build_audit(motivation: motivation, user: user, verification_correct: verification_correct) if motivation_required?
    verification.state = ::Verification::State::COMPLETED
    verification.save!
  end

  def verification_correct
    [true, 1, '1', 't', 'T', 'true', 'TRUE', 'on', 'ON'].include?(@verification_correct) unless @verification_correct.nil?
  end

  private

  def motivation_required?
    verification.suspected_fraud || verification_incorrect?
  end

  def verification_incorrect?
    [false, 0, '0', 'f', 'F', 'false', 'FALSE', 'off', 'OFF'].include?(@verification_correct)
  end
end
