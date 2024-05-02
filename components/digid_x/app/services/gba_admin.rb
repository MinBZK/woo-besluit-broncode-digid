
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

class GbaAdmin
  module Accounts
    GBA_2500022 = "2500022"
    GBA_2500023 = "2500023"
  end

  def self.change_password(old_password, new_password, update_gba = false)
    credentials = non_active_account

    return "invalid password for #{credentials['gba_ssl_username']}" unless credentials["gba_ssl_password"].eql?(old_password)

    if update_gba
      client        = create_client(non_active_account) # Create a client for the GBA service
      gba_response = request(client, new_password) # Send request to GBA service

      return "not a valid change_password request" unless gba_response.present?

      change_password_return = gba_response.body[:change_password_response][:change_password_return]
      if change_password_return[:code].to_i == 0
        credentials["gba_ssl_password"] = new_password
        Configuration.gba_user = credentials
        "succesfully changed password for #{credentials['gba_ssl_username']}"
      else
        "password not changed: (error: #{change_password_return[:code]}, omschrijving: #{change_password_return[:omschrijving]})"
      end
    else
      credentials["gba_ssl_password"] = new_password
      Configuration.gba_user = credentials
      "succesfully changed password for #{credentials['gba_ssl_username']} (database only)"
    end
  end

  def self.switch_account
    alternate(active_gba_user).tap do |value|
      Configuration.find_by(name: :active_gba_user).update_attribute(:value, value)
    end
  end

  def self.username
    active_account["gba_ssl_username"]
  end

  def self.password
    active_account["gba_ssl_password"]
  end

  def self.active_account
    JSON.parse(gba_user_credentials(active_gba_user))
  end

  def self.non_active_account
    JSON.parse(gba_user_credentials(alternate(active_gba_user)))
  end

  def self.gba_user_credentials(user)
    Configuration.get_string_without_cache("gba_user_#{user}")
  end

  def self.alternate(gba_user)
    if gba_user.eql?(Accounts::GBA_2500022)
      Accounts::GBA_2500023
    elsif gba_user.eql?(Accounts::GBA_2500023)
      Accounts::GBA_2500022
    else
      raise "current user (#{gba_user}) not found"
    end
  end

  def self.active_gba_user
    Configuration.get_string_without_cache("active_gba_user")
  end

  def self.create_client(credentials)
    Savon.client(
      log: false,
      ssl_cert_key_file: ssl_cert_key_file,
      ssl_cert_file: ssl_cert_file,
      ssl_cert_key_password: Rails.application.secrets.private_key_password,
      ssl_ca_cert_file:  ssl_ca_cert_file,
      endpoint: APP_CONFIG["urls"]["external"]["gba"],
      wsdl: Rails.root.join("config/wsdl/LrdPlus1_1.wsdl"),
      ssl_verify_mode: :client_once,
      basic_auth: [credentials["gba_ssl_username"], credentials["gba_ssl_password"]]
    )
  end

  def self.ssl_ca_cert_file
    file = APP_CONFIG["gba_ssl_ca_cert_file"]&.path
    Rails.root.join(file).to_s if file
  end

  def self.ssl_cert_key_file
    file = APP_CONFIG["gba_ssl_cert_key_file"]&.path
    Rails.root.join(file).to_s if file
  end

  def self.ssl_cert_file
    file = APP_CONFIG["gba_ssl_cert_file"]&.path
    Rails.root.join(file).to_s if file
  end

  def self.request(client, new_password)
    client.call(:change_password) do
      message(in0: new_password)
    end
  end
end
