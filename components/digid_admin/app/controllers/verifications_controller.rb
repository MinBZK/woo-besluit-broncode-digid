
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

class VerificationsController < ApplicationController
  # because Verification is an ActiveResource (pre)load will actually connect to defined
  # Verification.site. Hence we skip loading the resource and fetch it manually.
  authorize_resource

  rescue_from ActiveResource::ConnectionError, with: :connection_error

  def edit
    @front_desk_id = params[:front_desk_id]
    @verification = Verification.find(params[:id])

    if request.xhr?
      render json: { dialog_content: render_to_string(partial: "edit"), title: t('handle_audit') }
    else
      render :edit
    end
  end

  def fraud_correct
    @verification = Verification.find(params[:verification_id])
    @verification.audit.state = 'confirmed'
    @verification.save
    instrument_logger('uc31.front_desk_audit_confirmed', front_desk_id: params[:front_desk_id])
    redirect_to front_desk_path(params[:front_desk_id])
  end

  def fraud_incorrect
    @verification = Verification.find(params[:verification_id])
    @verification.audit.state = 'negated'
    @verification.save
    instrument_logger('uc31.front_desk_audit_negated', front_desk_id: params[:front_desk_id])
    redirect_to front_desk_path(params[:front_desk_id])
  end

  def show
    @verification = Verification.find(params[:id])
    instrument_logger('uc31.front_desk_audit_case_shown', front_desk_id: params[:front_desk_id])

    if request.xhr?
      render json: { dialog_content: @verification ? render_to_string(partial: "show") : t("not_found") , title: t("audit_release") }
    else
      render :show
    end
  end

  private

  def connection_error
    flash[:alert] = t('front_desks.lost_connection')
    redirect_to front_desks_path
  end
end
