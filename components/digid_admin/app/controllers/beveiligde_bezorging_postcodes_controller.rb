
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

class BeveiligdeBezorgingPostcodesController < ApplicationController
  load_and_authorize_resource
  respond_to :html

  def index
    @beveiligde_bezorging_postcodes = BeveiligdeBezorgingPostcode.page(params[:page]).per(10)
    instrument_logger('uc40.beveiligde_bezorging_postcodes_inzien_gelukt', manager_id: session[:current_user_id])
  end

  def create
    new_postcode = BeveiligdeBezorgingPostcode.new(postcode_gebied: params[:postcode_gebied])
    if new_postcode.save
      instrument_logger('uc40.beveiligde_bezorging_postcode_aanmaken_gelukt', manager_id: session[:current_user_id], postcode_gebied: new_postcode.postcode_gebied)
      flash[:notice] = "Nieuw Postcodegebied #{new_postcode.postcode_gebied} toegevoegd"
    else
      flash[:alert] = "Toevoeging mislukt omwille van: #{new_postcode.errors.full_messages.join(' ,')}"
    end
    redirect_to beveiligde_bezorging_postcodes_path
  end

  def destroy
    destroyed_postcode = BeveiligdeBezorgingPostcode.find(params[:id]).destroy
    instrument_logger('uc40.beveiligde_bezorging_postcode_verwijderen_gelukt', manager_id: session[:current_user_id], postcode_gebied: destroyed_postcode.postcode_gebied)
    flash[:notice] = 'Postcodegebied verwijderd van de lijst voor beveiligde bezorging.'
    redirect_to beveiligde_bezorging_postcodes_path
  end
end
