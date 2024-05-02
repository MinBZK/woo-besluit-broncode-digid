
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

class Registration < AccountBase
  has_many :activation_letters, autosave: true, dependent: :destroy
  belongs_to :nationality

  alias_attribute :name, :burgerservicenummer

  module Status
    ABORTED   = 'aborted'.freeze # afgebroken
    COMPLETED = 'completed'.freeze
    INITIAL   = 'initial'.freeze # initieel
    REQUESTED = 'requested'.freeze # aangevraagd
  end

  include Stateful

  def self.month_count(bsn)
    bsn_is(bsn).with_valid_recovery_code.in_last_month.count
  end

  def self.bsn_is(bsn)
    where burgerservicenummer: bsn
  end

  def self.with_valid_recovery_code
    where gba_status: 'valid_recovery'
  end

  def self.in_last_month
    where 'created_at > ?', Time.now - 1.month
  end

  def self.too_soon
    snelheid_herstelcode = ::Configuration.get_int('snelheid_herstelcode')
    where 'created_at > ?', Time.now - snelheid_herstelcode.hours
  end

  def self.postcodes(postcode)
    where postcode: postcode
  end

  def self.valid
    where gba_status: 'valid'
  end

  def self.find_bsns_from_postcodes(content)
    bsns = []
    content.each do |postcode|
      bsns << Registration.valid.postcodes(postcode.gsub(/\s+/, '')).pluck(:burgerservicenummer)
    end
    bsns.flatten.uniq
  end
end
