
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

class AppVersion < AccountBase
  include FourEyes

  OPERATING_SYSTEMS = ["Android", "iOS", "UWP", "macOS", "Kiosk", "idCheckerAndroid", "idCheckeriOS", "robotFramework"]
  RELEASE_TYPES = ["Productie", "Beta"]

  ransacker :version do
    Arel.sql("operating_system, INET_ATON(SUBSTRING_INDEX(CONCAT(`app_versions`.`version`,'.0.0.0'),'.',3))")
  end

  scope :valid, -> { where('NOW() >= not_valid_before AND (not_valid_on_or_after IS NULL OR NOW() < not_valid_on_or_after) AND (kill_app_on_or_after IS NULL OR NOW() < kill_app_on_or_after)') }
  scope :other_apps, -> (record) {
    where.not(id: record.id)
    .where(operating_system: record.operating_system, release_type: record.release_type)
  }

  validate :past_dates_are_readonly, on: :update
  validates :operating_system, presence: true
  validates :operating_system, inclusion: { in: OPERATING_SYSTEMS },
                               allow_blank: true

  validates :release_type, presence: true
  validates :release_type, inclusion: { in: RELEASE_TYPES },
                           allow_blank: true

  validates :version, presence: true
  validates :version, uniqueness: { case_sensitive: true, scope: [:operating_system, :release_type] },
                      format: { with: /\A\d+.\d+.\d+\z/ },
                      allow_blank: true

  # NOT_VALID_BEFORE
  # ===========================================================================
  validates :not_valid_before, presence: true

  validates_date :not_valid_before, on_or_after: :today,
                                    allow_blank: :blank,
                                    on: :create

  validates_date :not_valid_before, on_or_after: :today,
                                    allow_blank: :blank,
                                    on: :update,
                                    if: -> { not_valid_before_changed? }

  # NOT_VALID_ON_OR_AFTER
  # ===========================================================================
  validates_date :not_valid_on_or_after, on_or_after: :today,
                                         allow_blank: true,
                                         if: -> { not_valid_on_or_after_changed? }

  validates_date :not_valid_on_or_after, after: :not_valid_before,
                                         allow_blank: true,
                                         if: -> { not_valid_on_or_after_changed? && !errors.key?(:not_valid_on_or_after) }

  validates_date :not_valid_on_or_after, before: :kill_app_on_or_after,
                                         allow_blank: true,
                                         if: -> { not_valid_on_or_after_changed? && !errors.key?(:not_valid_on_or_after) && kill_app_on_or_after? }

  # KILL_APP_ON_OR_AFTER
  # ===========================================================================
  validates_date :kill_app_on_or_after, on_or_after: :today,
                                        allow_blank: true,
                                        if: -> { kill_app_on_or_after_changed? }

  validates_date :kill_app_on_or_after, after: :not_valid_before,
                                        allow_blank: true,
                                        if: -> { kill_app_on_or_after_changed? && !errors.key?(:kill_app_on_or_after) }

  validates_date :kill_app_on_or_after, after: :not_valid_on_or_after,
                                        allow_blank: true,
                                        if: -> { kill_app_on_or_after_changed? && !errors.key?(:kill_app_on_or_after) && not_valid_on_or_after? }

  def status
    return :kill_app        if kill_app?
    return :force_update    if force_update?
    return :update_warning  if update_warning?
    return :active          if _valid?
    raise "Could not determine the status for AppVersion: #{inspect}"
  end

  def name
    "#{version}/#{operating_system}"
  end

  def active?
    status == :active
  end

  def higher_active_version_exists?
    AppVersion.valid
              .where("INET_ATON(SUBSTRING_INDEX(CONCAT(`app_versions`.`version`,'.0.0.0'),'.',3)) > INET_ATON(?)", version)
              .where(operating_system: operating_system)
              .where.not(id: id)
              .any?
  end

  def newer_version_available_for_force_update?
    AppVersion.where("? = '' OR `app_versions`.`not_valid_before` <= ?", not_valid_on_or_after, not_valid_on_or_after)
    .where("? = '' OR `app_versions`.`not_valid_on_or_after` IS NULL OR `app_versions`.`not_valid_on_or_after` > ?", not_valid_on_or_after, not_valid_on_or_after)
    .where("`app_versions`.`kill_app_on_or_after` IS NULL OR `app_versions`.`not_valid_on_or_after` > ?", kill_app_on_or_after)
    .other_apps(self).any?{|record| Gem::Version.new(version) < Gem::Version.new(record.version)} || not_valid_on_or_after.blank?
  end

  def other_version_available_for_kill_app?
    AppVersion.where("`app_versions`.`kill_app_on_or_after` IS NULL OR `app_versions`.`kill_app_on_or_after` > ?", kill_app_on_or_after)
    .where("`app_versions`.`not_valid_on_or_after` IS NULL OR `app_versions`.`not_valid_on_or_after` > ?", kill_app_on_or_after)
    .where("`app_versions`.`not_valid_before` <= ?", kill_app_on_or_after || not_valid_on_or_after)
    .other_apps(self).any? || (AppVersion.count == 1 && AppVersion.first.kill_app_on_or_after.blank? && kill_app_on_or_after.blank?)
  end

  def other_active_version_exists?
    AppVersion.valid
              .where(operating_system: operating_system)
              .where(release_type: release_type)
              .where.not(id: id)
              .any?
  end

  private

  def not_valid_before_readonly?
    not_valid_before_changed? && not_valid_before_was && not_valid_before_was < Date.today && persisted?
  end

  def not_valid_on_or_after_readonly?
    not_valid_on_or_after_changed? && not_valid_on_or_after_was && not_valid_on_or_after_was < Date.today && persisted?
  end

  def kill_app_on_or_after_readonly?
    kill_app_on_or_after_changed? && kill_app_on_or_after_was && kill_app_on_or_after_was < Date.today && persisted?
  end

  def past_dates_are_readonly
    errors.add(:not_valid_before_was, 'Change of not_valid_before not allowed!') if not_valid_before_readonly?
    errors.add(:not_valid_on_or_after_was, 'Change of not_valid_on_or_after not allowed!') if not_valid_on_or_after_readonly?
    errors.add(:kill_app_on_or_after_was, 'Change of kill_app_on_or_after not allowed!') if kill_app_on_or_after_readonly?
  end

  def _valid?
    (Date.today >= not_valid_before) &&
      (not_valid_on_or_after? ? not_valid_on_or_after > Date.today : true) &&
      (kill_app_on_or_after? ? not_valid_on_or_after > Date.today : true)
  end

  def update_warning?
    _valid? && higher_active_version_exists?
  end

  def force_update?
    return true if not_valid_on_or_after? && Date.today >= not_valid_on_or_after
  end

  def kill_app?
    return true if kill_app_on_or_after? && Date.today >= kill_app_on_or_after
  end
end
