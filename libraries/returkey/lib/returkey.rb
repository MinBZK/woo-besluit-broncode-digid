
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

require 'active_support/inflector'
require 'ostruct'
require 'erb'
require 'nokogiri'
require 'net/http'
require 'uri'
require 'rack'
require 'xmldsig'

GEM_PATH = File.absolute_path(File.join(File.dirname(__FILE__), '..'))

require File.join(GEM_PATH, '/lib/returkey/version.rb')
require File.join(GEM_PATH, '/lib/returkey/app.rb')
require File.join(GEM_PATH, '/lib/returkey/engine.rb')
require File.join(GEM_PATH, '/lib/returkey/helpers/xml_helper.rb')
require File.join(GEM_PATH, '/lib/returkey/helpers/saml_helper.rb')
require File.join(GEM_PATH, '/lib/returkey/helpers/artifact_helper.rb')
require File.join(GEM_PATH, '/lib/returkey/rack_redirect.rb')
require File.join(GEM_PATH, '/lib/returkey/rack_response.rb')
require File.join(GEM_PATH, '/lib/returkey/saml_response.rb')
require File.join(GEM_PATH, '/lib/returkey/saml_artifact_redirect.rb')
require File.join(GEM_PATH, '/lib/returkey/saml_artifact_response.rb')
require File.join(GEM_PATH, '/lib/returkey/saml/artifact_response.rb')
require File.join(GEM_PATH, '/lib/returkey/saml/artifact_resolve.rb')
require File.join(GEM_PATH, '/lib/returkey/saml/request.rb')
require File.join(GEM_PATH, '/lib/returkey/saml/response.rb')
