
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

class RecoverAccountMethod
  include ActiveModel::Model

  attr_accessor :method, :account_id

  validate :email_checks?, if: -> { method.eql?("email") }
  validate :gba_checks?,   if: -> { method.eql?("letter") }

  # <Controle: Snelheid herstellen per e-mail>
  def email_checks?
    return if sent_more_than_one_hour_ago?

    Log.instrument("206", account_id: account_id)
    errors.add(:base, I18n.t("activemodel.errors.models.recover_account.attributes.recovery_code_email_expired",
                             time_blocked: I18n.l(blocked_till, format: :time_text_tzone_in_brackets)))
  end

  def gba_checks?
    account = Account.find(account_id)
    return unless account.present?

    match = RecoverAccountChecks::Queue.new(account).letter_checks
    return unless match

    match.log
    errors.add(:base, match.error_message)
  end

  private

  def sent_more_than_one_hour_ago?
    last_created = last_created_recovery_code
    last_created.blank? || (Time.zone.now.to_i - last_created["created_at"].to_i > 3600)
  end

  def last_created_recovery_code
    RecoveryCode.where(account_id: account_id).where(bearer: "password").last
  end

  def blocked_till
    last_created = last_created_recovery_code
    last_created["created_at"] + 60.minutes
  end
end
