
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

module AccountsHelper
  def mobile_number(phone_number, trunk = 0)
    return unless phone_number.present?

    if (nl_nummer = phone_number.match(/^(\+31|0031)(\d*)$/))
      "#{trunk}#{nl_nummer[2]}"
    else
      phone_number
    end
  end

  def time_helper(time, format, display_not_found = true)
    if time.present?
      l(time, format: format)
    elsif display_not_found
      t('not_found')
    end
  end

  def group_helper(bsn)
    links = []
    Subscriber.where(bsn: bsn).each do |subscriber|
      links << link_to(subscriber.subscription_name, pilot_group_path(subscriber.subscription))
    end
    raw links.join(', ')
  end

  def zekerheidsniveau_helper(zekerheidsniveau)
    if zekerheidsniveau == 20
      green t('accounts.fields.labels.active')
    else
      red t('accounts.fields.labels.inactive')
    end
  end

  def zekerheidsniveau_date_helper(last_change_security_level_at)
    if last_change_security_level_at.nil?
      "(datum laatste wijziging: niet bekend)"
    else
      "(datum laatste wijziging: #{l(last_change_security_level_at.to_date, format: :default)})"
    end
  end

  def policy_helper(policy)
    if policy.present? && (policy == 2 || policy == 1)
      "#{policy} (#{t('accounts.fields.labels.current_policy')})"
    else
      "#{t('accounts.fields.labels.unknown')} (#{t('accounts.fields.labels.current_policy')})"
    end
  end
end
