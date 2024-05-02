
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

class KiosksController < ApplicationController
  include FourEyesReviewControllerConcern
  include FourEyesReviewsHelper
  load_and_authorize_resource only: [:index, :edit, :update, :destroy]

  def index
    @kiosks_in_review = FourEyesReview.where(record_table: "Kiosk")
    @kiosks = Kiosk.all
    instrument_logger('kiosk.inzien', subject_type: Log::SubjectTypes::KIOSK) # 1133
  end

  def edit
    @cancel_button = true
    log(:edit)
  end

  def update
    if @kiosk.update_for_review(params[:kiosk], manager: current_user)
      log(:update)
      redirect_to review_kiosks_path(@kiosk.review)
    else
      render :edit
    end
  end

  def destroy
    if params[:kiosk] && params[:kiosk][:note].present?
      @kiosk.note = params[:kiosk][:note]
    else
      @kiosk.errors.add(:note, :blank)
      render_four_eyes_dialog
      return
    end
    if @kiosk.destroy_for_review(manager: current_user)
      log_destroy
      redirect_via_js(review_kiosks_path(@kiosk.review))
    else
      redirect_via_js(edit_kiosk_path @kiosk)
    end
  end

  private

  def log(action)
    case action
      when :edit, :edit_review
        instrument_logger('kiosk.edit', kiosk_id: @kiosk.kiosk_id, naam: @kiosk.naam) # 1137
      when :update, :update_review
        log_changes(@kiosk.review)
      when :review
        instrument_logger('kiosk.four_eyes.inzien', kiosk_id: @four_eyes_review.updated.kiosk_id, naam: @four_eyes_review.updated.naam) # 1139
      when :accept
        instrument_logger('kiosk.four_eyes.accept', kiosk_id: @four_eyes_review.updated.kiosk_id, naam: @four_eyes_review.updated.naam) # 1140
      when :reject
        instrument_logger('kiosk.four_eyes.reject', kiosk_id: @four_eyes_review.updated.kiosk_id, naam: @four_eyes_review.updated.naam) # 1141
      when :withdraw
        instrument_logger('kiosk.four_eyes.withdraw', kiosk_id: @four_eyes_review.updated.kiosk_id, naam: @four_eyes_review.updated.naam)
    end
  end

  def log_changes(review)
    instrument_logger('kiosk.updated',
      kiosk_id: review.updated.kiosk_id,
      naam: review.updated.naam,
      status: review.updated.human_status,
      status_was: review.original.human_status
      ) # 1138
  end

  def log_destroy
    instrument_logger(
      'kiosk.destroyed',
      kiosk_id: @kiosk.kiosk_id,
      naam: @kiosk.naam
    ) # 1136
  end

  def kiosk_params
    params.require(:kiosk).permit(:kiosk_id, :naam, :adres, :woonplaats, :postcode, :status, :note)
  end
end
