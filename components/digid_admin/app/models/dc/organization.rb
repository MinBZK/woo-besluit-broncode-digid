
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

module Dc
  class Organization < ::DigidUtils::Dc::Organization
    include FourEyes

    OIN_FORMAT = /^[0-9]{20}$/

    validate :check_status_active_from

    def connections(page: nil)
      @_connections ||= Dc::Connection.where(organization_id: id, page: page)
    end

    def self.list
      JSON.parse(client.get("#{Organization::base_path}/all").body)
    end

    def check_status_active_from
      unless status&.active_from&.present?
        errors.add("Geldigheid:", "Datum ingang moet zijn ingevuld")
      end

      organization_roles.each do |role|
        unless role&.status&.active_from&.present?
          errors.add("Organization/Role Geldigheid:", "Datum ingang moet zijn ingevuld")
        end
      end
    end

    def check_oin
      errors.add("OIN:", "Formaat onjuist") unless OIN_FORMAT.match(oin)
    end

    def valid?
      check_status_active_from
      check_oin
      [status.valid?, organization_roles.map(&:valid?).all?, status&.active_from&.present?].all?
    end
  end
end
