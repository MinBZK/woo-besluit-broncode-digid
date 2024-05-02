
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
  class FlatHash < Hash
    attr_reader :separator

    def initialize(hash = nil, **kwargs)
      super()
      @separator = kwargs[:separator] || "/"
      merge!(hash) if hash
    end

    def merge!(other)
      if other.is_a?(self.class)
        super
      else
        merge_hash(other)
      end
    end

    def merge(other)
      if other.is_a?(self.class)
        super
      else
        dup.merge_hash(other)
      end
    end

    def merge_hash(other, prefix = "")
      other.each do |key, value|
        path = prefix + key
        if value.is_a?(Hash)
          merge_hash(value, path + separator)
        else
          self[path] = value
        end
      end
      self
    end

    # rubocop:disable Metrics/MethodLength
    def to_h
      result = {}
      each do |key, value|
        path = key.split(separator)

        nested = result
        last = path.pop
        path.each do |part|
          nested = (nested[part] ||= {})
          raise ArgumentError, "#{path.join(separator)} collides on #{part} with #{nested}" unless nested.class == Hash
        end

        nested[last] = value
      end
      result
    end
    # rubocop:enable Metrics/MethodLength
  end
end
