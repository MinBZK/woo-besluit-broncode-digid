
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

class NationalitiesController < ApplicationController
  load_and_authorize_resource
  respond_to :html
  before_action :cancel_button, only: [:new, :create, :edit, :update]
  before_action :back_to_overview, only: [:edit, :update]

  # GET /nationalities
  def index
    instrument_logger("1442", subject_type: Log::SubjectTypes::NATIONALITY)
  end

  # GET /nationalities/1
  def show
    instrument_logger("1423", entity: t("activerecord.models.nationality"), subject_type: Log::SubjectTypes::NATIONALITY, subject_id: @nationality.id)
  end

  # GET /natiƒ /new
  def new
  end

  # GET /nationalities/1/edit
  def edit
  end

  # POST /nationalities
  def create
    if @nationality.save
      instrument_logger("1424", entity: t("activerecord.models.nationality"), subject_type: Log::SubjectTypes::NATIONALITY, subject_id: @nationality.id)
      redirect_to @nationality, notice: create_success(@nationality)
    else
      render :new
    end
  end

  # PUT /nationalities/1
  def update
    if @nationality.update(params[:nationality])
      instrument_logger("1425", entity: t("activerecord.models.nationality"), subject_type: Log::SubjectTypes::NATIONALITY, subject_id: @nationality.id)
      redirect_to @nationality, notice: update_success(@nationality)
    else
      render :edit
    end
  end

  # DELETE /nationalities/1
  def destroy
    if @nationality.destroy
      instrument_logger("1426", entity: t("activerecord.models.nationality"), subject_type: Log::SubjectTypes::NATIONALITY, subject_id: @nationality.id)
      redirect_to(nationalities_path, notice: "Nationaliteit succesvol verwijderd.")
    else
      @nationalities = Nationality.all
      render :index
    end
  end

  private
  def cancel_button
    @cancel_button = true
  end

  def back_to_overview
    @back_to_overview = true
  end
end
