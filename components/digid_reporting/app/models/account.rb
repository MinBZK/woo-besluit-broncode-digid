
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

class Account < ActiveRecord::Base
  # QQQ copied from digid_x app!

  # struct for levels of authentication
  class LoginLevel
    PASSWORD      = 10
    PASSWORD_OTP  = 20
    DEFAULT       = nil
    ALL           = [PASSWORD, PASSWORD_OTP, DEFAULT]
  end

  module Status
    INITIAL   = 'initial'.freeze   # Initieel
    REQUESTED = 'requested'.freeze # Aangevraagd
    ACTIVE    = 'active'.freeze    # Actief
    REVOKED   = 'revoked'.freeze   # Gerevoceerd door bemiddelende instantie (bijv. SVB)
    SUSPENDED = 'suspended'.freeze # Opgeschort
    EXPIRED   = 'expired'.freeze   # Vervallen
    REMOVED   = 'removed'.freeze   # Opgeheven
  end

  LEVELS = { 9 => 'recover_mobiel', 10 => 'basis', 20 => 'midden' }

  has_many :sectorcodes,                         :dependent => :destroy
  has_one  :distribution_entity,                 :dependent => :destroy
  has_one  :email
  has_one  :password_tool,               dependent: :destroy
  has_many :sms_tools,                  dependent: :destroy, class_name: 'Authenticators::SmsTool'
  has_many :app_authenticators,         dependent: :destroy, class_name: 'Authenticators::AppAuthenticator'

  scope :no_sms, -> {joins("LEFT OUTER JOIN `sms_tools` ON `accounts`.`id` = `sms_tools`.`account_id`").where("sms_tools.account_id" => nil)}
  scope :period, -> (period) {where("accounts.created_at < ?", period.end_ts.to_date)}
  scope :email_not_verified, -> {joins(:email).where("emails.status" => Email::Status::NOT_VERIFIED)}
  scope :email_verified, -> {joins(:email).where("emails.status" => Email::Status::VERIFIED)}
  scope :active, -> {where(status: Status::ACTIVE)}

  scope :ww, -> {joins(:password_tool)}
  scope :sms, -> {joins(:sms_tools)}
  scope :no_ww, -> {joins("LEFT OUTER JOIN `password_tools` ON `accounts`.`id` = `password_tools`.`account_id`").where("password_tools.account_id" => nil)}
  scope :no_app, -> {joins("LEFT OUTER JOIN `app_authenticators` ON `accounts`.`id` = `app_authenticators`.`account_id`").where("app_authenticators.account_id" => nil)}
  scope :only_midden_app, -> {Arel::Table.new('only_midden')
Authenticators::AppAuthenticator.select(Arel.star).from(
  Authenticators::AppAuthenticator.select("`app_authenticators`.`account_id` AS ac, MAX(`app_authenticators`.`substantieel_activated_at`) AS only_midden").where(
    Authenticators::AppAuthenticator.arel_table[:status].eq('active')
  ).having("only_midden IS NULL").group(Authenticators::AppAuthenticator.arel_table[:account_id]).as('only_midden')
)}
  scope :at_least_substantial, -> {at_least_sub = Arel::Table.new('at_least_sub')
Authenticators::AppAuthenticator.select(Arel.star).from(
  Authenticators::AppAuthenticator.select("`app_authenticators`.`account_id` AS ac, MAX(`app_authenticators`.`substantieel_activated_at`) AS at_least_sub"
    ).where(
    Authenticators::AppAuthenticator.arel_table[:status].eq('active')
  ).having("at_least_sub IS NOT NULL").group(Authenticators::AppAuthenticator.arel_table[:account_id]).as('at_least_sub')
)}
  scope :ww_no_sms_no_app, -> (period) {active.period(period).ww.no_sms.no_app}
  scope :ww_sms_no_app, -> (period) {active.period(period).ww.sms.no_app}
  scope :ww_sms_only_midden_app, -> (period) {active.period(period).ww.sms.where("`accounts`.`id` IN (?)", only_midden_app.pluck(:ac))}
  scope :ww_no_sms_only_midden_app, -> (period) {active.period(period).ww.no_sms.where("`accounts`.`id` IN (?)", only_midden_app.pluck(:ac))}
  scope :no_ww_no_sms_only_midden_app, -> (period) {active.period(period).no_ww.no_sms.where("`accounts`.`id` IN (?)", only_midden_app.pluck(:ac))}
  scope :ww_sms_at_least_substantial, -> (period) {active.period(period).ww.sms.where("`accounts`.`id` IN (?)", at_least_substantial.pluck(:ac))}
  scope :ww_no_sms_at_least_substantial, -> (period) {active.period(period).ww.no_sms.where("`accounts`.`id` IN (?)", at_least_substantial.pluck(:ac))}
  scope :no_ww_no_sms_at_least_substantial, -> (period) {active.period(period).no_ww.no_sms.where("`accounts`.`id` IN (?)", at_least_substantial.pluck(:ac))}

  def gebruikersnaam
    password_tool&.username
  end

  def gebruikersnaam=(username)
    create_password_tool created_at: Time.zone.now, updated_at: Time.zone.now unless password_tool
    password_tool.username = username
  end

  def via_balie?
    distribution_entity.present?
  end

  def phone_number
    sms_tools.active.first.try(:phone_number)
  end

  def self.destroy_all
    fail 'Please do not use, see Account.delete_with_associations'
  end
end
