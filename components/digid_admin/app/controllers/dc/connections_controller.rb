
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
  class ConnectionsController < Dc::BaseController

    # GET /connections
    def index
      @connections = Dc::Connection.all(page: params[:page_connection])
      @connection = Dc::Connection.new
      @connections_in_review = FourEyesReview.where(record_table: "Dc::Connection")
    end

    def search
      @connection = Dc::Connection.new(params[:dc_connection])
      @connections = Dc::Connection.search(search_query.merge(page: params[:page_connection]))
      @connections_in_review = FourEyesReview.where(record_table: "Dc::Connection")
      log(:search)

      render :index
    end

    # GET /dc/connections/1
    # GET /dc/connections/1.xml
    def show
      @results = @connection.process_results(page: params[:page_process_result])
      @services = @connection.services(page: params[:page_service])
      @legacy_service_ids = @connection.legacy_service_ids
      @search_criteria = { webservice_id_in: @legacy_service_ids }.merge(params[:log_search]) if params[:log_search]
      log(:show)
    end

    # GET /dc/connections/new
    # GET /dc/connections/new.xml
    def new
      @connection = ::Dc::Connection.new
      @remote = request.xhr?
    end

    # GET /connections/1/edit
    def edit
    end

    # POST /dc/connections
    # POST /dc/connections.xml
    def create
      @connection = ::Dc::Connection.new(params[:dc_connection])

      if @connection.save_for_review(manager: current_user)
        log(:create)
        redirect_to(review_dc_connections_path(@connection.review))
      else
        render :new
      end
    end

    # PUT /dc/connections/1
    # PUT /dc/connections/1.xml
    def update
      if @connection.update_for_review(params[:dc_connection], manager: current_user)
        log(:update)
        redirect_to(review_dc_connections_path(@connection.review))
      else
        render :edit
      end
    end

    # DELETE /dc/connections/1
    # DELETE /dc/connections/1.xml
    def destroy
      if @connection.destroy_for_review(manager: current_user)
        log(:destroy)
        redirect_to(review_dc_connections_path(@connection.review))
      else
        flash[:alert] = "Verwijderen van aansluiting is niet gelukt."
        flash[:alert] << " Deze aansluiting heeft nog diensten." if  @connection.services.any?
        redirect_to(dc_connection_path(@connection))
      end
    end

    def upload_csv
      if params[:csv_upload]
        @dry_run = params[:csv_upload][:dry_run].to_s == "1"
        @response = JSON.parse(Dc::Service.upload_csv(params[:csv_upload][:csv_file], @dry_run).body)
      else
        flash[:alert] = "Geen csv bestand gekozen."
        redirect_to(dc_connections_path)
      end
    end

    def fetch_metadata
      DcCollectMetadataJob.perform_async(params[:id])

      head :ok
    end

    def show_error_message
      @connections = []
      @connection = Dc::Connection.new
      flash.now[:alert] = "Let op: Aansluitingen kunnen op dit moment niet worden opgehaald"
      render :index
    end

    def search_query
      @connection.attributes.merge(params.dig(:dc_connection).slice(:status, :organization).to_h).symbolize_keys
    end

    def log_options
      { entity: "Aansluiting", dc__connection_id: @connection.try(:id) }.compact
    end
  end
end
