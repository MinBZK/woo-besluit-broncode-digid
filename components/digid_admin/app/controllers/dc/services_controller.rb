
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

module Dc
  class ServicesController < Dc::BaseController

    before_action :find_connection, only: [:new, :create, :edit, :update, :review]

    # GET /services
    def index
      @services = Dc::Service.all(page: params[:page_service])
      @service = Dc::Service.new
      @services_in_review = FourEyesReview.where(record_table: "Dc::Service")
    end

    def search
      @service = Service.new(params[:dc_service])
      @services = Dc::Service.search(search_query.merge(page: params[:page_service]))
      @services_in_review = FourEyesReview.where(record_table: "Dc::Service")
      log(:search)

      render :index
    end

    # GET /dc/services/1
    # GET /dc/services/1.xml
    def show
      @search_criteria = { webservice_id: @service.legacy_service_id }.merge(params[:log_search]) if params[:log_search]
      log(:show)
    end

    def new
      @service = Dc::Service.new(connection_id: @connection.id)
    end

    # POST /dc/services
    def create
      @service = ::Dc::Service.new(params[:dc_service])
      @service.digid = true

      if @service.save_for_review(manager: current_user)
        log(:create)
        redirect_to(review_dc_services_path(@service.review))
      else
        render :new
      end
    end


    # GET /services/1/edit
    def edit
    end

    # PUT /dc/services/1
    # PUT /dc/services/1.xml
    def update
    if @service.update_for_review(params[:dc_service], manager: current_user)
        log(:update)
        redirect_to(review_dc_services_path(@service.review), notice: 'Dienst is successvol geupdate.')
      else
       render :edit
      end
    end

    # DELETE /dc/services/1
    # DELETE /dc/services/1.xml
    def destroy
     if @service.destroy_for_review(manager: current_user)
        log(:destroy)
        redirect_to(review_dc_services_path(@service.review), notice: 'De dienst is verwijderd.')
      else
        flash[:alert] = "Verwijderen van dienst is niet gelukt."
        redirect_to(dc_services_path(@service))
      end
    end

    def show_error_message
      @services = []
      @service = Dc::Service.new
      flash.now[:alert] = "Let op: Diensten kunnen op dit moment niet worden opgehaald"
      render :index
    end

    def search_query
      @service.search_attributes.merge(params.dig(:dc_service).slice(:connection).to_h).symbolize_keys
    end

    def log_options
      { entity: "Dienst", dc__service_id: @service.try(:id) }.compact
    end

    private

    def find_connection
      @connection = params[:connection_id] ? Connection.find(params[:connection_id]) : @service.connection
    end
  end
end
