
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

class Manager < AccountBase
  include FourEyes
  SESSION_TIMEOUT = 15.minutes.to_i

  attr_accessor :remove_certificate, :added_roles, :removed_roles

  has_many :logs
  has_and_belongs_to_many :roles, -> { order(:name) }, validate: false, after_add: :role_added, after_remove: :role_removed # rubocop:disable Rails/HasAndBelongsToMany
  has_many :alarm_notifications_managers
  has_many :alarm_notifications, through: :alarm_notifications_managers

  default_scope { order(:account_name) }
  scope :superusers, -> { where(superuser: true) }
  scope :active, -> { where(active: true) }
  scope :with_notify, -> { where('(notify_email = 1 AND email IS NOT NULL) OR (notify_sms = 1 AND mobile_number IS NOT NULL)') }
  scope :to_be_notified_by_sms, -> { where('notify_sms = 1 AND mobile_number IS NOT NULL') }
  scope :to_be_notified_by_email, -> { where('notify_email = 1 AND email IS NOT NULL') }

  validates :account_name, presence: true, uniqueness: { case_sensitive: true }
  validates :distinguished_name, uniqueness: { case_sensitive: true }, allow_blank: true
  validates :mobile_number, format: { with: /\A\d{8}\z/, message: :eight_digits }
  validates :name, presence: true
  validates :surname, presence: true

  validates :email, allow_blank: true,
                    format: { with: Regexp.only(CharacterClass::EMAIL, ignore_case: true) },
                    length: { maximum: 254 }

  validates :email, allow_blank: true,
                    format: { with: Regexp.only(CharacterClass::EMAIL_LEN_TO_AT_SIGN, ignore_case: true) }

  validate :check_certificate
  validate :at_least_one_superuser_assertion
  validate :notify_via_present?

  freeze_under_review :roles

  after_validation :modify_error_messages

  before_update :update_inactive_at
  before_update :check_certificate_removal

  serialize :last_accounts
  serialize :last_webservices

  def self.frozen_ids
    ids = []
    Role.get_reviews.each do |review|
      ids += (review.original.manager_ids + review.updated.manager_ids).map(&:to_i)
    end
    ids.uniq
  end

  def frozen_for_review?
    self.class.frozen_ids.include?(id)
  end

  def roles_updated_since?(updating_started_at)
    latest_role_updated_at = roles.pluck(:four_eyes_updated_at).compact.max
    latest_role_updated_at >= updating_started_at if latest_role_updated_at && updating_started_at
  end

  def to_s
    name
  end

  # Consolidate email error messages

  def modify_error_messages
    return unless errors.include?(:email)
    errors.delete(:email)
    errors.add(:email, :invalid)
  end

  def account_viewed(account)
    self.last_accounts = [] unless last_accounts.is_a? Array
    last_accounts.delete account.id if last_accounts.include?(account.id)
    last_accounts.unshift(account.id)
    last_accounts.slice!(9, last_accounts.count - 10) if last_accounts.count > 10
    update_attribute(:last_accounts, last_accounts)
  end

  def webservice_viewed(webservice)
    self.last_webservices = [] unless last_webservices.is_a? Array
    last_webservices.delete webservice.id if last_webservices.include?(webservice.id)
    last_webservices.unshift(webservice.id)
    last_webservices.slice!(9, last_webservices.count - 10) if last_webservices.count > 10
    update_attribute(:last_webservices, last_webservices)
  end

  def last_viewed_accounts
    accounts = Account.where(id: last_accounts).to_a
    return [] unless last_accounts
    last_accounts.map do |account_id|
      accounts.find { |a| a.id == account_id }
    end.compact
  end

  def last_viewed_webservices
    webservices = Webservice.where(id: last_webservices).to_a
    return [] unless last_webservices
    last_webservices.map do |webservice_id|
      webservices.find { |w| w.id == webservice_id }
    end.compact
  end

  def certificate=(pem)
    if pem && pem.respond_to?(:read)
      pem.tempfile.seek(0) # This ensure we can read it multiple times
      pem = pem.read
    end

    cert = OpenSSL::X509::Certificate.new(pem)
    if Certificate.verify_ca(cert, APP_CONFIG["digidentity_ca_file"]&.path)
      self.distinguished_name = cert.subject.to_s(OpenSSL::X509::Name::RFC2253)
      self.fingerprint = OpenSSL::Digest::SHA1.new(cert.to_der).to_s.scan(/../).map{ |s| s.upcase }.join(":")
    else
      self.distinguished_name = 'NOT_VALID'
      self.fingerprint = 'NOT_VALID'
    end
  rescue
    self.distinguished_name = 'NOT_VALID'
    self.fingerprint = 'NOT_VALID'
  end

  def full_name
    "#{name} #{surname}"
  end

  def allowed_notifications
    AlarmNotification.all.select do |alarm_notification|
      alarm_notification.allowed_for?(self)
    end
  end

  def subscribed_notifications
    alarm_notifications.select do |alarm_notification|
      alarm_notification.allowed_for?(self)
    end
  end

  def self.email_subscribers_for(key)
    notification = AlarmNotification.find_by_report_type(key)
    return [] if notification.nil?
    notification.managers_to_be_notified_by_email
  end

  def self.sms_subscribers_for(key)
    notification = AlarmNotification.find_by_report_type(key)
    return [] if notification.nil?
    notification.managers_to_be_notified_by_sms
  end

  def sanitized_params=(params)
    if params.try(:[], :remove_certificate)
      if params[:remove_certificate] == "1"
        params[:distinguished_name] = nil
        params[:fingerprint] = nil
      end
    end

    if params.try(:[], :certificate)
      if params[:certificate].respond_to?(:read)
        params[:certificate].rewind # ensure the file read pointer is at the beginning of the file
        params[:certificate] = params[:certificate].read
      end
    end
    @sanitized_params = params
  end

  private

  # store modified roles to log after save
  def role_added(role)
    self.added_roles ||= []
    self.added_roles.push(role)
  end

  def role_removed(role)
    self.removed_roles ||= []
    self.removed_roles.push(role)
  end

  def notify_via_present?
    return if notify_sms.present? || notify_email.present?
    return if alarm_notifications.empty?
    errors.add(:notify_via, I18n.t('activerecord.errors.messages.empty'))
  end

  def check_certificate
    return unless distinguished_name == 'NOT_VALID'
    errors.add(:certificate, I18n.t('certificates.ca_verification_failed'))
    self.distinguished_name = nil
    self.fingerprint = nil
  end

  def check_certificate_removal
    return unless remove_certificate == "1"
    self.distinguished_name = nil
    self.fingerprint = nil
  end

  def update_inactive_at
    self.inactive_at = (active? ? nil : DateTime.current) if active_changed?
  end

  def at_least_one_superuser_assertion
    return unless Manager.superusers.count == 1
    errors.add(:superuser, :at_least_one_superuser_assertion) if superuser_changed? && superuser == false
  end
end
