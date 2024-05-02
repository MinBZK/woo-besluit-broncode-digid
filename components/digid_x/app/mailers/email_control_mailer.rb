
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

# mailer for email check during a registration
class EmailControlMailer < ApplicationMailer
  # create a new activation code
  # email the user an email with an activation request
  # ED001
  # ED002
  def email_control_code(kwargs = {})
    @account_id = kwargs[:account_id]
    @recipient = kwargs[:recipient]
    @controle_code = kwargs[:controle_code]
    @confirm_email = kwargs[:account_exists] ? "email.body.confirm_email_change" : "email.body.confirm_email_request"
    @not_on_page = kwargs[:account_exists] ? "email.body.not_on_page_short" : "email.body.not_on_page_long"
    send_and_log(SentEmail::Reason::EMAIL)
  end

  # EDB002
  def email_control_code_balie(kwargs = {})
    @account_id = kwargs[:account_id]
    @recipient = kwargs[:recipient]
    @controle_code = kwargs[:controle_code]
    send_and_log(SentEmail::Reason::EMAIL_BALIE)
  end

  # ED029
  def email_control_code_app(kwargs = {})
    @account_id = kwargs[:account_id]
    @recipient = kwargs[:recipient]
    @controle_code = kwargs[:controle_code]
    send_and_log(SentEmail::Reason::EMAIL_APP)
  end

  private

  def send_and_log(reason)
    begin
      i18n_mail(account_id: @account_id, to: @recipient, subject: "email.subject.confirm_email")
    rescue StandardError
      Log.instrument("143", account_id: @account_id)
    else
      Log.instrument("142", account_id: @account_id)
    end
    SentEmail.create(account_id: @account_id, reason: reason)
  end
end
