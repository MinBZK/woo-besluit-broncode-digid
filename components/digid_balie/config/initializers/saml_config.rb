
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

if ENV['ASSET_PRECOMPILE'].blank?
  Saml.setup do |config|
    provider_stores = Saml::ProviderStores::File.new(
      'config/metadata',
      APP_CONFIG.dig("eherkenning","encryption_key_file")&.path,
      Rails.application.secrets.private_key_password,
      APP_CONFIG.dig("eherkenning", "signing_key_file")&.path,
      Rails.application.secrets.private_key_password
    )

    APP_CONFIG['metadata'].presence.to_a.each do |(_, metadata)|
      provider_stores.add_metadata(metadata)
    end

    config.register_store :file, provider_stores, default: true

    config.ssl_certificate = OpenSSL::X509::Certificate.new File.read(APP_CONFIG.dig("eherkenning","signing_cert_file")&.path)
    config.ssl_private_key = OpenSSL::PKey::RSA.new(::File.read(APP_CONFIG.dig("eherkenning", "signing_key_file")&.path), Rails.application.secrets.private_key_password)
  end
end
