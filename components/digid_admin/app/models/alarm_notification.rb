
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

class AlarmNotification < AccountBase
  has_many :alarm_notifications_managers
  has_many :managers, through: :alarm_notifications_managers

  scope :by_report_type, (lambda do |key|
    where('identifier = ?', [:receive_alerts, key.to_sym].to_yaml)
  end)

  serialize :identifier, Array

  def allowed_for?(user)
    if user && !user.active?
      user = user.clone
      user.active = true
    end
    Ability.new(user).can?(*identifier)
  end

  def self.find_by_report_type(key)
    by_report_type(key).first
  end

  def managers_to_be_notified_by_email
    managers.active.to_be_notified_by_email.select do |manager|
      allowed_for?(manager)
    end
  end

  def managers_to_be_notified_by_sms
    managers.active.to_be_notified_by_sms.select do |manager|
      allowed_for?(manager)
    end
  end
end
