
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

class LetterRequest
  attr_accessor :account, :registration, :action, :error, :next_possible_registration

  def initialize(account_id:, action:, request_speed_limit:, request_max:, registration: nil)
    @account = Account.find(account_id)
    @registration = registration
    @action = action
    @request_speed_limit = request_speed_limit
    @request_max = request_max
  end

  def valid?
    reg_action = "valid_#{@action}"

    if registration.nil?
      @error = "no_registration"
      return false
    elsif @registration.registration_too_soon?(@request_speed_limit, [reg_action], nil)
      @error = "too_soon"
      return false
    elsif (next_possible_registration = @registration.registration_too_often?(@request_max, reg_action, nil))
      @error = "too_often"
      @next_possible_registration = next_possible_registration
      return false
    elsif registration.activation_letters.count >= 2
      @error = "too_many_letter_requests"
      return false
    else
      return true
    end
  end

  def prepare_letter activation_method
    @registration.update_attribute(:gba_status, "request")
    Log.instrument("155", account_id: @account.id, registration_id: @registration.id, hidden: true)
    BrpRegistrationJob.schedule(request_type: "#{@action}", registration_id: @registration.id, web_registration_id: nil, account_id: @account.id, activation_method: activation_method)
  end

end
