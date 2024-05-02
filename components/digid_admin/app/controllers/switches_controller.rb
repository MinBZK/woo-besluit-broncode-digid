
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

class SwitchesController < ApplicationController
  load_and_authorize_resource
  respond_to :html

  def index
    @switches = Switch.page(params[:page])
    @switches = @switches.where.not(name: 'Tonen testen betrouwbaarheidsniveau') unless APP_CONFIG["test_betrouwbaarheidsniveau"]
    @ns_switches = NsSwitch.all rescue []
    @app_switches = AppSwitch.all if APP_CONFIG['use_switches_from_app_ms']
    instrument_logger('pilot_switch.pilot_switches_inzien_gelukt', switch_id: '')
  end

  def show
    instrument_logger('pilot_switch.inzien_gelukt', switch_name: @switch.name, switch_id: @switch.id)
  end

  def edit
    instrument_logger('pilot_switch.inzien_aanpassen_gelukt', switch_name: @switch.name, switch_id: @switch.id)
  end

  def update
    @switch.assign_attributes(switch_params)

    description_changed = @switch.description_changed?
    status_changed = @switch.status_changed?

    if @switch.changed?
      if @switch.save
        instrument_logger('1544', switch_name: @switch.name, switch_id: @switch.id) if description_changed
        instrument_logger(log_mapping[@switch.text_status.to_sym], switch_name: @switch.name, switch_id: @switch.id) if status_changed
        redirect_to switch_path(@switch), notice: update_success(@switch)
      else
        instrument_logger('pilot_switch.wijzigen_mislukt', switch_name: @switch.name, switch_id: @switch.id)
        render :edit
      end
    else
      redirect_to switch_path(@switch)
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

  def switch_params
    params.require(:switch).permit(:description, :status)
  end
end
