
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
    module Apps
      class RemoveController < MyDigid::BaseController
        def remove_warning
          Log.instrument("1350", account_id: current_account.id)
          if current_account.app_authenticators.where(id: params[:id], status: Authenticators::AppAuthenticator::Status::PENDING).exists?
            flash.now[:notice] = t("warning_delete_app")
            render_simple_message(ok: my_digid_apps_remove_chosen_to_proceed_url(id: params[:id]), cancel: my_digid_apps_remove_chosen_to_cancel_url(id: params[:id]))
          else
            Log.instrument("1351", account_id: current_account.id)
            flash.now[:notice] = t("cannot_delete_app")
            render_simple_message(ok: my_digid_url)
          end
        end

        def chosen_to_proceed
          app_deleted = false
          Log.instrument("1353", account_id: current_account.id)
          ActiveRecord::Base.transaction do
            app = current_account.app_authenticators.where(id: params[:id], status: Authenticators::AppAuthenticator::Status::PENDING)
            ActivationLetter.where(
              "registrations.burgerservicenummer" => current_account.bsn,
              "activation_letters.letter_type" => "uitbreiding_app",
              "activation_letters.controle_code" => app.pluck(:activation_code)
              ).joins(:registration).delete_all
            app_deleted = app.delete_all > 0
          end
          if app_deleted
            flash[:notice] = t("remove_app_success")
            Log.instrument("1354", account_id: current_account.id)
          else
            flash[:notice] = t("remove_app_failed")
            Log.instrument("1355", account_id: current_account.id)
          end
          redirect_to my_digid_url
        end

        def chosen_to_cancel
          Log.instrument("1352", account_id: current_account.id)
          redirect_to my_digid_url
        end
      end
    end
  end
end
