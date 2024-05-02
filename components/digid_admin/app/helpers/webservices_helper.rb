
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

module WebservicesHelper
  def web_service_status(object)
    if object.try(:active?) || object.try(:active)
      html = 'Actief'
      if object.active_from
        html << " <strong>van</strong> #{I18n.l(object.active_from, format: :default)}"
      end
      if object.active_until
        html << " <strong>tot</strong> #{I18n.l(object.active_until, format: :default)}"
      end
    else
      html = 'Inactief'
    end
    raw html
  end

  def web_service_redirect_url(webservice)
    if webservice.check_redirect_url?
      html = 'Actief'
      html << " <strong>Domein</strong> #{webservice.redirect_url_domain}"
    else
      html = 'Inactief'
    end
    raw html
  end

  def herkomst_helper(webservice_id)
    if webservice_id
      webservice = Webservice.find(webservice_id)
      return webservice.name if webservice
    else
      'DigiD'
    end
  end

  def not_after_helper(date_array)
    date_array.map { |not_after_date| I18n.l(not_after_date, format: :only_date) }.join(', ')
  end
end
