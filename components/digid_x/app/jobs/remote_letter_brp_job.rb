
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

# frozen_string_literal: true

class RemoteLetterBrpJob < BrpRegistrationJob
  attr_accessor :account_id

   def self.schedule(options)
    run_job_at = schedule_job
    if run_job_at
      perform_at(run_job_at, options[:request_type], options[:registration_id], options[:web_registration_id], options[:activation_method], options[:account_id], options[:card_type])
      true
    else
      false
    end
  end

  def perform(request_type, registration_id, web_registration_id, activation_method, account_id, card_type)
    super(request_type, registration_id, web_registration_id, activation_method)

    unblock_letter(account_id, card_type) if registration.gba_status == "valid_unblock_letter"
  end

  def unblock_letter(account_id, card_type)
    registration.update_attribute(:status, Registration::Status::REQUESTED)

    if card_type == "NL-Rijbewijs"
      registration.update_letters_to_finished_and_expiration(Configuration.get_int("geldigheidstermijn_deblokkeringscode_brief"), ActivationLetter::LetterType::AANVRAAG_DEBLOKKERINGSCODE_RIJBEWIJS)
    else
      registration.update_letters_to_finished_and_expiration(Configuration.get_int("geldigheidstermijn_deblokkeringscode_brief"), ActivationLetter::LetterType::AANVRAAG_DEBLOKKERINGSCODE_IDENTITEITSKAART)
    end

    unblock_code = registration.activation_letters.last.controle_code
    registration.wid.update_attribute(:unblock_code, unblock_code)
    card_type = card_type == "NL-Rijbewijs" ? I18n.t("document.driving_licence") : I18n.t("document.id_card")
    Log.instrument("985", account_id: account_id, wid_type: card_type)
  end
end
