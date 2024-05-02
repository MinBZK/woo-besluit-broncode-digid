
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

require 'jsonclient'
class MicroservicesController < ApplicationController
  authorize_resource
  respond_to :html
  def index
    instrument_logger('1347')

    begin
      @consul_services = fetch_consul_services
      @microservices = []
      @consul_services.each do |service_name, value|
        next if service_name == "consul" # Consul itself has no healthchecks defined
        health_check = fetch_health_check(service_name)
        @microservices << Microservice.new(name: service_name, status: health_check[0]["Status"])
      end
    rescue => e
      Rails.logger.error(e)
      flash[:alert] = "Kon geen verbinding maken met de health check service: #{e}"
    end
  end

  private

  def fetch_consul_services
    consul_url.present? ? consul_client.get(consul_url + "/v1/catalog/services").body : []
  end

  def fetch_health_check(service_name)
    consul_client.get(consul_url + "/v1/health/checks/#{service_name}").body
  end

  def consul_client
    @consul_client ||= JSONClient.new
  end


  def consul_url
    APP_CONFIG['urls']['internal']['consul']
  end
end
