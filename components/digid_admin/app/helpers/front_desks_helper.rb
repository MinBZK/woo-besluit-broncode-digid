
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

module FrontDesksHelper
  # UCT acronym defined in inflections initializer
  #
  # Unfortunately Rails' inflector does not preserve acronyms which already
  # are upcased e.g.
  #
  # 'utc'.humanize => 'UTC'
  # 'UTC'.humanize => 'utc'
  #
  # This helper is used in table headers which are humanized again and thus
  # converting the acronym in the desired format.
  def front_desk_utc_offset
    @front_desk.tz.formatted_offset_utc.humanize
  end

  def select_time_zone_filter(filter)
    front_desk_search = params[:front_desk_search]
    selected_filter = params[:front_desk_search][:time_zone] if front_desk_search
    empty_filter = !front_desk_search || !selected_filter
    # select previously selected filter
    return true if front_desk_search && selected_filter == filter
    # select NL time as default
    return true if empty_filter && filter == 'nl'
  end
end
