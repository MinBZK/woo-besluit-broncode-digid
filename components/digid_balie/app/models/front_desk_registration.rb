
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

class FrontDeskRegistration < ActiveResource::Base
  self.site = APP_CONFIG['urls']['internal']['x'] + '/iapi'
  headers['X-Auth-Token'] = Rails.application.secrets.iapi_token

  module PasswordToolStatus
    ACTIVE    = 'active'.freeze
    BLOCKED   = 'blocked'.freeze
    INACTIVE  = 'inactive'.freeze
  end

  def created_at
    attributes[:created_at].to_datetime
  end

  def to_verification
    Verification.new(
      activation_code: activation_letters.first.controle_code,
      activation_code_end_date: activation_letters.first.activation_code_end_date,
      birthday: date_of_birth,
      citizen_service_number: burgerservicenummer,
      first_names: activation_letters.first.first_names,
      front_desk_code: baliecode,
      front_desk_registration_id: id,
      front_desk_registration_created_at: created_at,
      front_desk_account_id: account.id,
      full_name: activation_letters.first.full_name,
      id_expires_at: id_valid_until,
      id_number: id_number,
      salutation: activation_letters.first.salutation,
      surname: activation_letters.first.surname,
      front_desk_code_expires_at: expires_at
    )
  end
end
