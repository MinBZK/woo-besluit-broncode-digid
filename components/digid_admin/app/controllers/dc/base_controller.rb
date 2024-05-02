
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

module Dc
  class BaseController < ApplicationController
    include FourEyesReviewControllerConcern
    load_and_authorize_resource only: [:index, :show, :edit, :new, :create, :update, :destroy]

    respond_to :html, :js

    rescue_from DigidUtils::Iapi::StatusError, with: :show_error_message

    def log(action)
      case action
      when :search
        instrument_logger("1422", log_options)
      when :show
        instrument_logger("1423", log_options)
      when :create
        instrument_logger("1424", log_options)
      when :update, :update_review
        instrument_logger("1425", log_options)
      when :destroy
        instrument_logger("1426", log_options)
      when :review
        instrument_logger("1432", log_options)
      when :accept
        instrument_logger("1433", log_options)
      when :reject
        instrument_logger("1434", log_options)
      when :withdraw
        instrument_logger("1435", log_options)
      end
    end

    def log_options
      {}
    end

    def show_error_message
      # not implemented
    end
  end
end
