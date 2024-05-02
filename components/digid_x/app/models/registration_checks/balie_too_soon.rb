
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
  class BalieTooSoon < RegistrationCheck
    attr_reader :session

    def initialize(session, registration, url)
      super(registration, url)
      @session = session
    end

    def match?
      @session[:balie] && @registration.registration_balie_too_soon?
    end

    def flash_msg(options = {})
      last_registration = @registration.get_previous_registration(%w(valid emigrated rni ministerial_decree))
      if last_registration.status_emigrated_or_rni_or_ministerial_decree?
        time_calc = last_registration.created_at + ::Configuration.get_int("tijd_tussen_balie_aanvragen").minutes
        options[:wait_until] = I18n.l(time_calc, format: :time_text_tzone_in_brackets)
        I18n.t("you_have_made_a_new_balie_request_too_soon", **options)
      else
        I18n.t("you_have_made_a_new_request_too_soon")
      end
    end

    def log
      Log.instrument("508", registration_id: @registration.id)
    end
  end
end
