
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

module DigidUtils
  class Ns
    class << self
      def client
        Thread.current[:ns_client] ||= Ns.new(url: APP_CONFIG["urls"]["internal"]["ns"], timeout: timeout)
      end

      def timeout
        APP_CONFIG["concern_timeout"] || 3
      end
    end

    attr_reader :client

    def initialize(url:, timeout:)
      @client = DigidUtils::Iapi::Client.new(url: url, timeout: timeout, ok_codes: [404])
    end

    def register_app(app_id, notification_id, account_id, device_name, receive_notifications, os_type, os_version)
      begin 
        JSON.parse(client.put("/iapi/register", body: {
        appId: app_id,
        notificationId: notification_id,
        accountId: account_id,
        deviceName: device_name,
        receiveNotifications: receive_notifications,
        osType: os_type,
        osVersion: os_version
        }).body)
      rescue DigidUtils::Iapi::Error => e
        Rails.logger.debug("Call to digid_ns failed, check if the service is up and running. Ignoring Error for now." + e.message)
        false
      end
    end

    def update_notification(app_id, account_id, notification_id, os_version)
      begin
        JSON.parse(client.post("/iapi/update_notification", body: {
          appId: app_id,
          accountId: account_id,
          notificationId: notification_id,
          osVersion: os_version
        }).body)
      rescue DigidUtils::Iapi::Error => e
        Rails.logger.debug("Call to digid_ns failed, check if the service is up and running. Ignoring Error for now." + e.message)
        false
      end
    end

    def deregister_app(app)
      begin
        JSON.parse(client.put("/iapi/deregister", body: {
          appId: app.user_app_id,
        }).body)
      rescue DigidUtils::Iapi::Error => e
        Rails.logger.debug("Call to digid_ns failed, check if the service is up and running. Ignoring Error for now." + e.message)
        false
      end
    end

    def send_notification(account_id, message_type, notification_subject, language)
      begin
        JSON.parse client.put("/iapi/send_notification", body: {
        accountId: account_id,
        messageType: message_type,
        notificationSubject: notification_subject,
        preferredLanguage: language
        }).body
      rescue DigidUtils::Iapi::Error => e
        Rails.logger.debug("Call to digid_ns failed, check if the service is up and running. Ignoring Error for now." + e.message)
        false
      end
    end

    def get_notifications(account_id:)
      begin
        JSON.parse(client.get("/iapi/get_notifications?accountId=#{account_id}").body)
      rescue DigidUtils::Iapi::Error => e
         Rails.logger.debug("Call to digid_ns failed, check if the service is up and running. Ignoring Error for now." + e.message)
        render json: { status: "OK", notifications: []}
      end
    end
  end
end
