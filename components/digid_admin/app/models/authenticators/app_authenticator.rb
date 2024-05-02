
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

module Authenticators
  class AppAuthenticator < AccountBase
    include Rails.application.routes.url_helpers

    module Status
      ACTIVE    = 'active'.freeze
      INACTIVE  = 'inactive'.freeze
      PENDING   = 'pending'.freeze
    end

    default_value_for(:user_app_id) { SecureRandom.uuid }

    belongs_to :account

    include Stateful

    delegate :active?, to: :state
    delegate :pending?, to: :state
    has_many :attempts,
             -> { where attempt_type: 'login_app' },
             as: :attemptable,
             dependent: :destroy
    scope :midden, -> { where(substantieel_activated_at: nil) }
    scope :substantieel, -> { where.not(substantieel_activated_at: nil) }
    scope :ordered, -> { order(activated_at: :desc) }

    def substantieel?
      !substantieel_activated_at.nil?
    end

    def authentication_level
      substantieel? ? Account::LoginLevel::SMARTCARD : Account::LoginLevel::TWO_FACTOR
    end

    def human_status
      I18n.t(status, scope: 'accounts.app_authenticator.status')
    end
  end
end
