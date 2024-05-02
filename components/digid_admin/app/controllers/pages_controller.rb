
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

class PagesController < ApplicationController
  # load_and_authorize_resource
  respond_to :html, :js
  include ActionView::Helpers::FormOptionsHelper

  # GET /pages/new
  def new
    @page = Page.new
    @remote = request.xhr?

    if request.xhr?
      render json: { dialog_body: render_to_string(partial: 'form'), dialog_title: "Scherm toevoegen" }
    else
      respond_with(@page)
    end
  end

  def create
    @page = Page.new(params[:page])
    @remote = request.xhr?

    if @page.valid?
      @screen = [[@page.screen, @page.screen]]
      if request.xhr?
        render json: {
          target: "#question_page", body: "#{options_for_select(@screen.concat(Question.options))}"
        }
      else
        redirect_to(new_question_path, alert: 'Pagina niet aangemaakt, zet javascript aan en probeer opnieuw')
      end
    else
      if request.xhr?
        render json: { dialog_body: render_to_string(partial: 'form'), dialog_title: "Scherm toevoegen" }
      else
        respond_with(@page)
      end
    end
  end
end
