
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

class Dws
  attr_reader :client

  def initialize(url:, timeout:)
    @client = DigidUtils::Iapi::Client.new(url: url, timeout: timeout, ok_codes: [404])
  end

  def check_pen_request(sequence_no:, document_type:, bsn:)
    # TODO: Remove hardcoded "DRIVING_LICENCE"
    response = client.post("/iapi/check_pen_request",
                            body: {
                              sequenceNo: sequence_no,
                              docType: "DRIVING_LICENCE",
                              bsn: bsn
                            })
    result(response)
  end

  def create_pen_request!(sequence_no:, document_type:, bsn:)
    # TODO: Remove hardcoded "DRIVING_LICENCE"
    response = client.post("/iapi/create_pen_request",
                            body: {
                              sequenceNo: sequence_no,
                              docType: "DRIVING_LICENCE",
                              bsn: bsn
                            })
    result(response)
  end

  def request_vpuk(sequence_no:, document_type:, bsn:)
    # TODO: Remove hardcoded "DRIVING_LICENCE"
    response = client.post("/iapi/create_puk_request",
                            body: {
                              sequenceNo: sequence_no,
                              docType: "DRIVING_LICENCE",
                              bsn: bsn
                            })
    result(response)
  end

  def notify_pin_success(sequence_no:, document_type:, bsn:)
    # TODO: Remove hardcoded "DRIVING_LICENCE"
    response = client.post("/iapi/notify_pin_success",
                            body: {
                              sequenceNo: sequence_no,
                              docType: "DRIVING_LICENCE",
                              bsn: bsn
                            })
    result(response)
  end

  def check_puk_request(sequence_no:, document_type:, bsn:)
    # TODO: Remove hardcoded "DRIVING_LICENCE"
    response = client.post("/iapi/check_puk_request",
                            body: {
                              sequenceNo: sequence_no,
                              docType: "DRIVING_LICENCE",
                              bsn: bsn
                            })
    result(response)
  end

  def check_bv_bsn(document_number:, document_type:)
    response = client.post("/iapi/check_bv_bsn",body: {documentType: document_type,documentNumber: document_number})
    result(response)
  end

  def bsnk_activate(bsn:)
    begin
      response = client.post("/iapi/bsnk_activate", body: {bsn: bsn})
    rescue DigidUtils::Iapi::StatusError => e
      return { faultReason: e.class.to_s, faultDescription: e.message }
    end
    result(response)
  end

  private

  def result(response)
    if response&.status_code != 200
      { status: "ERROR", faultReason: response.status_code.to_s, faultDescription: "http code is not 200" }
    else
      response.result.slice("status", "vpuk", "faultReason", "faultDescription", "pip")
    end
  end
end
