
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

class Log < ActiveRecord::Base
  # Set here the connection, so it is used in all subclasses
  if APP_CONFIG['log_read_separated']
    establish_connection "#{Rails.env}_read".to_sym
    self.primary_key = :id
  end

  belongs_to :account
  belongs_to :webservice
  belongs_to :manager
  belongs_to :subject, polymorphic: true

  class SubjectTypes
    ACCOUNT                   = 'Account'.freeze
    ADMIN_REPORT              = 'AdminReport'.freeze
    APP_VERSION               = 'AppVersion'.freeze
    THIRD_PARTY_APP           = 'ThirdPartyApp'.freeze
    BLACKLISTED_PHONE_NUMBER  = 'BlacklistedPhoneNumber'.freeze
    BULK_REMOVAL              = 'BulkOrder'.freeze
    FRONT_DESK                = 'FrontDesk'.freeze
    MANAGER                   = 'Manager'.freeze
    NATIONALITY               = 'Nationality'.freeze
    NEWS_ITEM                 = 'NewsItem'.freeze
    ORGANIZATION              = 'Organization'.freeze
    PILOT_GROUP               = 'PilotGroup'.freeze
    ROLE                      = 'Role'.freeze
    SECTOR                    = 'Sector'.freeze
    SSO_DOMAIN                = 'SsoDomain'.freeze
    SWITCH                    = 'Switch'.freeze
    WEBSERVICE                = 'Webservice'.freeze
    KIOSK                     = 'Kiosk'.freeze
    DC_CONNECTION             = 'Dc::Connection'.freeze
    DC_ORGANIZATION           = 'Dc::Organization'.freeze
    DC_SERVICE                = 'Dc::Service'.freeze
    WHITELISTED_PHONE_NUMBER  = "WhitelistedPhoneNumber".freeze
  end

  scope :not_manager, -> { where(manager_id: nil) }
  scope :transactions, -> (transaction_id) { where(transaction_id: transaction_id).includes(:webservice, :account) }

  # Subject actions
  scope :account_actions, -> (sector_numbers) { not_manager.where(sector_number: sector_numbers) }
  scope :account_actions_with_code, -> (sector_numbers, code) { account_actions(sector_numbers).where(code: code) }
  scope :actions_with_subject_id_and_code, -> (code, subject_type, subject_id) { subject_actions_with_subject_id(subject_type, subject_id).where(code: code) }
  scope :bulk_order_actions, -> { where(manager_id: -1).where(subject_type: Log::SubjectTypes::BULK_REMOVAL) }
  scope :front_desk_actions, -> (front_desk_id) { where(subject_id: front_desk_id).where(subject_type: Log::SubjectTypes::FRONT_DESK).where.not(pseudoniem: nil) }
  scope :dc_service_actions, -> (dc_service_id) { where(subject_id: dc_service_id).where(subject_type: Log::SubjectTypes::DC_SERVICE) }
  scope :manager_actions, -> (manager_id) { where(manager_id: manager_id) }
  scope :subject_actions, -> (subject_type) { where(subject_type: subject_type) }
  scope :subject_actions_with_subject_id, -> (subject_type, subject_id) { subject_actions(subject_type).where(subject_id: subject_id) }
  scope :webservice_actions, -> (webservice_id) { not_manager.where(webservice_id: webservice_id) }

  def self.manager_actions_with_subject_id(subject_type, subject_id)
    scope = where('manager_id IS NOT NULL AND pseudoniem IS NULL')
    if %w(Account Webservice).include?(subject_type)
      scope.where("#{subject_type.downcase}_id" => subject_id)
    else
      scope.where(subject_type: subject_type, subject_id: subject_id)
    end
  end

  def self.manager_actions_for_subject(subject_type)
    scope = where('manager_id IS NOT NULL AND manager_id != -1 AND pseudoniem IS NULL')
    if %w(Account Webservice).include?(subject_type)
      scope.where("#{subject_type.downcase}_id IS NOT NULL")
    else
      scope.where(subject_type: subject_type)
    end
  end

  def self.instrument(name, payload = {}, &block)
    ActiveSupport::Notifications.instrument("digid_admin.#{name}", payload, &block)
  end

  def self.to_csv
    result = ["\xEF\xBB\xBFsep=,"]
    result << CSV.generate_line(["Tijdstip", "Omschrijving", "Ip adres", "Webdienst"], encoding: 'UTF-8', row_sep: nil)
    find_each {|l| result << CSV.generate_line([l.created_at, l.name, l.ip_address, l.webservice&.name ], encoding: 'UTF-8', row_sep: nil) }
    result.join("\n")
  end

  def readonly?
    true
  end
end
