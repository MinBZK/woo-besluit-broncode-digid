
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

class Role < AccountBase
  include FourEyes

  attr_accessor :added_managers, :removed_managers

  has_and_belongs_to_many :managers, -> { order(:name) }, validate: false, after_add: :manager_added, after_remove: :manager_removed # rubocop:disable Rails/HasAndBelongsToMany
  has_and_belongs_to_many :permissions, -> { order(:name) } # rubocop:disable Rails/HasAndBelongsToMany

  accepts_nested_attributes_for :managers

  default_scope { order(:name) }

  validates :name, presence: true, uniqueness: { case_sensitive: false }

  freeze_under_review :managers

  def self.frozen_ids
    ids = []
    Manager.get_reviews.each do |review|
      ids += (review.original.role_ids + review.updated.role_ids).map(&:to_i)
    end
    ids.uniq
  end

  def frozen_for_review?
    self.class.frozen_ids.include?(id)
  end

  def managers_updated_since?(updating_started_at)
    latest_manager_updated_at = managers.pluck(:four_eyes_updated_at).compact.max
    latest_manager_updated_at >= updating_started_at if latest_manager_updated_at && updating_started_at
  end

  def to_s
    name
  end

  private

  # store modified roles to log after save
  def manager_added(manager)
    self.added_managers ||= []
    self.added_managers.push(manager)
  end

  def manager_removed(manager)
    self.removed_managers ||= []
    self.removed_managers.push(manager)
  end

end
