
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

class RevokeIdentityCardFlow
    FLOW = {
    start:                      { transitions: [:choose_method, :verify_code, :failed, :cancelled] },
    choose_method:              { transitions: [:verify_app, :verify_wid, :verify_code, :cancelled, :failed] },

    # Verification methods
    verify_code:                { transitions: [:revocation_authenticated, :cancelled, :failed] },
    verify_wid:                 { transitions: [:revocation_authenticated, :cancelled, :failed] },

    verify_app:                 { transitions: [:qr_app, :revocation_authenticated, :confirm_in_app, :cancelled, :failed] },
    qr_app:                     { transitions: [:confirm_in_app, :enter_pin, :revocation_authenticated, :cancelled, :failed] },
    confirm_in_app:             { transitions: [:enter_pin, :revocation_authenticated, :cancelled, :failed] },
    enter_pin:                  { transitions: [:revocation_authenticated, :cancelled, :failed] },

    # Store revocation
    revocation_authenticated:   { transitions: [:saving_revocation, :failed] },
    saving_revocation:          { transitions: [:revocation_saved, :verify_code, :failed] },
    revocation_saved:           { transitions: [:completed] },

    # Abort / Cancel
    failed:                     { transitions: [] },
    cancelled:                  { transitions: [] },

    # Complete activation wizard
    completed:                  { transitions: [] }
  }.freeze

  include FlowControl

  def initialize
    # WORKAROUND do a deep copy so we can store sensitive data on specific flow steps
    @flow = Marshal.load(Marshal.dump(FLOW))
    @state = :start
  end

  def authenticate_revocation(controller)
    # TOOD Check if we are intrekking or blocking the rijbewijs, inspect current state?
    transition_to!(:revocation_authenticated)
  end

  alias_method :verify_app_completed, :authenticate_revocation
  alias_method :verify_wid_completed, :authenticate_revocation
  alias_method :verify_code_completed, :authenticate_revocation

  def card_type
    self[:revocation_authenticated][:card_type]
  end

  def process
    :revoke_identity_card
  end
end
