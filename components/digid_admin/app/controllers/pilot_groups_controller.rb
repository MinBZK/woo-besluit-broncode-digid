
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

class PilotGroupsController < ApplicationController
  load_and_authorize_resource

  def edit
    @subscribers = @pilot_group.subscribers.order(bsn: :asc).page(params[:page])
    pilot_group_log('viewed_successfully')
  end

  def index
    pilot_group_log('listed_successfully', subject_type: ::Log::SubjectTypes::PILOT_GROUP)
  end

  def show
    @subscribers = @pilot_group.subscribers.order(bsn: :asc).page(params[:page])
    pilot_group_log('viewed_successfully')
  end

  def update
    @subscribers = @pilot_group.subscribers.order(bsn: :asc).page(params[:page])
    if @pilot_group.update(pilot_group_params)
      flash[:notice] = t('pilot_group_updated_successfully')
      pilot_group_log('updated_successfully')
      redirect_to pilot_group_path(@pilot_group)
    else
      pilot_group_log('update_failed')
      render :edit
    end
  end

  private

  def pilot_group_log(message, args = {})
    instrument_logger(
      "pilot_group.#{message}",
      args.merge(
        pilot_group: @pilot_group.try(:name),
        pilot_group_id: params[:id]
      )
    )
  end

  def pilot_group_params
    params.require(:pilot_group).permit(:description)
  end
end
