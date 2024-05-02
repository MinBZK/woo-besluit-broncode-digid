
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

class AccountRequestFlow
  FLOW = {
    # /apps/account_requests/start
    started: { transitions: [:existing_application_check] },

    # /apps/account_requests/existing_application
    existing_application_check: { transitions: [:existing_account_check, :existing_application_found] },
    # /apps/account_requests/replace_application
    existing_application_found: { transitions: [:existing_account_check] },

    # /apps/account_requests/existing_account
    existing_account_check: { transitions: [:account_created, :existing_account_found] },
    # /apps/account_requests/replace_account
    existing_account_found: { transitions: [:account_created] },

    # /apps/session
    account_created: { transitions: [:confirmed] },

    # After :confirmed the legacy endpoints don't support transition_to()
    # /apps/challenge
    # /apps/challenge_reponse
    # /apps/pincode
    confirmed: { transitions: [:challenged] },
    challenged: { transitions: [:verified] },

    # We don't transition into this state. This state just gets set on a cancel
    application_canceled: { transitions: [] },

    # After /apps/pincode we should have the state in completed. This state will be copied into upgrade_state
    # Below are the transitions for the upgrade_state
    completed: { transitions: [:waiting_for_documents] },

    # ID check
    waiting_for_documents: { transitions: [:documents_received, :no_documents_found, :aborted, :cancelled, :failed] },
    no_documents_found: { transitions: [:documents_received, :aborted, :cancelled, :failed] },
    documents_received: { transitions: [:scanning_in_progress, :scanning_foreign_document, :aborted, :cancelled, :failed] },
    scanning_in_progress: { transitions: [:verified, :aborted, :cancelled, :failed, :documents_received] },
    scanning_foreign_document: { transitions: [:verified, :aborted, :cancelled, :failed] },
    verified: { transitions: [] },
    refuted: { transitions: [:waiting_for_documents, :documents_received] }
  }.freeze

  include FlowControl

  def initialize(flow_specification: FLOW, state: nil)
    @flow = Marshal.load(Marshal.dump(FLOW))
    @state = state
  end

  def process
    :account_request_app
  end
end
