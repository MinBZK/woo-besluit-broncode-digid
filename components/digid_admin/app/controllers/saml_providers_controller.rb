
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

class SamlProvidersController < ApplicationController
  respond_to :html, :js

  def download_metadata
    @saml_provider = SamlProvider.find(params[:id])
    send_data @saml_provider.cached_metadata, filename: 'metadata.xml', disposition: 'inline'
  end

  def show_metadata
    @saml_provider = SamlProvider.find(params[:id])
    @metadata_urls = @saml_provider.metadata_urls

    render json: {
      dialog_body: render_to_string(partial: "metadata"),
      dialog_title: "Metadata"
    }
  end

  # GET /saml_providers/1/edit
  def edit_metadata
    @saml_provider = SamlProvider.find(params[:id])
    @remote = true

    render json: {
      dialog_body: render_to_string(partial: "metadata_form"),
      dialog_title: "Metadata"
    }
  end

  # PUT /saml_providers/1
  # PUT /saml_providers/1.xml
  def update
    @remote = true
    begin
      @saml_provider = SamlProvider.find(params[:id])
      uploaded_io = params[:saml_provider][:metadata_file]
      @saml_provider.cached_metadata = uploaded_io.read if uploaded_io.present?
      @saml_provider.metadata_url = params[:saml_provider][:metadata_url] if params[:saml_provider][:metadata_url].present?
      if @saml_provider.save
        render layout: false
      else
        render json: {
          dialog_body: render_to_string(partial: "metadata_form"),
          dialog_title: "Metadata"
        }
      end
    rescue
      flash.now[:error] = I18n.t('flash.saml_providers.metadata.upload')
      render json: {
        dialog_body: render_to_string(partial: "metadata_form"),
        dialog_title: "Metadata"
      }
    end
  end
end
