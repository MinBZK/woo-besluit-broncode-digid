
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
  class FrontDesksController < ApplicationController
    skip_before_action :authenticate!
    skip_before_action :require_front_desk!
    skip_before_action :verify_authenticity_token
    around_action :set_time_zone, only: %i[index show]

    def index
      if params[:search]
        search = "%#{params[:search]}%"
        front_desks = FrontDesk.where('code LIKE ? OR name LIKE ?', search, search)
      else
        front_desks = FrontDesk.all
      end
      render json: front_desks, each_serializer: FrontDeskSerializer
    end

    def create
      front_desk = FrontDesk.new(front_desk_params)
      if front_desk.save
        head :created, location: iapi_front_desk_path(id: front_desk.id)
      else
        head :unprocessable_entity
      end
    end

    def show
      if front_desk
        render json: front_desk, serializer: FrontDeskSerializer
      else
        head :not_found
      end
    end

    def update
      if front_desk
        if front_desk.update(front_desk_params)
          head :no_content
        else
          head :unprocessable_entity
        end
      else
        head :not_found
      end
    end

    def destroy
      if front_desk
        front_desk.destroy
        head :no_content
      else
        head :not_found
      end
    end

    private

    def front_desk_params
      params.require(:front_desk).permit(:id, :kvk_number, :code, :name,
                                         :establishment_number, :location,
                                         :time_zone, :alarm_unchecked_accounts,
                                         :alarm_fraud_suspension,
                                         :created_at, :updated_at, :max_issues, :blocked)
    end

    def front_desk
      @front_desk ||= FrontDesk.find_by_id(params[:id])
    end
  end
end
