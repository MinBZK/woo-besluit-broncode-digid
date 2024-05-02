
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

class Sms::Gateway
  include HTTParty

  attr_accessor :gateway, :timeout, :message, :phone_number, :sender, :reference, :locale

  def self.factory(options)
    options[:spoken] ? Spoken.new(options) : Regular.new(options)
  end

  def initialize(options = {})
    validate(options)

    options.each { |key, value| instance_variable_set(:"@#{key}", value) unless value.nil? }
  end

  def deliver
    response = send_request
    if response.ok? && delivered?(response)
      logger.debug "#{klass} SMS delivered to #{phone_number}"
      true
    else
      logger.error "#{klass} SMS not delivered to #{phone_number}: #{response.code} #{response.body}"
      false
    end
  rescue EOFError, Timeout::Error, SocketError => e
    logger.error "#{klass} SMS error to #{phone_number}: #{e.class} #{e.message}"
    false
  end

  def send_request
    self.class.post(gateway, body: message_object.body.to_json, timeout: @timeout, headers: message_object.headers)
  end

  def klass
    self.class.to_s.gsub(/^.*::/, '').to_s
  end

  def message_object
    Object.const_get("Sms::Message::#{klass}").new(
      phone_number: phone_number.gsub(/\A\+/, '00'),
      message: message,
      sender: sender,
      reference: reference,
      locale: locale
    )
  end

  private
  def logger
    @@logger ||= defined?(Rails) ? ::Rails.logger : ::Logger.new('log/sms.log')
  end

  def required_parameters
    [:sender, :gateway, :message, :phone_number]
  end

  def validate(options)
    raise ValidationError unless required_parameters.all? { |k| !options[k].nil? }
  end

  class ValidationError < StandardError
  end
end
