
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

class SectorsController < ApplicationController
  load_and_authorize_resource
  respond_to :html, :js

  # GET /sectors
  def index
    instrument_logger('uc12.sectoren_inzien_gelukt', manager_id: session[:current_user_id])
  end

  # GET /sectors/1
  def show
    instrument_logger('uc12.sector_inzien_gelukt', sector_id: @sector.id)
  end

  # GET /sectors/new
  def new
    @remote = request.xhr?

    if request.xhr?
      render json: {
          dialog_body: render_to_string(partial: "form"),
          dialog_title: "Sector toevoegen"
        }
    end
  end

  # GET /sectors/1/edit
  def edit
  end

  # POST /sectors
  # POST /sectors.xml
  def create
    @remote = request.xhr?

      if @sector.save
        instrument_logger('uc12.sector_aanmaken_gelukt', sector_id: @sector.id)

        if request.xhr?
           render json: { target: "#sectors", append: "<option value='#{@sector.id}'>#{@sector.name}></option>" }
        else
          redirect_to @sector, notice: create_success(@sector)
        end
      else
        instrument_logger('uc12.sector_aanmaken_mislukt', errors: @sector.errors)

        if request.xhr?
          render json: { dialog_body: render_to_string(partial: "form"), dialog_title: "Sector toevoegen" }
        else
          render :new
        end
    end
  end

  # PUT /sectors/1
  # PUT /sectors/1.xml
  def update
    respond_to do |format|
      if @sector.update(params[:sector])
        instrument_logger('uc12.sector_wijzigen_gelukt', sector_id: @sector.id)
        format.html { redirect_to @sector, notice: update_success(@sector) }
        format.xml  { head :ok }
      else
        instrument_logger('uc12.sector_wijzigen_mislukt', sector_id: @sector.id, errors: @sector.errors)
        format.html { render :edit }
        format.xml  { render xml: @sector.errors, status: :unprocessable_entity }
      end
    end
  end

  # DELETE /sectors/1
  # DELETE /sectors/1.xml
  def destroy
    @sector.destroy

    respond_to do |format|
      format.html { redirect_to(sectors_path) }
      format.xml  { head :ok }
    end
  end
end
