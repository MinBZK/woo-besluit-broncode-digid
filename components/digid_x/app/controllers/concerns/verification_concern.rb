
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
module VerificationConcern
  extend ActiveSupport::Concern

  included do
    helper_method :set_page_title_and_page_header
  end

  def set_page_title_and_page_header(page_name, flow)
    @page_name = page_name

    if flow[flow.state][:page_name].present?
      page_name = flow[flow.state][:page_name]
    end

    if flow[flow.state][:header].present?
      @page_header = flow[flow.state][:header]
    else
      @page_header = t(page_name, scope: [:headers, session[:session], flow.process])
    end

    if flow[flow.state][:page_title].present?
      @page_title = flow[flow.state][:page_title]
    else
      @page_title = t(page_name, scope: [:titles, session[:session], flow.process])
    end
  end
end
