
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

class AtRequestMailer < ActionMailer::Base
  default from: APP_CONFIG['mailer_default_email_address'] || "noreply@#{APP_CONFIG['hosts']['digid']}"

  def approve(at_request_id:)
    @at_request = Eid::AtRequest.find(at_request_id)
    return if @at_request.created_by.email.blank?
    subject = default_i18n_subject(environment: Rails.env.capitalize, name: at_request_id)
    mail(to: @at_request.created_by.email, subject: subject, &:text)
  end

  def reject(at_request_id:, rejected_by_id:, rejected_at:)
    @at_request = Eid::AtRequest.find(at_request_id)
    @rejected_by = Manager.find(rejected_by_id)
    return if @at_request.created_by.email.blank?
    @rejected_at = rejected_at

    subject = default_i18n_subject(environment: Rails.env.capitalize, name: at_request_id)
    mail(to: @at_request.created_by.email, subject: subject, &:text)
  end

  def sign(at_request_id:, recipient:)
    @at_request = Eid::AtRequest.find(at_request_id)
    attachments[@at_request.filename] = @at_request.raw
    subject = default_i18n_subject(sequence_no: @at_request.sequence_no)
    subject = "#{subject} (#{Rails.env})" unless Rails.env.productie?
    mail(to: recipient, subject: subject, &:text)
  end
end
