
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

module Iapi
  class FrontDeskRegistrationsController < ApplicationController
    skip_before_action :verify_authenticity_token
    authorize_with_token :iapi_token

    def search
      front_desk_registration = Registration.front_desk.where(front_desk_registration_params).first

      unless front_desk_registration
        # since active_model_serializers(0.10) nil cannot be serialized. Example:
        # FrontDeskRegistrationSerializer.new(nil).to_json
        return render json: {front_desk_registration: nil}
      end

      render json: front_desk_registration, root: :front_desk_registration, serializer: FrontDeskRegistrationSerializer
    end

    def update
      Registration.transaction do

        registration = Registration.front_desk.find(front_desk_update_params[:front_desk_registration_id])
        if registration && registration.baliecode_valid?
          account = Account.requested.with_bsn(registration.burgerservicenummer).first

          # update gedigheids termijn van letter & sms_tool
          registration.activation_letter.update_attribute(:geldigheidsduur, calculate_new_valid_period(registration.activation_letter)) # Expiring is calculated from created_at
          account.pending_sms_tool.update_attribute(:geldigheidstermijn, calculate_new_valid_period(account.pending_sms_tool)) if account.pending_sms_tool # Expiring is calculated from created_at

          # Password_authenticator expiring is calculated from updated_at
          account.password_authenticator.update(status: Authenticators::Password::Status::PENDING, geldigheidstermijn: ::Configuration.get_int("balie_default_geldigheidsduur_activatiecode"))
          account.distribution_entity.update(balie_id: front_desk_update_params[:front_desk_id])
          changes = account.password_authenticator.previous_changes[:status]

          if changes && (changes.first == Authenticators::Password::Status::BLOCKED && changes.last == Authenticators::Password::Status::PENDING)
            ActivationCodeRetrievedAtFrontDeskMailer.delay(queue: "email").activation_code_retrieved_at_front_desk(account_id: account.id, recipient: account.email.adres)
          end
        end
      end
      head :ok
    end

    private

    def front_desk_registration_params
      params.require(:front_desk_registration).permit(:baliecode, :burgerservicenummer, :status)
    end

    def front_desk_update_params
      params.require(:front_desk_registration).permit(:front_desk_id, :front_desk_registration_id)
    end

    def calculate_new_valid_period(object)
      ((Time.zone.now - object.created_at) / 1.day).ceil + ::Configuration.get_int("balie_default_geldigheidsduur_activatiecode")
    end

  end
end
