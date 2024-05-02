
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

class QuestionsController < ApplicationController
  load_and_authorize_resource
  respond_to :html

  # GET /questions
  def index
    instrument_logger('358')
  end

  # GET /questions/1
  def show
  end

  # GET /questions/new
  def new
  end

  # GET /questions/1/edit
  def edit
  end

  # POST /questions
  def create
    if @question.save
      instrument_logger('uc33.vraag_aanmaken_gelukt', question_id: @question.id)
      redirect_to @question, notice: create_success(@question)
    else
      render :new
    end
  end

  # PUT /questions/1
  def update
    if @question.update(params[:question])
      instrument_logger('uc33.vraag_wijzigen_gelukt', question_id: @question.id)
      redirect_to @question, notice: update_success(@question)
    else
      render :edit
    end
  end

  # DELETE /questions/1
  def destroy
    @question.destroy
    instrument_logger('uc33.vraag_verwijderen_gelukt', question_id: @question.id)
    redirect_to(questions_path)
  end

  def log_search
    instrument_logger('364')
  end

  # GET /questions/preview
  def preview
    authorize! :read, Question
    @question.id = params[:id]
    @questions = Question.for(@question.page).to_a
    @questions.delete_if { |question| question.id == @question.id }
    @questions.push(@question)
    @questions.sort! do |a, b|
      if a.position != b.position
        a.position <=> b.position
      elsif !a.id
        1
      else
        !b.id ? -1 : a.id <=> b.id
      end
    end
    render :preview, layout: false
  end
end
