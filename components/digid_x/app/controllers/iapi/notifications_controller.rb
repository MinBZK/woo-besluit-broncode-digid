
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

# frozen_string_literal: true

module Iapi
  class NotificationsController < ApplicationController
    skip_before_action :verify_authenticity_token
    authorize_with_token :iapi_token

    def send_pin_request_notification
      return head 400 if params[:bsn].blank? || params[:docType].blank? # bad request
      # endpoint that is being called by DWS to send e-mail/sms for succesfully requesting pin from RDW/RVIG
      return render json: { status: "ERROR", message: "Invalid document type" } unless ["NI", "NL-Rijbewijs"].include?(params[:docType])
      sectorcode = Sectorcode.find_by(sectoraalnummer: params[:bsn])
      return render json: { status: "ERROR", message: "BSN not found" } if sectorcode.blank?
      account = Account.find_by(id: sectorcode.account_id)
      return render json: { status: "ERROR", message: "BSN found but has no account" } if account.blank?

      if account.email_activated?
        NotificatieMailer.delay(queue: "email").notify_request_pin_rijbewijs(account_id: account.id, recipient: account.adres) if params[:docType] == "NL-Rijbewijs"
      elsif account.phone_number.present?
        sms_service = SmsChallengeService.new(account: account)
        account.with_language { sms_service.send_sms(message: t("sms_message.SMS18", wid_type: t("document.driving_licence")), spoken: false) if params[:docType] == "NL-Rijbewijs" }
      else
        return render json: { status: "ERROR", message: "Account has no e-mail and no sms" }
      end
    end
  end
end
