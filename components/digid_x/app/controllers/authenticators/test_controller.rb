
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
  class TestController < ApplicationController
    include AuthenticationSession
    include AppLinkHelper

    before_action :check_session_time
    before_action :update_session
    before_action :cancel_button_login?, only: [:create]

    def new
      @auth = Authentication.new
      @auth.type_account = "test"
      set_login_page_elements
      @page_name = "C1C"
      @other_questions = []
      Log.instrument("60", authentication: session[:authentication], webservice_id: webservice_id, webservice_name: webservice.try(:name))
      Log.instrument("885", authentication: session[:authentication], webservice_id: webservice_id, webservice_name: webservice.try(:name), hidden: true) if webdienst_vereist_substantieel?

      session[:start_time] = Time.zone.now.to_f
    end

    def create
      return render_not_found unless show_test_betrouwbaarheidsniveau?
      @auth = Authentication.new authentication_params

      if @auth.valid?
        Log.instrument("850", account_id: @auth.account_or_ghost.id, level: Account::LEVELS[@auth.test_zekerheidsniveau.to_i], webservice_id: webservice.id)
        Log.instrument("857", account_id: @auth.account_or_ghost.id, level: Account::LEVELS[@auth.test_zekerheidsniveau.to_i], webservice_name: webservice_name, webservice_id: webservice.id)

        @auth.account_or_ghost.password_authenticator.seed_old_passwords(@auth.password) if @auth.account_or_ghost.is_a?(Account)
        session[:mydigid_logged_in] = false if @auth.account_or_ghost.id != session[:account_id]
        session[:account_id] = @auth.account_or_ghost.id
        session[:weak_password] = @auth.weak_password?
        session[:authentication][:card_type] = "Testlogin" if session[:authentication]
        session[:authentication][:card_reader_type] = "Testlogin" if session[:authentication]

        redirect_to handle_after_authentication(authentication_params[:test_zekerheidsniveau].to_i, @auth.account_or_ghost)
      else
        unless @auth.test_account_for_gbav?
          flash.now[:alert] = I18n.t("messages.authentication.no_testaccount")
          Log.instrument("856", account_id: @auth.account.try(:id), webservice_id: webservice.id, hidden: true)
        end
        set_login_page_elements
        @page_name = "C1C"
        @other_questions = []
        flash.now[:alert] = @auth.errors[:not_valid].join(" ").html_safe
        render :new
      end
    end
  end
end
