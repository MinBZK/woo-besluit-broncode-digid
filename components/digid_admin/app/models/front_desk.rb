
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

class FrontDesk < ActiveResource::Base
  self.site = "#{APP_CONFIG["urls"]["internal"]["balie"]}/iapi/"

  schema do
    string 'name', 'kvk_number', 'establishment_number', 'location', 'time_zone', 'code'
    integer 'alarm_unchecked_accounts', 'alarm_fraud_suspension', 'max_issues'
  end

  validates :name, :kvk_number, :location,
            :alarm_unchecked_accounts, :alarm_fraud_suspension,
            :code, :max_issues,
            presence: true

  validate :valid_time_zone?

  def valid_time_zone?
    errors.add(:time_zone, :invalid) unless TZInfo::Timezone.all_country_zone_identifiers.include?(time_zone)
  end

  def created_at
    attributes[:created_at].to_datetime
  end

  def updated_at
    attributes[:updated_at].to_datetime
  end

  def time_zone_object
    attributes[:time_zone] && FrontDeskTimeZone[attributes[:time_zone]]
  end

  alias tz time_zone_object

  def self.ransack(q)
    FrontDesk.find(:all, params: { search: q })
  rescue
    nil
  end
end
