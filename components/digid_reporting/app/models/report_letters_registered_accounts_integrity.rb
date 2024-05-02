
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

class ReportLettersRegisteredAccountsIntegrity < AdminReport
  extend ApplicationHelper

  def self.report_name
    "Aangevraagde accounts"
  end

  def self.report(start_date = nil)
    snapshot = first_snapshot_after(start_date)

    if snapshot.present?
      logger.info "snapshot: #{snapshot.inspect}"
      # QQQ AR relation!? has_many
      counter_array = SnapshotCounter.where(:snapshot_id => snapshot.id)

      counter_hash = {}
      counter_array.each do |c|
        counter_hash[c.name] = c.value
        logger.info "counter: #{c.name}. value: #{c.value}"
      end

      headers = [ 'TA2', 'TA1', 'NAanD', 'NAct', 'NVerl', 'NIng' ]
      values = []

      all_counters_present = true
      headers.each do |r|
        if counter_hash[r].present?
          values << counter_hash[r]
        else
          values << ""
          all_counters_present = false
          logger.warn "Counter not present: #{r}"
        end
      end

      oknok = 'NOK'
      if all_counters_present
        left = counter_hash['TA2'] - counter_hash['TA1']
        right = counter_hash['NAanD']  -
                counter_hash['NAct'] -
                counter_hash['NVerl'] -
                counter_hash['NIng']

         # QQQ OK/NOK??
         oknok = left == right ? 'OK' : 'NOK'
       else
          logger.error "Not all counters present, integrity check outcome: #{oknok}"
       end

      # Jira DigiD-818, add Date column
      start_date = Time.now.yesterday.beginning_of_day if start_date.nil?
      headers.prepend 'Periode'
      values.prepend "#{format_report_period(ReportParam::DAY, start_date)}"

      headers.prepend 'TA2 - TA1 = NAanD - NAct - NVerl - NIng'
      values.prepend oknok

      results = [headers, values]
    else
      logger.error "No snapshot found for: #{start_date}"
    end

# Aangevraagde accounts
# Aanvragen voor nieuwe accounts worden opgeslagen in een aanvraagtabel. Aan deze tabel worden gegevens toegevoegd op het moment dat een eindgebruiker een aanvraag indient. Gegevens worden verwijderd als een aanvraag leidt tot een activering van een account of als de aanvraag
# verloopt, of als de gebruiker de aanvraag vervangt door een nieuwe.

# Zij:
# NAanD   =  Aantal succesvolle aanvragen (nieuw, her- of voor uitbreiding) ingediend door eindgebruikers rechtstreeks via DigiD.
# NAct  =  Het aantal succesvolle activeringen.
# NVerl  =  Het aantal aanvragen dat vervalt doordat de geldigheidstermijn is verstreken.
# NIng  =  Het aantal aanvragen dat is ingetrokken/overschreven door de eindgebruiker.
# TA1  =  Snapshot van het aantal lopende aanvragen op tijdstip T1.
# TA2  =  Snapshot van het aantal lopende aanvragen op tijdstip T2.

# Dan geldt voor de aantallen gebeurtenissen tussen tijdstippen T1 en T2:
# TA2 - TA1 = NAanD - NAct - NVerl - NIng

  end
end
