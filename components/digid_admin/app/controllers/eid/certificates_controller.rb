
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

module Eid
  class CertificatesController < BaseController
    load_and_authorize_resource

    def index
      instrument_logger("uc54.eid_certificate_overview_shown")
      @certificates = Eid::Certificate.all.sort_by(&:not_after)
    end

    def show
      instrument_logger("1117", eid__certificate_id: @certificate.id, subject: @certificate.subject)
    end

    def create
      flash[:action] = "Het importeren van het eID certificaat"

      unless params[:certificate]
        flash[:error] = I18n.t("eid.errors.no_file")
        redirect_to eid_certificates_path
        return
      end

      begin
        response = eid_client.post('certificates', params[:certificate].read, {'Content-Type' => 'application/octet-stream'})
      rescue DigidUtils::Iapi::StatusError => e
        flash[:error] = begin
                          JSON.parse(e.res.body)["message"]
                        rescue JSON::ParserError
                        end
        flash[:error] ||= 'Technische fout'
        instrument_logger("uc54.eid_certificate_import_error", file_name: params[:certificate].original_filename, error: flash[:error])
        redirect_to eid_certificates_path
        return
      end
      certificate = Eid::Certificate.find(response.result['id'])
      instrument_logger("uc54.eid_certificate_imported", eid__certificate_id: certificate.id, subject: certificate.subject)
      if certificate.type == 'AT'
        request = AtRequest.find_by(document_type: certificate.documentType, authorization: certificate.authorization, sequence_no: certificate.subject[-5..-1])
        if request
          request.update_attribute(:status, AtRequest::Status::FINISHED)
          instrument_logger("1110", eid__at_request_id: request.id, document_type: request.document_type, sequence_no: request.sequence_no)
        end
      end
      redirect_to eid_certificates_path
    end

    def download
      instrument_logger("uc54.eid_certificate_downloaded", eid__certificate_id: @certificate.id, subject: @certificate.subject)
      send_data(Base64.decode64(@certificate.raw), filename: @certificate.filename)
    end
  end
end
