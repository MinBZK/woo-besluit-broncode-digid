
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

module DigidUtils
  class Hsm
    attr_reader :client

    def initialize(url:, timeout:)
      url += "/" unless url.end_with?("/")
      url += "iapi/" unless url.end_with?("iapi/")
      @client = Iapi::Client.new(url: url, timeout: timeout)
    end

    def polymorph_to_encrypted_identity(polymorph:, oin:, ksv:, version: 1)
      transform_single(polymorph: polymorph, oin: oin, ksv: ksv, pseudonym: false, version: version)["identity"]
    end

    def polymorph_to_encrypted_pseudonym(polymorph:, oin:, ksv:, version: 1)
      transform_single(polymorph: polymorph, oin: oin, ksv: ksv, identity: false, version: version)["pseudonym"]
    end

    def transform_single(polymorph:, oin:, ksv:, pseudonym: true, identity: true, version: 1)
      client.post("bsnk/transform/single", polymorph: polymorph, oin: oin, ksv: ksv,
                                           pseudonym: pseudonym, identity: identity,
                                           targetMsgVersion: version).result
    end

    def transform_multiple(polymorph:, requests:, version: 1)
      client.post("bsnk/transform/multiple", polymorph: polymorph, requests: requests, targetMsgVersion: version).result
    end

    def activate(bsn:, type: "PIP", signed: false, status_provider_oin: nil, status_provider_ksv: nil, activator: nil, authorized_party: nil)
      client.post("bsnk/activate", identifier: bsn, type: type, signed: signed,
                                   status_provider_oin: status_provider_oin,
                                   status_provider_ksv: status_provider_ksv,
                                   activator: activator,
                                   authorized_party: authorized_party).result["polymorph"]
    end

    def decrypt_keys(certificate:, closing_key_version: 1, pseudonym: true, identity: true)
      client.post("bsnk/service-provider-keys", certificate: certificate, closing_key_version: closing_key_version,
                                                pseudonym: pseudonym, identity: identity).result
    end
  end
end
