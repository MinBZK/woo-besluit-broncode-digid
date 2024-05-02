
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

class OrganizationsController < ApplicationController
  load_and_authorize_resource
  respond_to :html, :js
  include ActionView::Helpers::FormOptionsHelper

  # GET /organizations
  def index
    @search        = Organization.ransack(params[:q])
    @organizations = @search.result(distinct: true).page(params[:page]).includes(:webservices)
    instrument_logger('uc23.organisatie_overzicht_inzien_gelukt', manager_id: session[:current_user_id])
  end

  # GET /organizations/1
  # GET /organizations/1.xml
  def show
    instrument_logger('uc23.organisatie_inzien_gelukt', organization_id: @organization.id)
  end

  # GET /organizations/new
  # GET /organizations/new.xml
  def new
    @remote = request.xhr?

    if request.xhr?
       render json: { dialog_body: render_to_string(partial: "form"), dialog_title: "Organizatie toevoegen" }
    else
      render :new
    end
  end

  # GET /organizations/1/edit
  def edit
  end

  # POST /organizations
  # POST /organizations.xml
  def create
    @remote       = request.xhr?

    if @organization.save
      instrument_logger('uc23.organisatie_aanmaken_gelukt', organization_id: @organization.id)

      if request.xhr?
        render json: { target: "#webservice_organization_id", body:  organization_options }
      else
        redirect_to(@organization, notice: 'Organisatie is succesvol aangemaakt.')
      end
    else
      if request.xhr?
       render json: { dialog_body: render_to_string(partial: "form"), dialog_title: "Organizatie toevoegen" }
      else
        render :new
      end
    end
  end

  # PUT /organizations/1
  # PUT /organizations/1.xml
  def update
    respond_to do |format|
      if @organization.update(params[:organization])
        instrument_logger('uc23.organisatie_wijzigen_gelukt', organization_id: @organization.id)
        format.html { redirect_to(@organization, notice: 'Organization was successfully updated.') }
        format.xml { head :ok }
      else
        format.html { render :edit }
        format.xml { render xml: @organization.errors, status: :unprocessable_entity }
      end
    end
  end

  # DELETE /organizations/1
  # DELETE /organizations/1.xml
  def destroy
    @organization.destroy

    if @organization.destroyed?
      instrument_logger('uc23.organisatie_verwijderen_gelukt', organization_id: @organization.id)
      redirect_to(organizations_path)
    else
      flash[:alert] = t('flash.organization.destroy_not_allowed')
      redirect_to(organization_path(@organization.id))
    end
  end

  private

  def organization_options(selected_organization = nil)
    options_for_select(Organization.all.map { |o| [o.name, o.id] }, selected_organization)
  end
end
