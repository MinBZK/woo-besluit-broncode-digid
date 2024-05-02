
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

require "net/sftp"

module LetterSftp
  def host
    APP_CONFIG["letter_sftp_server"]
  end

  def username
    APP_CONFIG["letter_sftp_username"]
  end

  def options
    if APP_CONFIG["letter_sftp_password"].present?
      { password: APP_CONFIG["letter_sftp_password"] }
    else
      { passphrase: Rails.application.secrets.private_key_password }
    end
  end

  def local_path
    APP_CONFIG["letter_local_path"]
  end

  def remote_upload_path
    APP_CONFIG["letter_remote_upload_path"]
  end

  def remote_download_path
    APP_CONFIG["letter_remote_download_path"]
  end

  def on_error(_e)
    raise "please implement on_error method in your class"
  end

  def start
    Net::SFTP.start(host, username, options) do |sftp|
      yield sftp
    end
  rescue => e
    on_error e
  end
end
