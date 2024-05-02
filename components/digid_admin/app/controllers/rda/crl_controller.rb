
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

module Rda
  class CrlController < BaseController
    load_and_authorize_resource

    def index
      instrument_logger("1192")
      @crls = Crl.all.sort_by(&:next_update)
    end

    def show
      instrument_logger("uc59.rda_crl_shown", rda__crl_id: @crl.id, issuer: @crl.issuer)
    end

    def create
      flash[:action] = "Het importeren van de RDA CRL"

      unless params[:crl]
        flash[:error] = I18n.t("rda.errors.no_file")
        redirect_to rda_crl_index_path
        return
      end

      begin
        response = rda_client.post('crls', params[:crl].read, {'Content-Type' => 'application/octet-stream'})
      rescue DigidUtils::Iapi::StatusError => e
        flash[:error] = begin
                          JSON.parse(e.res.body)["message"]
                        rescue JSON::ParserError
                        end
        flash[:error] ||= 'Technische fout'
        instrument_logger("uc59.rda_crl_import_error", file_name: params[:crl].original_filename, error: flash[:error])
      else
        crl = Crl.find(response.result['id'])
        instrument_logger("uc59.rda_crl_imported", rda__crl_id: crl.id, issuer: crl.issuer)
      end
      redirect_to rda_crl_index_path
    end

    def download
      instrument_logger("uc59.rda_crl_downloaded", rda__crl_id: @crl.id, issuer: @crl.issuer)
      send_data(Base64.decode64(@crl.raw), filename: @crl.filename)
    end
  end
end
