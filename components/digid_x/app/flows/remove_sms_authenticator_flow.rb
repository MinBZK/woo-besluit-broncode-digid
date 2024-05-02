
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

class RemoveSmsAuthenticatorFlow
  # initialize > verify_password
  FLOW = {
    verify_password: { page_name: "D20A", transitions: [:completed] },
    completed: { transitions: [] }
  }.freeze

  include FlowControl

  def initialize
    @flow = FLOW.dup
    @state = :verify_password
  end

  def process
    :remove_sms_verification
  end

  def verify_password_completed(controller)
    account = controller.send(:current_account)

    if account.email_activated?
      NotificatieMailer.delay(queue: "email").notify_sms_controle_deactivated(account_id: account.id, recipient: account.adres)
    end
    sms_service = SmsChallengeService.new(account: account)
    account.with_language { sms_service.send_sms(message: controller.t("sms_message.SMS23"), spoken: false) }

    account.sms_tools.destroy_all
    account.destroy_old_email_recovery_codes do
      Log.instrument("889", account_id: account.id, attribute: "telefoonnummer", hidden: true)
    end
    Log.instrument("171", account_id: account.id)

    controller.flash[:notice] = controller.t("sms_code_authenticaton_removed")
    @state = :completed
  end
end
