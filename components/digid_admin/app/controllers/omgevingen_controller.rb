
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

class OmgevingenController < ApplicationController
  load_and_authorize_resource
  def index
    @environment_list = ['SSSSSSSSS', 'SSSSSSSS', 'SSSSSSSS', 'SSSS', 'SSSS', 'SSSS', 'SSSS', 'SSSS', 'SSSS']
    @environment_list << 'SS' << 'SS' if Rails.env.SS? || Rails.env.SS? || Rails.env.SS?
    @environment_list << 'SS' if Rails.env.SS?
    @omg = APP_CONFIG['urls']['external']['webdav'] ? client.get('omgevingen.json').result : {}
    render :layout=>false
    instrument_logger('uc15.inzien_overzicht_omgeving_gelukt')
  end

  private

  def client
    DigidUtils::Iapi::Client.new(url: APP_CONFIG['urls']['external']['webdav'], timeout: 5)
  end
end
