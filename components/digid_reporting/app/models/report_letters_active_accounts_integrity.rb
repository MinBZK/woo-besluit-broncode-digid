
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

class ReportLettersActiveAccountsIntegrity < AdminReport
  extend ApplicationHelper

  def self.report_name
    "Aantal actieve accounts inclusief opgeschorte accounts"
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

      headers = ['TAct2', 'TAct1', 'NAct', 'NOph', 'NVervang', 'NVerloop', 'NDelBeh', 'NCrAdm', 'NCrBeh', 'NDelAdm' ]
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
        left = counter_hash['TAct2'] - counter_hash['TAct1']
        right = counter_hash['NAct'] -
        counter_hash['NOph'] -
        counter_hash['NVervang'] -
        counter_hash['NVerloop'] -
        counter_hash['NDelBeh'] +
        counter_hash['NCrAdm'] +
        counter_hash['NCrBeh'] -
        counter_hash['NDelAdm']
         # QQQ OK/NOK??
         oknok = left == right ? 'OK' : 'NOK'
       else
          logger.error "Not all counters present, integrity check outcome: #{oknok}"
       end

      # Jira DigiD-818, add Date column

      start_date = Time.now.yesterday.beginning_of_day if start_date.nil?
      headers.prepend 'Periode'
      values.prepend format_report_period(ReportParam::DAY, start_date)

      headers.prepend 'TAct2 - TAct1 = NAct - NOph - NVervang - NVerloop - NDelBeh + NCrAdm + NCrBeh - NDelAdm'
      values.prepend oknok

      results = [headers, values]
    else
      logger.error "No snapshot found for: #{start_date}"
      nil
    end

# Aantal actieve accounts inclusief opgeschorte accounts
# Het aantal actieve accounts (status actief inclusief status opgeschort die weer actief gemaakt kan worden) neemt toe door het succesvol activeren van een aangevraagd account door de eindgebruiker en daalt door eindgebruikers die hun account opheffen of een vervangend account aanvragen. Accounts die te lang niet worden gebruikt krijgen een status opgeheven en verminderen daarmee het aantal actieve accounts.

# Accounts kunnen ook direct worden aangemaakt en verwijderd door afnemers (testaccounts, accounts in andere sectoren dan BSN / Sofi) waardoor het aantal actieve accounts wijzigt.

# Zij:
# NAct  =  Het aantal succesvolle activeringen.
# NOph  =  Het aantal malen dat een account vervalt door dat de eindgebruiker het account opheft.
# NVervang  =  Het aantal malen dat een account vervalt doordat de gebruiker een vervangend account heeft geactiveerd.
# NVerloop  =  Het aantal malen dat een account verloopt doordat de gebruiker het account te lang niet heeft gebruikt (36 maanden in 4.01).
# NDelBeh  =  Het aantal accounts verwijderd door Beheerders.
# NCrBeh  =  Het aantal (test) accounts aangemaakt door Beheerders (indien van toepassing)
# NCrAdm  =  Het aantal accounts direct aangemaakt door afnemers via de Admin interface AanmakenSectorAccountOnderwater (alleen testaccounts).
# NDelAdm  =  Het aantal accounts verwijderd door afnemers via de Admin interface Revoceren.
# TAct1   =  Snapshot van het aantal actieve (inclusief opgeschorte) accounts op tijdstip T1.
# TAct2  =  Snapshot van het aantal actieve (inclusief opgeschorte) accounts op tijdstip T2.

# Dan geldt voor de aantallen gebeurtenissen tussen tijdstippen T1 en T2:
# TAct2 - TAct1 = NAct - NOph - NVervang - NVerloop - NDelBeh + NCrAdm + NCrBeh - NDelAdm

  end
end
