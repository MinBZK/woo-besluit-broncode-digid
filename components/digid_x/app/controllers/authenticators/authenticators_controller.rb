
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

module Authenticators
  class AuthenticatorsController < ApplicationController
    protected

    def webservice
      @webservice ||= Webservice.from_authentication_session(session[:authentication])
    end

    private

    def add_flow_step(flow, step)
      if flow.index(step)
        flow[0..flow.index(step) - 1] + step
      else
        flow + step
      end
    end

    # move user to either:
    #   form activation code(s) if activation session
    # otherwise
    #   form check_email if type account email is submitted
    #   finalize when no email is submitted
    def fan_out
      if session[:session].present? && session[:session].eql?("sign_in")
        if session[:weak_password]
          session[:weak_password_level] = 20
          redirect_to renew_weak_password_url
        elsif webservice.basis_or_midden_to_substantieel? && current_account.midden_active?
          @page_name = "C22"
          @page_title = t(@page_name, scope: "titles")
          set_deprecation_redirect_details
          render "authentications/sign_in_method_deprecation_warning_id_check"
        else
          redirect_to handle_after_authentication(20, current_account)
        end
      else
        session[:sms_options][:passed?] = true
        redirect_to session[:sms_options][:return_to]
      end
    end
  end
end
