
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

module Authenticators
  class AppAuthenticator < ActiveRecord::Base
    include HasBlockingManager
    include NsClient

    module Status
      ACTIVE    = "active"
      INACTIVE  = "inactive"
      PENDING   = "pending"
    end

    include Activatable

    belongs_to :account
    has_many :attempts, -> { where(attempt_type: "activation") }, as: :attemptable, dependent: :destroy

    # Internals
    validates :user_app_id, uniqueness: { case_sensitive: true }, presence: true

    # User defined
    validates :device_name, presence: true, if: -> { state.active? }

    validate :valid_pip_and_signature?, if: -> { signature_of_pip != nil }

    default_value_for :status, "inactive"
    default_value_for(:user_app_id) { SecureRandom.uuid }

    scope :most_recent, -> { order(:created_at).limit(1) }
    scope :activated_by_letter, -> { where(issuer_type: ["letter", "letter_international", "letter_secure_delivery", "letter_personal_delivery", "gemeentebalie"]) }
    scope :expired, lambda {
                      pending.where("`geldigheidstermijn` IS NULL AND DATEDIFF(?, `created_at`) > ?", Time.zone.now, ::Configuration.get_int("geldigheid_brief"))
                             .or(pending.where("DATEDIFF(?, `updated_at`) > `geldigheidstermijn`", Time.zone.now))
                    }
    scope :ordered, -> { order(activated_at: :desc) }
    scope :active_or_pending, -> { where(status: [Status::ACTIVE, Status::PENDING]) }

    scope :least_recent, -> { active.order(:last_sign_in_at).first }

    delegate :active?, to: :state
    delegate :pending?, to: :state

    def app_code
      read_attribute(:instance_id).[](0..5)&.upcase if read_attribute(:instance_id)
    end

    def device_name
      read_attribute(:device_name).truncate(35) if read_attribute(:device_name)
    end

    def self.clean_up
      delete_all.tap do
        Attempt.clean_up_missing(self)
      end
    end

    def max_number_of_failed_attempts
      ::Configuration.get_int("pogingen_activationcode_app")
    end

    def canonical_type
      self.class.name.demodulize.underscore
    end

    def activate!
      activate_with(type: :sms_controle)
    end

    def activate_with_password!
      activate_with(type: :password)
    end

    def activate_with_substantieel!
      activate_with(type: :rda)
    end

    def activate_with_activation_code!(destroy_pending = false)
      activate_with(type: :activation_code, destroy_pending: destroy_pending)
    end

    def activate_with_app!
      activate_with(type: issuer_type) # activate with current issuer_type
    end

    def mark_as_pending!(destroy_pending = false)
      account.blocking_manager_app.reset!
      transaction do
        self.status = Status::PENDING
        self.requested_at = Time.zone.now
        save!
      end

      self.class.pending.where(account_id: account_id).where.not(id: id).destroy_all if destroy_pending
    end

    def substantieel?
      !substantieel_activated_at.nil?
    end

    def hoog?
      !wid_activated_at.nil?
    end

    def valid_pip_and_signature?
      unless validate_pip_signature(signature: self.signature_of_pip)
        errors.add(:valid_pip_and_signature, "signature_of_pip is not valid")
      end
    end

    def self.substantieel?
      where("substantieel_activated_at IS NOT NULL").any?
    end

    def authentication_level
      return Account::LoginLevel::SMARTCARDPKI if hoog?
      return Account::LoginLevel::SMARTCARD if substantieel?

      Account::LoginLevel::TWO_FACTOR
    end

    def substantieel_activated_at
      self[:substantieel_activated_at] ||  self[:wid_activated_at]
    end

    def substantieel_document_type
      self[:substantieel_document_type] ||  self[:wid_document_type]
    end

    private

    def validate_pip_signature(signature:)
      false
    end

    def max_amount_of_apps
      @max_amount_of_apps ||= ::Configuration.get_int("Maximum_aantal_DigiD_apps_eindgebruiker")
    end

    def activate_with(type:, destroy_pending: false)
      account.blocking_manager_app.reset!
      transaction do
        self.status = Status::ACTIVE
        self.activated_at = Time.zone.now
        if type == :activation_code
          self.activation_code = nil
          self.geldigheidstermijn = nil
        else
          self.requested_at = Time.zone.now
          self.issuer_type = type.to_s
        end

        if account.app_authenticators.active.count >= max_amount_of_apps
          app = account.app_authenticators.least_recent
          ns_client.deregister_app(app)
          Log.instrument("1449", account_id: account_id, app_code: app.app_code, device_name: app.device_name, last_sign_in_at: I18n.l((app.last_sign_in_at || app.created_at).to_date))
          app.destroy
        end

        account.app_authenticators.pending.where.not(id: id).destroy_all if destroy_pending

        save!
      end
    end
  end
end
