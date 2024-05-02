
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

class NsSwitchesController < ApplicationController
  load_resource
  authorize_resource(Switch)

  respond_to :html

  def show
    instrument_logger('pilot_switch.inzien_gelukt', switch_name: @ns_switch.name, switch_id: @ns_switch.id)
  end

  def edit
    instrument_logger('pilot_switch.inzien_aanpassen_gelukt', switch_name: @ns_switch.name, switch_id: @ns_switch.id)
  end

  def update
    description_changed = @ns_switch.description != ns_switch_params["description"]
    status_changed = @ns_switch.status != ns_switch_params["status"]

    @ns_switch.description = ns_switch_params["description"]
    @ns_switch.status = ns_switch_params["status"]

    if description_changed || status_changed
      if @ns_switch.save
        instrument_logger('1544', switch_name: @ns_switch.name, switch_id: @ns_switch.id) if description_changed
        instrument_logger(log_mapping[@ns_switch.text_status.to_sym], switch_name: @ns_switch.name, switch_id: @ns_switch.id) if status_changed
        redirect_to ns_switch_path(@ns_switch), notice: update_success(@ns_switch)
      else
        instrument_logger('pilot_switch.wijzigen_mislukt', switch_name: @ns_switch.name, switch_id: @ns_switch.id)
        render :edit
      end
    else
      redirect_to ns_switch_path(@ns_switch)
    end
  end

  private

  def log_mapping
    { pilot_group: "602",
      all: "1541",
      inactive: "1542",
      partly: "1543",
      description: "1544" }
  end

  def ns_switch_params
    params.require(:ns_switch).permit(:description, :status)
  end
end
