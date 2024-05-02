
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

# Retrieves BRP (GBA) info for each removed BSN in a BulkOrder.
module Bulk
  class AddressRetriever
    attr_reader :bulk_order

    def initialize(bulk_order)
      @bulk_order = bulk_order
    end

    def can_be_retrieved?
      @bulk_order.persisted? && @bulk_order.order_finished_status?
    end

    def retrieve!
      unless can_be_retrieved?
        log_not_completed("onverwachte status (#{@bulk_order.human_status})")
        return false
      end

      prepare
      perform
      # finalize is done from last GbaRequestJob
      true
    rescue Exception => e # rubocop:disable Lint/RescueException
      finalize_with_exception(e)
      raise
    end

    # Called from last GbaRequestJob
    def finalize!
      finalize
    end

    private

    # Vastleggen start opvragen adressen
    # * de datum/tijd van starten wordt opgeslagen bij de opdracht en getoond op S072 Bulkopdracht
    # * de status van de opdracht wordt op 'Adressen worden opgevraagd' gezet
    # * log#689 wordt weggeschreven "Opvragen adressen bulkopdracht [id opdracht]/[naam opdracht] gestart", alleen in beheermodule te tonen (niet in Mijn Geschiedenis)
    def prepare
      bulk_order.touch(:brp_started_at) # Update time
      bulk_order.brp_started_status! # Update status
      Log.instrument('uc46.bulk_order_brp_started', id: @bulk_order.id, name: @bulk_order.name, type: @bulk_order.bulk_type, bulk_order_id: @bulk_order.id) # log#689
    end

    # Ophalen adresgegevens bij BRP
    # * voor elk met de bulkopdracht verwijderd account worden de volgende gegevens opgevraagd bij het BRP:
    #  * actuele adresgegevens; hierbij worden dezelfde gegevens uit het BRP opgehaald als bij het aanvragen van een nieuw account (waar een brief met activeringscode wordt verstuurd)
    #  * status van de persoonslijst
    # * voor elk opgevraagd adres worden logregels weggeschreven, alleen in beheermodule te tonen (niet in Mijn Geschiedenis):
    #  * log#684 "BRP opvraging adres voor BSN [BSN] start"
    #  * log#685 "BRP opvraging adres voor BSN [BSN] gelukt"
    #  * log#686 "BRP opvraging adres voor BSN [BSN] mislukt; time-out"
    def perform
      bsns = bulk_order.bulk_order_bsns
      schedule_times = SchedulerBlock.schedule_multiple('bulk-brp', bsns.size)
      schedule_times.each_with_index do |delayed_to, i|
        GbaRequestRetrieveJob.perform_at(delayed_to, bsns[i].bsn, bulk_order.id, i + 1 == bsns.size)
      end
      bulk_order.touch(:brp_last_run_at)
    end

    # Vastleggen einde uitvoering
    # * de datum/tijd van het afronden van het opvragen van de adresgegevens (en daarmee het afronden van de bulkopdracht) wordt opgeslagen bij de opdracht en getoond op S072 bulkopdracht
    # * de status van de opdracht wordt op 'Uitgevoerd' gezet
    # * log#682 wordt weggeschreven indien de opdracht succesvol is afgerond "Bulkopdracht [id opdracht]/[naam opdracht] uitgevoerd", alleen in beheermodule te tonen (niet in Mijn Geschiedenis)
    def finalize
      @bulk_order.touch(:finalized_at)
      @bulk_order.finalized_status!
      Log.instrument('uc46.bulk_order_brp_finished', id: @bulk_order.id, name: @bulk_order.name, type: @bulk_order.bulk_type) # log#682
    end

    def finalize_with_exception(e)
      log_not_completed(e.message.truncate(120))
    end

    def log_not_completed(reason)
      Log.instrument('uc46.bulk_order_brp_not_completed', id: @bulk_order.id, name: @bulk_order.name, type: @bulk_order.bulk_type, reason: reason) # log#683
    end
  end
end
