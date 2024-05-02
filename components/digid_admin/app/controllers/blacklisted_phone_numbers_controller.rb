
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

class BlacklistedPhoneNumbersController < ApplicationController
  load_and_authorize_resource

  def index
    @blacklisted_phone_numbers = BlacklistedPhoneNumber.order(Arel.sql('CAST(SUBSTR(prefix FROM 1) AS UNSIGNED)')).ransack.result.page(params[:page])
    instrument_logger('blacklisted_phone_number.inzien', subject_type: Log::SubjectTypes::BLACKLISTED_PHONE_NUMBER)
  end

  def new
  end

  def create
    if @blacklisted_phone_number.save
      instrument_logger(
        'blacklisted_phone_number.created',
        prefix: @blacklisted_phone_number.prefix,
        blacklisted_phone_number: @blacklisted_phone_number.id,
        subject_type: Log::SubjectTypes::BLACKLISTED_PHONE_NUMBER
      )
      flash[:notice] = t('blacklisted_phone_number.created_successfully')
      redirect_via_js_or_http(blacklisted_phone_numbers_path)
    else
      render :new
    end
  end

  def edit
    instrument_logger(
      'blacklisted_phone_number.edit',
      prefix: @blacklisted_phone_number.prefix,
      blacklisted_phone_number: @blacklisted_phone_number.id,
      subject_type: Log::SubjectTypes::BLACKLISTED_PHONE_NUMBER)
  end

  def update
    if @blacklisted_phone_number.update(blacklisted_phone_number_params)
      instrument_logger(
        'blacklisted_phone_number.updated',
        prefix: @blacklisted_phone_number.prefix,
        blacklisted_phone_number: @blacklisted_phone_number.id,
        subject_type: Log::SubjectTypes::BLACKLISTED_PHONE_NUMBER
      )
      flash[:notice] = t('blacklisted_phone_number.updated_successfully')
      redirect_via_js_or_http(blacklisted_phone_numbers_path)
    else
      render :edit
    end
  end

  def destroy
    @blacklisted_phone_number.destroy
    instrument_logger(
      'blacklisted_phone_number.destroyed',
      prefix: @blacklisted_phone_number.prefix,
      blacklisted_phone_number: @blacklisted_phone_number.id,
      subject_type: Log::SubjectTypes::BLACKLISTED_PHONE_NUMBER)
    redirect_to blacklisted_phone_numbers_path
  end

  private

  def blacklisted_phone_number_params
    params.require(:blacklisted_phone_number).permit(
      :prefix,
      :description
    )
  end
end
