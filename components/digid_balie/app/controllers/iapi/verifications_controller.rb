
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
  class VerificationsController < ApplicationController
    skip_before_action :authenticate!
    skip_before_action :require_front_desk!
    skip_before_action :verify_authenticity_token

    def index
      verifications = Verification.where(verification_params)
      render json: verifications, each_serializer: VerificationsSerializer, include: '*.*'
    end

    def show
      render json: verification, serializer: VerificationsSerializer, include: '*.*'
    end

    def update
      verification.audit.update_attribute(:state, audit_params[:state])
      head :ok
    end

    def unaudited
      verifications_unaudited = Verification.where(verification_params).unaudited
      render json: verifications_unaudited, each_serializer: VerificationsSerializer, include: '*.*'
    end

    def fraud_suspicion
      verifications_fraud_suspicion = Verification.where(verification_params).fraud_suspicion
      render json: verifications_fraud_suspicion, each_serializer: VerificationsSerializer, include: '*.*'
    end

    private

    def audit_params
      params.require(:audit).permit(:state)
    end

    def verification_params
      params
        .except(:format, :verification, :user, :audit)
        .permit(:id, :citizen_service_number, :id_number,
                :id_expires_at, :id_established, :suspected_fraud,
                :created_at, :updated_at, :front_desk_code,
                :activation_code, :state, :salutation, :full_name,
                :activation_code_end_date, :first_names, :surname,
                :birthday, :front_desk_id, :user_id, :front_desk_registration_id,
                :activated_at, :activated, :front_desk_account_id,
                :id_signaled, :nationality)
    end

    def verification
      @verification ||= Verification.find_by!(id: params[:id])
    end
  end
end
