
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

module Iapi
  class ANumbersController < ApplicationController
    skip_before_action :verify_authenticity_token
    authorize_with_token :iapi_token

    def create
      account_ids = Sectorcode.where(sector_id: Sector.get("bsn"), sectoraalnummer: params[:bsn]).pluck(:account_id)
      Sectorcode.where(sector_id: Sector.get("a-nummer"), account_id: account_ids).destroy_all
      account_ids.each do |account_id|
        Sectorcode.create(sector_id: Sector.get("a-nummer"), account_id: account_id, sectoraalnummer: params[:a_number])
      end
    end

    def update
      sectorcodes = Sectorcode.where(sector_id: Sector.get("a-nummer"), sectoraalnummer: params[:old_a_number])
      sectorcodes.each do |sectorcode|
        account_id = sectorcode[:account_id]
        sectorcode.update(sectoraalnummer: params[:new_a_number])
        Log.instrument("1514", message_type: "Wa11", account_id: account_id)
      end

    end
  end
end
