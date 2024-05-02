
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

class RegistrationFlow
  FLOW = {
    # Step 1 - Create registration
    registration:                 { transitions: [:registration_verified, :balie_registration] },
    registration_verified:        { transitions: [:new_account] },

    balie_registration:           { transitions: [:balie_registration_verified] },
    balie_registration_verified:  { transitions: [:new_account] },

    # Step 2 - Create account
    new_account:                  { transitions: [:new_account_verified] },
    new_account_verified:         { transitions: [:verify_sms, :new_account] },

    # Step 2.1 - Verify sms authenticator
    verify_sms:                   { transitions: [:verify_sms_completed, :new_account] },
    verify_sms_completed:         { transitions: [:verify_email] },

    # Step 2.x - Verify email authenticator
    verify_email:                 { transitions: [:verify_email_completed, :completed, :new_account, :change_email] },
    verify_email_completed:       { transitions: [:completed] },
    change_email:                 { transitions: [:verify_email] },

    # Complete activation wizard
    completed:                    { transitions: [] }
  }.freeze

  include FlowControl

  def initialize
    @flow = Marshal.load(Marshal.dump(FLOW))
    @state = :registration
  end

  def process
    :registration
  end

  def skip_sms_verification!
    if @state == :new_account_verified
      self.transition_to!(:verify_sms)
      self.transition_to!(:verify_sms_completed)
    end
  end

  def skip_email_verification!
    if @state == :verify_sms_completed
      self.transition_to!(:verify_email)
      self.transition_to!(:verify_email_completed)
    end
  end

end
