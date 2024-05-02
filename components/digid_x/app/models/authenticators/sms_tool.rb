
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
  class SmsTool < ActiveRecord::Base
    module Status
      ACTIVE    = "active"
      INACTIVE  = "inactive"
      PENDING   = "pending" # uitbreiding
    end

    include Activatable

    attr_accessor :current_phone_number, :in_registration_flow

    before_save :set_issuer_type

    belongs_to :account
    has_many :attempts, -> { where(attempt_type: "activation") }, as: :attemptable, dependent: :destroy
    delegate :active?, to: :state
    scope :expired, lambda {
                      pending.where("`geldigheidstermijn` IS NULL AND `issuer_type` IN ('front_desk', 'letter_international') AND DATEDIFF(?, `created_at`) > ?", Time.zone.now, ::Configuration.get_int("balie_default_geldigheidsduur_activatiecode"))
                             .or(pending.where("`geldigheidstermijn` IS NULL AND DATEDIFF(?, `created_at`) > ?", Time.zone.now, ::Configuration.get_int("geldigheid_brief")))
                             .or(pending.where("DATEDIFF(?, `updated_at`) > `geldigheidstermijn`", Time.zone.now))
                    }
    default_value_for :status, Status::PENDING
    default_value_for :gesproken_sms, false

    validates :gesproken_sms, inclusion: { in: [nil, true, false] }
    validates :phone_number, presence: { if: -> { phone_number_changed? } }
    validate :phone_number_maximum?
    validate :phone_number_valid?, if: -> { phone_number_changed? && errors["phone_number"].empty? }
    validate :changed_phone_number
    before_destroy :create_account_history

    def self.clean_up
      delete_all.tap do
        Attempt.clean_up_missing(self)
      end
    end

    def redirect_to_url(url_options)
      Rails.application.routes.url_helpers.authenticators_check_mobiel_url(url_options)
    end

    def canonical_type
      self.class.name.demodulize.underscore
    end

    def max_number_of_failed_attempts
      ::Configuration.get_int("pogingen_activationcode_sms")
    end

    def phone_number
      DigidUtils::PhoneNumber.normalize(super)
    end

    def phone_number=(phone_number)
      formatted_phone_number = DigidUtils::PhoneNumber.normalize(phone_number)
      if account
        account.sms_challenges.where(status: ::SmsChallenge::Status::PENDING).update(mobile_number: formatted_phone_number, status: ::SmsChallenge::Status::INVALID)
      end
      super(formatted_phone_number)
    end

    def user_friendly_phone_number
      phone_number.to_s.gsub(/^\+316/, "06").gsub(/^00/, "+")
    end

    private
    def create_account_history
      if active? && !destroyed_by_association
        account&.account_histories.create(mobiel_nummer: phone_number)
      end
    end

    def account
      @account = Account.find account_id if account_id
    end

    def set_issuer_type
      self.issuer_type = account.password_authenticator&.issuer_type if account && issuer_type.blank?
    end

    def phone_number_valid?
      number = phone_number
      return if number.blank?
      return if DigidUtils::PhoneNumber.valid?(number)

      if DigidUtils::PhoneNumber.dutch?(number) && !DigidUtils::PhoneNumber.blacklisted?(number)
        errors.add(:phone_number, I18n.t("activerecord.errors.models.sms_tool.attributes.phone_number.too_short"))
      else
        errors.add(:phone_number, I18n.t("activerecord.errors.models.sms_tool.attributes.phone_number.invalid"))
      end

      return if changes["phone_number"].first.blank?

      Log.instrument("151", account_id: account_id)
    end

    # validate the number of associated accounts with this phone_number.
    # it adds an error to 'errors' if something is wrong
    def phone_number_maximum?
      return if WhitelistedPhoneNumber.allowed_to_have_unlimited?(phone_number)

      account_ids = Account.where(status: [Account::Status::ACTIVE, Account::Status::REQUESTED]).where.not(id: account_id).joins(:sms_tools).where("phone_number = ?", phone_number.to_s).pluck(:id)

      return if account_ids.uniq.count < ::Configuration.get_int("telefoonnummer_maximum")

      errors.add(:phone_number, I18n.t("you_have_reached_the_mobile_numbers_maximum"))
      Log.instrument("152", account_id: account_id)
    end

    def changed_phone_number
      return unless current_phone_number.present? && current_phone_number == phone_number

      errors.add(:phone_number, I18n.t(in_registration_flow ? "this_phone_number_is_already_coupled_previous" : "this_phone_number_is_already_coupled"))
      Log.instrument("779", account_id: account_id)
    end
  end
end
