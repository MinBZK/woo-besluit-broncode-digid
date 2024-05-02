
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

# model for authentication means (middel) "password tool"
module Authenticators
  class Password < ActiveRecord::Base
    module Status
      ACTIVE    = "active"
      BLOCKED   = "blocked" # blocked - strictly for aanvragen via balie, which are useless as long as they are not printed
      PENDING   = "pending"
      INITIAL   = "initial"
    end

    include Activatable
    self.table_name = "password_tools"

    attr_accessor :current_password, :password

    validates :username, presence: true
    validates :username, length: { in: 6..32 }, if: :username_changed?
    validates :username, format: { with: Regexp.only(CharacterClass::USERNAME) }
    validate :username_uniek?, if: :username_changed?

    has_many :attempts, -> { where(attempt_type: "activation") }, as: :attemptable, dependent: :destroy
    belongs_to :account

    default_value_for :status, Status::INITIAL

    validates :password, associated_confirmation: true
    validate :password_valid, if: -> { account.present? && password.present? }

    before_save :encrypt_password, if: -> { password.present? }
    delegate :active?, to: :state
    scope :not_me, ->(id) { where("id != ?", id) if id }

    def check_policy(password:)
      return self.policy if self.policy > 0

      pw_check = PasswordCheck.new username: username, password: password
      if pw_check.valid?
        self.update(policy: PasswordCheck::POLICY)
      else
        self.update(policy: 1)
      end

      self.policy
    end

    def max_number_of_failed_attempts
      ::Configuration.get_int("pogingen_activationcode")
    end

    def encrypt_password
      self.encrypted_password, self.password_salt = DigidUtils::Crypto::Password.create(password)
      self.password = nil
      self.password_confirmation = nil
    end

    def verify_password(password)
      DigidUtils::Crypto::Password.verify(password, self.encrypted_password, self.password_salt)
    end

    def change_password(password, confirmation)
      # TODO: Investigate why this is necessary and why it is a save operation
      # update(:attempts => 0)
      self.encrypted_password    = nil
      self.password              = password
      self.password_confirmation = confirmation
      self.policy                = PasswordCheck::POLICY
      save
    end

    def password_valid
      check = PasswordCheck.new username: username, password: password
      errors.add(:password, check.errors[:password]) if check.invalid?
    end

    def seed_old_passwords(password)
      # is this an old DigiD account?
      return unless password_salt.nil? || password_salt.length < 32

      # fire before_save filter and encrypts password with a salt
      update(password: password)
    end

    # Validate the uniqueness of the submitted username. Generates 3 alternative
    # usernames when invalid.
    def username_uniek?
      accounts = Authenticators::Password.not_me(id).where("username = ?", username)
      ghosts = UserGhost.where(gebruikersnaam: username).where("blocked_till >= ?", Time.zone.now)

      return unless (accounts.count + ghosts.count) > 0


      suggestions = []
      unless username.blank?
        3.times do
          suggestions << create_non_existing_username(username)
        end
      end

      errors.add(:username, :unique, suggestions: suggestions.join("; "))
    end

    # Helper function for create_possible_usernames. Creates one random
    # non-existing username/
    def create_non_existing_username(seed_name)
      scheme = (SecureRandom.random_number * 3).floor
      suggestion = case scheme
                   when 0
                     seed_name + possible_digits + possible_digits
                   when 1
                     seed_name + possible_chars + possible_digits + possible_digits
                   else
                     seed_name + possible_digits + possible_digits + possible_chars
                   end

      # Create another one when it already exists
      if Authenticators::Password.where(username: suggestion).count > 0
        create_non_existing_username(seed_name)
      else
        suggestion
      end
    end

    # return a random special characted in the range !@\#$%^&*()-_=+,.
    def possible_chars
      "!@\#$%^&*()-_=+,.".at((SecureRandom.random_number * 16).floor)
    end

    # returns a random digit between 0 and 9
    def possible_digits
      "1234567890".at((SecureRandom.random_number * 10).floor)
    end


    # add fields

    # - password (attr)
    # - policy (depr?)
    # - weak_password_skip_count (depr?)
    # - encrypted_password
    # - password_salt

  end
end
