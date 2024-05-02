
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

require 'csv'

# class for loading csv files from bzrk (gemeenten)
# what is does is this:
# try to load csv_file passed, if nil, load "../shared/csv/codes.csv"
# because these files are UTF16 Little Endian (god knows why), and
# we want to use FastCSV, we need to convert it to something it understands
# so:
# convert to UTF8 the file passed if it's not converted or the file passed is newer
# at this point a converted file always exists, so load it up to codes.
#
# @csv = Csv.new("../shared/csv/codes.csv")
# @csv.fetch
#
# p @csv.codes["0001"]
# "Adorp"
#
class LoadCsv

  def initialize(csv_file)
    if csv_file
      @file_name = csv_file
      @file_name_converted = File.dirname(@file_name) + "/" + File.basename(@file_name, ".csv") + "_converted" + File.extname(@file_name)

      @codes = {}
    else
      raise "no path"
    end
  end

  # converts a csv file so we get a hash from codes to gemeentes
  def fetch
    if !converted? || newer?
      convert_to_utf8
    end

    CSV.foreach(@file_name_converted) do |row|
      @codes[row[0]] = {
        :omschrijving => row[1],
        :nieuwe_code => row[2],
        :datum_ingang => row[3],
        :datum_eind => row[4]
      }
    end
  end

  # 1126 -> Noordbroek, 0033
  # 0033 -> Oosterbroek, 1987
  # 1987 -> Menterwolde
  def codes(code)
    return unless @codes[code]
    if @codes[code][:nieuwe_code].empty?
      @codes[code][:omschrijving]
    else
      if !@codes[code][:datum_eind].empty? && (Date.today > Date.parse(@codes[code][:datum_eind]))
        codes(@codes[code][:nieuwe_code])
      else
        @codes[code][:omschrijving]
      end
    end
  end

  # codes is a hash from briefcodes to count.
  # sort the hash (it becomes an array)
  # and create a csv as follows: briefcode;count\n..
  def generate(codes)
    CSV.generate(:col_sep => ";") do |csv|
      codes.sort{|x,y| x[0] <=> y[0]}.each do |code_count|
        csv << [code_count[0], code_count[1]]
      end
    end
  end

  private

  # checks if there is a converted (from UTF-16LE to UTF-8) file
  def converted?
    File.exist? @file_name_converted
  end

  # tells if there's a new file, so we must convert again
  def newer?
    File.stat(@file_name).atime > File.stat(@file_name_converted).atime
  end

  # Data files are exported as Little Endian UTF-16. We need to parse as UTF-8
  def convert_to_utf8
    begin
      contents = File.open(@file_name).read

      unless contents.valid_encoding?
        converted = contents.encode('UTF-8', 'UTF-16LE')
        converted.gsub!("\uFEFF", "") # strip the BOM (byte order mark) from the first line of input
      end

      output = File.open(@file_name_converted, 'w')
      output.write(converted || contents)
      output.close
    rescue Exception => e
      puts e.message
    end
  end

end
