
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

class IdentityCardPresenter < BasePresenter

  presents :identity_card
  attr_reader :eid_toeslag, :opmerking

  def eid_middel
    return "Identiteitskaart" if not_issued?
    if identity_card.status
      color = identity_card.active? ? "green" : "red"
      "<p class=\"#{color}\">Identiteitskaart</p>".html_safe
    end
  end

  def status
    if identity_card.nil?
       return admin_status = t('niet_uitgegeven', scope: [:accounts, :identity_card])
    elsif not_issued?
      admin_status = t('niet_uitgegeven', scope: [:accounts, :identity_card])
    elsif identity_card.status == "Geactiveerd"
       admin_status = t('actief', scope: [:accounts, :identity_card])
    elsif identity_card.status == "Uitgereikt"
      admin_status = t('uitgereikt', scope: [:accounts, :identity_card])
    elsif identity_card.status == "Tijdelijk geblokkeerd"
      admin_status = t('tijdelijk_geblokkeerd', scope: [:accounts, :identity_card])
    else
      admin_status = t(identity_card.human_status, scope: [:accounts, :identity_card])
    end
  end

  def status_bron
    return t("status_bron.rvig") if not_issued? || identity_card.status_bron.blank?
    t("status_bron.#{identity_card.status_bron.parameterize.underscore}")
  end

  def laatste_statuswijziging
    time_helper identity_card.date_time_status, :slash
  end

  def documentnummer
    identity_card.document_no
  end

  def not_issued?
    identity_card.nil? || identity_card.status.blank?
  end

  def type
    html = "<strong>"
    html << t('type', scope: [:accounts, :identity_card])
    html << "</strong>"
    html.html_safe
  end

  def colored_status
    if @object.active?
      strong green @object.human_status.humanize
    elsif @object.status == "Tijdelijk geblokkeerd"
      strong red @object.status.humanize
    else
      strong red t(@object.status, scope: [:accounts, :identity_card])
    end
  end
end
