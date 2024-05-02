
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

module RvigClient
  extend ActiveSupport::Concern

  included do
    helper_method :presentable_identity_card if respond_to?(:helper_method)
  end

  def rvig_client
    Thread.current[:rvig_client] ||= DigidUtils::DhMu::RvigClient.new(
      endpoint: APP_CONFIG["urls"]["external"]["mu_rvig_endpoint"],
      read_timeout: ::Configuration.get_int("rvig_timeout"),
      ssl_cert_key_file: Rails.root.join(APP_CONFIG["rvig_ssl_cert_key_file"]&.path || APP_CONFIG["saml_ssl_cert_key_file"]&.path).to_s,
      ssl_cert_file: Rails.root.join(APP_CONFIG["rvig_ssl_cert_key_file"]&.path || APP_CONFIG["saml_ssl_cert_file"]&.path).to_s,
      ssl_cert_key_password: Rails.application.secrets.private_key_password,
      ssl_ca_cert_file: APP_CONFIG["gba_ssl_ca_cert_file"]&.path.present? ? Rails.root.join(APP_CONFIG["gba_ssl_ca_cert_file"]&.path).to_s : nil,
      ssl_verify_mode: :peer,
      log: Rails.env.test? ? false : true,
      logger: Rails.logger,
      pretty_print_xml: true
    )
  end

  def fetch_identity_cards
    if current_account.bsn.present?
      rvig_client.get(bsn: current_account.bsn).tap do |identity_cards|
        session[:rvig_statuses] = identity_cards.map do |dl|
          { status: dl.human_status, document_no: dl.document_no, sequence_no: dl.sequence_no, canonical_type: "identiteitskaart" }
        end
      end
    end
  end

  def presentable_identity_card
    if tonen_identiteitskaart_switch?
      session[:rvig_statuses]&.select{|identity_card| identity_card[:status] == "actief"}&.map{|card| OpenStruct.new(card.merge(canonical_type: "identiteitskaart"))} || []
    else
      []
    end
  end

  def active_identity_cards_in_session?
    session[:rvig_statuses]&.select{|identity_card| identity_card[:status] == "actief"}.present?
  end
end
