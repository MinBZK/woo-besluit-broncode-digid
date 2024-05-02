
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
module MyDigid
  class VerificationsController < BaseController
    include FlowBased
    include AppSessionConcern

    def new
      if logged_in_with_pass?
        current_flow.transition_to!(:verify_with_password)
        redirect_to my_digid_controleer_wachtwoord_url
      elsif logged_in_with_wid?
        current_flow.transition_to!(:verify_with_wid)
        redirect_to my_digid_new_session_wid_url
      elsif logged_in_with_app?
        current_flow.transition_to!(:verify_with_app)
        if logged_in_web_to_app?
          redirect_to my_digid_web_to_app_url
        else
          redirect_to my_digid_controleer_app_start_url
        end
      end
    end

    def verification_cancelled
      if session[:app_session_id].blank? || cancelled_in_app?
        Log.instrument("1421", human_process: log_process, account_id: current_account.id)
        if cancelled_in_app?
          flash.now[:notice] = t("digid_app.cancel_process", human_process: t("process_names.notice.#{current_flow.process}"))
          return render_simple_message(ok: my_digid_url)
        end
      end

     redirect_to(my_digid_url)
    end
  end
end
