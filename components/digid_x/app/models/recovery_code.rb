
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

# TODO: Split this model into a model that holds the recovery code in the database and a model that validates recovery codes when a user inputs them
class RecoveryCode < ActiveRecord::Base
  before_validation :find_code

  validates :herstelcode, presence: true
  validates :herstelcode, format: { with: /\AH\w*\z/i }, if: -> { herstelcode.present? }
  validate :valid_recovery_code?

  after_validation :count_attempt

  belongs_to :account

  scope :by_letter, -> { where(via_brief: true) }
  scope :by_email, -> { where(via_brief: false) }

  scope :expired, lambda {
    where("geldigheidstermijn IS NOT NULL")
      .where("created_at < DATE_SUB(?, INTERVAL geldigheidstermijn SECOND)", Time.zone.now)
  }
  scope :not_expired, lambda {
    where("geldigheidstermijn IS NOT NULL")
      .where("created_at > DATE_SUB(?, INTERVAL geldigheidstermijn SECOND)", Time.zone.now)
  }
  scope :not_used,    -> { where(used: false) }
  scope :last_month,  -> { where("created_at > ?", 1.month.ago) }

  def self.for_account(id)
    where(account_id: id)
  end

  def find_code
    @recovery_code = RecoveryCode.where(herstelcode: herstelcode)
                                 .where(account_id: account_id)
                                 .find_by(used: false)
  end

  def count_attempt
    return unless errors.messages.values.include?([I18n.t("activerecord.errors.models.recovery_code.attributes.herstelcode.invalid")])

    register_attempt if herstelcode.present?
  end

  def valid_recovery_code?
    return unless @recovery_code.blank? && herstelcode.present? && herstelcode.match?(/\AH\w*\z/i)

    register_attempt if herstelcode.present?
    errors.add(:herstelcode, :faulty)
  end

  def expired?
    Time.zone.now.to_i > (@recovery_code.created_at.to_i + @recovery_code.geldigheidstermijn.to_i)
  end

  def create_recovery_code(code_data)
    transaction do
      remove_recovery_codes(code_data[:account_id], code_data[:recovery_method])
      code_data[:code] = VerificationCode.generate("H") if code_data[:code].blank?
      RecoveryCode.new do |r|
        r.account_id = code_data[:account_id]
        r.used = false
        r.herstelcode = code_data[:code]
        r.geldigheidstermijn = expiry_date(code_data)
        r.bearer = code_data[:recovery_method]
        r.via_brief = code_data[:via_brief]
        r.save(validate: false)
      end
    end
  end

  # removes previous codes for this account and recovery_method,
  # when recovery_method is not passed, all codes for this account will be removed
  def remove_recovery_codes(account_id, recovery_method = nil)
    if recovery_method
      RecoveryCode.where(["account_id = ? AND bearer = ?", account_id, recovery_method]).destroy_all
    else
      RecoveryCode.where(["account_id = ?", account_id]).destroy_all
    end
  end

  def send_new_recovery_code(account, recovery_data)
    data = {}
    data["herstelcode"] = recovery_data["herstelcode"]
    data["expiry_date_time"] = expiry_date_time(recovery_data["created_at"], recovery_data["geldigheidstermijn"])
    AanvraagHerstelcodeMailer.delay(queue: "email").aanvraag_herstelcode(account_id: account.id, recipient: account.adres, payload: data)
  end

  def expiry_date(code_data)
    account = Account.find_by(id: code_data[:account_id])
    if account.wachtwoordherstel_allowed_with_sms? && !code_data[:send_letter_anyway]
      expiry_date = ::Configuration.get_int("geldigheidstermijn_herstel_email")
      expiry_date.minutes.to_i
    elsif code_data[:send_letter_anyway]
      expiry_date = ::Configuration.get_int("geldigheidstermijn_herstel_brief")
      expiry_date.days.to_i
    end
  end

  def expiry_date_time(date, expiry_time)
    expiry_date_seconds = date.to_i + expiry_time.to_i
    I18n.l(Time.zone.at(expiry_date_seconds), format: :medium)
  end

  def register_attempt
    account = Account.find_by(id: account_id)
    account.recovery_code_attempts << Attempt.new(attempt_type: "recover")
  end

  def to_used!
    update_attribute(:used, true)
  end

  def code_id
    @recovery_code.id
  end

  def check_sms?
    !@recovery_code.via_brief
  end
end
