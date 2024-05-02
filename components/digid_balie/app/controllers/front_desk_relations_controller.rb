
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

class FrontDeskRelationsController < ApplicationController
  skip_before_action :require_front_desk!

  def new
    dispatch_based_on_resulting_front_desks
  end

  def create
    unless front_desks_for_current_user.pluck(:id).include?(front_desk_relation_params[:front_desk_id].to_i)
      front_desk_log('baliemdw_niet_gemachtigd', front_desk_id: front_desk_relation_params[:front_desk_id])
      redirect_to root_path, notice: t('current_front_desk_not_authorized')
      return
    end

    if current_user.update(front_desk_id: front_desk_relation_params[:front_desk_id])
      session[:current_front_desk_id] = current_user.front_desk_id
      front_desk_log('front_desk_employee_front_desk_selected', front_desk_id: current_front_desk.code)
      front_desk_log('front_desk_employee_assigned_to_front_desk', front_desk_id: current_front_desk.code)
      redirect_to root_path, notice: t('front_desk_selection_successful')
    else
      render :new
    end
  end

  private

  def dispatch_based_on_resulting_front_desks
    case front_desks_for_current_user.count
    when 0
      if any_blocked_front_desks_for_user
        notice = t('front_desk_selection_only_blocked_front_desk_for_eherkenning_info')
      else
        notice = t('front_desk_selection_no_front_desk_for_eherkenning_info')
        front_desk_log('front_desk_employee_log_in_fail_unknown_front_desk')
      end
      front_desk_log('front_desk_employee_no_front_desk_found_for_identifier')
      reset_session
      redirect_to authenticate_path, notice: notice
    when 1
      session[:current_front_desk_id] = front_desks_for_current_user.first.id
      front_desk_log('front_desk_employee_assigned_to_front_desk', front_desk_id: front_desks_for_current_user.first.code)
      redirect_to root_path
    else
      front_desk_log('front_desk_employee_start_selection_of_front_desk')
    end
  end

  def front_desks_for_current_user
    @front_desks_for_current_user ||= FrontDesk.not_blocked.find_by_kvk_info(
      kvk_number: session[:kvk_number],
      establishment_number: session[:establishment_number]
    )
  end

  def any_blocked_front_desks_for_user
    FrontDesk.blocked.find_by_kvk_info(
      kvk_number: session[:kvk_number],
      establishment_number: session[:establishment_number]
    ).present?
  end

  def front_desk_relation_params
    params.require(:user).permit(:front_desk_id)
  end
end
