
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

class SubscribersController < ApplicationController
  load_and_authorize_resource
  before_action :current_pilot_group

  def create
    @subscriber = current_pilot_group.subscribers.build(create_subscriber_params)
    if @subscriber.save
      instrument_logger('subscriber.added_successfully',
                        bsn: @subscriber.bsn,
                        pilotgroep: current_pilot_group.name,
                        pilot_group_id: current_pilot_group.id) # logregel 597
    else
      flash[:bsn_error] = @subscriber.errors[:bsn].join
    end
    redirect_to edit_pilot_group_path(current_pilot_group)
  end

  def destroy
    subscriber = current_pilot_group.subscribers.find(params[:id])
    if subscriber
      instrument_logger('subscriber.deleted_successfully',
                        bsn: subscriber.bsn,
                        pilotgroep: current_pilot_group.name,
                        pilot_group_id: current_pilot_group.id) # logregel 598

      subscriber.destroy
    end
    redirect_to edit_pilot_group_path(current_pilot_group)
  end

  def destroy_all
    destroyed = current_pilot_group.subscribers.map(&:destroy)
    if destroyed.any?
      instrument_logger('subscriber.all_deleted_successfully',
                        pilotgroep: current_pilot_group.name,
                        pilot_group_id: current_pilot_group.id) # logregel 599
    end
    redirect_to edit_pilot_group_path(current_pilot_group)
  end

  private

  def current_pilot_group
    @current_pilot_group ||= Subscription.find(params[:pilot_group_id])
  end

  def create_subscriber_params
    params.require(:subscriber).permit(:bsn, :subscription_id)
  end
end
