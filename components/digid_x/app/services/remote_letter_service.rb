
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

class RemoteLetterService
  attr_accessor :account, :registration, :action, :error, :next_possible_registration, :card_type

  def initialize(account_id:, sequence_no:, card_type:, action:)
    self.account = Account.find(account_id)
    self.registration = Registration.create_fake_aanvraag self.account.bsn
    self.registration.create_wid(sequence_no: sequence_no, card_type: card_type, action: action) if action == "unblock"
    self.action = action
    self.card_type = card_type
  end

  def valid?
    reg_action = "valid_#{self.action}_letter"
    if self.registration.registration_too_soon?("snelheid_aanvragen_deblokkeringscode_identiteitsbewijs", [reg_action], self.registration.wid.try(:id))
      Log.instrument("981", account_id: self.account.id, wid_type: self.card_type)
      self.error = "too_soon"
      return false
    elsif (next_possible_registration = self.registration.registration_too_often?("blokkering_aanvragen_deblokkeringscode_identiteitsbewijs", reg_action, self.registration.wid.try(:id)))
      Log.instrument("983", account_id: self.account.id, wid_type: self.card_type)
      self.error = "too_often"
      self.next_possible_registration = next_possible_registration
      return false
    else
      return true
    end
  end

  def prepare_letter
    self.registration.update_attribute(:gba_status, "request")
    Log.instrument("155", account_id: self.account.id, registration_id: self.registration.id, hidden: true)
    RemoteLetterBrpJob.schedule(request_type: "#{self.action}_letter", registration_id: self.registration.id, web_registration_id: nil, account_id: self.account.id, card_type: self.card_type)
    clean_pending_unblock_letter_requests(registration.wid)
  end

  def error_message
    self.error
  end

  def next_registration_date
    I18n.l(self.next_possible_registration, format: :date) if self.next_possible_registration
  end

  private

  def clean_pending_unblock_letter_requests(wid)
    previous_registration_ids = Wid.where.not(id: wid.id).where(sequence_no: wid.sequence_no, card_type: wid.card_type, action: "unblock").pluck(:registration_id)
    previous_registration = Registration.where(id: previous_registration_ids, gba_status: "valid_unblock_letter").last
    if previous_registration
        if previous_registration.activation_letters.last.try(:state) == ActivationLetter::Status::SENT
          Log.instrument("1008", registration_id: previous_registration.id, account_id: self.account.id, wid_type: wid.card_type)
        else
          Log.instrument("1009", registration_id: previous_registration.id, account_id: self.account.id, wid_type: wid.card_type)
        end
    end

    Wid.where.not(id: wid.id).where(sequence_no: wid.sequence_no, card_type: wid.card_type, action: "unblock").update_all(unblock_code: nil)
  end
end
