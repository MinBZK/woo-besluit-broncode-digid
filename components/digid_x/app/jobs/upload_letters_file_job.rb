
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

class UploadLettersFileJob
  include LetterSftp

  # Upload letters to letter provider
  # activation_letter_file status: ready, sent
  def perform
    return unless Rails.application.config.ftp_letters

    upload_file(file, control_file)
  end

  def upload_file(file_name, control_file)
    start do |sftp|
      # Letter file
      sftp.upload(File.join(local_path, "letters", file_name), File.join(remote_upload_path, file_name), mkdir: false) do |event, _uploader|
        if event == :finish
          Log.instrument "333"
          ActivationLetterFile.where(filename: file_name).update_all(status: ::ActivationLetterFile::Status::SENT)
          # Letter control file
          sftp.upload(File.join(local_path, "csv", control_file), File.join(remote_upload_path, control_file), mkdir: false)
        end
      end
    end
  end

  def perform_single(activation_letter_id)
    return unless Rails.application.config.ftp_letters

    letter = ActivationLetterFile.find(activation_letter_id)

    write_to_file("digid_global/letters", letter.filename,  letter.xml_content.force_encoding("UTF-8"))
    write_to_file("digid_global/csv", letter.filename_csv, letter.csv_content.force_encoding("UTF-8"))

    upload_file(letter.filename, letter.filename_csv)
  end


  def on_error(e)
    Alarm.new.send_alert(category: "briefbestanden",
                         subject: "Briefbestand afleveren niet mogelijk",
                         body: "Het is niet mogelijk het briefbestand te versturen naar briefleverancier")
    Log.instrument("334")
    Rails.logger.error("Briefbestand afleveren mislukt met error: " + e.message)
  end

  private
  def write_to_file(path, filename, content)
    Dir.chdir(path) do
      File.open(filename, "w") { |file| file.write(content) }
    end
  rescue Errno::ENOENT
    FileUtils.mkdir_p(path)
    write_to_file(path, filename, content)
  end

  def control_file
    "C-#{Time.zone.now.localtime.strftime('%Y%m%d')}.csv"
  end

  def file
    "A-#{Time.zone.now.localtime.strftime('%Y%m%d')}-ehv.xml"
  end
end
