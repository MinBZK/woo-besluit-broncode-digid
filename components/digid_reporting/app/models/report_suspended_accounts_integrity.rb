
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

class ReportSuspendedAccountsIntegrity < AdminReport
  extend ApplicationHelper

  def self.report_name
    "Opgeschorte accounts"
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

      headers = [ 'TOps2', 'TOps1', 'NOpsAan', 'NOpsUit', 'NOpsDelBeh', 'NOpsDelAdm' ]
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
        left = counter_hash['TOps2'] - counter_hash['TOps1']
        right = counter_hash['NOpsAan'] - counter_hash['NOpsUit'] - counter_hash['NOpsDelBeh'] - counter_hash['NOpsDelAdm']
         # QQQ OK/NOK??
         oknok = left == right ? 'OK' : 'NOK'
      else
        logger.error "Not all counters present, integrity check outcome: #{oknok}"
      end

      # Jira DigiD-818, add Date column
      start_date = Time.now.yesterday.beginning_of_day if start_date.nil?
      headers.prepend 'Periode'
      values.prepend "#{format_report_period(ReportParam::DAY, start_date)}"

      headers.prepend 'TOps2 - TOps1 = NOpsAan - NOpsUit - NOpsDelBeh - NOpsDelAdm'
      values.prepend oknok

      results = [headers, values]
    else
      logger.error "No snapshot found for: #{start_date}"
    end

# Opgeschorte accounts
# Het aantal opgeschorte accounts neemt toe door beheerders die een account opschorten. Het neemt af door beheerders die een opschorting ongedaan maken, en in alle situaties waarbij een opgeschort account wordt opgeheven/verwijderd.

# Noot: Volgens de Usecases kan voor een opgeschort account geen vervangend account worden aangevraagd of worden opgeheven door een gebruiker. Een opgeschort account zal ook niet verlopen na de maximale periode van inactiviteit (36 maanden in 4.01). Een opgeschort account kan wel door een beheerder worden verwijderd of de opschorting kan door de beheeder ongedaan gemaakt worden; een opgeschort account kan ook door een afnemer via de Admin interface worden verwijderd (Revoceren).

# Zij:
# NOpsAan    =  Het aantal opschortingen door beheerders
# NOpsUit    =  Het aantal malen dat een opschorting ongedaan wordt gemaakt door een beheerder.
# NOpsDelBeh  =  Het aantal opgeschorte accounts verwijderd door Beheerders.
# NOpsDelAdm  =  Het aantal opgeschorte accounts verwijderd door afnemers via de Admin interface   Revoceren.
# TOps1     =  Snapshot van het totaal aantal opgeschorte accounts op tijdstip T1
# TOps2    =  Snapshot van het totaal aantal opgeschorte accounts op tijdstip T2

# Dan geldt voor de aantallen gebeurtenissen tussen tijdstippen T1 en T2:
# TOps2 - TOps1 = NOpsAan - NopsUit - NOpsDelBeh - NOpsDelAdm



  end
end
