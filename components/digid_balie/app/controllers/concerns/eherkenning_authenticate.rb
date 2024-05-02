
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

module EherkenningAuthenticate
  extend ActiveSupport::Concern

  private

  def eherkenning_authenticate!

    if eh_response.successful?
      user = User.find_or_create_by(pseudonym: eh_response.pseudonym)
      session[:current_front_desk_id] = user.front_desk_id
      session[:current_user_id]       = user.id
      session[:kvk_number]            = eh_response.kvk_number
      session[:establishment_number]  = eh_response.establishment_number
      front_desk_log('front_desk_employee_log_in_successful')

      if current_front_desk.present? && current_front_desk.blocked?
        front_desk_log('front_desk_employee_assigned_to_front_desk', front_desk_id: current_front_desk.code)
        front_desk_log('front_desk_employee_log_in_fail_front_desk_blocked')

        reset_session
        redirect_to authenticate_path, notice: t('front_desk_selection_only_blocked_front_desk_for_eherkenning_info')
      else
        front_desk_log('front_desk_employee_assigned_to_front_desk', front_desk_id: current_front_desk.code) if current_front_desk.present?
        redirect_to root_path, notice: t('signed_in_succesfully')
      end
    else
      front_desk_log('front_desk_employee_log_in_fail_unknown_autorisation')
      redirect_to authenticate_path, notice: t('sign_in_failed')
    end
  end
end
