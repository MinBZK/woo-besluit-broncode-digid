
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

# You must require this file in application.rb, above the Application
# definition, for this to work. For example:
#
#   # PIDs prepended to logs
#   if Rails.env.production?
#     require File.expand_path('../../lib/pid_logger', __FILE__)
#   end
#
#   module MyApp
#     class Application < Rails::Application

require 'active_support/buffered_logger'

class PidLogger < ActiveSupport::BufferedLogger

  SEVERITIES = Severity.constants.sort_by{|c| Severity.const_get(c) }

  def add(severity, message = nil, progname = nil, &block)
    return if @level > severity
    message = (message || (block && block.call) || progname).to_s
    # If a newline is necessary then create a new message ending with a newline.
    # Ensures that the original message is not mutated.
    message = "#{message}\n" unless message[-1] == ?\n
    message.gsub!(/^(.+)/, "[#{$$}] #{SEVERITIES[severity]} \\1")
    buffer << message
    auto_flush
    message
  end

  class Railtie < ::Rails::Railtie
    initializer "swap in PidLogger" do
      Rails.logger = PidLogger.new(Rails.application.config.paths['log'].first, Rails.logger.level)
      ActiveSupport::Dependencies.logger = Rails.logger
      Rails.cache.logger = Rails.logger
      ActiveSupport.on_load(:active_record) do
        ActiveRecord::Base.logger = Rails.logger
      end
      ActiveSupport.on_load(:action_controller) do
        ActionController::Base.logger = Rails.logger
      end
      ActiveSupport.on_load(:action_mailer) do
        ActionMailer::Base.logger = Rails.logger
      end
    end
  end

end
