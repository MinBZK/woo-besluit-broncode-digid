
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
  module Account
    module DigidHoog
      module DrivingLicence
        class ExistingUnblockLetterRequestsController < ApplicationController
            include AppSessionConcern
            include RdwClient

            before_action :check_switches
            before_action :render_not_found_if_account_deceased

            def show
              @page_name  = "D52"
              @page_title = t("titles.D52")
              @page_header = t("replace_running_unblock_code_request")
              session[:sequence_no] = params[:sequence_no]
              session[:card_type] = params[:card_type]
              session[:existing_unblock_letter_request_licence] = true

              Log.instrument("1011", wid_type: t("document.driving_licence", locale: :nl), account_id: current_account.id)

              begin
                @driving_licence = rdw_client.get(bsn: current_account.bsn, sequence_no: session[:sequence_no]).first
                if !@driving_licence.deblockable?
                  return render_not_found
                end
              rescue DigidUtils::DhMu::RdwError => e
                return redirect_via_js_or_html my_digid_url
              end

              if !current_account.unblock_letter_requested?(bsn: current_account.bsn, sequence_no: params[:sequence_no], card_type: params[:card_type])
                Log.instrument("1010", wid_type: t("document.driving_licence", locale: :nl), account_id: current_account.id, hidden: true)
                flash.now[:notice] = t("digid_hoog.request_unblock_letter.no_pending_request", wid_type: t("document.driving_licence")).html_safe
                render_simple_message(ok: my_digid_url)
              end
            end

            def cancel
              Log.instrument("1012", wid_type: t("document.driving_licence", locale: :nl), account_id: current_account.id)
              redirect_to my_digid_url
            end

            private

            def check_switches
              if request.referer
                return if driving_licence_enabled? && show_driving_licence?(current_account.bsn)
                Log.instrument("1414", wid_type: t("document.driving_licence", locale: :nl), account_id: current_account.id, hidden: true)
                flash.now[:alert] = t("digid_hoog.request_unblock_letter.abort.wid_switch_off", wid_type: t("document.driving_licence")).html_safe
                render_simple_message(ok: my_digid_url)
              else
                check_tonen_rijbewijs_switch
              end
            end
        end
      end
    end
  end
end
