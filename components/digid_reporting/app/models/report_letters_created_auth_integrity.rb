
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

class ReportLettersCreatedAuthIntegrity < AdminReport
  extend ApplicationHelper

  def self.report_name
    "Brieven aangemaakt"
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

      headers = [ 'TBr2', 'TBr1', 'NMB_D', 'NMB_B', 'NMB_Adm', 'NH', 'NV', 'NS' ]
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
        left = counter_hash['TBr2'] - counter_hash['TBr1']
        right = counter_hash['NMB_D'] + counter_hash['NMB_B'] + counter_hash['NMB_Adm'] + counter_hash['NH'] - counter_hash['NV'] - counter_hash['NS']
         # QQQ OK/NOK??
         oknok = left == right ? 'OK' : 'NOK'
       else
          logger.error "Not all counters present, integrity check outcome: #{oknok}"
       end

      # Jira DigiD-818, add Date column
      start_date = Time.now.yesterday.beginning_of_day if start_date.nil?
      headers.prepend PERIOD_HEADER
      values.prepend "#{format_report_period(ReportParam::DAY, start_date)}"

      headers.prepend 'TBr2 - TBr1 = NMB_D + NMB_B + NMB_Adm + NH - NV - NS'
      values.prepend oknok

      results = [headers, values]
    else
      logger.error "No snapshot found for: #{start_date}"
      nil
    end

# "Zij:
# NMB_D  = Aantal succesvolle aanvragen (nieuw, her of uitbreiding) account op niveau midden en basis direct bij DigiD
# NMB_B = Aantal succesvolle aanvragen (nieuw, her of uitbreiding) account op niveau midden en basis via balie
# NMB_Adm = Aantal succesvolle aanvragen (nieuw, her- of uitbreiding) account op niveau midden en Basis via het WSDL voor Administratie
# NH  = Aantal succesvolle aanvragen voor een herstelcode via een brief .
# NV  = Aantal verwijderde aanvragen (doordat een aanvraag wordt ingetrokken/overschreven door een nieuwe aanvraag voordat de brief  wordt verstuurd).
# NS  = De som van het aantal briefopdrachten (voor herstel en activering) verstuurd aan de leverancier.
# TBr1  = Snapshot van de teller van het aantal brieven in de wachtrij voor de briefleverancier op tijdstip T1.
# TBr2  = Snapshot van de teller van het aantal brieven in de wachtrij voor de briefleverancier op tijdstip T2.

# Dan geldt voor de aantallen gebeurtenissen tussen tijdstippen T1 en T2:
# TBr2 - TBr1 = NMB_D + NMB_B + NMB_Adm + NH - NV - NS
# "
  end
end
