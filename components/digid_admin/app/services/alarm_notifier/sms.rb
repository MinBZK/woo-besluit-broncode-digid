
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

class AlarmNotifier
  class Sms
    def initialize(report_type, message)
      @report_type = report_type
      @message = message
    end

    def send_reports
      mobile_numbers.each { |number| send_sms(number) }
    end

    private

    def send_sms(number)
      return if %w(acc1).include?(Rails.env)
      ::Sms.deliver(sms_params(number))
    end

    def sms_params(number)
      { sender: @message.sender, phone_number: number,
        message: @message.body, gateway: gateway, timeout: sms_timeout }
    end

    def subscribers
      Manager.sms_subscribers_for(@report_type)
    end

    def mobile_numbers
      subscribers.map(&:mobile_number).compact.map { |nr| "+316#{nr}" }.uniq
    end

    def gateway
      gateways = APP_CONFIG['sms_gateway']['regular'].split(',')
      key = "sms_gateways_regular"
      Redis.current.lpop(key) || (Redis.current.rpush(key, gateways * 5) && Redis.current.lpop(key))
    end

    def sms_timeout
      Configuration.get_string('sms_timeout')
    end
  end
end
