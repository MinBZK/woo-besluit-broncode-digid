
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

# ----------------------------------------------------------------
# Authentication model
#
# Authentication model facilitates authentication for DigiD
#
# Params: username, password
#
# A UserGhost is a fake Account without an existing username.
# ----------------------------------------------------------------
class Authentication
  include ActiveModel::Model
  include ActiveModel::Validations::Callbacks

  USERNAME_MAX_LENGTH = 190

  attr_accessor :username, :password, :session_type, :level, :type_account, :webservice_id, :webservice_name, :card_type, :remember_login, :test_zekerheidsniveau
  attr_writer :account_or_ghost

  after_validation :log_errors

  validates :username,                   presence: true
  validates :password,                   presence: true

  validate :blocked_account?,            if: -> { name_and_password_present? }
  validate :password_matches?,           if: -> { no_blocked_errors? }
  validate :activate_actief?,            if: -> { name_and_password_correct? && session_type.eql?("activation") }
  validate :account_aangevraagd?,        if: -> { name_and_password_correct? && (session_type.in?(["sign_in", "recover_account"]) || type_account == "test") }
  validate :account_actief?,             if: -> { name_and_password_correct? }
  validate :account_has_minimum_level?,  if: -> { name_and_password_correct? && level.present? } # check if Account has the minimum level
  validate :gebruiken_sms?,              if: -> { name_and_password_correct? }
  validate :test_account?,               if: -> { test_zekerheidsniveau.present? }

  validates :test_zekerheidsniveau, numericality: { greater_than_or_quals: 10 }, if: -> { type_account == "test" }

  def weak_password?
    return false if account_or_ghost.is_a?(UserGhost) || authenticator.nil?
    authenticator.check_policy(password: password) < PasswordCheck::POLICY
  end

  def level_basis?
    type_account.eql?("basis")
  end

  # Find account or create a ghost
  def account_or_ghost
    @account_or_ghost ||= self.class.find_account_or_ghost(username).tap do |account|
      Log.instrument("65", webservice_id: webservice_id) unless account.is_a?(Account)
    end
  end

  def account
    @account_or_ghost if @account_or_ghost.is_a? Account
  end

  def test_account?
    errors.add(:not_valid, I18n.t("messages.authentication.no_testaccount")) unless account && account.bsn && account.bsn.start_with?("9")
  end

  def test_account_for_gbav?
    test_zekerheidsniveau.present? && account && account.bsn && account.bsn.start_with?("9")
  end

  def self.find_account_or_ghost(username)

    # Truncate username to prevent errors in creating user ghosts
    username_truncated = username[0...USERNAME_MAX_LENGTH] if username
    # Use Case Sensitive search by adding the COLLATE
    # We cannot add a default collation because new accounts have to be unique with case insensitive constraints
    account = Authenticators::Password.find_by("username = ? COLLATE utf8mb4_bin", username_truncated.to_s)&.account
    if account.present?
      # We do a dummy search against timing attacks. Is dit echt nodig? Benchmark geeft hier een minimaal verschil. De UserGhost is veel groter, maar niet te gebruiken voor bij een timing attack.
      UserGhost.find_by("gebruikersnaam = ? COLLATE utf8mb4_bin", username_truncated.to_s)
      account # but return account
    else
      ghost = UserGhost.find_by("gebruikersnaam = ? COLLATE utf8mb4_bin", username_truncated.to_s)
      ghost || UserGhost.create(gebruikersnaam: username_truncated.to_s)
    end
  end

  def no_app_or_sms?
    !(account_or_ghost.app_authenticators.any? || account_or_ghost.sms_in_uitbreiding?)
  end

  private

  def authenticator
    account_or_ghost.try(:password_authenticator)
  end

  def authenticating_account?
    account_or_ghost.is_a?(Account)
  end

  def add_blocked_account_log_and_error
    # TODO find cause: rarely happens, using current time is better than causing 500 error
    failed_attempt = account_or_ghost.blocking_manager.timestamp_first_failed_attempt.nil? ? Time.zone.now : account_or_ghost.blocking_manager.timestamp_first_failed_attempt

    msg = I18n.t(
      "middel_blocked_until",
      since: I18n.l(failed_attempt, format: :date_time_text_tzone_in_brackets),
      count: account_or_ghost.blocking_manager.max_number_of_failed_attempts,
      until: I18n.l(account_or_ghost.blocking_manager.blocked_till, format: :time_text_tzone_in_brackets),
      minutes: account_or_ghost.blocking_manager.blocked_time_left_in_minutes
    )

    payload = { webservice_id: webservice_id }
    payload[:account_id] = account_or_ghost.id if authenticating_account?
    Log.instrument("64", payload)
    errors.add(:blocked, msg)
  end

  #------------
  # Validations
  #------------
  #

  def blocked_account?
    return unless account_or_ghost.blocking_manager.blocked?
    add_blocked_account_log_and_error
  end


  # Check if given password matches with account password
  def password_matches?
    return if authenticator && authenticator.verify_password(password)

    account_or_ghost.blocking_manager.register_failed_attempt!
    if account_or_ghost.blocking_manager.blocked?
      account_or_ghost.void_last_sms_challenge_for_action(session_type)
      add_blocked_account_log_and_error
    else
      Log.instrument("67", account_id: account_or_ghost.id, webservice_id: webservice_id) if account_or_ghost.is_a?(Account)
      if session_type.eql?("activation")
        errors.add(:authentication, I18n.t("messages.authentication.not_found_activation"))
      else
        errors.add(:authentication, I18n.t("messages.authentication.not_found"))
      end
    end
  end

  # Check if account is aangevraagd en van type account
  def account_aangevraagd?
    return unless authenticating_account? && account_or_ghost.state.requested?

    Log.instrument("108", account_id: account_or_ghost.id, webservice_id: webservice_id)
    errors.add(:not_valid, I18n.t("messages.authentication.requested", url: "activeer_digid"))
  end

  # Check if account is actief and of type account
  def account_actief?
    return unless authenticating_account? && !(account_or_ghost.state.active? || account_or_ghost.state.requested?)

    if account_or_ghost.state.initial?
      errors.add(:authentication, I18n.t(account_or_ghost.state, scope: "messages.authentication"))
    else
      errors.add(:not_valid, I18n.t(account_or_ghost.state, scope: "messages.authentication", url: "aanvragen"))
    end
    Log.instrument("108", account_id: account_or_ghost.id, webservice_id: webservice_id)
  end

  # check if someone is trying to activate an active account
  def activate_actief?
    return unless authenticating_account?
    if account_or_ghost.sms_in_uitbreiding? || account_or_ghost.mobiel_kwijt_in_progress?
      return
    elsif account_or_ghost.status == Account::Status::REQUESTED || account_or_ghost.pending_sms_tool.present?
      account_or_ghost.basis_aanvraag? || account_or_ghost.midden_aanvraag?
      Log.instrument("1305", account_id: account_or_ghost.id)
      return
    else
      Log.instrument("1304", account_id: account_or_ghost.id, webservice_id: webservice_id)
      errors.add(:not_valid, I18n.t("messages.activation.already_activated"))
    end
  end

  def account_has_minimum_level?
    return unless level.eql?("midden") && account_or_ghost.is_a?(Account)

    return if account_or_ghost.active_sms_tool

    payload = account_or_ghost.is_a?(Account) ? { account_id: account_or_ghost.id, webservice_id: webservice_id } : { webservice_id: webservice_id }
    Log.instrument("110", payload)
    errors.add(:no_minimum_level, I18n.t("activerecord.errors.templates.no_minimum_level"))
  end

  def level_midden?
    "midden" == type_account
  end

  # Het systeem stelt vast dat de eindgebruiker over het authenticatiemiddel SMS beschikt. <#Controle: Authenticatiemiddel SMS>.
  def gebruiken_sms?
    return unless account_or_ghost.is_a?(Account) && type_account
    return if level_midden? && account_or_ghost.sms_tools.active?
    if (level_basis? && account_or_ghost.login_level_two_factor? &&
       !account_or_ghost.sms_tools.active?) || (level_midden? &&
       !account_or_ghost.sms_tools.active?)
      Log.instrument("66",
                     account_id: account_or_ghost.id,
                     webservice_id: webservice_id)
    end
    determine_sms_error
  end

  def determine_sms_error
    if level == "midden"
      i18n_key = ["login_requires_midden"]
      i18n_key << (account_or_ghost.app_authenticator_active? ? "active_app_authenticator" : "inactive_app_authenticator")
      return if no_app_or_sms?
    elsif level == "basis" && level_midden?
      i18n_key = ["user_chooses_midden"]
    else
      return
    end

    i18n_key << (account_or_ghost.sms_in_uitbreiding? ? "sms_pending" : "sms_missing")
    i18n_key = i18n_key.join(".")

    message = I18n.t(
      i18n_key,
      back_url: Rails.application.routes.url_helpers.sign_in_with_app_path, # inside digid domain use only relative paths
      my_digid_url: Rails.application.routes.url_helpers.my_digid_url,
      webservice: webservice_name || webservice.name
    ).html_safe

    errors.add(:use_sms, message)
  end

  def log_errors
    Log.instrument("63", webservice_id: webservice_id) unless name_and_password_present?
  end

  # tells if a username and password is filled-out
  def name_and_password_present?
    username.present? && password.present?
  end

  def no_blocked_errors?
    name_and_password_present? && !errors.has_key?(:blocked)
  end

  # tells if a username and password is correct
  def name_and_password_correct?
    no_blocked_errors? && !errors.has_key?(:authentication)
  end

  def webservice
    Webservice.find(webservice_id)
  end
end
