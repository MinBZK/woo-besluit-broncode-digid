
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

class DeactivateAppFlow
  FLOW = {
    start: { transitions: [:verify_with_password, :verify_with_app, :verify_with_wid, :cancelled, :failed] },
    verify_with_password: { transitions: [:verified, :cancelled, :failed] },

    # AppVerification
    verify_with_app: { transitions: [:verify_app, :verified, :confirm_in_app, :enter_pin, :cancelled, :failed] },
    verify_app:       { transitions: [:qr_app, :confirm_in_app, :enter_pin, :verified, :cancelled, :failed] },
    qr_app:           { transitions: [:confirm_in_app, :enter_pin, :verified, :cancelled, :failed] },
    confirm_in_app:   { transitions: [:enter_pin, :verified, :cancelled, :failed] },
    enter_pin:        { transitions: [:verified, :cancelled, :failed] },

    # WidVerification
    verify_with_wid: { transitions: [:verified, :cancelled, :failed] },
    verified: { transitions: [:completed] },

    cancelled: { transitions: [] },
    failed: { transitions: [] },
    completed: { transitions: [] }
  }.freeze

  include FlowControl

  def initialize
    @flow = Marshal.load(Marshal.dump(FLOW))
    @state = :start
  end

  def process
    :deactivate_app
  end
end
