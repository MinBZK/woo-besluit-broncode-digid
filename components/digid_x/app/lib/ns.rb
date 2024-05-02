
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

class Ns
  attr_reader :client

  def initialize(url:, timeout:)
    @client = DigidUtils::Ns.client
  end

  def register_app(app_id, notification_id, account_id, app_code, device_name, receive_notifications, os_type, os_version)
    response = client.register_app(app_id, notification_id, account_id, device_name, receive_notifications, os_type, os_version)
    register_mns_log(response, app_code, device_name, account_id)
  end

  def update_notification(app_id, account_id, notification_id, app_code, device_name, os_version)
    response = client.update_notification(app_id, account_id, notification_id, os_version)
    Log.instrument("1457", account_id: account_id, app_code: app_code, device_name: device_name, hidden: true) if response["status"] == "OK"
    Log.instrument("1458", account_id: account_id, app_code: app_code, device_name: device_name, hidden: true) if response["status"] == "APP_NOT_FOUND"
    register_mns_log(response, app_code, device_name, account_id)
    deregister_mns_log(response, app_code, device_name, account_id)

    response
  end

  def deregister_app(app)
    deregister_mns_log(client.deregister_app(app), app.app_code, app.device_name, app.account_id)
  end

  def send_notification(account_id, message_type, notification_subject, language)
    client.send_notification(account_id, message_type, notification_subject, language)
  end

  def get_notifications(account_id:)
    client.get_notifications(account_id: account_id)
  end

  private
  def register_mns_log(response, app_code, device_name, account_id)
    Log.instrument("1452", app_code: app_code, device_name: device_name, account_id: account_id, hidden: true) if response["status"] == "OK"
    Rails.logger.debug("Push notification registration failed") if response["status"] == "NOK"
    response
  end

  def deregister_mns_log(response, app_code, device_name, account_id)
    Log.instrument("1453", account_id: account_id, app_code: app_code, device_name: device_name, hidden: true) if response["status"] == "OK"
    Rails.logger.debug("Push notification deregistration failed") if response["status"] == "NOK"
    response
  end
end
