
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
  class ExistingRequestsController < ApplicationController

    before_action :check_registration, only: [:show]

    # GET /lopende_aanvraag (existing_request_url) - A1D
    #
    # When an user requests an account and the system notices that a request
    # already exists for the given citizen service number, the user is asked
    # what he wants: replace the existing request or not?
    def show
      @last_registration_balie = Account.last_registration_currently_in_progress_balie?(current_registration.burgerservicenummer, Sector.get("bsn"))

      @confirm = Confirm.new(value: true)
      @page_name = "A1D"
      @page_title = t("titles.A1D")
      Log.instrument("20", registration_id: current_registration.id)
    end

    # POST /lopende_aanvraag (existing_request_url) - A1D
    #
    # Continues or stops the registration process based on the user's choice.
    def create
      @confirm = Confirm.new(confirm_params)
      if @confirm.yes?
        session[:heraanvraag] = true
        replace_account
      else
        Log.instrument("24", registration_id: current_registration.id)
        redirect_to(APP_CONFIG["urls"]["external"]["digid_home"])
      end
    end

    # GET /lopende_aanvraag/annuleren (cancel_existing_request_url) - A1D
    #
    # Show the default cancellation screen (G2).
    def cancel
      # FIXME: probably need to refactor the cancellation process
      flash.now[:notice] = t("you_will_return_to_homepage_digid_cancel")

      @page_name  = "G2"
      @confirm_to = confirm_cancel_url
      @return_to  = cancel_cancel_url

      render_partial_or_return_json(".main-content", "shared/cancel", "shared/_cancel")
    end

    private

    # TODO: refactor (it could probably use scopes for letter type and status)
    def log_if_letter_was_not_send
      # find previous succeeded registration
      return unless current_registration

      previous_registration = Registration.where("id <> ?", current_registration.id).requested
                                          .where(burgerservicenummer: current_registration.burgerservicenummer,
                                                 gba_status: "valid").last
      return unless previous_registration

      previous_registration.activation_letters.each do |letter|
        next unless ActivationLetter::LetterType::AANVRAAG_WITH_SMS == letter.letter_type

        if letter.state.finished?
          Log.instrument("22")
        else
          Log.instrument("23")
        end
      end
    end

    def replace_account
      sector_ids = [Sector.all.map(&:id)].flatten
      Account.registration_currently_in_progress(current_registration.burgerservicenummer, sector_ids).each do |account_id|
        account = Account.where(id: account_id).first
        Log.instrument("21", account_id: account.id)
        account.destroy if account.present?
      end
      log_if_letter_was_not_send
      redirect_via_js_or_http(new_account_url)
    end

    def check_registration
      render_not_found unless current_registration
    end
  end
end
