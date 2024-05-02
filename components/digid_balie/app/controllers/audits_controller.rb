
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

class AuditsController < ApplicationController
  around_action :set_time_zone, only: %i[index show]

  def index
    front_desk_log('front_desk_audit_started')
    @verifications = Verification.accessible_by(current_ability, :index_audits).order(activated_at: :asc)
    @page_id = 'BAC.10'
  end

  def show
    @verification = Verification.accessible_by(current_ability, :show_audit).find(params[:id])
    front_desk_log('front_desk_audit_one_case_shown', account_id: @verification.front_desk_account_id)
    @form = AuditForm.new(verification: @verification)
    @page_id = 'BAC.20'
  end

  def update
    @verification = Verification.accessible_by(current_ability, :show_audit).find(params[:id])
    @form = AuditForm.new(audit_params.merge(user: current_user, verification: @verification))

    if @form.submit
      if @form.verification_correct
        front_desk_log('454', account_id: @verification.front_desk_account_id)
      else
        front_desk_log('front_desk_audit_case_marked_as_fraud', account_id: @verification.front_desk_account_id)
      end
      redirect_to audits_path
    else
      @page_id = 'BAC.20'
      render :show
    end
  end

  private

  def audit_params
    params.require(:audit_form).permit(:motivation, :verification_correct, :user_id)
  end
end
