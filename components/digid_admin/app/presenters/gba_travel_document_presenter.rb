
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

class GbaTravelDocumentPresenter < GbaBasePresenter
  presents :gba_data

  delegate :nummer_reisdocument, to: :gba_data

  def soort_reisdocument
    text = Account::GBA_TRAVEL_DOCUMENT_TYPE_MAPPINGS[gba_data.soort_reisdocument&.to_sym] || "#{I18n.t('unknown')} (#{gba_data.soort_reisdocument})"
    gba_data.onderzoek? ? "#{text} <span>#{I18n.t('under_investigation')}</span>".html_safe : text
  end

  def datum_einde_geldigheid
    format_date(gba_data.datum_einde_geldigheid_reisdocument)
  end

  def datum_inhouding_vermissing
    format_date(gba_data.datum_inhouding_vermissing_reisdocument)
  end

  def aanduiding_inhouding_vermissing
    return nil unless gba_data.aanduiding_inhouding_vermissing_reisdocument
    Account::GBA_TRAVEL_DOCUMENT_STATE_MAPPINGS[gba_data.aanduiding_inhouding_vermissing_reisdocument.to_sym] || "#{I18n.t('unknown')} (#{gba_data.aanduiding_inhouding_vermissing_reisdocument})"
  end
end
