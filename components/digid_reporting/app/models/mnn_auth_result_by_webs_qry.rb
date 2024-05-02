
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

include ApplicationHelper

class MnnAuthResultByWebsQry  < MnnBaseQry

  def initialize(type:, webservice:)
    super(name: "MnnAuthResultByWebsQry_#{RESULT_TYPES[type]}_#{webservice.name}", type: type, filter_by_webservices: true)
    @webservice_ids = [webservice.id]
  end

  def self.get_types
    return [:success, :failure]
  end

  def get_success_codes
    codes_success = [
      "uc2.authenticeren_basis_gelukt",
      "uc2.authenticeren_midden_gelukt",
      "uc2.sso_authenticatie_gelukt"
    ]
  end

  def get_failure_codes
    codes_failure = [
    "uc2.authenticeren_mislukt_niet_ingevuld",
    "uc2.authenticeren_mislukt_geblokkeerd",
    "uc2.authenticeren_mislukt_geen_match",
    "uc2.authenticeren_mislukt_wachtwoord",
    "uc2.authenticeren_mislukt_sms_te_snel",
    "uc2.authenticeren_mislukt_afgebroken",
    "uc2.authenticeren_mislukt_sessie_verlopen",
    "uc2.authenticeren_mislukt_geen_midden",
    "uc2.authenticeren_mislukt_geannuleerd"
    ]
  end
end
