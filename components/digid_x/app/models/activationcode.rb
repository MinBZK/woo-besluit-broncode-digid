
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

class Activationcode
  include ActiveModel::Model
  include ActiveModel::Validations::Callbacks

  attr_accessor :activationcode, :account, :authenticator

  validates :activationcode, code: { allow_blank: true, starts_with: "A" }, presence: true # Uses CodeValidator
  validates :account, presence: true

  after_validation :count_attempt
  after_validation :log_errors

  def activationcode_correct?
    return false if account.nil? || authenticator.nil?
    authenticator.activation_code_match?(activationcode)
  end

  def count_attempt
    return if account.nil? || authenticator.nil?
    return unless errors.messages.values.include?([I18n.t("activemodel.errors.models.activationcode.attributes.activationcode.incorrect_due_to_app")]) ||
      errors.messages.values.include?([I18n.t("activemodel.errors.models.activationcode.attributes.activationcode.incorrect", letter_sent_at: letter_sent_at)]) # FIXME: We don't count attempts not starting with 'A'
    
    authenticator.add_one_attempt
  end

  def letter_sent_at
    date = Registration.where(burgerservicenummer: account&.bsn).last&.activation_letters&.last&.letter_sent_at&.to_date
    I18n.l(date) if date
  end

  def log_errors
    return if account.nil?
    if activationcode.blank?
      Log.instrument("89", account_id: account.id)
    elsif errors.any?
      Log.instrument("88", account_id: account.id)
    end
  end
end
