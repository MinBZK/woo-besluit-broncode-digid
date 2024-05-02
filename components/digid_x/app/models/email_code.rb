
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

class EmailCode
  include ActiveModel::Model
  include ActiveModel::Validations::Callbacks

  attr_accessor :account, :code

  after_validation(:count_attempt)

  validates :code, code: { allow_blank: true, starts_with: "E" },
                   presence: true

  def count_attempt
    email.add_one_attempt if errors[:code].include?(I18n.t("activemodel.errors.models.email_code.attributes.code.incorrect"))
    email.update_attribute :status, Email::Status::BLOCKED if email.reached_attempts_limit?
  end

  def code_correct?
    if email.controle_code? && code.casecmp(email.controle_code.upcase).zero? && !email.expired?
      true
    else
      Log.instrument("48", account_id: account.id)
      false
    end
  end

  delegate :email, to: :account
end
