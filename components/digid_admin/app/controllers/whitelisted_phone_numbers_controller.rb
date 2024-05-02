
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

class WhitelistedPhoneNumbersController < ApplicationController
  include FourEyesReviewControllerConcern
  include FourEyesReviewsHelper

  load_and_authorize_resource

  # GET /whitelisted_phone_numbers
  def index
    scope = WhitelistedPhoneNumber.order(Arel.sql('CAST(SUBSTR(phone_number FROM 1) AS UNSIGNED)'))
    @search = scope.ransack(params[:q])
    @whitelisted_phone_numbers = @search.result.page(params[:page])
    @whitelisted_phone_numbers_in_review = FourEyesReview.where(record_table: "WhitelistedPhoneNumber")
  end

  # GET /whitelisted_phone_numbers/new
  def new; end

  # POST /whitelisted_phone_numbers
  def create
    if @whitelisted_phone_number.save_for_review(manager: current_user)
      log(:create)
      flash[:notice] = t('whitelisted_phone_number.created_successfully')
      redirect_via_js_or_http(review_whitelisted_phone_numbers_path(@whitelisted_phone_number.review))
    else
      render :new
    end
  end

  # GET /whitelisted_phone_numbers/:id/edit
  def edit
  end

  # PUT /whitelisted_phone_numbers/:id/edit
  def update
    if @whitelisted_phone_number.update_for_review(whitelisted_phone_number_params, manager: current_user)
      log(:update)
      flash[:notice] = t('whitelisted_phone_number.updated_successfully')
      redirect_via_js_or_http(review_whitelisted_phone_numbers_path(@whitelisted_phone_number.review))
    else
      render :edit
    end
  end

  # DELETE /whitelisted_phone_numbers/:id
  def destroy
    @whitelisted_phone_number.destroy_for_review(manager: current_user)
    log(:destroy)
    redirect_to whitelisted_phone_numbers_path
  end

  private

  def whitelisted_phone_number_params
    params.require(:whitelisted_phone_number).permit(:note, :phone_number, :description)
  end

  def log(action)
    instrument_logger({
      search: "1422",
      show: "1423",
      create: "1424",
      update: "1425",
      edit_review: "1425",
      update_review: "1425",
      destroy: "1426",
      review: "1432",
      accept: "1433",
      reject: "1434",
      withdraw: "1435"
    }[action], log_options)
  end

  def log_options
    { entity: "Whitelisted telefoonnummer #{@whitelisted_phone_number.phone_number}", whitelisted_phone_number_id: @whitelisted_phone_number.id }
  end
end
