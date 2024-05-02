
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

# polymorphic attempts model,
# may be attached to anything that
# requires tracking of attempts
class Attempt < ActiveRecord::Base
  belongs_to :attemptable, polymorphic: true

  validates :attempt_type, inclusion: %w(app_activation_by_letter change_app_pin login login_app recover activation email revocation unblocking username_registration)

  default_scope { order("created_at DESC") }
  scope :created_after,   ->(time) { where("created_at > ?", time) }
  scope :created_before,  ->(time) { where("created_at < ?", time) }
  scope :current_month, -> { created_after(Time.zone.now.beginning_of_month).created_before(Time.zone.now) }

  class << self
    def clean_up_missing(attemptable)
      sql = <<-SQL
        DELETE `attempts` FROM `attempts`
        LEFT JOIN `#{attemptable.table_name}`
          ON `attempts`.attemptable_id = `#{attemptable.table_name}`.id
        WHERE `attempts`.`attemptable_type` = '#{attemptable.name}'
          AND `#{attemptable.table_name}`.id IS NULL
      SQL
      self.connection.update(sql)
    end
  end
end
