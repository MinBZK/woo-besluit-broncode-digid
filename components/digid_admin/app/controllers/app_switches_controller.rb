
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

class AppSwitchesController < ApplicationController
  load_resource
  authorize_resource(Switch)

  respond_to :html

  def show
    instrument_logger('pilot_switch.inzien_gelukt', switch_name: @app_switch.name, switch_id: @app_switch.id)
  end

  def edit
    instrument_logger('pilot_switch.inzien_aanpassen_gelukt', switch_name: @app_switch.name, switch_id: @app_switch.id)
  end

  def update
    description_changed = @app_switch.description != app_switch_params["description"]
    status_changed = @app_switch.status != app_switch_params["status"]

    @app_switch.description = app_switch_params["description"]
    @app_switch.status = app_switch_params["status"]

    if description_changed || status_changed
      if @app_switch.save
        instrument_logger('1544', switch_name: @app_switch.name, switch_id: @app_switch.id) if description_changed
        instrument_logger(log_mapping[@app_switch.text_status.to_sym], switch_name: @app_switch.name, switch_id: @app_switch.id) if status_changed
        redirect_to app_switch_path(@app_switch), notice: update_success(@app_switch)
      else
        instrument_logger('pilot_switch.wijzigen_mislukt', switch_name: @app_switch.name, switch_id: @app_switch.id)
        render :edit
      end
    else
      redirect_to app_switch_path(@app_switch)
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

  def app_switch_params
    params.require(:app_switch).permit(:description, :status)
  end
end
