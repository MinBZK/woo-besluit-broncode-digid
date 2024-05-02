
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

module Registrations
  class ExistingAccountsController < ApplicationController
    before_action :render_not_found_if_account_deceased
    before_action(:qualified_account?)

    # GET /bestaande_digid (existing_account_url) - A1C
    #
    # When an user requests an account and the system notices that an account
    # already exists for the given citizen service number, the user is asked
    # what he wants: replace the existing account or not?
    def show
      @confirm = Confirm.new(value: true)
      if balie_session?
        @page_name    = ::Configuration.get_boolean("balie_aanvraag_voor_rni") ? "A1E" : "A1B"
        @page_title   = t("titles.A1CB")
        @page_header  = t("request_digid_abroad")
      else
        @page_name    = "A1"
        @page_title   = t("titles.A1C")
        @page_header  = t("request_digid")
      end
      Log.instrument("25", registration_id: current_registration.id)
    end

    # POST /bestaande_digid (existing_account_url) - A1C
    #
    # Continues or stops the registration process based on the user's choice.
    def create
      @confirm = Confirm.new(confirm_params)
      if @confirm.yes?
        session[:heraanvraag] = true
        Log.instrument("27", registration_id: current_registration.id)
        redirect_to(new_account_url)
      else
        Log.instrument("28", registration_id: current_registration.id)
        redirect_to(APP_CONFIG["urls"]["external"]["digid_home"])
      end
    end

    # GET /bestaande_digid/annuleren (cancel_existing_account_url) - A1C
    #
    # Show the default cancellation screen (G2).
    def cancel
      # FIXME: probably need to refactor the cancellation process
      flash.now[:notice] = t("you_will_return_to_homepage_digid_cancel")

      @confirm_to = confirm_cancel_url
      @page_name  = "G2"
      @return_to  = cancel_cancel_url

      render_partial_or_return_json(".main-content", "shared/cancel", "shared/_cancel")
    end

    private

    def qualified_account?
      redirect_to(new_registration_url) unless current_registration
    end
  end
end
