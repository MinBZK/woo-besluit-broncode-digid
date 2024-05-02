
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

module Saml
  class WidAuthnResponse
    delegate :in_response_to, :_id, to: :@response

    def initialize(response)
      @response = response
    end

    def success?
      status_code == Saml::TopLevelCodes::SUCCESS
    end

    def to_s
      [status_code, status_description, account_id].compact.join(", ")
    end

    def app_session_id
      attribute = @response.assertion.attribute_statement.attributes.find { |attr| attr.name == "app_session_id" }
      attribute ? attribute.attribute_values.first.content : nil
    end

    def account
      @account ||= begin
        ::Rails.logger.info "Retrieving account for #{account_id}"
        Account.find_by(id: account_id)
      end
    end

    def document_type
      attribute = @response.assertion.attribute_statement.attributes.find { |attr| attr.name == "document_type" }
      attribute ? attribute.attribute_values.first.content : nil
    end

    def status_description
      @response.status.status_detail.status_description
    end

    private

    def status_code
      @response.status.status_code.value
    end

    def account_id
      @response.assertion.try(:subject).try(:name_id)
    end
  end
end
