
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

class ChangePasswordFlow
  FLOW = {
    start:            { transitions: [:choose_method, :with_password, :with_app, :completed, :cancelled] },

    # Step 1 - choose to change with old password or app
    choose_method:    { transitions: [:with_password, :with_app, :cancelled] },

    # Step 2 - change with old password
    with_password:    { transitions: [:completed, :cancelled] },

    # Step 2 - Enter new password
    with_app:         { transitions: [:verify_app, :cancelled, :failed] },

    verify_app:       { transitions: [:qr_app, :password_change_authenticated, :confirm_in_app, :cancelled, :failed] },
    qr_app:           { transitions: [:confirm_in_app, :enter_pin, :password_change_authenticated, :cancelled, :failed] },
    confirm_in_app:   { transitions: [:enter_pin, :password_change_authenticated, :cancelled, :failed] },
    enter_pin:        { transitions: [:password_change_authenticated, :cancelled, :failed] },

    password_change_authenticated: { transitions: [:completed, :failed] },

    completed:        { transitions: [] },
    failed:           { transitions: [] },
    cancelled:        { transitions: [] }
  }.freeze

  include FlowControl

  def initialize
    @flow = Marshal.load(Marshal.dump(FLOW))
    @state = :start
  end

  def authenticate_password_change(controller)
    transition_to!(:password_change_authenticated)
  end
  alias_method :verify_app_completed, :authenticate_password_change

  def process
    :change_password
  end
end
