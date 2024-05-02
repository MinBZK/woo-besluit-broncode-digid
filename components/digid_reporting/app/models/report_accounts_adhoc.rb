
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

# The ad hoc report on accounts for use in fraud detection
class ReportAccountsAdhoc
  attr_reader :report_type, :job_id, :name, :manager_id

  def initialize(options)
    @report_type = options.fetch :report_type
    @job_id      = options.fetch :job_id
    @name        = options.fetch :report_name
    @manager_id  = options.fetch :manager_id
  end

  def report_name
    return @name unless @name.blank?

    "Accountgegevens ad hoc"
  end

  def report_type
    #@report_type
    ::AdminReport::Type::ADHOC_GBA
  end

  def periode_start
    # QQQ this report doesn't have periode_start / end. Avoid calling it from admin_report
  end

  def periode_end
    # QQQ this report doesn't have periode_start / end. Avoid calling it from admin_report
  end

  def report(_start_date=nil)
    csv_string = ""
    bsn_sector_id = Sector.get("bsn")
    csv_string = CSV.generate do |csv|
      csv << [
        "Sectoraalnummer",
        "Gebruikersnaam",
        "Telefoonnummer",
        "Nieuw telefoonnummer",
        "Emailadres",
        "Aanvraagdatum",
        "Mutatiedatum",
        "Activatiedatum",
        "Laatste Login",
        "BSN",
        "GBA status",
        "Geboortedatum",
        "Geslachtsaanduiding",
        "Voornamen",
        "Adellijke titel",
        "Voorvoegsel geslachtsnaam",
        "Geslachtsnaam",
        "Aanduiding naamgebruik",
        "Voorvoegsel geslachtsnaam partner",
        "Geslachtsnaam partner",
        "Gemeente van inschrijving",
        "Straatnaam",
        "Huisnummer",
        "Huisletter",
        "Huisnummertoevoeging",
        "Aanduiding bij huisnummer",
        "Postcode",
        "Woonplaats",
        "Locatieomschrijving"
      ]

      # Fetch accounts
      while true
        item = Redis.current.lpop(redis_key)
        break unless item
        gba = JSON.parse(item)

        Account.includes(:password_tool).joins(:sectorcodes).where("sectorcodes.sector_id = ?", bsn_sector_id).where("sectorcodes.sectoraalnummer = ?",  gba['bsn']).find_each do |a|
          csv << [
            gba['bsn'],
            a.gebruikersnaam,
            a.phone_number,
            "DigiD4:nvt",
            (a.email.present? ? a.email.adres : ""),
            a.created_at,
            a.updated_at,
            (a.password_tool.present? ? a.password_tool.updated_at : ""),
            a.last_sign_in_at,
            gba['bsn'],
            gba['status'],
            gba['geboortedatum'],
            gba['geslachtsaanduiding'],
            gba['voornamen'],
            gba['adellijke_titel'],
            gba['voorvoegsel_geslachtsnaam'],
            gba['geslachtsnaam'],
            gba['aanduiding_naamgebruik'],
            gba['voorvoegsel_geslachtsnaam_partner'],
            gba['geslachtsnaam_partner'],
            gba['gemeente_van_inschrijving'],
            gba['straatnaam'],
            gba['huisnummer'],
            gba['huisletter'],
            gba['huisnummertoevoeging'],
            gba['aanduiding_bij_huisnummer'],
            gba['postcode'],
            gba['woonplaats'],
            gba['locatieomschrijving']
          ]
        end
      end
    end

    return csv_string
  ensure
    Redis.current.del(redis_key)
  end

  private

  def redis_key
    "brp:fraud:#{@job_id}"
  end
end
