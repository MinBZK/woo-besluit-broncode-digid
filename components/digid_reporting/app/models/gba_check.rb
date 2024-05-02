
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

class GbaCheck
  def self.send_mail
    #Perform check on lifetime of GBA accounts
    #2015-06-01 FF

    report = "GBA #{Rails.env.capitalize}\naccount\t\tleeftijd\n"
    ['SSSSSSSSSSSSSSSS', 'SSSSSSSSSSSSSSSS'].map do |name|
      report << [name, gba_user_date(name)].join(",") + "\n"
    end
    report << "(Geldigheid max 90 dagen)\n"
    report << "Active account is: " + active_gba_user
    report << "\nSource is #{Socket.gethostname.upcase}\n\n"

    GbaCheckMailer.csv_report(report).deliver_now
  end

  private

  def self.gba_user_date(name)
    Configuration.find_by(name: name).updated_at.to_date.tomorrow.step(Date.yesterday, 1).count
  end

  def self.active_gba_user
    Configuration.find_by(name: 'active_gba_user').value
  end
end
