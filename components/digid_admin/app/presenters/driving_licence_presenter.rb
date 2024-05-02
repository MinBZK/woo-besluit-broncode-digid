
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

class DrivingLicencePresenter < BasePresenter
  presents :driving_licence

  def eid_middel
    return "Rijbewijs" if not_issued?
    if driving_licence.status
      color = driving_licence.active? ? "green" : "red"
      "<p class=\"#{color}\">Rijbewijs</p>".html_safe
    end
  end

  def status
    if driving_licence.nil?
      return admin_status = t('niet_uitgegeven', scope: [:accounts, :driving_licence])
    elsif not_issued?
      admin_status = t('niet_uitgegeven', scope: [:accounts, :driving_licence])
    elsif driving_licence.status == "Actief" && driving_licence.status_mu == "Inactief"
      admin_status = t('actief', scope: [:accounts, :driving_licence])
    elsif driving_licence.status == "Uitgereikt" && ["Actief", "Inactief"].include?(driving_licence.status_mu)
      admin_status = t('uitgereikt', scope: [:accounts, :driving_licence])
    else
      admin_status = t(driving_licence.human_status, scope: [:accounts, :driving_licence])
    end

    if hide_rdw_suspension?
      suffix = ""
    elsif rdw_suspended?
      suffix = " - geschorst"
    end
    "#{admin_status}#{suffix}"
  end

  def hide_rdw_suspension?
    ["Gerevoceerd", "Inactief"].include?(driving_licence.status)
  end

  def rdw_suspended?
    driving_licence.status_mu == "Inactief"
  end

  def status_bron
    return t("status_bron.rdw") if not_issued? || driving_licence.status_bron.blank?
    t("status_bron.#{driving_licence.status_bron.parameterize.underscore}")
  end

  def laatste_statuswijziging
    time_helper driving_licence.last_updated_at, :slash
  end

  def documentnummer
    driving_licence.document_no
  end

  def opmerking
    if driving_licence.note
      html = driving_licence.note[0..25]
      if driving_licence.note.size > 25
        html << " ... " + link_to("...", "", class: 'note-button')
        html << "<div class=\"note\" style=\"display:none;\">#{driving_licence.note}</div>"
      end
      html.html_safe
    end
  end

   def eid_toeslag
    driving_licence.eidtoeslag
  end

  def not_issued?
    driving_licence.nil? || (driving_licence.status.blank? && driving_licence.status_mu.blank?)
  end

  def type
    html = "<strong>"
    html << t('type', scope: [:accounts, :driving_licence])
    html << "</strong>"
    html.html_safe
  end

  def colored_status
    if @object.active?
      strong(green(status.humanize))
    else
      strong(red(t(status, scope: [:accounts, :driving_licence])))
    end
  end
end
