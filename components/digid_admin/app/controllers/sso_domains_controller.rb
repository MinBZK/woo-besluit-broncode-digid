
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

class SsoDomainsController < ApplicationController
  load_and_authorize_resource
  respond_to :html, :js

  # GET /sso_domains
  def index
    instrument_logger('uc29.sso_domein_overzicht_inzien_gelukt', manager_id: session[:current_user_id])
  end

  # GET /sso_domains/1
  def show
    instrument_logger('uc29.sso_domein_inzien_gelukt', domain: @sso_domain.name, sso_domain_id: @sso_domain.id, manager_id: session[:current_user_id])
  end

  # GET /sso_domains/new
  def new
    @remote = request.xhr?

    if request.xhr?
      render json: {
        dialog_body: render_to_string(partial: "form"),
        dialog_title: "SSO Domein toevoegen"
      }
    end
  end

  # GET /sso_domains/1/edit
  def edit
  end

  # POST /sso_domains
  # POST /sso_domains.xml
  def create
    @remote = request.xhr?

    if @sso_domain.save
      instrument_logger('uc29.sso_domein_aanmaken_gelukt', domain: @sso_domain.name, manager_id: session[:current_user_id])

      if request.xhr?
        render json: { redirect_url: sso_domain_path(@sso_domain) }
      else
        redirect_to(@sso_domain, notice: 'Sso domain was successfully created.')
      end
    else
      instrument_logger('uc29.sso_domein_aanmaken_mislukt', domain: @sso_domain.name, errors: @sso_domain.errors, manager_id: session[:current_user_id])

      if request.xhr?
        render json: { dialog_body: render_to_string(partial: "form"), dialog_title: "SSO Domein toevoegen" }
      else
        render :new
      end
    end
  end

  # PUT /sso_domains/1
  # PUT /sso_domains/1.xml
  def update
    respond_to do |format|
      instrument_logger('uc29.sso_domein_wijzigen_gelukt', domain: @sso_domain.name, manager_id: session[:current_user_id])
      if @sso_domain.update(params[:sso_domain])
        format.html { redirect_to(@sso_domain, notice: 'Sso domain was successfully updated.') }
        format.xml  { head :ok }
      else
        instrument_logger('uc29.sso_domein_wijzigen_gelukt', domain: @sso_domain.name, manager_id: session[:current_user_id], errors: @sso_domain.errors)
        format.html { render :edit }
        format.xml  { render xml: @sso_domain.errors, status: :unprocessable_entity }
      end
    end
  end

  # DELETE /sso_domains/1
  # DELETE /sso_domains/1.xml
  def destroy
    if SamlProvider.destroy_sso_domain(@sso_domain)
      instrument_logger('uc29.sso_domein_verwijderen_gelukt', domain: @sso_domain.name, manager_id: session[:current_user_id])
    else
      instrument_logger('uc29.sso_domein_verwijderen_mislukt', domain: @sso_domain.name, manager_id: session[:current_user_id])
    end

    respond_to do |format|
      format.html { redirect_to(sso_domains_url) }
      format.xml  { head :ok }
    end
  end
end
