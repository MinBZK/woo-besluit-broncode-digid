
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

module RegistrationChecks
  class BalieTooOften < RegistrationCheck
    attr_reader :session

    def initialize(session, registration, url)
      super(registration, url)
      @session = session
    end

    def match?
      @session[:balie] && @registration.registration_balie_too_soon_in_a_month?
    end

    def flash_msg(options = {})
      options[:count] = ::Configuration.get_int("aantal_balie_aanvragen_per_maand")
      options[:wait_until] = I18n.l(@registration.registration_balie_too_soon_in_a_month_wait_time.created_at + 1.month, format: :date)
      I18n.t("you_requested_an_account_too_often", **options)
    end

    def log
      Log.instrument("509", registration_id: @registration.id)
    end
  end
end
