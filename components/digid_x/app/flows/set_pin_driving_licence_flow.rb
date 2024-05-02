
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

class SetPinDrivingLicenceFlow
    FLOW = {
      set_pin_link:       { transitions: [:choose_cardreader, :failed] },
      choose_cardreader:  { transitions: [:app_chosen, :usb_chosen, :web_to_app_chosen, :failed] },

      app_chosen:         { transitions: [:scan_qr_code, :web_to_app_chosen, :failed] },
      usb_chosen:         { transitions: [:scan_wid, :failed]},
      web_to_app_chosen:  { transitions: [:scan_wid, :wid_scanned, :app_chosen, :failed]},

      scan_qr_code:       { transitions: [:qr_code_scanned, :failed] },
      qr_code_scanned:    { transitions: [:scan_wid, :failed] },

      scan_wid:           { transitions: [:pin_reset_success, :failed] },

      pin_reset_success:  { transitions: [:completed, :start_activation, :failed] },
      start_activation:   { transitions: [:wid_scanned, :failed] },

      wid_scanned:        { transitions: [:update_status, :failed] },

      update_status:      { transitions: [:completed, :failed] },

      failed:             { transitions: [] },
      completed:          { transitions: [] },

      redirect_to:        { }
    }.freeze

  include FlowControl

  def initialize
    # WORKAROUND do a deep copy so we can store sensitive data on specific flow steps
    @flow = Marshal.load(Marshal.dump(FLOW))
    @state = :set_pin_link
  end

  def process
    :set_pin_driving_license
  end
end
