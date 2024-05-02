
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

password = %w(test development).include?(Rails.env) ? "development" : (Rails.application.secrets.private_key_password || ENV["password"] || ARGV.first)

saml_private_key_data = APP_CONFIG["saml_signing_cert_key_file"]&.read
ssl_private_key_data = APP_CONFIG["saml_ssl_cert_key_file"]&.read

saml_private_key = OpenSSL::PKey::RSA.new(saml_private_key_data, password)
ssl_private_key = OpenSSL::PKey::RSA.new(ssl_private_key_data, password)

$redis ||= Redis.new(APP_CONFIG["dot_environment"] ? DigidUtils.redis_options.merge(logger: Rails.logger) : DigidUtils.redis_options)
$redis.set("saml_private_key", saml_private_key.to_pem)
$redis.set("ssl_private_key", ssl_private_key.to_pem)
