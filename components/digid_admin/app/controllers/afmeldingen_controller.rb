
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

class AfmeldingenController < ApplicationController
  include SearchEngineHelper
  include Concerns::Gba
  load_and_authorize_resource

  before_action :before_new, only: :new

  def new
    gba = gba_data!(params[:bsn])
    if gba && !gba['status'].eql?('not_found')
      @sector_pretty_name = Sector.find_by(name: 'bsn').pretty_name
      @afmelding = current_afmeldlijst.subscribers.new(bsn: params[:bsn], created_at: Time.now)
    elsif gba && gba['status'].eql?('not_found')
      flash[:alert] = "Burgerservicenummer (#{params[:bsn]}) kon niet in de GBA gevonden worden"
      redirect_to afmeldlijsten_path
    end
  rescue GbaClient::ResponseError
    flash[:notice] = 'Het GBA is niet bereikbaar'
    redirect_to afmeldlijsten_path
  end

  def create
    current_afmeldlijst.subscribers.create(create_afmelding_params)
    instrument_logger('uc32.afmelding_aanmaken_gelukt', subject: create_afmelding_params[:bsn])
    flash[:notice] = "Burgerservicenummer (#{create_afmelding_params[:bsn]}) toegevoegd aan de afmeldlijst."
    redirect_to afmeldlijsten_path
  end

  def destroy
    afmelding = current_afmeldlijst.subscribers.find(params[:id])
    instrument_logger('uc32.afmelding_verwijderen_gelukt', subject: afmelding.bsn)
    afmelding.destroy
    flash[:notice] = 'Afmelding verwijderd van de afmeldlijst.'
    redirect_to afmeldlijsten_path
  end

  private

  def before_new
    # do we have a valid bsn?
    if (params[:bsn] =~ /\A[0-9]{8,9}\z/) && elf_proef_helper(params[:bsn])
      # do we have a DigiD account for this bsn?
      sectorcode = Sectorcode.find_by_sectoraalnummer_and_sector_id(params[:bsn], Sector.get('bsn'))
      if sectorcode
        flash[:alert] = "Afmelding mislukt, burgerservicenummer (#{params[:bsn]}) heeft een account"
        redirect_to afmeldlijsten_path
      else
        # do we already have a Afmelding?
        afmelding = Afmeldlijst.first.subscribers.where(bsn: params[:bsn])
        if afmelding.any?
          flash[:alert] = "Afmelding mislukt, burgerservicenummer (#{params[:bsn]}) is al afgemeldt."
          redirect_to afmeldlijsten_path
        end
      end
    else
      flash[:alert] = 'Afmelding mislukt, het ingevoerde burgerservicenummer is niet geldig.'
      redirect_to afmeldlijsten_path
    end
  end

  def create_afmelding_params
    params.require(:afmelding).permit(:bsn, :created_at)
  end
end
