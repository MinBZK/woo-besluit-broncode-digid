
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

# DigiD 4.0 CreateLettersJob
#
# Usage:
# bundle exec rails runner -e <environment> CreateLettersJob.new.perform

require "letter"
class CreateLettersFileJob
  # creates the letter xmls from ActivationLetters that are marked as "finished"
  def perform
    build_letters
    alf = ActivationLetterFile.new(
      filename: xml_filename,
      filename_csv: csv_filename,
      xml_content: letter_builder.xml,
      csv_content: letter_builder.csv,
      status: ::ActivationLetterFile::Status::READY)
    ActivationLetterFile.transaction do
      alf.save!
      ActivationLetter.mark_as_sent letter_builder.letter_ids
    end
    Log.instrument("331")
    alf
  rescue
    Log.instrument("332")
    raise
  end

  private

  def letter_builder
    @letter_builder ||= LetterBuilder.new(APP_CONFIG["csv_codes_file"]&.path)
  end

  # gets the ActivationLetters,
  def build_letters
    # create the xml file
    letters = ActivationLetter.finished
    letter_builder.create_xml_from letters
    create_letters_file letter_builder

    # create the csv file
    create_controle_bestand letter_builder.csv
    letter_builder
  end

  # Briefbestanden beginnen altijd met een 'A-' en eindigen met '-ehv.xml'
  def create_letters_file(letter)
    Dir.chdir(letter_directory) do
      File.open(xml_filename, "w") { |xml_file| xml_file.write(letter.xml) }
    end
  rescue Errno::ENOENT
    FileUtils.mkdir_p(letter_directory)
    create_letters_file(letter) #retry
  end

  def xml_filename
    @letterxml_filename ||= "A-" + Time.zone.now.localtime.strftime("%Y%m%d") + "-ehv.xml"
  end

  def csv_filename
    @csv_filename ||= "C-" + Time.zone.now.localtime.strftime("%Y%m%d") + ".csv"
  end

  # Controlebestanden beginnen altijd met een 'C-' , gevolgd door de datum in 'yyyymmdd' formaat en eindigen met '.csv'.
  # Dus: een voorbeeld van een controlebestand: 'C- 20110227.csv'.
  def create_controle_bestand(csv)
    Dir.chdir("digid_global/csv") do
      File.open(csv_filename, "w") { |csv_file| csv_file.write(csv) }
    end
  end

  def letter_directory
    "digid_global/letters"
  end
end
