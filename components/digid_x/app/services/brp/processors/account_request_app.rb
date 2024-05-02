
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

module Brp
  module Processors
    class AccountRequestApp < Base
      def perform
        @registration.gba_status = get_gba_status
        @registration.create_letter(@gba)
        @registration.save
      end

      private

      def get_gba_status
        gba.status = "investigate_address" if gba.onderzoek_adres?
        
        if !match_registration_data_with_gba
          Log.instrument("10", hidden: true, registration_id: @registration.id)
          return "mismatch_personal_data_with_gba"
        elsif %w[not_found suspended_error suspended_unknown].include? gba.status
          Log.instrument("11", hidden: true, registration_id: @registration.id)
          return "gba_under_investigation"
        elsif %w[emigrated rni ministerial_decree].include?(gba.status)
          Log.instrument("14", hidden: true, registration_id: @registration.id)
          return "gba_emigrated_RNI"
        elsif @gba.status == "deceased"
          Log.instrument("13", hidden: true, registration_id: @registration.id)
          return "gba_deceased"
        elsif @gba.status == "investigate_address"
          Log.instrument("12", hidden: true, registration_id: @registration.id)
          return "gba_address_under_investigation"
        elsif Afmeldlijst.bsn_op_afmeldlijst?(@gba.bsn)
          Log.instrument("475", registration_id: @registration.id)
          return "BSN_unsubscribed"
        end

        "valid"
      end

      def match_registration_data_with_gba
        if !birthday_matches_gba?
          Rails.logger.warn("GBA request birthday mismatch. Registration: '#{@registration.geboortedatum}', GBA: '#{@gba.geboortedatum}'")
          return false
        elsif @registration.postcode != @gba.postcode.to_s
          Rails.logger.warn("GBA request postcode mismatch. Registration: '#{@registration.postcode}', GBA: '#{@gba.postcode}'")
          return false
        elsif @registration.huisnummer != @gba.huisnummer.to_s
          Rails.logger.warn("GBA request huisnummer mismatch. Registration: '#{@registration.huisnummer}', GBA: '#{@gba.huisnummer}'")
          return false
        end

        true
      end
    end
  end
end
