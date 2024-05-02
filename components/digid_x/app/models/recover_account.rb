
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
# Recover account model
#
# Recover account model facilitates account recovery for DigiD
#
# Params: username, burgerservicenummer
#
# ----------------------------------------------------------------
class RecoverAccount
  include ActiveModel::Model
  include ActiveModel::Validations::Callbacks
  extend ActiveModel::Naming

  attr_accessor :gebruikersnaam, :burgerservicenummer, :account, :starting_point

  validates :gebruikersnaam, presence: true
  validates :burgerservicenummer, presence: true, bsn_format: true
  validate :valid_account?, if: :username_and_bsn?
  validate :gba_checks?, if: :username_and_bsn?
  after_validation :register_attempt, if: :username_and_bsn?

  def valid_account?
    return true if blocked_username_bsn?
    account_exists?
  end

  def blocked_username_bsn?
    remove_expired_attempts # house-keeping: not related to specific acounts
    allowed_auth_attempts = ::Configuration.get_int("aantal_invoerpogingen_gebruikersnaam_bsn") - 1 # since we did not save this attempt to the db yet
    if (attempts_amount("username").to_i >= allowed_auth_attempts.to_i) || (attempts_amount("bsn").to_i >= allowed_auth_attempts.to_i)
      msg = I18n.t("activemodel.errors.models.recover_account.attributes.blocked", time_blocked: I18n.l(blocked_till, format: :time_text_tzone_in_brackets))
      errors.add(:blocked, msg)
      Log.instrument("189", time: blocked_till)
      true
    else
      false
    end
  end

  def account_exists?
    @account = Account.joins(:sectorcodes).joins(:password_authenticator)
                      .where("password_tools.username = ? COLLATE utf8mb4_bin", gebruikersnaam)
                      .where("sectorcodes.sectoraalnummer=?", burgerservicenummer.try(:rjust, 9, "0"))
                      .find_by("sectorcodes.sector_id IN (?)", [Sector.get("bsn"), Sector.get("sofi")])

    return true if @account.present? && @account.state.active?

    if @account.present?
      errors.add(:authentication, I18n.t(@account.state, scope: "recover_accounts.authentications.errors.account"))
      Log.instrument("190", account_id: @account.id)
    else
      errors.add(:authentication, I18n.t("activemodel.errors.models.recover_account.attributes.account_recovery_not_exists"))
      Log.instrument("191")
    end
    false
  end

  def gba_checks?
    return unless @account.present? && !@account.wachtwoordherstel_allowed_with_sms? && starting_point.eql?("herstellen_wachtwoord")

    match = RecoverAccountChecks::Queue.new(@account).letter_checks
    return unless match

    match.log
    errors.add(:blocked, match.error_message)
  end

  private

  def remove_expired_attempts
    expiry_time = Time.zone.now.to_i - (::Configuration.get_int("duur_herstelpogingen_invoeren_gebruikersnaam_bsn") * 60)
    RecoveryAttempt.where(["created_at < ?", Time.zone.at(expiry_time)]).delete_all
  end

  def attempts_amount(type)
    attempt = type == "username" ? gebruikersnaam : burgerservicenummer
    RecoveryAttempt.where(attempt: attempt, attempt_type: type).count
  end

  def register_attempt
    return if errors.empty?

    RecoveryAttempt.create(attempt: gebruikersnaam, attempt_type: "username")
    RecoveryAttempt.create(attempt: burgerservicenummer, attempt_type: "bsn")
  end

  def blocked_till
    account_geblokkeerd = ::Configuration.get_int("duur_herstelpogingen_invoeren_gebruikersnaam_bsn")
    account_geblokkeerd.minutes.from_now
  end

  def username_and_bsn?
    gebruikersnaam.present? && burgerservicenummer.present?
  end
end
