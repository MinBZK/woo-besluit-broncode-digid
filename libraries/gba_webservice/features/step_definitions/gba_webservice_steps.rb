
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

Before do
  # For local testing
  @gba_url = 'SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS'

  # For testing on Testomgeving with GBA-V proefomgeving
  #SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
  # @ssl = { 'ssl_cert_key_file'     => '/home/ruby/www/digid/shared/config/gbaproef-client.digid.nl.key',
  #          'ssl_cert_file'         => '/home/ruby/www/digid/shared/config/gbaproef-client.digid.nl.crt',
  #SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
  #SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
  #SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
end

# Happy flow
Stel /^ik doe een aanvraag naar de GBA webservice$/ do
  @search = {'10120' => 'PPPPPPPPP'}
  @userdata = GbaWebservice.get_gba_data(@gba_url, @search, @ssl)
end

Stel /^ik doe een aanvraag naar de GBA webservice met de "([^"]*)" van iemand met de status "([^"]*)"$/ do |bsn, status|
  @search = {'10120' => bsn }
  @status = status
end

Stel /^ik doe een aanvraag naar de GBA webservice met bsn "(.*?)"$/ do |bsn|
  @search = {'10120' => bsn }
  @userdata = GbaWebservice.get_gba_data(@gba_url, @search, @ssl)

end

Dan /^moet ik de juiste status uit de GBA webservice krijgen$/ do
  @userdata = GbaWebservice.get_gba_data(@gba_url, @search, @ssl)
  expect(@userdata['status']).to eq(@status)
end

Dan /^moet ik de gevraagde gegevens uit de GBA webservice krijgen$/ do
  expect(@userdata.marshal_dump).to be
end
