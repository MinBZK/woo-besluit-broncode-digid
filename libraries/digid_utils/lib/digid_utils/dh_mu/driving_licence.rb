
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
    class DrivingLicence
      ALLOWED_STATUS = %w[Uitgereikt Actief Geblokkeerd Gerevoceerd].freeze
      ALLOWED_STATUS_MU = %w[Actief Inactief Gerevoceerd].freeze
      def initialize(data)
        @data = data
        @data[:allow_pin_reset] = false
      end

      def [](key)
        @data[key]
      end

      def []=(key, value)
        @data[key] = value
      end

      def inactive?
        human_status == "niet_actief"
      end

      def issued?
        status == "Uitgereikt" && status_mu == "Actief"
      end

      def active?
        status == "Actief" && status_mu == "Actief"
      end

      def blocked?
        status == "Geblokkeerd" && %w[Actief Inactief].include?(status_mu)
      end

      # Gerevoceerd
      def revoked?
        [status, status_mu].include?("Gerevoceerd") && status != "Inactief"
      end

      # Geschorst
      def suspended?
        status == "Actief" && status_mu == "Inactief"
      end

      def level
        active? ? 4 : 0
      end

      # rubocop:disable Metrics/MethodLength
      def human_status
        if active?
          "actief"
        elsif suspended?
          "geschorst"
        elsif blocked?
          "geblokkeerd"
        elsif revoked?
          "ingetrokken"
        else
          "niet_actief"
        end
      end
      # rubocop:enable Metrics/MethodLength

      # Kan activeren
      def activatable?
        status == "Uitgereikt" && status_mu == "Actief"
      end

      # Kan intrekken
      def revokable?
        %w[Uitgereikt Actief Geblokkeerd].include?(status) && %w[Gerevoceerd].include?(status_mu) == false
      end

      # Kan deblokkeren
      def deblockable?
        status == "Geblokkeerd" && status_mu == "Actief"
      end

      # kan pincode instellen / aanvragen
      def pin_resettable?
        %w[Uitgereikt Actief Geblokkeerd].include?(status) && %w[Inactief Gerevoceerd].include?(status_mu) == false
      end

      def css_class
        if %w[niet_actief geblokkeerd geschorst].include?(human_status)
          "setting-inactive"
        elsif active?
          "setting-active"
        else
          ""
        end
      end

      def valid?
        status_valid? && status_mu_valid?
      end

      def status_valid?
        ALLOWED_STATUS.include?(status)
      end

      def status_mu_valid?
        ALLOWED_STATUS_MU.include?(status_mu)
      end

      def last_updated_at
        [
          Time.zone.parse("#{@data[:wyz_dat_st_mu]} #{@data[:wyz_tyd_st_mu].rjust(6, "0")}"),
          Time.zone.parse("#{@data[:wyz_dat_st_burg]} #{@data[:wyz_tyd_st_burg].rjust(6, "0")}")
        ].max
      end

      def canonical_type
        "rijbewijs"
      end

      def eidtoeslag_payment_info?
        ["Betaald", "Niet betaald"].include?(eidtoeslag)
      end

      def eidtoeslag_not_applicable?
        eidtoeslag == "Niet van toepassing"
      end

      def eidtoeslag_not_paid?
        eidtoeslag == "Niet betaald"
      end

      def eidtoeslag_betaald?
        eidtoeslag == "Betaald"
      end

      def status
        @data[:stat_e_id_burg]
      end


      def status_mu
        @data[:stat_e_id_mu]
      end

      def status_bron
        @data[:bron_stat_burg]
      end

      def sequence_no
        @data[:e_id_volg_nr]
      end

      def document_no
        @data[:ryb_nr]
      end

      def note
        @data[:toel_e_id_stat]
      end

      def eidtoeslag
        @data[:stat_e_id_toesl]
      end
    end
  end
end
