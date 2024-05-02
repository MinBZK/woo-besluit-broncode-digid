
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
  class OrganizationsController < Dc::BaseController

    include ActionView::Helpers::FormOptionsHelper

    # GET /organizations
    def index
      @organizations = Dc::Organization.all(page: params[:page_organization])
      @organization = Dc::Organization.new
      @organizations_in_review = FourEyesReview.where(record_table: "Dc::Organization")
    end

    def search
      @organization = Dc::Organization.new(params[:dc_organization])
      @organizations = Dc::Organization.search(search_query.merge(page: params[:page_organization]))
      @organizations_in_review = FourEyesReview.where(record_table: "Dc::Organization")

      log(:search)

      render :index
    end

    # GET /dc/organizations/1
    # GET /dc/organizations/1.xml
    def show
      @services = [] #@organization.services(page: params[:page_service])
      @connections = @organization.connections(page: params[:page_connection])

      log(:show)

      if request.xhr?
        return render json: { body: "#{options_for_select(@organization.organization_roles.map{|x| [x.type, x.id]})}" }
      end
    end

    # GET /dc/organizations/new
    # GET /dc/organizations/new.xml
    def new
      @organization = ::Dc::Organization.new
      @remote = request.xhr?
    end

    # GET /organizations/1/edit
    def edit
    end

    # POST /dc/organizations
    # POST /dc/organizations.xml
    def create
      @organization = ::Dc::Organization.new(params[:dc_organization])

      if @organization.save_for_review(manager: current_user)
        log(:create)
        redirect_to(review_dc_organizations_path(@organization.review))
      else
        @organization.check_organization_roles
        render :new
      end
    end

    # PUT /dc/organizations/1
    # PUT /dc/organizations/1.xml
    def update
      if @organization.update_for_review(params[:dc_organization], manager: current_user)
        log(:update)
        redirect_to(review_dc_organizations_path(@organization.review))
      else
        @organization.check_organization_roles
        render :edit
      end
    end

    # DELETE /dc/organizations/1
    # DELETE /dc/organizations/1.xml
    def destroy
      if @organization.destroy_for_review(manager: current_user)
        log(:destroy)
        redirect_to(review_dc_organizations_path(@organization.review), notice: "De organisatie is verwijderd.")
      else
        flash[:alert] = "Verwijderen van organisatie is niet gelukt."
        flash[:alert] << " Deze organisatie heeft nog aansluitingen/diensten." if @organization.connections.any? || @organization.services.any?
        redirect_to(dc_organization_path(@organization))
      end
    end

    def upload_csv
      if params[:csv_file].present?
        file = params[:csv_file]
        dc_params = {}
        dc_params[:file] = Base64.encode64(file.read) if file.respond_to?(:read)
        dc_params[:dry_run] = params[:dry_run].to_s == "1"
        @response = JSON.parse(DigidUtils::Dc.client.post("/iapi/dc/organizations/csv_upload", dc_params).body)
      else
        flash[:alert] = "Geen Upload csv bestand gekozen."
        redirect_to(dc_organizations_path)
      end
    end

    def show_error_message
      @organizations = []
      @organization = ::Dc::Organization.new
      flash.now[:alert] = "Let op: Organisaties kunnen op dit moment niet worden opgehaald"
      render :index
    end

    private

    def search_query
      @organization.attributes.merge(params.dig(:dc_organization).slice(:status).to_h).symbolize_keys
    end

    def log_options
      { entity: "Organisatie", dc__organization_id: @organization.try(:id) }.compact
    end
  end
end
