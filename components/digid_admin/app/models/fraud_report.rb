
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

# This model is used to:
# * check CanCan authorization.
# * pass BSNs to GbaRequestJob and process the BRP/GBA response.
# * ideally it would also be used for validations, etc. (TODO?)
class FraudReport
  include ActiveModel::Model
  attr_accessor :report_name, :manager_id, :batch_id, :bsns

  def initialize(*args)
    super
    @batch_id ||= SecureRandom.uuid
  end

  def bsns
    @bsns || []
  end

  def schedule
    # Ideally we want to use here Sidekiq Pro and the batch functionalitity,
    # but now we do poor man's batch: 10 minutes after the last enqueued job finished we schedule the report.
    # The last enqueued job isn't necessarily the last executed job, but 10 minutes give enough room here.
    SchedulerBlock.schedule_multiple('bulk-brp', bsns.size).each_with_index do |delayed_to, i|
      args = [bsns[i], batch_id]
      if i + 1 == bsns.size
        args << { report_name: report_name, manager_id: manager_id }
      end
      GbaRequestFraudJob.perform_at(delayed_to, *args)
    end
  end

  def finish
    # Schedule the job to perform after 10 minutes
    # Was already in place before move to Sidekiq, could have something to do with database synchronization
    # CHECK before removing
    AccountReportRequestJob.perform_in(10.minutes,
      report_type:  'account',
      job_id:       batch_id,
      report_name:  report_name,
      manager_id:   manager_id
    )
  end
end
