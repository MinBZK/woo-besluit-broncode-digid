
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

class RegistrationBalie < Registration
  # inherits validations of Registration

  # make sure Balie Registrations are stored in the normal Registration table
  def self.table_name
    "registrations"
  end

  attr_accessor :valid_until_day, :valid_until_month, :valid_until_year

  validates :valid_until_day,
            numericality: { allow_blank: false, greater_than_or_equal_to: 1, less_than_or_equal_to: 31, only_integer: true, message: :invalid, if: -> { valid_until_day.present? } },
            presence: true, unless: -> { Configuration.get_boolean("balie_aanvraag_voor_rni") }
  validates :valid_until_month,
            numericality: { allow_blank: false, greater_than_or_equal_to: 1, less_than_or_equal_to: 12, only_integer: true, message: :invalid, if: -> { valid_until_month.present? } },
            presence: true, unless: -> { Configuration.get_boolean("balie_aanvraag_voor_rni") }
  validates :valid_until_year,
            numericality: { allow_blank: false, greater_than_or_equal_to: 1895, less_than_or_equal_to: ->(_) { 20.years.from_now.year }, message: :invalid, only_integer: true, if: -> { valid_until_year.present? } },
            presence: true, unless: -> { Configuration.get_boolean("balie_aanvraag_voor_rni") }

  validate :valid_until_is_valid_date,    unless: :errors_present_on_components_of_valid_until?
  validate :valid_until_date_not_in_past, unless: :errors_present_on_components_of_valid_until?,
                                          if:     :valid_until_is_valid_date?

  validates :id_number, presence: true, id_number_format: { allow_blank: true }, unless: -> { Configuration.get_boolean("balie_aanvraag_voor_rni") }

  after_validation :set_valid_until, unless: -> { self.errors.present? }

  # block registration data (bsn) and return false if something fishy is going on
  def no_gba_info_leakage?
    # retrieve the amount of tries in the last 15 minutes
    penalty = Rails.application.config.performance_mode ? [0] : max_registration_permutations
    if penalty.first > 0
      log_and_set_blocked_data(penalty)
      false
    else
      # not many tries recently, but check if it's still blocked
      gba_block = GbaBlock.find_by(blocked_data: burgerservicenummer)
      if gba_block && gba_block.blocked_till > Time.zone.now
        errors.add(:leakage, I18n.t("brp_messages.blocked", until: I18n.l(gba_block.blocked_till, format: :date_comma_time_text_tzone_in_brackets)))
        false
      else
        true
      end
    end
  end

  # returns an item from an array of 2 items: [x,"bsn"] and [y,"adres"],
  # x and y are the number of form submits with the same data MINUS the max allowed.
  # item with the largest x or y is returned.
  def max_registration_permutations
    since = 15.minutes.ago
    gba_guard_bsn_max = Configuration.get_int("gba_guard_bsn_max")
    bsn_attempts = Registration.where(burgerservicenummer: burgerservicenummer,
                                      gba_status: %w(request not_found invalid birthday_incorrect))
                               .where("created_at > ?", since).count

    gba_guard_birthdate_and_id_max = Configuration.get_int("gba_guard_birthdate_and_id_max")
    birthdate_and_id_attempts = Registration.where(geboortedatum: geboortedatum, id_number: id_number,
                                                   gba_status: %w(request not_found invalid birthday_incorrect))
                                            .where("created_at > ?", since).count

    [[(bsn_attempts - gba_guard_bsn_max), "bsn"], [(birthdate_and_id_attempts - gba_guard_birthdate_and_id_max), "birthdate_and_id"]].max
  end

  def log_and_set_blocked_data(penalty)
    blocked_till = Time.zone.now + [((3 + penalty.first)**2 * Configuration.get_int("gba_guard_delay_factor")).minutes, Configuration.get_int("gba_guard_delay_max").hours].min
    # too many recent tries, exponentially create new wait time and store (or update) in GbaBlock table

    if penalty.second.eql?("bsn")
      blocked_data = burgerservicenummer
      Log.instrument("18", date: I18n.l(blocked_till, format: :sms))
    else
      blocked_data = geboortedatum + nationality_id.to_s + id_number
      Log.instrument("396", id_number: id_number, nationality_id: nationality_id.to_s, date_of_birth: geboortedatum, date: I18n.l(blocked_till, format: :sms))
    end
    GbaBlock.create_or_update_blocked_till(blocked_data, blocked_till)
    errors.add(:leakage, I18n.t("brp_messages.blocked", until: I18n.l(blocked_till, format: :date_comma_time_text_tzone_in_brackets)))
  end

  def id_number=(new_value)
    return false if new_value.nil?  
    new_value = new_value.upcase  if self.attributes["nationality_id"] == Nationality.dutch_id
    self[:id_number] = new_value.strip
  end

  def self.create_letter(id, content)
    registration = Registration.find(id)
    code = VerificationCode.generate("A")

    registration.activation_letters.create do |letter|
      letter.gba             = content.to_json.to_s.gsub("null", '""')
      letter.status          = ::ActivationLetter::Status::CREATED
      letter.letter_type     = ActivationLetter::LetterType::CREATE
      letter.geldigheidsduur = Configuration.get_int("balie_default_geldigheidsduur_baliecode")
      letter.controle_code   = code
    end
  end

  def valid_until_day
    @valid_until_day || valid_until.try(:day)
  end

  def valid_until_month
    @valid_until_month || valid_until.try(:month)
  end

  def valid_until_year
    @valid_until_year || valid_until.try(:year)
  end

  private

  def valid_until_is_valid_date
    errors.add :valid_until, :invalid unless valid_until_is_valid_date? || Configuration.get_boolean("balie_aanvraag_voor_rni")
  end

  def valid_until_is_valid_date?
    calculated_valid_until.present?
  end

  def valid_until_date_not_in_past
    errors.add :valid_until, :invalid_past if Time.zone.today >= calculated_valid_until
  end

  def set_valid_until
    self.valid_until = calculated_valid_until
  end

  def calculated_valid_until
    Date.civil(valid_until_year.to_i, valid_until_month.to_i, valid_until_day.to_i)
  rescue ArgumentError
    nil
  end

  def errors_present_on_components_of_valid_until?
    (errors[:valid_until_day] + errors[:valid_until_month] + errors[:valid_until_year]).present?
  end
end
