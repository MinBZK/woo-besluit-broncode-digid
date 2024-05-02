
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

# model for registrations of aanvragen, heraanvragen, uitbreidingen en herstelpogingen
class Registration < ActiveRecord::Base
  module Status
    ABORTED   = "aborted" # afgebroken
    COMPLETED = "completed"
    INITIAL   = "initial" # initieel
    REQUESTED = "requested" # aangevraagd
  end

  include Stateful

  validates :burgerservicenummer, bsn_format: { allow_blank: true },
                                  format: { allow_blank: true, with: Regexp.only(CharacterClass::BSN_FORMAT) },
                                  presence: true

  validates :postcode, format: { allow_blank: true, with: Regexp.only(CharacterClass::POSTAL_CODE) }

  validates :geboortedatum_dag, numericality: { allow_blank: true, greater_than_or_equal_to: 0, less_than_or_equal_to: 31, only_integer: true, message: :invalid },
                                presence: true
  validates :geboortedatum_maand, numericality: { allow_blank: true, greater_than_or_equal_to: 0, less_than_or_equal_to: 12, only_integer: true, message: :invalid },
                                  presence: true
  validates :geboortedatum_jaar, numericality: { allow_blank: true, greater_than_or_equal_to: 1895, message: :invalid, only_integer: true, unless: -> { geboortedatum_jaar == "0000" } },
                                 presence: true
  validates :geboortedatum_jaar, numericality: { less_than_or_equal_to: ->(_) { Time.zone.today.year }, message: :invalid_future }
  validate(:birthdate_valid?)

  validates :huisnummer, format: { allow_blank: true, with: Regexp.only(CharacterClass::HOUSE_NUMBER) }
  validates :huisnummertoevoeging, format: { allow_blank: true, with: Regexp.only(CharacterClass::HOUSE_NUMBER_ADDITION) }

  after_validation :set_birthdate, unless: -> { self.errors.present? }
  after_validation :upcase_postcode_letters

  has_many :activation_letters, autosave: true, dependent: :destroy
  has_one :wid, autosave: true, dependent: :destroy
  belongs_to :nationality

  delegate :geldigheidsduur, to: :activation_letter

  scope :abandoned,   -> { where("updated_at < ?", (Configuration.get_int("session_keep_alive_retries") * Configuration.get_int("session_expires_in")).minutes.ago) }
  scope :expired,     -> { where("updated_at < ?", 6.weeks.ago) }
  scope :front_desk,  -> { where.not(baliecode: nil) }
  scope :not_me,      lambda { |registration_id|
    where("id != ?", registration_id)
  }
  scope :status_null, -> { where(status: nil) }

  attr_accessor :choose, :geboortedatum_dag, :geboortedatum_maand, :geboortedatum_jaar

  default_value_for :status, ::Registration::Status::INITIAL

  # retrieves a_nummer from the gba data - gives "" when no a-nummer
  def self.get_a_nummer(registration_id)
    letter = ActivationLetter.find_by(registration_id: registration_id)
    letter && letter.gba["010110"]
  end

  # check if an account exists for this sectoraalnummer which
  # has been created in the past day (configurable through "snelheid_aanvragen")
  # or for extensions (configurable through "snelheid_uitbreidingsaanvragen")
  def registration_too_soon?(snelheid_config, gba_status, wid_id = nil)
    aantal_dagen_configured = ::Configuration.get_int(snelheid_config)
    current_wid = Wid.find(wid_id) if wid_id.present?
    Registration.not_me(id).requested.where(burgerservicenummer: burgerservicenummer, gba_status: gba_status).where("created_at > ?", (Time.zone.now - aantal_dagen_configured.days)).each do |registration|
      if wid_id.blank? || (registration.wid.present? && registration.wid.sequence_no == current_wid.sequence_no && registration.wid.card_type == current_wid.card_type && registration.wid.action == "unblock")
        return true
      end
    end
    return false
  end

  def get_previous_registration(gba_status)
    registrations = Registration.not_me(id).requested
                                .where("burgerservicenummer = ?", burgerservicenummer)
                                .where("gba_status IN (?)", gba_status)

    registrations.last
  end

  # check if a certain number (configurable through "tijd_tussen_balie_aanvragen") of
  # registration exists for this sectoraalnummer which has been created in the past day.
  def registration_balie_too_soon?
    tijd_tussen_balie_aanvragen = ::Configuration.get_int("tijd_tussen_balie_aanvragen")
    registrations = Registration.not_me(id).requested
                                .where("burgerservicenummer = ?", burgerservicenummer)
                                .where("gba_status = 'valid' OR gba_status = 'emigrated' OR gba_status = 'rni'")
                                .where("created_at > ?", Time.zone.now - tijd_tussen_balie_aanvragen.minutes)
    registrations.count > 0
  end

  # check if a certain number (configurable through "aantal_balie_aanvragen_per_maand") of
  # registration exists for this sectoraalnummer which has been created in the past month.
  def registration_balie_too_soon_in_a_month?
    aantal_balie_aanvragen_per_maand = ::Configuration.get_int("aantal_balie_aanvragen_per_maand")
    registrations = last_months_balie_registrations
    registrations.count >= aantal_balie_aanvragen_per_maand
  end

  def registration_balie_too_soon_in_a_month_wait_time
    last_months_balie_registrations.order("id DESC").first
  end

  def last_months_balie_registrations
    Registration.not_me(id).requested
                .where("burgerservicenummer = ?", burgerservicenummer)
                .where("gba_status = 'valid' OR gba_status = 'emigrated' OR gba_status = 'rni'")
                .where("created_at > ?", 1.month.ago)
  end

  # check if GBA account has an a-nummer
  def a_number?
    Registration.get_a_nummer(id).blank? ? false : true
  end

  # check if a new registration is requested too often after another request
  # the duration is configured through parameter "blokkering_aanvragen"
  def registration_too_often?(blokkering_config, gba_status, wid_id = nil)
    counter = 0
    current_wid = Wid.find(wid_id) if wid_id.present?
    Registration.not_me(id).requested.where(burgerservicenummer: burgerservicenummer, gba_status: gba_status).where(created_at: Time.zone.now.beginning_of_month..Time.zone.now.end_of_month).each do |registration|
      if wid_id.blank? || (registration.wid.present? && registration.wid.sequence_no == current_wid.sequence_no && registration.wid.card_type == current_wid.card_type && registration.wid.action == "unblock")
        counter += 1
      end
    end
    limit = ::Configuration.get_int(blokkering_config)
    if limit > 0 && counter >= limit
      Time.zone.now.next_month.beginning_of_month
    else
      return false
    end
  end

  # call delayed_job for the gba request
  # request_type is either "registration" for normal requests through the website
  # or "webdienst" in case of request coming in through svb.nl
  # or "extension" in case of a sms uitbreiding
  def retrieve_brp!(request_type, web_registration_id = nil)
    BrpRegistrationJob.schedule(request_type: request_type, registration_id: id, web_registration_id: web_registration_id)
  end

  # return true is no leakage
  # block registration data (bsn,adres) and return false if something fishy is going on
  def no_gba_info_leakage?
    # retrieve the amount of tries in the last 15 minutes
    penalty = Rails.application.config.performance_mode ? [0] : max_registration_permutations
    if penalty.first > 0
      log_and_set_blocked_data(penalty)
      false
    else
      # not many tries recently, but check if it's still blocked
      gba_block = GbaBlock.find_by(blocked_data: burgerservicenummer) || GbaBlock.find_by(blocked_data: postcode + huisnummer)
      if gba_block && gba_block.blocked_till > Time.zone.now
        errors.add(:leakage, I18n.t("brp_messages.blocked", until: I18n.l(gba_block.blocked_till, format: :date_comma_time_text_tzone_in_brackets)))
        false
      else
        true
      end
    end
  end

  def log_and_set_blocked_data(penalty)
    blocked_till = Time.zone.now + [((3 + penalty.first)**2 * Configuration.get_int("gba_guard_delay_factor")).minutes, Configuration.get_int("gba_guard_delay_max").hours].min
    # too many recent tries, exponentially create new wait time and store (or update) in GbaBlock table

    if penalty.second.eql?("bsn")
      blocked_data = burgerservicenummer
      Log.instrument("18", date: I18n.l(blocked_till, format: :sms))
    else
      blocked_data = postcode + huisnummer
      Log.instrument("19", date_of_birth: geboortedatum, postcode: postcode, house_number: huisnummer, date: I18n.l(blocked_till, format: :sms))
    end
    GbaBlock.create_or_update_blocked_till(blocked_data, blocked_till)

    errors.add(:leakage, I18n.t("brp_messages.blocked", until: I18n.l(blocked_till, format: :date_comma_time_text_tzone_in_brackets)))
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
    gba_guard_adres_max = Configuration.get_int("gba_guard_adres_max")
    adres_attempts = Registration.where(postcode: postcode,
                                        huisnummer: huisnummer,
                                        gba_status: %w(request not_found invalid birthday_incorrect))
                                 .where("created_at > ?", since).count

    [[(bsn_attempts - gba_guard_bsn_max), "bsn"], [(adres_attempts - gba_guard_adres_max), "adres"]].max
  end

  # returns the "activatie brief" associated with this registration
  def activation_letter
    activation_letters.find_by(letter_type: %w(uitbreiding_app activation_aanvraag_met_sms uitbreiding_sms
                                               recovery_sms balie_aanvraag aanvraag_deblokkeringscode_eid activation_app_one_device
                                               app_notification_letter activeringscode_aanvraag_via_digid_app))
  end

  # set all attached letters of this registration to "finished"
  def finish_letters(type_account)
    activation_letters.update_all(status: ::ActivationLetter::Status::FINISHED, letter_type: type_account)
  end

  def update_letters_to_finished_and_expiration(geldigheidsduur, type_account)
    activation_letters.update_all(status:           ::ActivationLetter::Status::FINISHED,
                                  letter_type:      type_account,
                                  geldigheidsduur:  geldigheidsduur
                                 )
  end

  # set all attached letters of this registration to "sent"
  def sent_letters(type_account)
    activation_letters.update_all(status: ::ActivationLetter::Status::SENT, letter_type: type_account)
  end

  # return activationcode(s)
  def activation_codes
    activation_letter.controle_code
  end

  # returns true if person either is not in GBA or has status emigrated
  def status_not_found_or_emigrated_or_rni_or_ministerial_decree?
    %w(rni emigrated ministerial_decree not_found invalid).include?(gba_status)
  end

  # returns true if person has status emigrated
  def status_emigrated_or_rni_or_ministerial_decree?
    %w(rni emigrated ministerial_decree).include?(gba_status)
  end

  # create a webdienst aanvraag so we can ask gba
  def self.create_fake_aanvraag(sectoraal_nummer)
    registration = Registration.new do |reg|
      reg.burgerservicenummer = sectoraal_nummer
      reg.gba_status = "request"
      reg.status = ::Registration::Status::INITIAL
    end
    registration.save(validate: false)
    registration
  end

  # creates a letter
  def create_letter(content, activeringstermijn = nil, activation_method = nil, letter_type = ActivationLetter::LetterType::CREATE)
    code = VerificationCode.generate("A")

    # if activeringstermijn is submitted (only in webdienst cases)
    # we use that, otherwise take it from config
    geldigheid_brief = if activeringstermijn.blank?
                         ::Configuration.get_int("geldigheid_brief")
                       else
                         activeringstermijn
                       end

    activation_letters.create do |letter|
      letter.gba             = content.to_json.to_s
      letter.status          = ::ActivationLetter::Status::CREATED
      letter.letter_type     = letter_type
      letter.geldigheidsduur = geldigheid_brief.to_i
      letter.controle_code   = code
    end
  end

  def balie_request?
    baliecode.present?
  end

  def geboortedatum_dag
    (@geboortedatum_dag if @geboortedatum_dag.present?) || (geboortedatum[6..-1] if geboortedatum)
  end

  def geboortedatum_maand
    (@geboortedatum_maand if @geboortedatum_maand.present?) || (geboortedatum[4, 2]  if geboortedatum)
  end

  def geboortedatum_jaar
    (@geboortedatum_jaar if @geboortedatum_jaar.present?) || (geboortedatum[0, 4]  if geboortedatum)
  end

  def expired?
    updated_at < 6.weeks.ago
  end

  def baliecode_valid?
    (created_at + ::Configuration.get_int("balie_default_geldigheidsduur_baliecode").days) > Time.zone.now
  end

  def self.application_too_soon?(bsn:)
    no_of_days_between_account_applications = ::Configuration.get_int("snelheid_aanvragen")
    return Registration.where(burgerservicenummer: bsn, status: Registration::Status::REQUESTED).where("created_at > ?", (Time.zone.now - no_of_days_between_account_applications.days)).exists?
  end

  def self.too_many_applications_this_month?(bsn:)
    max_amount_of_account_application_per_month = ::Configuration.get_int("blokkering_aanvragen")
    this_month = Time.zone.now.beginning_of_month..Time.zone.now.end_of_month
    return max_amount_of_account_application_per_month <= Registration.where(burgerservicenummer: bsn, status: Registration::Status::REQUESTED).where(created_at: this_month).count
  end

  private #----------------------------------------------------------------------

  # checks if birthdate is valid (and not in future)
  def birthdate_valid?
    # only check birthdate as a whole if no errors occured on the individual fields
    return if errors[:geboortedatum_dag].present? || errors[:geboortedatum_maand].present? || errors[:geboortedatum_jaar].present?

    day = geboortedatum_dag.to_i
    month = geboortedatum_maand.to_i
    year = geboortedatum_jaar.to_i

    # only allow (D)DMMYYYY, (0)0(M)MYYYY, (0)0(0)0YYYY, (0)0(0)00000
    if (month.zero? && day.nonzero?) || (year.zero? && month.nonzero?)
      errors.add(:geboortedatum, :invalid)
    else
      # use valid (fake) values for cases where day, month and/or year is zero
      begin
        date = Date.new(year.zero? ? 2000 : year, month.zero? ? 1 : month, day.zero? ? 1 : day)
        errors.add(:geboortedatum, :invalid_future) if date > Time.zone.today
      rescue
        errors.add(:geboortedatum, :invalid)
      end
    end
  end

  def set_birthdate
    self.geboortedatum = geboortedatum_jaar + geboortedatum_maand.rjust(2, "0") + geboortedatum_dag.rjust(2, "0")
  end

  # make all postcode letters uppercase
  def upcase_postcode_letters
    postcode.upcase! unless postcode.blank?
  end
end
