
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

# Non persistent helper model.
# Used in ReportAccDispAuthlvlWeekly to calculate counters
class AccDispAuthlvl
  attr_accessor :dispensary
  attr_accessor :dispensary_code
  attr_accessor :authentication_lvl
  attr_accessor :accounts
  attr_accessor :total_accounts

  def initialize
    @logger = Rails.logger
    @dispensary = ""
    @dispensary_code = 0
    @authentication_lvl = ""
    @accounts = 0
    @total_accounts = 0
    @logger.debug "DEBUG AccDispAuthlvl.#{__method__} -> initialized AccDispAuthlvl instance."
  end

  def equal?(o)
    o.is_a?(AccDispAuthlvl) && dispensary == o.dispensary && authentication_lvl == o.authentication_lvl
  end

  def to_a
    [@dispensary, @dispensary_code, @authentication_lvl, @accounts, @total_accounts]
  end

  def to_s
    "#{@dispensary}, #{@dispensary_code}, #{@authentication_lvl}, #{@accounts}, #{@total_accounts}"
  end
 end
