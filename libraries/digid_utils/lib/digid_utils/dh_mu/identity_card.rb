
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
  module DhMu
    class IdentityCard
      ALLOWED_STATUS = %w[Uitgereikt Geactiveerd Geblokkeerd Ingetrokken].freeze
      def initialize(data)
        @data = data
      end

      def [](key)
        @data[key]
      end

      def inactive?
        human_status == "niet_actief"
      end

      def issued?
        status == "Uitgereikt"
      end

      def active?
        status == "Geactiveerd"
      end

      def blocked?
        status == "Geblokkeerd"
      end

      def revoked?
        status == "Ingetrokken"
      end

      def level
        active? ? 4 : 0
      end

      def human_status
        if active?
          "actief"
        elsif blocked?
          "geblokkeerd"
        elsif revoked?
          "ingetrokken"
        else
          "niet_actief"
        end
      end

      # Kan activeren
      def activatable?
        status == "Uitgereikt"
      end

      # Kan intrekken
      def revokable?
        %w[Uitgereikt Geactiveerd Geblokkeerd].include?(status)
      end

      # Kan deblokkeren
      def deblockable?
        status == "Geblokkeerd"
      end

      # kan pincode instellen / aanvragen
      def pin_resettable?
        %w[Uitgereikt Geactiveerd Geblokkeerd].include?(status)
      end

      def css_class
        if %w[niet_actief geblokkeerd].include?(human_status)
          "setting-inactive"
        elsif active?
          "setting-active"
        else
          ""
        end
      end

      def valid?
        status_valid?
      end

      def status_valid?
        ALLOWED_STATUS.include?(status)
      end

      def last_updated_at
        @data[:status_date_time]
      end

      def canonical_type
        "identiteitskaart"
      end

      def date_time_status
        @data[:status_date_time]
      end

      def status
        @data[:status]
      end

      def status_bron
        @data[:status_bron]
      end

      def sequence_no
        @data[:sequence_nr]
      end

      def document_no
        @data[:document_nr]
      end
    end
  end
end
