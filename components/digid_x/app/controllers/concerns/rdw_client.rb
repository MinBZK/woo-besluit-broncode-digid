
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

module RdwClient
  extend ActiveSupport::Concern

  included do
    helper_method :presentable_driving_licence if respond_to? :helper_method
  end

  def rdw_client
    Thread.current[:rdw_client] ||= DigidUtils::DhMu::RdwClient.new(
      endpoint: APP_CONFIG["urls"]["external"]["mu_endpoint"],
      open_timeout: ::Configuration.get_int("rdw_timeout"),
      read_timeout: ::Configuration.get_int("rdw_timeout"),
      ssl_cert_key_file: Rails.root.join(APP_CONFIG["rdw_ssl_cert_key_file"]&.path || APP_CONFIG["saml_ssl_cert_key_file"]&.path).to_s,
      ssl_cert_file: Rails.root.join(APP_CONFIG["rdw_ssl_cert_key_file"]&.path || APP_CONFIG["saml_ssl_cert_file"]&.path).to_s,
      ssl_ca_cert_file: APP_CONFIG["gba_ssl_ca_cert_file"]&.path.present? ? Rails.root.join(APP_CONFIG["gba_ssl_ca_cert_file"]&.path).to_s : nil,
      ssl_cert_key_password: Rails.application.secrets.private_key_password,
      ssl_verify_mode: :peer,
      log: Rails.env.test? ? false : true,
      namespaces: {"xmlns:a" => "http://www.w3.org/2005/08/addressing"}
    )
  end

  def fetch_driving_licences
    if current_account.bsn.present?
      rdw_client.get(bsn: current_account.bsn).tap do |driving_licences|
        session[:rdw_statuses] = driving_licences.map do |dl|
          { status: dl.human_status, document_no: dl.document_no, sequence_no: dl.sequence_no, canonical_type: "rijbewijs" }
        end
      end
    end
  end

  def presentable_driving_licence
    if tonen_rijbewijs_switch?
      session[:rdw_statuses]&.select{|dl| dl[:status] == "actief"}&.map{|card| OpenStruct.new(card.merge(canonical_type: "rijbewijs"))} || []
    else
      []
    end
  end

  def active_driving_licences_in_session?
    session[:rdw_statuses]&.select{|dl| dl[:status] == "actief"}.present?
  end
end
