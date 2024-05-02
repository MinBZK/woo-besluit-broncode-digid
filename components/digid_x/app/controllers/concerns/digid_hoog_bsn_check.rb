
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

module DigidHoogBsnCheck
  extend ActiveSupport::Concern
  extend AppSessionConcern

  included do
    before_action :check_bsn
  end

  private

  def check_bsn
    return if current_account.bsn

    Log.instrument("924", wid_type: wid_type(:nl), account_id: current_account.id)
    flash.now[:notice] = t("digid_hoog.activate.no_bsn", wid_type: wid_type)
    render_simple_message(ok: my_digid_url)
  end

  def wid_type(locale = I18n.locale)
    if current_flow[:card_type_chosen] == "NL-rijbewijs"
      t("document.driving_licence", locale: locale)
    elsif params[:card_type] == "NL-rijbewijs"
      t("document.driving_licence", locale: locale)
    else
      t("document.id_card", locale: locale)
    end
  end
end
