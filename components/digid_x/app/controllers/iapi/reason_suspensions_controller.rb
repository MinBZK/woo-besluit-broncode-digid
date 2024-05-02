
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
  class ReasonSuspensionsController < ApplicationController
    skip_before_action :verify_authenticity_token
    authorize_with_token :iapi_token

    def update
      account_ids = Sectorcode.where(sector_id: Sector.get("a-nummer"), sectoraalnummer: params[:a_number]).pluck(:account_id)
      account_ids.each do |account_id|
        account = Account.where(id: account_id).first
        current_reason_suspension = account.reason_suspension
        new_reason_suspension = ["O", "F"].include?(params[:status]) ? params[:status] : nil
        if new_reason_suspension != current_reason_suspension
          account.update(reason_suspension: new_reason_suspension, reason_suspension_updated_at: Time.now)
          Log.instrument("1515", account_id: account_id, old_reason: current_reason_suspension || "Leeg", new_reason: new_reason_suspension || "Leeg")
        end
      end
    end
  end
end
