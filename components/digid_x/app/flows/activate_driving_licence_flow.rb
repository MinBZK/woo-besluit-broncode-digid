
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

class ActivateDrivingLicenceFlow
    # initialize > choose_card_reader > app_chosen > scan_qr_code > qr_code_scanned > scan_wid > wid_scanned > completed
    FLOW = {
    start:              { transitions: [:information_page, :failed] },
    information_page:   { transitions: [:choose_card_reader, :failed] },

    # Step 1 - choose card reader
    choose_card_reader: { transitions: [:app_chosen, :usb_chosen, :web_to_app_chosen, :failed] },
    app_chosen:         { transitions: [:scan_qr_code, :failed, :web_to_app_chosen] },
    usb_chosen:         { transitions: [:scan_wid, :failed]},
    web_to_app_chosen:  { transitions: [:scan_wid, :wid_scanned, :app_chosen, :failed]},

    # Step 2 - scan qr code
    scan_qr_code:       { transitions: [:qr_code_scanned, :failed] },
    qr_code_scanned:    { transitions: [:scan_wid, :failed] },

    # Step 3 - Scan driving licence
    scan_wid:           { transitions: [:wid_scanned, :failed] },
    wid_scanned:        { transitions: [:update_status, :failed] },

    update_status:      { transitions: [:completed, :failed] },

    # Abort activation wizard
    failed:             { transitions: [] },

    # Complete activation wizard
    completed:          { transitions: [] },

    redirect_to:        { }
  }.freeze

  include FlowControl

  def initialize
    # WORKAROUND do a deep copy so we can store sensitive data on specific flow steps
    @flow = Marshal.load(Marshal.dump(FLOW))
    @state = :start
  end

  def process
    :activate_driving_license
  end
end
