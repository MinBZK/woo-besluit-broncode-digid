
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

class Verification < ActiveRecord::Base
  establish_connection(:"#{Rails.env}_balie")
  module State
    COMPLETED = 'completed'
    INITIAL   = 'initial'
    REJECTED  = 'rejected'
    VERIFIED  = 'verified'
    # HANDLED   = 'handled'
  end


  belongs_to :user
  has_one :audit, dependent: :destroy
  has_one :verification_correction, dependent: :destroy

  scope :unaudited, lambda {
    joins('LEFT OUTER JOIN audits ON audits.verification_id = verifications.id')
      .where('(audits.verification_id IS NULL AND verifications.state = ?)',
             Verification::State::VERIFIED)
      .includes(:user, :audit, :verification_correction)
  }

  scope :pending_fraud_suspicion, lambda {
    joins('LEFT OUTER JOIN audits ON audits.verification_id = verifications.id')
      .where('(audits.verification_id IS NULL AND verifications.state = ? AND verifications.suspected_fraud = 1) OR (audits.state = ?) AND (audits.created_at < ?)',
             Verification::State::VERIFIED, Audit::State::INITIAL, 1.business_day.ago)
      .includes(:user, :audit, :verification_correction)
  }

  # include Stateful

  # default_value_for :state, State::INITIAL

  # belongs_to :front_desk
  # belongs_to :user
  # has_one :audit, dependent: :destroy
  # has_one :verification_correction, dependent: :destroy

  # scope :activated, -> { where(state: [Verification::State::COMPLETED, Verification::State::VERIFIED]) }
  # scope :with_correction, -> { joins(:verification_correction).where.not(verification_corrections: { verification_id: nil }) }

  # validates :front_desk_code, uniqueness: { conditions: -> { where.not(state: State::INITIAL) } }

  # accepts_nested_attributes_for :audit, reject_if: :all_blank
end
