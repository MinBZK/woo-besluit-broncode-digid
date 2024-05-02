
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

module EidasUitConcern
  extend ActiveSupport::Concern

  def eidas_uit?
    session[:authentication] && (eidas_oin.eql? session[:authentication][:ad_entity_id]&.split(':')&.fetch(4))
  end

  def app_session_eidas_uit?
    redis.hget(app_session_key, "eidas_uit") == "true"
  end

  def eidas_oin
    APP_CONFIG["eidas_oin"].to_s.rjust(20, '0')
  end

  def brp_oin
    APP_CONFIG["brp_oin"].to_s.rjust(20, '0')
  end

  def transforms_for_eidas_uit(pip:)
    transforms = hsm_client.transform_multiple(
      polymorph: pip,
      requests: {
        eidas_oin => { ksv: APP_CONFIG["eidas_ksv"], identity: false, pseudonym: true, includeLinks: true },
        brp_oin => { ksv: APP_CONFIG["brp_ksv"], identity: true, pseudonym: false, includeLinks: false }
      },
      version: APP_CONFIG["encrypted_structure_version"]
    )

    return {
      polymorph_pseudonym: transforms[eidas_oin]["pseudonym"],
      polymorph_identity: transforms[brp_oin]["identity"]
    }
  end
end
