
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

require "json"

class EnvironmentsOverview
  class << self
    def investigate
      info = {
        ruby_version: ruby_version,
        ruby_created_at: ruby_created_at,
        brp: brp,
        digid_created_at: Time.zone.now,
        digid_version: digid_version,
        environment: Rails.env
      }

      $stdout.puts "--- BEGIN INVESTIGATION ---"
      $stdout.puts info.to_json
      $stdout.puts "--- END INVESTIGATION ---"
    end

    private

    def digid_version
      path = Rails.root.join(".git-version")
      path.exist? ? path.read.strip : nil
    end

    def ruby_created_at
      File.file?(RbConfig.ruby) ? File.ctime(RbConfig.ruby) : nil
    end

    def ruby_version
      path = Rails.root.join(".ruby-version")
      path.exist? ? path.read.strip : nil
    end

    def brp
      case APP_CONFIG["urls"]["external"]["gba"]
      when /\/\/lap\.gbav\.idm/
        "Proeftuin"
      when /\/\/gbav\.idm/
        "Productie"
      when /stubs|127\.0\.0\.1:87/
        "Stub"
      else
        ""
      end
    end
  end
end
