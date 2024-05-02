
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

module RegistrationChecks
  class RegistrationChecksQueue
    attr_reader :queue, :registration, :button_to

    def initialize(registration, button_to)
      @registration = registration
      @button_to = button_to
      @queue = []
    end

    def registration_checks(session)
      @queue << RegistrationChecks::BalieTooSoon.new(session, @registration, @button_to)
      @queue << RegistrationChecks::BalieTooOften.new(session, @registration, @button_to)
      @queue << RegistrationChecks::TooSoon.new(session, @registration, @button_to)
      @queue << RegistrationChecks::TooOften.new(session, @registration, @button_to)

      @queue.find(&:match?)
    end

    def account_checks(bsn, sector_ids)
      @queue << RegistrationChecks::OpAfmeldingLijst.new(@registration, bsn,             @button_to)
      @queue << RegistrationChecks::AccountPostponed.new(@registration, bsn, sector_ids, @button_to)
      @queue << RegistrationChecks::ReplaceAccountBlocked.new(@registration, bsn, @button_to)
      @queue.find(&:match?)
    end
  end
end
