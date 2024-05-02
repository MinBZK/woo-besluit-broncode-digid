
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

class VerificationCorrectionForm
  include ActiveModel::Model
  include Concerns::DelegationOnBlank

  attr_accessor :id_expires_at_day, :id_expires_at_month, :id_expires_at_year, :motivation, :verification, :nationality
  attr_writer :id_number

  delegate :verification_correction, to: :verification
  delegate_on_blank :motivation, to: :verification_correction

  validates :id_number, presence: true, id_number_format: { allow_blank: true }
  validates_presence_of :motivation, :id_expires_at_day, :id_expires_at_month, :id_expires_at_year
  validates_numericality_of :id_expires_at_day, allow_blank: true,
                                                greater_than_or_equal_to: 1,
                                                less_than_or_equal_to: 31,
                                                only_integer: true,
                                                message: :invalid
  validates_numericality_of :id_expires_at_month, allow_blank: true,
                                                  greater_than_or_equal_to: 1,
                                                  less_than_or_equal_to: 12,
                                                  only_integer: true,
                                                  message: :invalid
  validates_numericality_of :id_expires_at_year, allow_blank: true,
                                                 greater_than_or_equal_to: Time.zone.today.year,
                                                 less_than_or_equal_to: Time.zone.today.year + 10,
                                                 only_integer: true,
                                                 message: :invalid
  validate :changes_present?
  validate :expires_at_valid?

  def save
    return unless valid?

    if verification_correction.present?
      verification_correction.update(motivation: motivation)
    else
      verification.create_verification_correction(original_id_number: verification.id_number,
                                                  original_id_expires_at: verification.id_expires_at,
                                                  motivation: motivation)
    end

    if verification.nationality.match?(CharacterClass::DUTCH)
      @id_number = @id_number.upcase
    end

    verification.update(id_expires_at: id_expires_at, id_number: id_number)
  end

  def id_expires_at_day
    @id_expires_at_day || verification.id_expires_at.try(:day)
  end

  def id_expires_at_month
    @id_expires_at_month || verification.id_expires_at.try(:month)
  end

  def id_expires_at_year
    @id_expires_at_year || verification.id_expires_at.try(:year)
  end

  def id_number
    @id_number || verification.id_number
  end

  def nationality
    @nationality || verification.nationality
  end

  private

  def changes_present?
    return unless id_number.eql?(original_id_number) && id_expires_at.eql?(original_id_expires_at)
    errors.add(:base, :no_changes)
  end

  def expires_at_valid?
    return if errors[:id_expires_at_year].present? || errors[:id_expires_at_month].present? || errors[:id_expires_at_day].present?

    if id_expires_at < Time.zone.today
      errors.add(:id_expires_at, :invalid_future)
    elsif id_expires_at > Time.zone.today + 10.years
      errors.add(:id_expires_at, :invalid_past)
    elsif !Date.valid_date?(*_id_expires_at)
      errors.add(:id_expires_at, :invalid)
    end
  end

  def original_id_number
    verification.id_number
  end

  def original_id_expires_at
    verification.id_expires_at
  end

  def id_expires_at
    if Date.valid_date?(*_id_expires_at)
      Date.new(*_id_expires_at)
    else
      verification.id_expires_at
    end
  end

  def _id_expires_at
    [id_expires_at_year.to_i, id_expires_at_month.to_i, id_expires_at_day.to_i]
  end
end
