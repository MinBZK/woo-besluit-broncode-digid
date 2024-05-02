
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

class Switch < AccountBase
  module Status
    INACTIVE = 0
    ALL = 1
    PILOT_GROUP = 2
    PARTLY = 3
  end

  TEXT_STATUS = self::Status.constants(false).each_with_object([]) do |constant, mapping|
    mapping[self::Status.const_get(constant)] = constant.to_s.downcase
  end

  belongs_to :pilot_group, required: false

  validates :name, :description, presence: true
  validate :check_pilot_group_status

  paginates_per 30

  def check_pilot_group_status
    return unless status == Status::PILOT_GROUP && pilot_group.nil?
    errors.add(:status, :no_pilot_group)
  end

  def text_status
    TEXT_STATUS[status]
  end

  def human_status
    I18n.t(text_status, scope: status_key).html_safe
  end

  # Switches with 'gedeeltelijk' option
  def with_partly_option?
    [
      "Koppeling met DigiD Hoog - Rijbewijs",
      "Koppeling met DigiD Hoog - Identiteitskaart",
      "PIN-(re)set rijbewijs",
    ].include?(self.name)
  end

  def status_key
    case self.name
    when 'Koppeling met DigiD Hoog - Rijbewijs'
      'pilot_switch.status_driving_licence'
    when 'Koppeling met DigiD Hoog - Identiteitskaart'
      'pilot_switch.status_identity_card'
    when "Versturen sms app-activatie"
      'pilot_switch.status_sms_app'
    when "PIN-(re)set rijbewijs"
      'pilot_switch.reset_driving_licence'
    when "PIN-reset identiteitskaart"
      'pilot_switch.reset_identity_card'
    else
      'pilot_switch.status'
    end
  end

  class << self
    def rijbewijs_partly_enabled?
      switch_partly_on?('Koppeling met DigiD Hoog - Rijbewijs')
    end

    def identiteitskaart_partly_enabled?
      switch_partly_on?('Koppeling met DigiD Hoog - Identiteitskaart')
    end

    private

    def switch_partly_on?(name, bsn = nil)
      switch = find_by(name: name)
      return nil unless switch
      switch.on? || switch.on_for_pilot_group?(bsn) || switch.partly?
    end
  end

  def on?
    status == Status::ALL
  end

  def on_for_pilot_group?(bsn = nil)
    status == Status::PILOT_GROUP && bsn && pilot_group && pilot_group.subscriber?(bsn)
  end

  def partly?
    status == Status::PARTLY
  end
end
