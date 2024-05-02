
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

module Scifi
  # eHerkenning is an external service and not available in all enviroments (for example development, test, acc1, acc3, acc4).
  # To test and demonstrate the FrontDesk functionality, we need a eHerkenning-like login functionality.
  class SessionsController < ApplicationController
    include EherkenningAuthenticate

    skip_before_action :verify_authenticity_token, only: :create
    skip_before_action :authenticate!, :require_front_desk!

    def create
      eherkenning_authenticate!
    end

    def new
      render :new, locals: { eherkenning_users: eherkenning_users, front_desks: FrontDesk.where(blocked: false).order(kvk_number: :asc) }
    end

    private

    # eHerkenning geeft altijd een KvK nummer terug en optioneel een Vestigingsnummer waarvoor
    # de gebruiker geautoriseerd is. Een persoon kan meerdere pseudoniemen hebben door bijvoorbeeld
    # in meerdere machtigingsregister geregistreerd te staan. Ook kan het pseudoniem verschillen per
    # eHerkenningsmiddel.
    def eherkenning_users
      [
        { name: 'PPPPPP', pseudonym: 'PPPPPP', kvk_number: 'PPPPPPPP', establishment_number: 'PPPPPPPPPPPP' },
        { name: 'PPPPPPPPP', pseudonym: 'PPPPPPPP', kvk_number: 'PPPPPPPP', establishment_number: 'PPPPPPPPPPPP' },
        { name: 'PPPPPPP', pseudonym: 'PPPPPPP', kvk_number: 'PPPPPPPP', establishment_number: nil },
        { name: 'PPPPP', pseudonym: 'PPPPP', kvk_number: 'PPPPPPPP', establishment_number: nil },
        { name: 'PPPPPPPP', pseudonym: 'PPPPPPPP', kvk_number: 'PPPPPPPP', establishment_number: 'PPPPPPPPPPPP' },
        { name: 'PPPPPPPPPPPP', pseudonym: 'PPPPPPPPPPP', kvk_number: 'PPPPPPPP', establishment_number: nil },
        { name: 'PPPPPPPPPPPPPPPPPPPP', pseudonym: 'PPPPPPPPP', kvk_number: 'PPPPPPPP', establishment_number: nil },
        { name: 'PPPPPPPPPPPPPPPPPPPP', pseudonym: 'PPPPPPPPP', kvk_number: 'PPPPPPPP', establishment_number: nil },
        { name: 'PPPPPPPPPPPPPPPPPPPPPPPPPPPPPP', pseudonym: 'PPPPPP', kvk_number: 'PPPPPPPP', establishment_number: nil, successful?: false }
      ]
    end

    def eh_response
      saml_response = eherkenning_users.keep_if { |u| u[:pseudonym] == params[:pseudonym] }.first
      @eh_response ||= OpenStruct.new({ successful?: true }.merge(saml_response))
    end

  end
end
