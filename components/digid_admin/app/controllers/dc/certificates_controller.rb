
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
  class CertificatesController < Dc::BaseController
    skip_load_and_authorize_resource

    # GET /connections
    def index
      unless can?(:read, Dc::Organization) && can?(:read, Dc::Connection) && can?(:read, Dc::Service)
        flash[:alert] = "U mist het privilege om deze handeling te mogen uitvoeren."
        redirect_to root_path
      end
      @certificates = Dc::Certificate.all(page: params[:page_certificate])
      @certificate = Dc::Certificate.new
    end

    def search
      @certificate = Dc::Certificate.new(params[:dc_certificate])
      @certificates = Dc::Certificate.search(search_query.merge(page: params[:page_certificate]))

      log(:search)

      render :index
    end

    private

    def certificate_active_from
      return if params.dig(:dc_certificate, :certificate, "active_from(1i)").blank?

      params[:dc_certificate][:certificate][:active_from] = DateTime.new(
          params[:dc_certificate][:certificate]["active_from(1i)"].to_i,
          params[:dc_certificate][:certificate]["active_from(2i)"].to_i,
          params[:dc_certificate][:certificate]["active_from(3i)"].to_i
      )
      params[:dc_certificate][:certificate]["active_from(1i)"] = nil
      params[:dc_certificate][:certificate]["active_from(2i)"] = nil
      params[:dc_certificate][:certificate]["active_from(3i)"] = nil
    end

    def certificate_active_until
      return if params.dig(:dc_certificate, :certificate, "active_until(1i)").blank?

      params[:dc_certificate][:certificate][:active_until] = DateTime.new(
          params[:dc_certificate][:certificate]["active_until(1i)"].to_i,
          params[:dc_certificate][:certificate]["active_until(2i)"].to_i,
          params[:dc_certificate][:certificate]["active_until(3i)"].to_i
      )
      params[:dc_certificate][:certificate]["active_until(1i)"] = nil
      params[:dc_certificate][:certificate]["active_until(2i)"] = nil
      params[:dc_certificate][:certificate]["active_until(3i)"] = nil
    end

    def search_query
      certificate_active_from
      certificate_active_until
      @certificate.attributes.merge(params.dig(:dc_certificate).slice(:organization, :connection, :service, :certificate).to_h).symbolize_keys
    end

    def show_error_message
      @certificates = []
      @certificate = ::Dc::Certificate.new
      flash.now[:alert] = "Let op: Certificaten kunnen op dit moment niet worden opgehaald"
      render :index
    end

    def log_options
      { entity: "Certificaten dienstencatalogus", dc__certificate_id: @certificate.try(:id) }.compact
    end
  end
end
