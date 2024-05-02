
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

module Aselect
  class Webservice < ActiveRecord::Base
    belongs_to :webservice, class_name: "::Webservice"
    has_many :sessions
    has_many :shared_secrets, class_name: "Aselect::SharedSecret", foreign_key: "aselect_webservice_id"

    # Returns true if a webservice is active
    def inactive?
      !(webservice.active? && webservice.authentication_method == ::Webservice::Authentications::ASELECT)
    end

    def app_url_valid?(url)
      webservice.redirect_url_valid?(url)
    end

    def self.checked_webservice_by_shared_secret(shared_secret)
      Rails.cache.fetch("checked_webservice_by_shared_secret-#{shared_secret}", expires_in: 1.minute) do
        if shared_secret.present?
          shared_secret         = shared_secret.split("-").last(6).join("-")
          aselect_shared_secret = Aselect::SharedSecret.where(shared_secret: Digest::SHA1.hexdigest(shared_secret).upcase).includes(:webservice).first
          aselect_shared_secret.webservice if aselect_shared_secret
        end
      end
    end

    def self.checked_webservice_by_distinguished_name(distinguished_name)
      Rails.cache.fetch("checked_webservice_by_distinguished_name-#{distinguished_name}", expires_in: 1.minute) do
        certificate = ::Certificate.find_by(distinguished_name: distinguished_name)
        Aselect::Webservice.find_by(webservice_id: certificate.webservice_id) if certificate
      end
    end

    def self.checked_webservice_by_fingerprint(fingerprint)
      Rails.cache.fetch("checked_webservice_by_fingerprint-#{fingerprint}", expires_in: 1.minute) do
        certificate = ::Certificate.find_by(fingerprint: fingerprint)
        Aselect::Webservice.find_by(webservice_id: certificate.webservice_id) if certificate
      end
    end
  end
end
