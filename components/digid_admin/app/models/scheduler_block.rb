
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
class SchedulerBlock < AccountBase
  include DigidUtils::BlockScheduler

  def calculate_key(time)
    (inside_office_hours?(time) ? "I" : "O") + number_from_time(time, 1.hour).to_s
  end

  def serial_of_new_block(time)
    if time.hour == office_hours[:start_hour] && time.min >= office_hours[:start_min]
      (office_hours[:start_min].minutes / delay_in_office_hours).round + 1
    elsif time.hour == office_hours[:end_hour] && time.min >= office_hours[:end_min]
      (office_hours[:end_min].minutes / delay_in_office_hours).round + 1
    else
      1
    end
  end

  def candidate
    delay = key[0] == "I" ? delay_in_office_hours : delay_outside_office_hours
    time_from_number(key[1..-1].to_i, delay)
  end

  private

  def delay_in_office_hours
    @delay_in_office_hours ||= 1.hour.to_f / Configuration.get_int('aantal_bulk_BRP_bevragingen_tijdens_kantoortijden')
  end

  def delay_outside_office_hours
    @delay_outside_office_hours ||= 1.hour.to_f / Configuration.get_int('aantal_bulk_BRP_bevragingen_buiten_kantoortijden')
  end

  def inside_office_hours?(time)
    time.between?(office_time_start(time), office_time_end(time))
  end

  def office_time_start(time = Time.zone.now)
    Time.zone.local(time.year, time.month, time.day, office_hours[:start_hour], office_hours[:start_min], 0)
  end

  def office_time_end(time = Time.zone.now)
    Time.zone.local(time.year, time.month, time.day, office_hours[:end_hour], office_hours[:end_min], 0)
  end

  def office_hours
    @office_hours ||= Configuration.brp_request_office_hours
  end
end
