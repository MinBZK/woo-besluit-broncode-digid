
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

require "net/http"

class DownloadLettersFileJob
  include LetterSftp

  # Download letters processed file from letter provider
  def perform
    return unless Rails.application.config.ftp_letters

    Log.instrument("377")
    start do |sftp|
      sftp.dir.foreach(remote_download_path) do |bestand|
        if bestand.name =~ /\ADD(\d){6}-\.xml\z/
          xml_string = sftp.download!(File.join(remote_download_path, bestand.name), nil)
          parse_xml(bestand.name, xml_string)
        end
      end
    end
  end

  def on_error(_e)
    Alarm.new.send_alert(category: "briefbestanden",
                         subject: "Controlebestand ophalen niet mogelijk",
                         body: "Het is niet mogelijk het controlebestand op te halen bij de briefleverancier")
    Log.instrument("336")
  end

  private

  def parse_xml(bestand_naam, xml)
    naam_verzonden = bestand_naam.gsub("DD", "A-20").gsub("-.", "-ehv.")
    alf = ActivationLetterFile.find_by(filename: naam_verzonden)
    if alf
      alf.processed_file = bestand_naam
      alf.processed_xml  = xml
      alf.status         = ActivationLetterFile::Status::PROCESSED
      alf.save!
      Log.instrument("335", file: bestand_naam)
    else
      Log.instrument("1504", file: bestand_naam)
    end
  end
end
