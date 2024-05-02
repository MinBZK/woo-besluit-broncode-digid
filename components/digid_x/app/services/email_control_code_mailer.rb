
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

class EmailControlCodeMailer
  attr_reader :account, :email, :type
  delegate :email, to: :account

  def initialize(account, type = "mijn")
    @account = account
    @type = type
  end

  def perform
    generate_activation_code
    deliver_mail
  end

  private

  def deliver_mail
    email_controller = EmailControlMailer.delay(queue: "email")

    case @type
    when "app"
      email_controller.email_control_code_app(account_id: account.id, recipient: account.adres, controle_code: email.controle_code)
    when "balie"
      email_controller.email_control_code_balie(account_id: account.id, recipient: account.adres, controle_code: email.controle_code)
    else
      email_controller.email_control_code(account_id: account.id, recipient: account.adres, controle_code: email.controle_code, account_exists: account.state.active?)
    end

    account.email_deliveries.create(scheduled: Time.zone.now)
  end

  def generate_activation_code
    email.delete_attempts
    email.controle_code = VerificationCode.generate("E")
    email.status = Email::Status::NOT_VERIFIED
    email.save!
  end
end
