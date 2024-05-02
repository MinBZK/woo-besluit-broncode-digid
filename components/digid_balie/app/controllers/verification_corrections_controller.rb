
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

class VerificationCorrectionsController < ApplicationController
  def new
    authorize! :correct, verification
    @form = VerificationCorrectionForm.new(verification: verification)
    @page_id = 'BA.30.1'
  end

  def create
    authorize! :correct, verification
    @form = VerificationCorrectionForm.new(verification_correction_form_params.merge(verification: verification))

    if @form.save
      session[:verification_correction_for] = verification.id
      front_desk_log('front_desk_id_check_correction', account_id: verification.front_desk_account_id)
      redirect_to edit_verification_path(verification)
    else
      errors = @form.errors.messages.reject { |_k, v| v.blank? }.keys

      if errors.include?(:base)
        front_desk_log('front_desk_id_check_failed_no_changes', account_id: verification.front_desk_account_id)
      end

      unless (errors & %i[motivation id_number id_expires_at id_expires_at_day id_expires_at_month id_expires_at_year motivation]).blank?
        front_desk_log('front_desk_id_check_correction_empty_fields', account_id: verification.front_desk_account_id)
      end

      @page_id = 'BA.30.1'
      render :new
    end
  end

  private

  def verification
    @verification ||= Verification.find(params[:verification_id])
  end

  def verification_correction_form_params
    params.require(:verification_correction_form).permit(:id_number,
                                                         :id_expires_at_day,
                                                         :id_expires_at_month,
                                                         :id_expires_at_year,
                                                         :motivation)
  end
end
