
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
  class AtRequestsController < BaseController
    load_and_authorize_resource

    def index
      instrument_logger("1104")
      @at_requests = AtRequest.all
    end

    def show
      instrument_logger("1105", eid__at_request_id: @at_request.id, document_type: @at_request.document_type, sequence_no: @at_request.sequence_no)
    end

    def create
      if @at_request.invalid?
        render :new
        return
      end

      begin
        res = eid_client.post('certificates/at-request', {
          documentType: @at_request.document_type, authorization: @at_request.authorization,
          sequenceNo: @at_request.sequence_no, reference: @at_request.reference
        })
      rescue DigidUtils::Iapi::StatusError => e
        flash.now[:action] = "Het aanmaken van het eID AT-verzoek"
        flash.now[:error] = begin
                              JSON.parse(e.res.body)["message"]
                            rescue JSON::ParserError
                              "Technische fout"
                            end
        instrument_logger("1103", eid__at_request_id: @at_request.id, document_type: @at_request.document_type, sequence_no: @at_request.sequence_no, error: flash[:error])
        render :new
        return
      end
      @at_request.status = AtRequest::Status::CREATED
      @at_request.created_by = current_user
      @at_request.raw = Base64.decode64(res.result["request"])
      if @at_request.save
        instrument_logger("1106", eid__at_request_id: @at_request.id, document_type: @at_request.document_type, sequence_no: @at_request.sequence_no)
        redirect_to eid_at_requests_path
      else
        render :new
      end
    end

    def download
      raise CanCan::AccessDenied unless @at_request.download?
      instrument_logger("1111", eid__at_request_id: @at_request.id, document_type: @at_request.document_type, sequence_no: @at_request.sequence_no)
      send_data(@at_request.raw, filename: @at_request.filename)
    end

    def approve
      raise CanCan::AccessDenied if @at_request.created_by == current_user
      if @at_request.status != AtRequest::Status::CREATED
        redirect_to eid_at_requests_path, alert: I18n.t('eid.errors.reviewed')
        return
      end
      @at_request.status = AtRequest::Status::APPROVED
      @at_request.approved_by = current_user
      @at_request.approved_at = Time.zone.now
      @at_request.save(validate: false)
      instrument_logger("1107", eid__at_request_id: @at_request.id, document_type: @at_request.document_type, sequence_no: @at_request.sequence_no)
      AtRequestMailer.delay(queue: 'email-admin').approve(at_request_id: @at_request.id)
      redirect_to eid_at_requests_path, notice: 'Verzoek succesvol geaccordeerd!'
    end

    def reject
      if @at_request.status != AtRequest::Status::CREATED
        redirect_to eid_at_requests_path, alert: I18n.t('eid.errors.reviewed')
        return
      end
      @at_request.status = AtRequest::Status::REJECTED
      @at_request.save(validate: false)
      instrument_logger("uc55.eid_at_request_rejected", eid__at_request_id: @at_request.id, document_type: @at_request.document_type, sequence_no: @at_request.sequence_no)
      AtRequestMailer.delay(queue: 'email-admin').reject(at_request_id: @at_request.id, rejected_by_id: current_user.id, rejected_at: Time.zone.now)
      redirect_to eid_at_requests_path, notice: 'Verzoek succesvol afgekeurd!'
    end

    def send_email
      if @at_request.status != AtRequest::Status::APPROVED && @at_request.status != AtRequest::Status::SENT
        redirect_to eid_at_requests_path, alert: I18n.t('eid.errors.aborted')
        return
      end

      case @at_request.document_type
      when DocumentType::DL
         recipient = ::Configuration.get_string('RDW_e-mailadres_eID_AT-verzoeken')
      when DocumentType::NIK
         recipient = ::Configuration.get_string('RVIG_e-mailadres_eID_AT-verzoeken')
      end

      if recipient.blank?
        redirect_to eid_at_requests_path, alert: I18n.t('eid.errors.no_email')
        return
      end

      @at_request.status = AtRequest::Status::SENT
      @at_request.sent_by = current_user
      @at_request.sent_at = Time.zone.now
      @at_request.save(validate: false)
      instrument_logger("1109", eid__at_request_id: @at_request.id, document_type: @at_request.document_type, sequence_no: @at_request.sequence_no)
      AtRequestMailer.delay(queue: 'email-admin').sign(at_request_id: @at_request.id, recipient: recipient)
      redirect_to eid_at_requests_path, notice: 'Verzoek succesvol verzonden!'
    end

    def abort
      if @at_request.status != AtRequest::Status::APPROVED && @at_request.status != AtRequest::Status::SENT
        redirect_to eid_at_requests_path, alert: I18n.t('eid.errors.aborted')
        return
      end
      @at_request.status = AtRequest::Status::ABORTED
      @at_request.save(validate: false)
      instrument_logger("uc55.eid_at_request_aborted", eid__at_request_id: @at_request.id, document_type: @at_request.document_type, sequence_no: @at_request.sequence_no)
      redirect_to eid_at_requests_path, notice: 'Verzoek succesvol afgebroken!'
    end
  end
end
