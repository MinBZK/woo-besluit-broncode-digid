
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

class SmsChallenge < AccountBase
  # search_fields :code, :status, :mobile_number, :action

  module Status
    CORRECT   = 'correct'.freeze # goed beantwoord
    INCORRECT = 'incorrect'.freeze # fout beantwoord
    PENDING   = 'pending'.freeze # onbeantwoord
    RECOVERY  = 'recovery'.freeze # mobiel herstel
  end

  ALL_STATES = [Status::PENDING, Status::CORRECT, Status::INCORRECT].freeze

  include Stateful

  class Action
    REGISTRATION    = 'registration'.freeze
    ACTIVATION      = 'activation'.freeze
    RECOVER_ACCOUNT = 'recover_account'.freeze
    MIJN_DIGID      = 'mijn_digid'.freeze
    SIGN_IN         = 'sign_in'.freeze
    ALL = [REGISTRATION, ACTIVATION, RECOVER_ACCOUNT, MIJN_DIGID, SIGN_IN].freeze
  end

  default_scope { order(created_at: :desc) }
end
