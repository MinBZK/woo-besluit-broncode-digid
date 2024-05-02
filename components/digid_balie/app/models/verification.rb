
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

class Verification < ActiveRecord::Base
  module State
    COMPLETED = 'completed'.freeze
    INITIAL   = 'initial'.freeze
    REJECTED  = 'rejected'.freeze
    VERIFIED  = 'verified'.freeze
  end

  attr_accessor :expires_in

  include Stateful

  after_initialize { self.state = State::INITIAL if state.blank? }

  belongs_to :front_desk
  belongs_to :user
  has_one :audit, dependent: :destroy
  has_one :verification_correction, dependent: :destroy

  scope :activated, -> { where(state: [Verification::State::COMPLETED, Verification::State::VERIFIED]) }
  scope :with_correction, -> { joins(:verification_correction).where.not(verification_corrections: { verification_id: nil }) }
  scope :today, -> { where(activated_at: Time.zone.today.at_midnight..Time.zone.today.at_end_of_day) }
  scope :last_7_days, -> { where('activated_at >= :time', time: 7.days.ago.at_midnight) }
  scope :recent, -> { completed.where('updated_at >= :time', time: 24.hours.ago) }
  scope :unaudited, lambda {
    joins('LEFT OUTER JOIN audits ON audits.verification_id = verifications.id')
      .where('(audits.verification_id IS NULL AND verifications.state = ?)',
             Verification::State::VERIFIED)
      .includes(:user, :audit, :verification_correction)
  }
  scope :fraud_suspicion, lambda {
    joins('LEFT OUTER JOIN audits ON audits.verification_id = verifications.id')
      .where('(audits.verification_id IS NULL AND verifications.state = ? AND verifications.suspected_fraud = 1) OR (audits.state = ?)',
             Verification::State::VERIFIED, Audit::State::INITIAL)
      .includes(:user, :audit, :verification_correction)
  }
  scope :front_desk_code_expired, -> { where('front_desk_code_expires_at <= ?', Time.zone.today + 1.day) }

  validates :front_desk_code, uniqueness: { conditions: -> { where.not(state: State::INITIAL) }, case_sensitive: true }

  accepts_nested_attributes_for :audit, reject_if: :all_blank

  def suspected_fraud_after_audit?
    audit.present? && !audit.verification_correct?
  end

  def sms_gateway gateway_type
    gateways = APP_CONFIG['sms_gateway'][gateway_type].split(',')
    key = "sms_gateways_#{gateway_type}"
    $redis.lpop(key) || ($redis.rpush(key, gateways * 5) && $redis.lpop(key))
  end
end
