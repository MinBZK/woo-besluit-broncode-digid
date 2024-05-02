
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

class Smscode
  include ActiveModel::Model
  include ActiveModel::Validations::Callbacks

  FORMAT = Regexp.new(/\A\d{6}\z/)
  ALLOWED_CHARACTERS = ["0", "1", "2", "3", "4", "5", "6", "7", "8", "9"]

  attr_accessor :smscode, :account, :session_type, :new_number, :webservice, :spoken

  validate :sms_challenge_exists?
  validate :sms_challenge_pending?, if: :sms_challenge_exists?
  validate :sms_expired_or_already_used?, if: :sms_challenge
  validates :smscode, presence: true, unless: :skip_smscode_correct_validation?
  validate :smscode_correct?, unless: :skip_smscode_correct_validation?
  after_validation :count_attempt, unless: :skip_count_attempt?

  def use!
    valid? && sms_challenge.present? && sms_challenge.update_attribute(:status, ::SmsChallenge::Status::CORRECT)
  rescue ActiveRecord::StaleObjectError
    Log.instrument("778", account_id: account.id, hidden: true)
    Log.instrument("73", account_id: account.id)
    false
  end

  private

  def sms_challenge_exists?
    # If we marked a challenge as invalid it shouldn't be found anymore
    return true if (sms_challenge.present? && sms_challenge.status != ::SmsChallenge::Status::INVALID)
    unless errors.has_key?(:smscode)
      errors.add(:smscode, :not_send)
      Log.instrument("902", hidden: true)
    end
    false
  end

  def sms_challenge_pending?
    case sms_challenge.state
    when ::SmsChallenge::Status::PENDING
      return true
    when ::SmsChallenge::Status::CORRECT
      errors.add(:smscode, :expired)
    when ::SmsChallenge::Status::INCORRECT
      errors.add(:smscode, :incorrect)
    when ::SmsChallenge::Status::INVALID
      errors.add(:smscode, :invalid)
    end
    return false
  end


  def sms_challenge
    @sms_challenge ||= account.sms_challenge(action: session_type, webservice: webservice, spoken: spoken || false)
  end

  def sms_expired_or_already_used?
    return unless sms_challenge.expired? || sms_challenge.state.correct?

    Log.instrument("778", account_id: account.id, hidden: true)
    Log.instrument("73", account_id: account.id)
    errors.add(:smscode, :expired)
  end

  def smscode_correct?
    return true if errors.added?(:smscode, :invalid, value: self.smscode)

    used_sms_characters = smscode.split(//)

    if sms_challenged_correctly?
      return true
    elsif used_sms_characters.any? { |x| ALLOWED_CHARACTERS.exclude?(x) }  # filled in sms code contains a character that is not allowed
      Log.instrument("72", account_id: account.id)
      errors.add(:smscode, I18n.t("activemodel.errors.models.smscode.attributes.smscode.consists_of_six"))

      spoken_sms = sms_challenge.spoken
      sms_code_length = 6 # amount of fields required for an sms code

      sms_code_length.times do |i|
        if used_sms_characters[i] !~ /\d/ && used_sms_characters[i].present? # not number and not empty
          errors.add(:smscode, I18n.t("activemodel.errors.models.smscode.attributes.smscode.incorrect_field",
                                      wrong_character: used_sms_characters[i],
                                      field_number: i + 1))
        end
      end
      errors.add(:smscode, I18n.t("activemodel.errors.models.smscode.attributes.smscode.try_again",
                                  spoken_or_sent: spoken_sms ? I18n.t("sms_spoken") : I18n.t("sms_sent")))
    elsif !smscode.match?(FORMAT) # not 6 numbers
      errors.add(:smscode, :invalid)
    else
      Log.instrument("72", account_id: account.id)
      errors.add(:smscode, :incorrect)
    end
  end

  def skip_smscode_correct_validation?
    sms_challenge.nil? || errors.added?(:smscode, :expired)
  end

  def skip_count_attempt?
    sms_challenge.nil? || sms_challenged_correctly? || smscode.blank? || smscode !~ FORMAT
  end

  def sms_challenged_correctly?
    smscode_matches? && session_type_matches? && mobile_numbers_match? && correct_webservice?
  end

  def correct_webservice?
    sms_challenge.webservice.blank? || webservice == sms_challenge.webservice
  end

  def session_type_matches?
    sms_challenge.action == session_type
  end

  def smscode_matches?
    DigidUtils::Crypto.eql_time_cmp(smscode, sms_challenge.code)
  end

  def mobile_numbers_match?
    return true if new_number.nil?
    new_number == sms_challenge.mobile_number
  end

  def count_attempt
    sms_challenge.increment!(:attempt)
    account.blocking_manager.register_failed_attempt! if %w(sign_in activation).include?(session_type)
  end
end
