
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
  class Base
    include ActiveModel::AttributeMethods
    include ActiveModel::Conversion
    include ActiveModel::Validations
    extend ActiveModel::Naming
    extend ActiveModel::Translation
    extend ActiveModel::Callbacks
    extend ActiveRecord::Reflection::ClassMethods

    class_attribute :_attributes
    self._attributes = []

    attribute_method_suffix "?"

    def initialize(options = {})
      options.each do |key, value|
        send("#{key}=", value)
      end
    end

    def logger
      ActiveRecord::Base.logger
    end

    def self.attributes(*names)
      attr_accessor *names
      define_attribute_methods names
      self._attributes += names
    end

    def attributes
      self._attributes.inject({}) do |hash, attr|
        hash[attr.to_s] = send(attr)
        hash
      end
    end

    def persisted?
      false
    end

    def webservice
      if aselect_request.aselect_type == "WSDL"
        @webservice ||= if APP_CONFIG["authentication_method"] == "fingerprint"
          Aselect::Webservice.checked_webservice_by_fingerprint(fingerprint)
        else
          Aselect::Webservice.checked_webservice_by_distinguished_name(distinguished_name)
        end
      else
        @webservice ||= Aselect::Webservice.checked_webservice_by_shared_secret(shared_secret)
      end
    end

    # Check a_select_server
    def check_a_select_server
      if a_select_server != Aselect.default_server
        Aselect::ResultCodes::ASELECT_SERVER_INVALID
      end
    end

    protected

    def attribute?(attribute)
      send(attribute).present?
    end
  end
end
