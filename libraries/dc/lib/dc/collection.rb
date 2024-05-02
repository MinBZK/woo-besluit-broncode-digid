
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

module Dc
  class Collection < ::ActiveResource::Collection
    attr_accessor :content, :pageable, :total_pages, :sort, :number_of_elements, :empty, :parent
    attr_writer :first, :size, :last, :number, :total_elements

    def initialize(parsed = {})
      @elements = parsed["content"]

      parsed.each do |key, value|
        public_send("#{key}=", value) if respond_to?("#{key}=")
      end
    end

    def count
      @total_elements
    end

    def current_page
      @number + 1
    end

    def limit_value
      @size
    end

    def first_page?
      @first
    end

    def last_page?
      @last
    end

    def next_page
      !last_page? && (current_page + 1)
    end

    def prev_page
      !first_page? && (current_page - 1)
    end
  end
end

