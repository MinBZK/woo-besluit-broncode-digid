
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

class ReportPostcodeAndHouseFraud < AdminReport
  extend ApplicationHelper

  def self.report_name
    "Postcode en huisnr per (her)aanvraag"
  end

  def self.report(start_date = nil)
    # Use last day as period if no day is supplied.
    start_ts, end_ts = get_day_start_end(start_date)
    rep_date = format_report_period(ReportParam::DAY, start_ts)
    header = ["Periode", "Postcode", "Huisnummer", "Huisletter", "Huisnummertoevoeging", "Brieftype", "Datum/tijd"]
    status = ActivationLetter::Status::SEND_TO_PRINTER

    logger.info "#{report_name} ===> Generating report for #{start_ts} .. #{end_ts}"

    result = [] << header
    ActivationLetter.where(created_at: (start_ts..end_ts), status: status).pluck(:letter_type, :gba, :created_at).each do |letter, gba, created_at|
      huisnummer, huisletter, toevoeging, postcode = JSON.parse(gba).values_at("SSSSSS", "SSSSSS", "SSSSSS", "SSSSSS")
      result << [rep_date, postcode, huisnummer, huisletter, toevoeging, briefcode(letter), format_timestamp(created_at)]
    end

    logger.info "#{report_name} ===> Result for query: #{result}"

    return result
  end

  def self.briefcode(letter_type)
    {
      "activation_aanvraag" => "001",
      "uitbreiding_sms" => "004",
      "activation_heraanvraag" => "005",
      "uitbreiding_app" => "006",
      "recovery_password" => "008",
      "recovery_sms" => "009",
      "aanvraag_deblokkeringscode_eid" => "011",
      "activation_app_one_device" => "012"
    }[letter_type]
  end
end
