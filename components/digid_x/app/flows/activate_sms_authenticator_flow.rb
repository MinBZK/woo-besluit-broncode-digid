
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

class ActivateSmsAuthenticatorFlow
  FLOW = {
    # Step 1 - enter phone number
    enter_phone_number:         { transitions: [:phone_number_verified, :aborted] },
    phone_number_verified:      { transitions: [:verify_sms] },

    # Step 2 - verify SMS
    verify_sms:                 { transitions: [:verify_sms_completed, :aborted] },
    verify_sms_completed:       { transitions: [:verify_app, :aborted] },

    # Step 3 - verify app
    verify_app:                 { transitions: [:qr_app, :verify_app_completed, :confirm_in_app, :failed, :aborted] },
    qr_app:                     { transitions: [:confirm_in_app, :enter_pin, :verify_app_completed, :failed, :aborted] },
    confirm_in_app:             { transitions: [:enter_pin, :verify_app_completed, :failed, :aborted] },
    enter_pin:                  { transitions: [:verify_app_completed, :failed, :aborted] },
    verify_app_completed:       { transitions: [:confirmation, :aborted] },

    # Step 4 - confirm
    confirmation:               { transitions: [:confirmation_completed, :aborted] },
    confirmation_completed:     { transitions: [:completed, :aborted] },

    # Abort activation wizard
    cancelled:                  { transitions: [] },

    # App verification failure
    failed:                     { transitions: [:aborted] },
    
    aborted:                    { transitions: [] },

    # Complete activation wizard
    completed:                  { transitions: [] }
  }.freeze

  include FlowControl

  def initialize
    @flow = Marshal.load(Marshal.dump(FLOW))
    @state = :enter_phone_number
  end

  def process
    :activate_sms_authenticator
  end

  def verify_sms_completed(_controller)
    transition_to!(:verify_sms_completed)
  end

  def verify_app_completed(_controller)
    transition_to!(:verify_app_completed)
  end
end
