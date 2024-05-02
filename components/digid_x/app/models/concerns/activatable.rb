
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

module Activatable
  extend ActiveSupport::Concern

  include Stateful
  include HasAttempts

  def controle_code
    activation_code.try(:upcase)
  end

  # check the expiry date of an activation code.
  # return the expiry time (days) if expired
  # return false if ok
  # geldigheidstermijn:
  # basis/midden: XX
  def expired?
    return geldigheidstermijn if updated_at < geldigheidstermijn.to_i.days.ago
    false
  end

  def reset_code
    update(activation_code: nil)
    delete_attempts
  end

  # match the activation_code with code
  def activation_code_match?(code)
    return code.casecmp("A1234VIJF").zero? if Rails.application.config.integration_mode
    activation_code.present? && code.casecmp(activation_code).zero? && !state.blocked?
  end
end
