
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

module DigidUtils
  class Property
    attr_accessor :key
    attr_accessor :value
    attr_accessor :path

    def initialize(key, value)
      @key  = key
      @value = value
      @path = file(value)

      self
    end

    def type
      case @key
      when /\_cert_file$/, /\_certificate$/
        "crt"
      when /\_key$/, /\_key_file$/
        "key"
      else
        "txt"
      end
    end

    def file(value)
      if File.exist?(value.to_s)
        return value
      else
        file_path = "./tmp/tmp_#{@key}.#{type}"
        # Ensure directory exists before writing file
        FileUtils.mkdir_p(File.dirname(File.expand_path(file_path)))
        File.open(file_path, "w+") do |f|
          f.write(value&.force_encoding("UTF-8"))
        end
        file_path
      end
    end

    def read
      if File.exist?(@value.to_s)
        File.read(@value.to_s)
      else
        @value.to_s
      end
    end

    alias_method :to_s, :value

    def method_missing(method, *args)
      @value.send(method, *args)
    end
  end
end
