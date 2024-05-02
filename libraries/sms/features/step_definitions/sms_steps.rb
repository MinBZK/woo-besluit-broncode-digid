
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

options = {
  sender: 'DigiD',
  gateway: 'SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS',
  message: 'Dit is een test',
  phone_number: 'PPPPPPPPPPPP',
  scenario: '1',
  timeout: 10
}

Stel /^ik stuur een SMS bericht$/ do
  stub_request(:get, /.*cm./).to_return(status: 200, body: '000', headers: {})
  stub_request(:get, /.*foobar./).to_return(status: 200, body: 'INVALID', headers: {})
  stub_request(:post, 'SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS').to_return(status: 200, body: "result code='0'", headers: {})
  @response = Sms.deliver(options)
end

Stel /^ik stuur een SMS bericht met een timeout$/ do
  stub_request(:get, /.*cm./).to_return(status: 200, body: '000', headers: {}).to_timeout
  stub_request(:get, /.*foobar./).to_return(status: 200, body: 'INVALID', headers: {}).to_timeout
  stub_request(:post, 'SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS').to_return(status: 200, body: "result code='0'", headers: {}).to_timeout
  @response = Sms.deliver(options)
end

Stel /^de configuratie is (.+)$/ do |juist|
  if juist != 'juist'
    options[:gateway] = 'http://www.foobarbestaatniet.nl/bestaatniet'
  end
end

Stel /^de gesproken SMS provider is geconfigureerd$/ do
  options[:gateway] = 'SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS'
  options[:spoken] = true
end

Dan /^moet ik een bericht terug krijgen dat de SMS is afgeleverd bij de provider$/ do
  @response.should == true
end

Dan /^moet ik een bericht terug krijgen dat de SMS niet is afgeleverd bij de provider$/ do
  @response.should == false
end
