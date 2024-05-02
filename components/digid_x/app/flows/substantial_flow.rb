
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

class SubstantialFlow
  FLOW = {
    initialized:                { transitions: [:retrieved, :cancelled] },

    # Authenticate
    retrieved:                   { transitions: [:confirmed, :aborted, :cancelled, :failed] },
    confirmed:                   { transitions: [:completed, :aborted, :cancelled, :failed] },
    completed:                   { transitions: [:waiting_for_documents, :aborted, :cancelled, :failed] },

    # ID check
    waiting_for_documents:       { transitions: [:documents_received, :no_documents_found, :aborted, :cancelled, :failed] },
    no_documents_found:          { transitions: [:documents_received, :aborted, :cancelled, :failed] },
    documents_received:          { transitions: [:scanning_in_progress, :scanning_foreign_document, :aborted, :cancelled, :failed] },
    scanning_in_progress:        { transitions: [:verified, :aborted, :cancelled, :failed, :documents_received] },
    scanning_foreign_document:   { transitions: [:verified, :aborted, :cancelled, :failed] },
    verified:                    { transitions: [] },
    refuted:                     { transitions: [:waiting_for_documents, :documents_received] }, # user can scan again

    # Cancelled scenario's
    aborted:                    { transitions: [] },
    failed:                     { transitions: [] },
    cancelled:                  { transitions: [] },
    timeout:                    { transitions: [] },

  }.freeze

  include FlowControl

  def initialize(flow_specification: FLOW, state: nil)
    @flow = flow_specification
    @state = state
  end

  def flow
    @flow
  end

  def process
    self.class.name.underscore
  end

end
