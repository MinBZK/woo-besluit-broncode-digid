
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

class GbaPresenter < GbaBasePresenter
  delegate :aanduiding_bij_huisnummer, :aanduiding_naamgebruik, :a_nummer, :bsn,
           :geslachtsnaam, :geslachtsnaam_partner, :huisletter, :huisnummer,
           :huisnummertoevoeging, :locatieomschrijving, :postcode, :straatnaam,
           :voornamen, :voorvoegsel_geslachtsnaam, :voorvoegsel_geslachtsnaam_partner,
           to: :gba_data

  def csv
    @csv ||= begin
      LoadCsv.new(APP_CONFIG["csv_codes_file"]&.path).tap do |csv|
        csv.fetch
      end
    end
  end

  def adellijke_titel
    Account::GBA_TITLE_MAPPINGS[gba_data.adellijke_titel&.to_sym]
  end

  # render a pretty birthdate, "00000000" means no known birthdate
  def geboortedatum
    text = format_date(gba_data.geboortedatum, I18n.t('unknown').downcase)
    gba_data.onderzoek_geboortedatum? ? "#{text} <span>#{I18n.t('under_investigation')}</span>".html_safe : text
  end

  def gemeente_van_inschrijving
    if gba_data.woonplaats.blank?
      csv.codes(gba_data.gemeente_van_inschrijving)
    else
      gba_data.woonplaats
    end
  end

  def geslachtsaanduiding
    Account::GBA_GENDER_MAPPINGS[gba_data.geslachtsaanduiding&.to_sym]
  end

  def nationaliteit
    return I18n.t('not_available') if gba_data.nationaliteiten.empty?
    ''.html_safe.tap do |value|
      gba_data.nationaliteiten.select(&:nationaliteit).sort_by(&:nationaliteit).each_with_index do |n, i|
        text = Account::GBA_NATIONALITY_MAPPINGS[n.nationaliteit&.to_sym] || n.nationaliteit
        value << ', ' if i > 0
        value << (n.onderzoek? ? "#{text}<span>/#{I18n.t('under_investigation')}</span>".html_safe : text)
      end
    end
  end

  def status
    return unless can? :view_gba_status, Account
    if %w(not_found investigate suspended_error suspended_unknown error).include?(gba_data.status)
      "<span>#{Account::GBA_STATUS_MAPPINGS[gba_data.status&.to_sym]}</span>".html_safe
    else
      Account::GBA_STATUS_MAPPINGS[gba_data.status&.to_sym]
    end
  end

  def reisdocumenten
    @reisdocumenten ||= gba_data.reisdocumenten.select { |doc| doc.vervaldatum && doc.vervaldatum > 3.months.ago }.map { |doc| GbaTravelDocumentPresenter.new(doc, @template) }
  end
end
