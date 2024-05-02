
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

# Global network
# Read the GeoIPCountryWhois.txt from the asset folder
# Perform a lookup for the country or a check if the IP
# address is dutch.
class GlobalNetwork

  def initialize()
    @log = Rails.logger
    @name = "GlobalNetworks"
    @networks = {}
    num_of_records = 0
    time_elapsed = time do
      num_of_records = load_file
    end
    @log.debug "DEBUG GlobalNetwork.#{__method__} ===> Initialized GlobalNetworks with #{num_of_records} data records in #{time_elapsed} seconds."
  end


  def get_name()
    return @name
  end

  # Get country for this specific network
  # Takes an IP4 adres and returns the
  # full country name.
  def get_country(ip_adres)
    result = "unknown"
    me = __method__
    begin
      a_network = find_network(ip_adres)
      result = a_network[5] if a_network.present?
      @log.debug "DEBUG GlobalNetwork.#{me} ===> Found country #{result}"
    rescue Exception => e
      @log.error "ERROR GlobalNetwork.#{me} ===> Catched exception #{e.message}, surpress it and return unknown country."
    end
    return result
  end

  # Return true if this IP4 network is in the Netherlands
  def netherlands?(ip_adres)
    "Netherlands" == get_country(ip_adres)
  end

  private
  # Helper methods

  # Turns an IP4 adres into a numeric value
  # 16777216 . 65536 . 256 . 1
  # 16777216 + 65536 + 512 + 0 = 16843264  => 1.1.2.0
  def make_numeric(ip_adres)
    number = 0
    octet_multipliers = [16777216, 65536, 256, 1]
    # multiply the octect's right to left
    if !ip_adres.nil? and valid_ip?(ip_adres)
      octets = ip_adres.split(".")
      4.times do |i|
        number += octets[i].to_i * octet_multipliers[i]
      end
    end
    #@log.debug "DEBUG GlobalNetwork.#{__method__} ===> ip adres, #{ip_adres}, numeric value is  #{number}"
    return number
  end

  # Pad each octect with zeros so we
  # get an uniform, 3 character, IP4 adres.
  def add_leading_zeros(ip_adres)
    ip_adres.split(".").each do |octet|
      while 3 > octet.length do
        octet = "0" + octet
      end
      padded_ip += octet + "."
    end
    comparable_ip = padded_ip.slice(0,15)
    #@log.debug "DEBUG GlobalNetwork.#{__method__} ===> ip adres, #{ip_adres}, changed to #{comparable_ip}"
    return comparable_ip
  end

  # Find exact or closest match of network
  def find_network(ip_adres)
    closest_match = nil
    me = __method__
    if valid_ip?(ip_adres)
      ip_number = make_numeric(ip_adres)
      # Should be implemented as tree or btree search.
      time_elapsed = time do
        @networks.keys.sort!.each do |key|
          if ip_number >= key
            closest_match = key
          else
            break
          end
        end
      end
      #@log.debug "DEBUG GlobalNetwork.#{me} ===> Finding the IP address, #{ip_adres} took  #{time_elapsed} seconds. Closest match is #{closest_match} ."
      if !registered_network?(ip_adres, closest_match )
        @log.error "ERROR GlobalNetwork.#{me} ===> The ip adres, #{ip_adres}, is not part of a known network!"
        raise "The ip adres is not part of a known network!"
      end
    end
    return @networks[closest_match]
  end

  # Sanity check ip address
  # Perform a simple check to
  # ensure the octet is in range 0 .. 255
  def valid_ip?(ip_adres)
    valid = true
    ip_adres.split(".").each do |octet|
      if octet.to_i > 255
        valid = false
        @log.error "DEBUG GlobalNetwork.#{__method__} ===> Invalid ip address #{ip_adres}"
      end
    end
    return valid
  end

  # Sanity check if network
  # is administerd in Geo file.
  def registered_network?(ip_adres, ip_match)
    #@log.debug "DEBUG GlobalNetwork.#{__method__} ===> ip_adres: #{ip_adres}; ip_match: #{ip_match}"
    result = false
    if !ip_adres.nil? and !ip_match.nil?
      result = @networks[ip_match][3].to_i > make_numeric(ip_adres) ? true : false
    end
    return result
  end

  # Read GeoIPCountryWhois.txt from disk
  # The file is stored in the data directory and
  # will be update in bi-weekly interval.
  # Every line will be stored in an array and put
  # in hash with the numerical start address of each network
  # as index.
  # On errors the method will raise a runtime exception.
  def load_file
    num_of_entries = 0
    file_name = APP_CONFIG['geo_ip_country_file_name']  #"GeoIPCountryCSV.txt"
    data_path = APP_CONFIG['geo_ip_country_file_path']  #"data"
    begin
      file = File.open(File.join(Rails.root, data_path, file_name), "r").each_line do |line|
      clean_line = line.gsub(/"/, "")
      the_line =clean_line.strip.split(',')
      # QQQ use only dutch Ip addresses, the_line[5] == 'Netherlands'
      @networks[the_line[2].to_i] = the_line[0..5] if the_line[5] == 'Netherlands'
      end
    rescue Exception => e
      @log.error "DEBUG GlobalNetwork.#{__method__} ===> Error processing #{file_name} : #{e.message}"
      raise "Error processing #{file_name} : #{e.message}"
    ensure
      file.close unless file.nil?
      @log.debug "DEBUG GlobalNetwork.#{__method__} ===> @networks hash has #{@networks.count} elements."
    end
    @networks.length
  end

  # Measures the elapsed time of the executed block.
  # Returns the time in seconds.
  def time
    start = Time.now
    yield
    Time.now - start
  end

  # def dump_result(logs)
  #   dumpfile = "#{Rails.root.to_s}/log/networks_#{Time.now.strftime("%y%m%d-%T.%L")}.txt"
  #   File.open(dumpfile, 'a') do |dump|
  #     logs.each do |log|
  #       dump.puts(log.inspect)
  #     end
  #   end
  # end
end
