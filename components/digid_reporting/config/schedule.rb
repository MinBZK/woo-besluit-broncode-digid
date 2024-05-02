
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

# Use this file to easily define all of your cron jobs.
#
# It's helpful, but not entirely necessary to understand cron before proceeding.
# http://en.wikipedia.org/wiki/Cron

set :output, lambda { "&> /var/log/whenever.log" }

if ENV['RAILS_ENV']
  set :environment, ENV['RAILS_ENV']
end

every 1.month, :at => '02:00am', roles: [:db] do
  runner "AdminReport.generate_monthly_reports"
end

every :monday, :at => '03:00am', roles: [:db] do
  runner "AdminReport.generate_weekly_reports"
end

every 1.day, :at => '00:45am', roles: [:db] do
  runner "AdminReport.generate_fraud_reports"
end

every 1.day, :at => '01:30am', roles: [:db] do
  runner "AdminReport.generate_integrity_reports"
end

every 1.day, :at => '00:15am', roles: [:db] do
  runner "AdminReport.cleanup_reports"
end

every 1.day, :at => '00:30am', roles: [:db] do
  runner "Snapshot.generate_snapshot"
end

every 5.minutes, roles: [:db] do
  runner "MnnQryRunner.run"
end

every 1.day, :at => '03:55am', roles: [:db] do
  rake "digistorm:send_mail"
end

every 1.day, :at => '03:50am', roles: [:db] do
  rake "ebv:send_mail"
end

every :monday, :at => '07:55am', roles: [:db] do
  rake "gba_check:send_mail"
end

# Learn more: http://github.com/PPPPP/whenever
