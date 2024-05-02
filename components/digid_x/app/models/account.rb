
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

require "digest/sha1"
class Account < ActiveRecord::Base
  include HasBlockingManager
  include RdwClient
  include RvigClient
  include DglClient
  def time_to_be_blocked_in_seconds
    ::Configuration.get_int("wachtwoord_sms_geblokkeerd").minutes
  end

  module LoginLevel
    PASSWORD      = 10
    TWO_FACTOR    = 20
    SMARTCARD     = 25
    SMARTCARDPKI  = 30
    DEFAULT       = nil
    ALL           = [PASSWORD, TWO_FACTOR, SMARTCARD, SMARTCARDPKI, DEFAULT].freeze
  end

  module Status
    INITIAL   = "initial"   # Initieel
    REQUESTED = "requested" # Aangevraagd
    ACTIVE    = "active"    # Actief
    REVOKED   = "revoked"   # Gerevoceerd door bemiddelende instantie (bijv. SVB)
    SUSPENDED = "suspended" # Opgeschort
    EXPIRED   = "expired"   # Vervallen
    REMOVED   = "removed"   # Opgeheven
  end

  module ReasonSuspension
    DECEASED = "O"
  end

  include Stateful

  TYPES = %w[basis midden].freeze
  LEVELS = { 10 => "basis", 20 => "midden", 25 => "substantieel", 30 => "hoog" }.freeze

  attr_accessor :sector_code # , :current_password, :current_emailadres, :password

  default_value_for :status, ::Account::Status::INITIAL

  has_one :distribution_entity,         dependent: :destroy
  has_one :email,                       dependent: :destroy
  has_one :password_authenticator,      dependent: :destroy, class_name: "Authenticators::Password"
  has_many :sms_tools,                  dependent: :destroy, class_name: "Authenticators::SmsTool"
  has_many :app_authenticators,         dependent: :destroy, class_name: "Authenticators::AppAuthenticator"

  has_many :account_histories
  has_many :email_deliveries,           dependent: :destroy, autosave: true
  has_many :recovery_codes,             dependent: :destroy
  has_many :sectorcodes,                dependent: :destroy
  has_many :sectorcodes_history # Never destroy for logging purposes
  has_many :sent_emails,                dependent: :destroy
  has_many :sms_challenges,             dependent: :destroy, autosave: true

  has_many :recovery_code_attempts,
           -> { where attempt_type: "recover" },
           class_name: "Attempt",
           as: :attemptable,
           dependent: :destroy

  has_many :username_registration_attempts,
           -> { where attempt_type: "username_registration" },
           class_name: "Attempt",
           as: :attemptable,
           dependent: :destroy

  delegate :adres, to: :email
  delegate :status, to: :email, prefix: true

  accepts_nested_attributes_for :sms_tools, reject_if: :phone_number_not_required?
  accepts_nested_attributes_for :email, reject_if: proc { |attributes| !attributes.find_all { |k, _v| k != "no_email" }.find { |_k, v| v.present? } || attributes["no_email"] == "1" }
  accepts_nested_attributes_for :password_authenticator, reject_if: :all_blank
  accepts_nested_attributes_for :distribution_entity, reject_if: :all_blank

  scope :expired_activation, ->() { where("accounts.status = ? AND accounts.created_at < ?", ::Account::Status::REQUESTED, Time.zone.now - 42.days) }

  # Due to the lower precedence of do..end we need to wrap the lambda in
  # parentheses. The Ruby Style guide specifies multi-line lambda's should
  # use do..end blocks.
  #
  # http://stackoverflow.com/questions/1476678/rails-named-scope-lambda-and-blocks
  # https://github.com/PPPPPPP/ruby-style-guide#lambda-multi-line
  scope :expired_password_tool, (lambda do
    joins(:password_authenticator)
      .where("password_tools.status = ?", Authenticators::Password::Status::PENDING)
  end)

  scope :expired_app, (lambda do
    joins(:app_authenticators)
      .where("app_authenticators.status = ?", Authenticators::Password::Status::PENDING)
  end)

  scope :with_bsn, (lambda do |bsn|
    joins(:sectorcodes)
      .where(sectorcodes: { sectoraalnummer: bsn, sector_id: Sector.get("bsn") })
  end)

  scope :with_anummer, (lambda do |anummer|
    joins(:sectorcodes)
      .where(sectorcodes: { sectoraalnummer: anummer, sector_id: Sector.get("a-nummer") })
  end)

  scope :with_sofi, (lambda do |sofi|
    joins(:sectorcodes)
      .where(sectorcodes: { sectoraalnummer: sofi, sector_id: Sector.get("sofi") })
  end)

  scope :with_oeb, (lambda do |oeb|
    joins(:sectorcodes)
      .where(sectorcodes: { sectoraalnummer: oeb, sector_id: Sector.get("oeb") })
  end)

  scope :stale, (lambda do
    timestamp = (2 * Configuration.get_int("session_expires_in")).minutes.ago
    where("updated_at < ?", timestamp)
  end)

  scope :not_me, ->(account_id) { where("id != ?", account_id) }

  scope :deceased, ->() { where("reason_suspension = ? AND reason_suspension_updated_at < ?", ReasonSuspension::DECEASED, ::Configuration.get_int("clean_up_accounts_with_reason_suspension_o").months.ago) }

  # Destroy / delete callbacks
  before_destroy :register_sectorcodes_history, prepend: true
  before_destroy :remove_afnemersindicatie, prepend: true
  before_destroy :create_account_history, prepend: true

  # Built-in validations
  validates :locale, inclusion: { in: %w[nl en] }, allow_blank: true
  validates :zekerheidsniveau, inclusion: { in: LoginLevel::ALL }
  validates_associated :email # used for password changed to give error on empty email.

  %w[a_nummer bsn oeb sofi].each do |sector_name|
    define_method(sector_name) do
      instance_variable_get("@#{sector_name}") || instance_variable_set(
        "@#{sector_name}",
        sectorcodes
          .joins(:sector)
          .find_by("sectors.name LIKE ?", sector_name.tr("_", "-"))
          .try(:sectoraalnummer)
      )
    end
  end

  def unblock_letter_requested?(bsn:, sequence_no:, card_type:)
    Registration.where(burgerservicenummer: bsn, gba_status: "valid_unblock_letter", status: "requested").each do |registration|
      if registration.wid.present? && registration.wid.sequence_no == sequence_no && registration.wid.card_type == card_type && registration.wid.action == "unblock" && !registration.wid.unblock_code.nil?
        return true
      end
    end
    false
  end

  def register_sectorcodes_history
    return if SectorcodesHistory.exists?(account_id: id)

    sectorcodes = Sectorcode.select(:sector_id, :sectoraalnummer).where(account_id: id)
    sectorcodes.each do |sectorcode|
      SectorcodesHistory.create(account_id: id, sector_id: sectorcode.sector_id, sectoraalnummer: sectorcode.sectoraalnummer)
    end
  end

  def remove_afnemersindicatie
    other_bsn_accounts = Account.with_bsn(bsn).where.not(id: id)
    if other_bsn_accounts.count == 0 && a_nummer
      response = dgl_client.delete("/iapi/afnemersindicatie/" + a_nummer)
      if response.code == 200
        Log.instrument("1496", account_id: id, hidden: true)
      else
        Log.instrument("1495", account_id: id, hidden: true)
      end
    end
  end

  def create_account_history
    AccountHistory.create account_id: other_accounts.pluck(:id).first || id, gebruikersnaam: password_authenticator&.username,
      mobiel_nummer: phone_number, email_adres: email.try(:adres)
  end

  def expires_at
    expires_in = sectorcodes.map(&:valid_for).compact.max
    expires_in.months.from_now if expires_in
  end

  def recovery_attempts
    recovery_code_attempts.count
  end

  def reason_suspension=(reason_suspension)
    self[:reason_suspension] = reason_suspension
  end

  def deceased?
    reason_suspension_updated_at.present? && reason_suspension == ReasonSuspension::DECEASED
  end

  def delete_recovery_code_attempts
    recovery_code_attempts.destroy_all
  end

  def too_many_recover_attempts?(allowed_attempts)
    recovery_attempts >= allowed_attempts
  end

  def register_authentication
    # TODO: Why clone it?
    self.last_sign_in_at = current_sign_in_at.clone if current_sign_in_at
    self.current_sign_in_at = Time.zone.now
    save(validate: false)
  end

  # creates an initial, blank account
  # sectornumbers is an array of arrays: [[sector_id, sectoraalnummer], [sector_id, sectoraalnummer]]
  def self.create_initial(sectornumbers, options)
    postcode = Registration.find_by_id(options[:registration_id]).try(:postcode) if options[:registration_id]
    web_registration = WebRegistration.find_by_id(options[:web_registration_id]) if options[:web_registration_id]

    issuer_type = if web_registration then IssuerType::LETTER_INTERNATIONAL
                  elsif BeveiligdeBezorgingPostcodeCheck.new(postcode).positive? then IssuerType::LETTER_SECURE_DELIVERY
                  else IssuerType::LETTER
    end
    Account.transaction do
      new_account = Account.new
      new_account.create_sectorcodes(sectornumbers)
      new_account.create_sectorcodes_history(sectornumbers)
      new_account.herkomst = web_registration.webdienst_id if web_registration
      new_account.password_authenticator = Authenticators::Password.new(status: Authenticators::Password::Status::INITIAL, issuer_type: issuer_type)
      new_account.issuer_type = IssuerType::REGULAR
      new_account.save(validate: false)
      new_account
    end
  end

  # creates an initial, blank account for a balie registration
  # sectornumbers is an array of arrays: [[sector_id, sectoraalnummer], [sector_id, sectoraalnummer]]
  def self.create_initial_balie(sectornumbers)
    Account.transaction do
      new_account = Account.new(status: ::Account::Status::INITIAL)
      new_account.create_sectorcodes(sectornumbers)
      new_account.create_sectorcodes_history(sectornumbers)
      new_account.password_authenticator = Authenticators::Password.new(status: Authenticators::Password::Status::INITIAL, issuer_type: "front_desk")
      new_account.issuer_type = IssuerType::DESK
      new_account.save(validate: false)
      new_account
    end
  end

  def self.create_for_request_station(sectornumbers)
    Account.transaction do
      new_account = Account.new(status: ::Account::Status::REQUESTED)
      new_account.create_sectorcodes(sectornumbers)
      new_account.create_sectorcodes_history(sectornumbers)
      new_account.issuer_type = IssuerType::MUNICIPAL
      new_account.save(validate: false)
      new_account
    end
  end

  # sectornumbers is an array of arrays: [[sector_id, sectoraalnummer], [sector_id, sectoraalnummer]]
  def create_sectorcodes(sectornumbers)
    sectornumbers.each do |sectornummer|
      sectorcodes << Sectorcode.new do |code|
        sector_number = create_sectorcode(sectornummer[0], sectornummer[1])
        code.sector_id = sectornummer[0]
        code.sectoraalnummer = sector_number
      end
    end
  end

  # sectornumbers is an array of arrays: [[sector_id, sectoraalnummer], [sector_id, sectoraalnummer]]
  def create_sectorcodes_history(sectornumbers)
    sectornumbers.each do |sectornummer|
      sectorcodes_history << SectorcodesHistory.new do |code|
        sector_number = create_sectorcode(sectornummer[0], sectornummer[1])
        code.sector_id = sectornummer[0]
        code.sectoraalnummer = sector_number
      end
    end
  end

  def create_sectorcode(sector_id, sectornummer)
    return sectornummer.rjust(9, "0") if [Sector.get("bsn"), Sector.get("sofi")].include?(sector_id)

    sectornummer
  end

  def external_code
    code = sectorcodes.joins(:sector).order(Sector.arel_table[:number_name]).first
    ExternalCode.get(code.sector, code.sectoraalnummer).external_code
  end

  def max_emails_per_day?(max_emails)
    email_deliveries.where("created_at >= ?", Time.zone.now.beginning_of_day).count >= max_emails
  end

  # returns the number of emails gotten since <duur_controlecode> (usually 24 hours)
  def email_deliveries_since
    duur_controlecode = ::Configuration.get_int("duur_controlecode") # hours
    email_deliveries.where("created_at > ?", Time.zone.now - duur_controlecode.hours).count
  end

  # determine possible delay time for controlecode emails
  def delivery_time
    aantal_controlecodes = ::Configuration.get_int("aantal_controlecodes")
    delay_controlecodes = ::Configuration.get_int("delay_controlecodes").minutes
    deliveries = email_deliveries_since
    delay_minutes = 0
    if deliveries > aantal_controlecodes
      delay_minutes = (deliveries - aantal_controlecodes) * delay_controlecodes # delay (minutes)
    end
    Time.zone.now + delay_minutes
  end

  # confirms an account request by
  # - setting the status to "aangevraagd"
  # - send letter(s) by post
  def confirm(session)
    update_attribute(:status, ::Account::Status::REQUESTED)

    # prepare letters
    letter_type = ActivationLetter::LetterType::AANVRAAG_WITH_SMS
    registration = Registration.find(session[:registration_id])
    registration.finish_letters(letter_type)
    registration.update_attribute(:status, ::Registration::Status::REQUESTED)

    if registration.balie_request?
      registration.sent_letters(ActivationLetter::LetterType::BALIE)
      create_distribution_tools
    end

    if session[:webdienst]
      web_reg = WebRegistration.find_by(id: session[:web_registration_id])
      web_reg.update_attribute(:aanvraagnummer, "") if web_reg.present? # verwijder aanvraagnummer bij succesvol aanvragen
    end

    create_authentication_tools registration
    pending_sms_tool.update_attribute(:geldigheidstermijn, registration.geldigheidsduur) if pending_sms_tool
    true
  rescue StandardError => e
    Rails.logger.error "Error in Account#confirm: #{e.message}"
    false
  end

  def create_or_update_password_authenticator(registration)
    # To counter bug 5.2.41.6, but by no means a real fix, code for registration, activation and recover should be rewritten!
    if password_authenticator.nil?
      create_password_authenticator(status: Authenticators::Password::Status::ACTIVE,
                                    activation_code: registration.activation_codes,
                                    geldigheidstermijn: registration.geldigheidsduur)
    else
      password_authenticator.update_attribute(:activation_code, registration.activation_codes)
    end
  end

  # create authentication tools:
  # - always : password_tool (authenticatiemiddel wachtwoord)
  # - midden : sms_tool      (authenticatiemiddel sms)
  def create_authentication_tools(registration)
    status = (registration.balie_request? ? Authenticators::Password::Status::BLOCKED : Authenticators::Password::Status::INITIAL)

    if password_authenticator.nil?
      create_password_authenticator(status: status,
                                    activation_code: registration.activation_codes,
                                    geldigheidstermijn: registration.geldigheidsduur)
    else

      password_authenticator.update(status: status, activation_code: registration.activation_codes, geldigheidstermijn: registration.geldigheidsduur)
    end
  end

  def create_distribution_tools
    self.distribution_entity = DistributionEntity.new do |tool|
      tool.status = ::DistributionEntity::Status::INACTIVE
    end
    distribution_entity.save!
  end

  def void_last_sms_challenge_for_action(action)
    return unless sms_tools.present?

    challenge = sms_challenge(action: action)
    return unless challenge

    challenge.attempt = 0
    challenge.status = ::SmsChallenge::Status::INCORRECT
    challenge.save!
  end

  def email_address_present?
    email.try(:adres).present?
  end

  def email_activated?
    email_address_present? && email.state.verified?
  end

  def email_not_activated?
    email_address_present? && email.state.not_verified?
  end

  def email_blocked?
    email_address_present? && email.state.blocked?
  end

  def email_pending?
    email_not_activated? || email_blocked?
  end

  def wachtwoordherstel_allowed_with_sms?
    email_activated? && sms_tools.active?
  end

  def email_skip_expired?
    if !email_activated?
      if email_requested.nil?
        return true
      end

      maximum_period = email_requested + ::Configuration.get_int("maximum_period_unregistered_email").months
      if Time.zone.now > maximum_period
        return true
      end
    end

    false
  end

  # returns the latest sms challenge which is not "beantwoord"
  #  or nil if none exists
  def sms_challenge(action: nil, number: nil, webservice: nil, spoken: false)
    challenges = sms_challenges
    challenges = challenges.where(spoken: spoken)
    challenges = challenges.where(action: action) if action
    challenges = challenges.where(mobile_number: DigidUtils::PhoneNumber.normalize(number)) if number
    challenges = challenges.where(webservice: webservice) if webservice && action == "sign_in"
    challenges.last
  end

  # returns the timestamp of the first sms challenge within this flow
  # which is not "beantwoord" or nil if none exists
  def time_stamp_of_first_sms_challenge(action)
    all_for_this_flow = sms_challenges.where(action: action)
    if ::Configuration.get_int("resetperiode_blokkadeteller")
      limited_to_time_window = all_for_this_flow.where("created_at > ?", ::Configuration.get_int("resetperiode_blokkadeteller").hours.ago)
    end
    limited_to_time_window.order(created_at: :asc).first.try(:created_at)
  end

  # return true when
  # - the last sms was sent not longer than {configurable} minutes ago..
  # for this type of action
  def sms_too_fast?(spoken: false)
    next_sms_wait_time(spoken: spoken).to_i > 0
  end

  def next_sms_wait_time(spoken: false)
    last_sms = sms_challenge(spoken: spoken)
    return unless last_sms.present?

    sms_snelheid = ::Configuration.get_int("sms_snelheid")
    return if last_sms.state.correct?

    ((last_sms.created_at + sms_snelheid.minutes) - Time.zone.now).seconds.ceil
  end

  # return how many accounts are currenly in request mode for this sectoraalnummer/sector
  def self.count_registration_currently_in_progress(sectoraalnummer, sector_ids)
    registration_currently_in_progress(sectoraalnummer, sector_ids).count
  end

  def self.last_registration_currently_in_progress_balie?(sectoraalnummer, sector_ids)
    return false if count_registration_currently_in_progress(sectoraalnummer, sector_ids) == 0

    last_reg = registration_currently_in_progress(sectoraalnummer, sector_ids).take
    last_reg.distribution_entity != nil
  end

  # return how many active accounts already exists for this sectoraalnummer/sector
  def self.count_account_already_exists(sectoraalnummer, sector_ids)
    account_already_exists(sectoraalnummer, sector_ids).count
  end

  def self.account_already_exits_status(sectoraalnummer, sector_ids, status)
    joins(:sectorcodes)
      .where(sectorcodes: { sectoraalnummer: sectoraalnummer, sector_id: sector_ids }, status: status)
  end

  def self.account_initial(sectoraalnummer, sector_ids)
    find_by_sectoraalnummer_and_sector_ids_and_status(sectoraalnummer, sector_ids, ::Account::Status::INITIAL)
  end

  # return accounts that are currenly in request mode for this sectoraalnummer/sector
  def self.registration_currently_in_progress(sectoraalnummer, sector_ids)
    find_by_sectoraalnummer_and_sector_ids_and_status(sectoraalnummer, sector_ids, ::Account::Status::REQUESTED)
  end

  def self.account_already_exists(sectoraalnummer, sector_ids)
    find_by_sectoraalnummer_and_sector_ids_and_status(sectoraalnummer, sector_ids, ::Account::Status::ACTIVE)
  end

  def self.account_revoked(sectoraalnummer, sector_ids)
    find_by_sectoraalnummer_and_sector_ids_and_status(sectoraalnummer, sector_ids, ::Account::Status::REVOKED)
  end

  def self.account_expired(sectoraalnummer, sector_ids)
    find_by_sectoraalnummer_and_sector_ids_and_status(sectoraalnummer, sector_ids, ::Account::Status::EXPIRED)
  end

  def self.account_removed(sectoraalnummer, sector_ids)
    find_by_sectoraalnummer_and_sector_ids_and_status(sectoraalnummer, sector_ids, ::Account::Status::REMOVED)
  end

  def self.account_suspended(sectoraalnummer, sector_ids)
    find_by_sectoraalnummer_and_sector_ids_and_status(sectoraalnummer, sector_ids, ::Account::Status::SUSPENDED)
  end

  # return how many postponed accounts already exists for this sectoraalnummer/sector
  def self.account_postponed(sectoraalnummer, sector_ids)
    find_by_sectoraalnummer_and_sector_ids_and_status(sectoraalnummer, sector_ids, ::Account::Status::SUSPENDED).count
  end

  def self.find_by_sectoraalnummer_and_sector_ids_and_status(sectoraalnummer, sector_ids, status)
    joins(:sectorcodes)
      .where(sectorcodes: { sectoraalnummer: sectoraalnummer, sector_id: sector_ids }, status: status)
  end

  def sms_in_uitbreiding_activatable?
    !sms_tools.active? && sms_tools.pending? && password_authenticator.present? && password_authenticator.state.active?
  end

  def sms_in_uitbreiding?
    sms_in_uitbreiding_activatable? && pending_sms_tool.activation_code.present? && !pending_sms_tool.expired?
  end

  def mobiel_kwijt_in_progress_activatable?
    sms_tools.pending? && sms_tools.active?
  end

  def mobiel_kwijt_in_progress?
    mobiel_kwijt_in_progress_activatable? && pending_sms_tool.activation_code.present? && !pending_sms_tool.expired?
  end

  def pending_activatable?
    sms_tools.pending? && pending_sms_tool.activation_code.present? && !pending_sms_tool.expired?
  end

  def basis_aanvraag?
    password_authenticator.present? && password_authenticator.state.pending? && sms_tools.empty?
  end

  def substantieel_aanvraag?
    app_authenticators.pending.pluck(:substantieel_activated_at).compact.present?
  end

  def midden_aanvraag?
    (password_authenticator.blank? && app_authenticator_pending? ||
     (sms_tools.pending? && (password_authenticator.blank? || !password_authenticator.state.active?)))
  end

  def midden_active?
    ((password_authenticator_active? || sms_tool_active? || app_authenticator_active?) && !zekerheidsniveau_substantieel?)
  end

  def active_sms_tool
    sms_tools.active.first
  end

  def pending_sms_tool
    sms_tools.pending.first
  end

  def app_authenticator_pending?
    app_authenticators.pending.any?
  end

  def app_authenticator_active?
    app_authenticators.active.any?
  end

  def sms_tool_active?
    sms_tools.active.any?
  end

  def password_authenticator_active?
    password_authenticator.active? if password_authenticator
  end

  def level
    LEVELS.key(authentication_level)
  end

  def via_balie?
    distribution_entity.present?
  end

  def zekerheidsniveau_basis?
    zekerheidsniveau == Account::LoginLevel::PASSWORD || zekerheidsniveau.nil?
  end

  # Returns true if this account has SMS enabled (Sms-controle = Actief)
  def login_level_two_factor?
    zekerheidsniveau == Account::LoginLevel::TWO_FACTOR
  end

  def zekerheidsniveau_substantieel?
    zekerheidsniveau == Account::LoginLevel::SMARTCARD
  end

  # return a human readable version of voorkeur inlogmethode (used to be zekerheidsniveau)
  def human_zekerheidsniveau
    if zekerheidsniveau.blank?
      I18n.t("messages.my_digid.niet_ingesteld")
    elsif zekerheidsniveau_basis?
      I18n.t("messages.account.type_basis")
    else
      I18n.t("messages.account.type_midden")
    end
  end

  def heraanvraag_account_deleted?
    !@heraanvraag_account_deleted.nil?
  end

  # activate all properties of an account
  def activate!(send_mail: true)
    transaction do
      if heraanvraag.present? && send_mail
        send_notifications_heraanvraag unless sms_in_uitbreiding?
        archive!
      elsif email_activated? && send_mail
        NotificatieMailer.delay(queue: "email").notify_activatie(account_id: id, recipient: adres)
      end

      assign_attributes(status: Status::ACTIVE, current_sign_in_at: Time.zone.now)
      save!(validate: false)

      if bsn
        response = dgl_client.post("/iapi/afnemersindicatie/" + bsn)
        if response.code == 200
          Log.instrument("1494", account_id: id, hidden: true)
        else
          Log.instrument("1493", account_id: id, hidden: true)
        end
      end

      password_authenticator&.update(status: Authenticators::Password::Status::ACTIVE, activation_code: nil)
      distribution_entity.update_attribute(:status, ::DistributionEntity::Status::ACTIVE) if via_balie?
    end
  end

  def activate_sms_tool!
    return unless sms_tools.pending?

    transaction do
      sms_tools.active.destroy_all
      pending_sms_tool.update(status: Authenticators::SmsTool::Status::ACTIVE, activation_code: nil, geldigheidstermijn: nil, activated_at: Time.zone.now)
    end
  end

  def old_account
    @old_account ||= heraanvraag.first
  end

  # Bij Activatie DigiD-account (aanvraag): (notify_activatie)
  # notify: Geregistreerde e-mailadres
  # Bij Activatie DigiD-account (heraanvraag): (notify_activatie_heraanvraag)
  # notify: Geregistreerde e-mailadres EN Geregistreerde e-mailadres bij bestaande account dat (automatische) opgeheven gaat worden
  # (TENZIJ dit dezelfde e- mailadressen zijn)
  def send_notifications_heraanvraag
    # there should really be only one, but hey, stuff happens, that's why we have an array
    NotificatieMailer.delay(queue: "email").notify_activatie_heraanvraag(account_id: id, recipient: adres) if email_activated?
    return if old_account.email.try(:adres).eql?(email.try(:adres))

    if old_account.email_activated?
      NotificatieMailer.delay(queue: "email").notify_activatie_heraanvraag(account_id: old_account.id, recipient: old_account.adres)
    end
  end

  def archive!
    old_account.account_histories.each do |account_history|
      account_history.update(account_id: id)
    end
    account_histories.build(gebruikersnaam: old_account.password_authenticator&.username,
                            mobiel_nummer: old_account.phone_number,
                            email_adres: old_account.email.try(:adres))
    Log.instrument("412", account_id: id, hidden: true, **Account.generate_remove_data(old_account))
  end

  def other_accounts
    other_bsn_accounts = Account.with_bsn(bsn).where.not(id: id)
    other_sofi_accounts = Account.with_sofi(sofi).where.not(id: id)
    other_bsn_accounts + other_sofi_accounts
  end

  # After succesful activation, an account must be the only one.
  # So delete other in case of an "heraanvraag".
  # The following must be true:
  # - if activating sector=bsn
  #     count nummer/code combination (except self)
  #       if count==0 and a-nummer is unique => ok (brand new digid)
  #       if count==1 and a-nummer matches   => ok (heraanvraag)
  #       if count==1 and a-nummer is attached to another account, set status to ""
  def destroy_other_accounts(options = {})
    app_in_uitbreiding = options[:app_in_uitbreiding]
    sms_in_uitbreiding = options[:sms_in_uitbreiding]
    other_accounts.each do |account|
      phone_number = (account.phone_number || account.pending_phone_number)
      account.destroy

      if (app_in_uitbreiding || sms_in_uitbreiding) && account.status == Status::REQUESTED
        @heraanvraag_account_deleted = true
        fields_for_both = [account.password_authenticator&.username, phone_number].compact.join(", ")
        Log.instrument("681", fields: fields_for_both, account_id: id)
      else
        fields_for_both = [account.password_authenticator&.username].compact.join(", ")
        Log.instrument("92", fields: fields_for_both, account_id: id)
      end
    end
  end

  def reset_zekerheidsniveau!
    return if zekerheidsniveau_basis?

    to_basis unless reload.sms_tools.active? || reload.app_authenticator_active?
  end

  # transform an existing account to an account of type 'basis'
  def to_basis
    update_column(:zekerheidsniveau, LoginLevel::PASSWORD)
  end

  def remove_mobiel
    # update_column(:mobiel_nummer, '')
    sms_tools.destroy_all
  end

  def pending_phone_number
    pending_sms_tool.try(:phone_number)
  end

  def phone_number
    active_sms_tool.try(:phone_number)
  end

  def pending_gesproken_sms
    pending_sms_tool.try(:gesproken_sms) || false
  end

  def gesproken_sms
    active_sms_tool.try(:gesproken_sms) || false
  end

  # Log registration validation errors
  def self.log_registration_errors(errors, account_id)
    errors = errors.messages

    log_username_errors(errors[:gebruikersnaam], account_id) if errors[:gebruikersnaam]
    log_mobile_errors(errors[:mobiel_nummer], account_id) if errors[:mobiel_nummer]

    if errors[:"email.adres"].present?
      Log.instrument("1357", account_id: account_id)
    elsif errors[:email]
      log_email_errors(errors[:email], account_id)
    end

    Log.instrument("37", account_id: account_id) if errors[:password].present?
    Log.instrument("36", account_id: account_id) if errors[:password_confirmation].present?
  end

  def self.destroy_all
    raise "Please do not use, see Account.delete_with_associations"
  end

  # Performance optimization used reflection to browse all relations of Account
  # and build separate delete queries, returns the found account ids
  def self.delete_with_associations
    ids = pluck("accounts.id")
    reflect_on_all_associations.each do |assoc|
      next unless assoc.options[:dependent].to_s == "destroy"

      klass       = assoc.class_name.to_s.classify.constantize
      polymorphic = assoc.options[:as]

      scope = if polymorphic
                klass.where "#{polymorphic}_id": ids, "#{polymorphic}_type": name
              else
                klass.where account_id: ids
              end

      scope.delete_all
    end
    ids
  end

  # Log username validation errors on screen A4
  def self.log_username_errors(username_errors, account_id)
    username_errors.each do |error|
      Log.instrument("36", account_id: account_id) if error =~ /^#{I18n.t("activerecord.errors.messages.blank", attribute: I18n.t('username'))}/
      if [I18n.t("activerecord.errors.models.account.attributes.gebruikersnaam.too_short"), I18n.t("activerecord.errors.models.account.attributes.gebruikersnaam.too_long")].include? error
        Log.instrument("35", account_id: account_id)
      end
      Log.instrument("38", account_id: account_id) if error == I18n.t("activerecord.errors.models.account.attributes.gebruikersnaam.invalid")
      Log.instrument("44", account_id: account_id) if error =~ /^#{I18n.t("messages.account.gebruikersnaam.uniek")}/
    end
  end

  # Log mobile validation errors on screen A4
  def self.log_mobile_errors(mobile_errors, account_id)
    mobile_errors.each do |error|
      Log.instrument("36", account_id: account_id) if error == I18n.t("activerecord.errors.messages.blank", attribute: I18n.t("mobile_number"))
      if error == I18n.t("activerecord.errors.models.account.attributes.mobiel_nummer.too_short") || error == I18n.t("activerecord.errors.models.account.attributes.mobiel_nummer.too_long") || error == I18n.t("activerecord.errors.models.account.attributes.mobiel_nummer.invalid")
        Log.instrument("40", account_id: account_id)
      end
    end
  end

  # Log email validation errors on screen A4
  def self.log_email_errors(email_errors, account_id)
    email_errors.each do |error|
      if error == I18n.t("activerecord.errors.models.email.attributes.adres.invalid") || error == I18n.t("activerecord.errors.models.email.attributes.adres.too_long")
        Log.instrument("39", account_id: account_id)
      end
    end
  end

  def seed_old_passwords(password)
    # is this an old DigiD account?
    return unless password_salt.nil? || password_salt.length < 32

    # fire before_save filter and encrypts password with a salt
    update(password: password)
  end

  def max_weak_password_skip_count_reached?
    (Configuration.get_date("uiterste_wijzigingsdatum_wachtwoord") < Time.zone.now) || (weak_password_skip_count >= max_weak_password_skip)
  end

  def reset_weak_password_skip_count!
    update_attribute(:weak_password_skip_count, 0)
  end

  def skips_left
    max_weak_password_skip - weak_password_skip_count
  end

  def authentication_level(without_authenticator = nil)
    authenticators = app_authenticators.active.where.not(id: without_authenticator)

    if app_authenticator_active? && authenticators.substantieel?
      "substantieel"
    elsif sms_tools.active? || authenticators.any?
      "midden"
    else
      "basis"
    end
  end

  def last_authenticator?
    authenticators = app_authenticators.active
    if password_authenticator_active?
      if sms_tool_active?
        authenticators += sms_tools.active
      else
        authenticators += [password_authenticator]
      end
    end
    is_last = authenticators.count == 1
    return false unless is_last
    return active_high_authenticators.count == 0
  end

  def active_two_factor_authenticators
    return active_local_two_factor_authenticators + active_high_authenticators
  end

  # Authenticators from our DB, excluding MUs
  def active_local_two_factor_authenticators
    authenticators = app_authenticators.active
    authenticators += sms_tools.active if password_authenticator_active?
    return authenticators
  end

  def multiple_two_factor_authenticators?
    local_count = active_local_two_factor_authenticators.count
    return true if local_count > 1
    return local_count + active_high_authenticators.count > 1
  end

  def active_high_authenticators
    active_driving_licences + active_id_cards
  end

  def active_driving_licences
    begin
      return rdw_client.get(bsn: bsn).select(&:active?)
    rescue Exception => e
      Rails.logger.warn("Retrieving driving_licences failed #{e.message}")
      return []
    end
  end

  def active_id_cards
    begin
      return rvig_client.get(bsn: bsn).select(&:active?)
    rescue Exception => e
      Rails.logger.warn("Retrieving id_cards failed #{e.message}")
      return []
    end
  end

  def destroy_old_email_recovery_codes(&_block)
    codes = recovery_codes.by_email.destroy_all
    yield self if block_given? && codes.size > 0
  end

  # send email to people whose account is about to expire
  def self.send_expiry_notifications
    Log.instrument("350")
    # get sectors that expire, except sector a-nummer
    Sector.where("valid_for>0")
          .where("warn_before>0")
          .where("name!='a-nummer'").find_each do |sector|
      send_expiry_notifications_for_sector(sector)
    end
  end

  def self.send_expiry_notifications_for_sector(sector, valid_for: nil, warn_before: nil)
    valid_for ||= sector.valid_for
    warn_before ||= sector.warn_before

    warning_date = (Time.now - (valid_for.months - warn_before.days)).to_date
    # find accounts for this sector which are about to expire (current_sign_in_at == warning_date)
    Account.joins(:sectorcodes)
           .active
           .where("sectorcodes.sector_id = ?", sector.id)
           .where(current_sign_in_at: (warning_date..(warning_date + 1.day)))
           .find_each do |account|
      # send an email to users which have serviceberichten on (and have an active emailaddress) and have an active DigiD
      if account.email_activated?
        AccountExpireNotificationMailer.delay(queue: "email").account_expire_notification(account_id: account.id, recipient: account.adres, valid_for: valid_for, warn_before: warn_before)
      end
    end
  end

  # send email to people that haven't activated yet and are running out of time
  def self.send_activation_reminders
    Log.instrument("799")
    activation_warning_time = ::Configuration.get_int("activation_warning_time").days

    Account.joins(:email)
                .requested
                .where("emails.status = ?", "verified")
                .find_each do |account|

      tool = account.password_authenticator || account.app_authenticators.pending.first
      next if tool.nil?

      deadline = tool.created_at.to_date + tool.geldigheidstermijn.to_i.days
      mail_date = deadline - activation_warning_time

      if mail_date == Date.today
        if account.reason_suspension_updated_at.present?
          if account.reason_suspension.eql? ReasonSuspension::DECEASED
            Log.instrument("1563", account_id: account.id, hidden: true)
            next
          end
        else
          # Perform BRP check
          Log.instrument("1559", account_id: account.id, hidden: true)

          registration = Registration.create_fake_aanvraag(account.bsn)
          BrpRegistrationJob.new.perform_request(bsn: account.bsn, request_type: "activation_email_reminder", registration_id: registration.id)
          registration.reload

          case registration.gba_status
          when "deceased", "not_found"
            Log.instrument("1562", account_id: account.id, hidden: true)
            next
          when "error"
            Log.instrument("1561", account_id: account.id, hidden: true)
          else
            Log.instrument("1560", account_id: account.id, hidden: true)
          end
        end
        ActivationReminderMailer.delay(queue: "email").activation_reminder(account_id: account.id, recipient: account.adres, deadline: deadline)
      end
    end
  end

  # DigiD accounts that have not been used for some time (sector.valid_for) will be marked by status "expired"
  def self.expire_accounts(max_accounts = nil)
    Log.instrument("800")
    Log.instrument("803", aantal: 0) if max_accounts.nil?
    return if max_accounts.nil?

    clean_up_expired_accounts = ::Configuration.get_int("clean_up_expired_accounts") # in months
    cleaned_accounts = 0
    # get sectors that expire, except sector a-nummer
    Sector.where("valid_for>0").where("name!='a-nummer'").find_each do |sector|
      expire_date = (Time.now - sector.valid_for.months).to_date
      remove_date = (Time.now - sector.valid_for.months - clean_up_expired_accounts.months).to_date
      # find accounts which are about to expire (current_sign_in_at == expire_date)
      accounts = Account.joins(:sectorcodes)
                        .select("accounts.*")
                        .where("sectorcodes.sector_id = ?", sector.id)
                        .where("status not in (?)", [::Account::Status::EXPIRED, ::Account::Status::REMOVED])
                        .where("blocked_till is null or blocked_till < ?", Time.now.to_date)
                        .where("current_sign_in_at < ?", expire_date)
                        .limit(max_accounts)
      accounts.each do |account|
        if account.current_sign_in_at < remove_date
          cleanup_account(account)
        else
          account.update_attribute(:status, ::Account::Status::EXPIRED)
          Log.instrument("346", account_id: account.id)
        end
      end
      cleaned_accounts += accounts.length
    end
    Log.instrument("803", aantal: cleaned_accounts)
  end

  def self.clean_up_expired_accounts(max_accounts = nil)
    Log.instrument("802")
    Log.instrument("803", aantal: 0) if max_accounts.nil?
    return if max_accounts.nil?

    clean_up_expired_accounts = ::Configuration.get_int("clean_up_expired_accounts") # in months
    cleaned_accounts = 0
    Sector.where("valid_for>0").where("warn_before>0").where("name!='a-nummer'").find_each do |sector|
      remove_date = (Time.now - sector.valid_for.months - clean_up_expired_accounts.months).to_date
      # find accounts which have been expired for a while
      accounts = Account.joins(:sectorcodes)
                        .select("accounts.*")
                        .where("sectorcodes.sector_id = ?", sector.id)
                        .expired
                        .where("current_sign_in_at < ?", remove_date)
                        .limit(max_accounts)
      accounts.each do |account|
        cleanup_account(account)
      end
      cleaned_accounts += accounts.length
    end
    Log.instrument("803", aantal: cleaned_accounts)
    Log.instrument("354")
  end

  def self.cleanup_account(account)
    account.status = ::Account::Status::REMOVED

    remove_data = generate_remove_data(account)

    # destroy_all_authenticators
    account.password_authenticator&.destroy
    account.sms_tools.destroy_all
    account.app_authenticators.destroy_all
    account.email&.destroy

    account.save(validate: false)
    Log.instrument("1497", account_id: account.id)
    Log.instrument("1236", account_id: account.id, hidden: true, **remove_data) # 1236
  end

  def with_language
    I18n.with_locale(try(:locale)) do
      yield
    end
  end

  def self.create_for_bsn(bsn:, locale:, issuer_type:)
    sectornumbers = [[Sector.get("bsn"), bsn]]
    new_account = Account.new(issuer_type: issuer_type, locale: locale.downcase)

    begin
      Account.transaction do
        new_account.create_sectorcodes(sectornumbers)
        new_account.create_sectorcodes_history(sectornumbers)
        new_account.save!
      end
    rescue StandardError => e
      Rails.logger.warn("Couldn't create new account: '#{new_account.errors.details}'")
      return
    end
    new_account
  end

  private

  def self.generate_remove_data(account)
    remove_data = {}
    remove_data[:username] = account.password_authenticator.try(:username) || ""
    remove_data[:phone_number] = account.phone_number || ""
    remove_data[:spoken_sms] = I18n.t(account.gesproken_sms)
    remove_data[:email] = account.email.try(:adres) || ""
    remove_data[:email_checked] = I18n.t(account.email.try(:status) == Email::Status::VERIFIED)
    remove_data[:betrouwbaarheidsniveau] = account.level
    remove_data[:phone_name] = account.app_authenticators.pluck(:device_name).join(",")
    remove_data[:sub_id_type] = account.app_authenticators.pluck(:substantieel_document_type).join(",")
    remove_data
  end

  def phone_number_not_required?(atr)
    atr["phone_number"].blank? && (!%w[front_desk letter_international].include?(password_authenticator&.issuer_type) || sectorcodes.where(sector_id: Sector.get("oeb")).exists?)
    # telephone number is required if issuer type balie or letter_international (web registrations) EXCEPT when letter_international is an oeb web registration
  end

  def heraanvraag
    sector_ids = Sector.fetch(sectorcodes.first.sector_id)
    Account.account_already_exists(sectorcodes.first.sectoraalnummer, sector_ids)
  end

  def max_weak_password_skip
    ::Configuration.get_int("max_weak_password_skip")
  end

  def user_friendly_phone_number
    normalized_mobiel_nummer.to_s.gsub(/^\+316/, "06").gsub(/^00/, "+")
  end

  def password_valid
    check = PasswordCheck.new(gebruikersnaam, password)
    errors.add(:password, check.errors[:password]) if check.invalid?
  end
end
