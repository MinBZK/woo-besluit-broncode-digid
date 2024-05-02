
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

module MyDigid
  module Account
    module DigidHoog
      class WidsController < ApplicationController
        authorize_with_token :iapi_token, only: [:check_puk_request]

        include RvigClient
        include RdwClient
        include DwsClient
        include DigidHoogBsnCheck

        def fetch
          begin
            if Switch.driving_licence_partly_enabled?
              @driving_licences = fetch_driving_licences

              # check if documents are allowed to reset pin
              if Switch.driving_licence_pin_reset_enabled? && @driving_licences.present?
                @driving_licences.map do |dl|
                  begin
                    response = check_puk_request(dl, current_account.bsn)
                  rescue => e
                     Rails.logger.info("ERROR: DWS call mislukt: #{e.message}")
                  end
                  next unless response.present?

                  dl[:allow_pin_reset] = response["status"] == "OK"
                end
              end

              log_invalid_driving_licence_status(@driving_licences, current_account) if @driving_licences.present?
            end
          rescue DigidUtils::DhMu::RdwError => e
            handle_errors(e)
          end

          begin
            if Switch.identity_card_partly_enabled?
              @identity_cards = fetch_identity_cards
              log_invalid_identity_card_status(@identity_cards, current_account) if @identity_cards.present?
            end
          rescue DigidUtils::DhMu::RvigError => e
            handle_errors(e)
          end


          render json: {
            hoog_authenticators: "#{render_to_string(partial: 'my_digid/account/digid_hoog/hoog_authenticators', formats: "html")}",
            driving_licences: "#{render_to_string(partial: 'my_digid/account/digid_hoog/driving_licence/driving_licences', formats: "html")}",
            identity_cards: "#{render_to_string(partial: 'my_digid/account/digid_hoog/identity_card/identity_cards', formats: "html")}",
          }
        end

        private

        def check_puk_request(driving_licence, bsn)
          dws_client.check_puk_request(sequence_no: driving_licence[:e_id_volg_nr], document_type: "NL-Rijbewijs", bsn: bsn)
        end

        def log_invalid_driving_licence_status(driving_licences, account)
          # Log invalid driving licences once per session
          return if session[:driving_licence_invalid_logged].present?
          driving_licences.each do |driving_licence|
            if !driving_licence.valid?
              session[:driving_licence_invalid_logged] = true
              Log.instrument("1050", status: driving_licence.status, account_id: account.id, hidden: true) if !driving_licence.status_valid?
              Log.instrument("1050", status: driving_licence.status_mu, account_id: account.id, hidden: true) if !driving_licence.status_mu_valid?
            end
          end
        end

        def log_invalid_identity_card_status(identity_cards, account)
          # Log invalid driving licences once per session
          return if session[:identity_card_invalid_logged].present?
          identity_cards.each do |identity_card|
            if !identity_card.valid?
              session[:identity_card_invalid_logged] = true
              Log.instrument("1051", status: identity_card.status, account_id: account.id, hidden: true) if !identity_card.status_valid?
            end
          end
        end

        def handle_errors(error)
          Rails.logger.error "Start handling RDW/RvIG error"

          case error
          when DigidUtils::DhMu::RdwFault
            @rdw_error = parse_rdw_fault(error)
          when DigidUtils::DhMu::RdwTimeout, DigidUtils::DhMu::RdwHttpError
            @rdw_error = t("rdw_error_try_again_later")
            Log.instrument("927", account_id: current_account.id, hidden: true)
          when DigidUtils::DhMu::RvigFault
            @rvig_error = parse_rvig_fault(error)
          when DigidUtils::DhMu::RvigTimeout, DigidUtils::DhMu::RvigHttpError
            @rvig_error = t("rvig_error_try_again_later")
            Log.instrument("932", account_id: current_account.id, hidden: true)
          end
        end

        def parse_rdw_fault(error)
          if error.code == "OG1"
            Log.instrument("1044", message: error.message, account_id: current_account.id, hidden: true)
            t("rdw_error_try_again_later")
          elsif error.code.present?
            Log.instrument("1044", message: error.message, account_id: current_account.id, hidden: true)
            t("rdw_error")
          else
            Rails.logger.error "ERROR: No response from RDW"
            Log.instrument("927", account_id: current_account.id, hidden: true)
            t("rdw_error")
          end
        end

        def parse_rvig_fault(error)
          if error.message == "TechnicalFault" # rvig OG1 in use case documentation
            Log.instrument("1045", message: error.message, account_id: current_account.id, hidden: true)
            t("rvig_error_try_again_later")
          elsif error.message == "NotFound" # rvig OG2 in use case documentation
            Log.instrument("1045", message: error.message, account_id: current_account.id, hidden: true)
            t("rvig_error")
          else
            Rails.logger.error "ERROR: No response from RvIG"
            Log.instrument("932", account_id: current_account.id, hidden: true)
            t("rvig_error")
          end
        end
      end
    end
  end
end

