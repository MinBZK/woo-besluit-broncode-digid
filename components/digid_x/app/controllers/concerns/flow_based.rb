
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

module FlowBased
  extend ActiveSupport::Concern

  included do
    after_action :complete_flow
    helper_method :current_flow
    rescue_from FlowError, with: :handle_flow_error
  end

  private

  def complete_flow
    session.delete(:flow) if session[:flow] && current_flow.completed?
  end

  def current_flow
    raise FlowError, "not in flow" unless session.key?(:flow)
    session[:flow]
  end

  def reset_flow
    session.delete(:flow)
  end

  def flow_error_redirect_url
    my_digid_url
  end

  def flow_active?
    session.key?(:flow)
  end

  def handle_flow_error(error)
    logger.debug(error.message)
    logger.debug(error.backtrace.join("\n"))
    logger.error("Error in flow. Redirecting..")
    reset_flow
    not_found(redirect_to_url: flow_error_redirect_url)

  end
end
