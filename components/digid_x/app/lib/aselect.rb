
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

module Aselect
  def self.table_name_prefix
    "aselect_"
  end

  mattr_accessor :default_as_url
  @@default_as_url = "#{APP_CONFIG['aselect_protocol']}://#{APP_CONFIG["hosts"]["digid"]}#{APP_CONFIG['aselect_default_as_path']}"

  mattr_accessor :default_login_url
  @@default_login_url = "#{APP_CONFIG['aselect_protocol']}://#{APP_CONFIG["hosts"]["digid"]}#{APP_CONFIG['aselect_default_login_path']}"

  def self.default_server
    APP_CONFIG["aselect_default_server"] || organization
  end

  def self.organization
    "DigiD"
  end

  def self.verification_time
    5.minutes
  end

  def self.protocol
    APP_CONFIG["aselect_protocol"]
  end

  def self.setup
    yield self
  end
end
