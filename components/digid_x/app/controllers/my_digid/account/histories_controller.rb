
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
    class HistoriesController < BaseController
      def index
        Log.instrument("124", account_id: current_account.id)
        redirect_to my_digid_history_url
      end

      def show
        raise(ActionController::RoutingError, "Not Found") if params[:page].to_i > 10_000_000 # prevent 500 error if params[:page] too high

        @page_name = "D2"
        subquery = Sectorcode.select(:sectoraalnummer).where(account_id: current_account.id).map(&:sectoraalnummer)
        all_account_ids = Sectorcode.select("DISTINCT account_id").where(sectoraalnummer: subquery).map(&:account_id)
        all_account_ids << SectorcodesHistory.select("DISTINCT account_id").where(sectoraalnummer: subquery).map(&:account_id)
        all_account_ids = all_account_ids.flatten.uniq
        @transactions = AccountLog.history(all_account_ids).page(params[:page]).per(10)
      end
    end
  end
end
