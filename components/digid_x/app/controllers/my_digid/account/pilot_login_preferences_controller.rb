
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
    class PilotLoginPreferencesController < BaseController
      before_action :no_active_two_factor
      before_action :render_not_found_if_account_deceased

      # GET [mijn.digid.nl] /inloggen_voorkeur
      def show
        # Needed for log #1416 and #1417
        session[:flow] = LoginPreferenceFlow.new
        @page_name = "D18A"
        Log.instrument("502", account_id: current_account.id) # 172
        session[:session] = "pilot_login_preferences"
      end

      # PUT [mijn.digid.nl] /inloggen_voorkeur
      def update
        @page_name = "D18A"

        new_zekerheidsniveau = account_params[:zekerheidsniveau].to_i

        # Als het zekerheidsniveau wordt verlaagd moet deze eerst bevestigd worden met een wachtwoord.
        if new_zekerheidsniveau < current_account.zekerheidsniveau.to_i

          session[:check_pwd] = { return_to: confirm_my_digid_pilot_login_preference_url,
                                  new_zekerheidsniveau: new_zekerheidsniveau,
                                  steps: { at: 2, of: 2 },
                                  page_name: "D11",
                                  page_title: t("titles.D11.change_zekerheidsniveau"),
                                  page_header: t('loginlevel_preference_detail')}

          redirect_via_js_or_html password_check_url
        else

          # Verder met de huidige implementatie.
          current_account.update!(zekerheidsniveau: new_zekerheidsniveau)
          if current_account.previous_changes.include?(:zekerheidsniveau)

            if new_zekerheidsniveau == ::Account::LoginLevel::TWO_FACTOR
              flash[:notice] = t("loginlevel_preference_set_to_20")
              current_account.update!(last_change_security_level_at: Time.zone.now)
              Log.instrument("618", account_id: current_account.id, active: I18n.t('loginLevel_midden'))
            end
          else
            flash[:notice] = t("loginlevel_preference_unchanged")
            Log.instrument("573", account_id: current_account.id)
          end
          redirect_to(my_digid_url)
        end
      end

      def confirm
        new_zekerheidsniveau = session[:check_pwd][:new_zekerheidsniveau].to_i

        current_account.update!(zekerheidsniveau: new_zekerheidsniveau)

        if current_account.previous_changes.include?(:zekerheidsniveau)
          if new_zekerheidsniveau == ::Account::LoginLevel::PASSWORD
            flash[:notice] = t("loginlevel_preference_set_to_10")
            current_account.update!(last_change_security_level_at: Time.zone.now)
            Log.instrument("618", account_id: current_account.id, active: I18n.t('loginLevel_basis'))
          end
        end

        redirect_to(my_digid_url)
      end

      def cancel
        Log.instrument("503", account_id: current_account.id)
        redirect_to(my_digid_url)
      end

      private

      def no_active_two_factor
        return if current_account.active_two_factor_authenticators.any?
        flash.now[:notice] = t("loginlevel_preference_you_need_at_least_one_active_two_factor")
        Log.instrument("637", account_id: current_account.id)
        render_simple_message(ok: my_digid_url)
      end

      def account_params
        params.require(:account).permit(:zekerheidsniveau)
      end
    end
  end
end
