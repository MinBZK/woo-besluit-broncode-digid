
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

class AppVersion < ActiveRecord::Base
  scope :valid, -> { where(":time_now >= not_valid_before AND (not_valid_on_or_after IS NULL OR :time_now < not_valid_on_or_after) AND (kill_app_on_or_after IS NULL OR :time_now < kill_app_on_or_after)", time_now: Time.zone.now) }

  def self.find_by_request(request)
    return unless request.version && request.operating_system && request.release_type
    version = find_by(
      version: request.version, operating_system: request.operating_system, release_type: request.release_type
    )

    return version if request.release_type != "Productie" || version&.allowed?
    # If this version is not allowed for Production, we check if the Beta is available
    beta = find_by(version: request.version, operating_system: request.operating_system, release_type: "Beta")
    # Only return beta variant if it is allowed
    beta&.allowed? ? beta : version
  end

  def status
    @status ||= if kill_app?
                  :kill_app
                elsif force_update?
                  :force_update
                elsif update_warning?
                  :update_warning
                elsif _valid?
                  :active
                else
                  :kill_app
                end
  end

  def allowed?
    [:active, :update_warning].include?(status)
  end

  def newest?
    status == :active
  end

  def higher_active_version_exists?
    AppVersion.valid
              .where("INET_ATON(SUBSTRING_INDEX(CONCAT(`app_versions`.`version`,'.0.0.0'),'.',3)) > INET_ATON(?)", version)
              .where(operating_system: operating_system)
              .where(release_type: release_type)
              .where.not(id: id)
              .exists?
  end

  def update_warning?
    _valid? && higher_active_version_exists?
  end

  def force_update?
    not_valid_on_or_after? && Time.zone.today >= not_valid_on_or_after
  end

  def kill_app?
    kill_app_on_or_after? && Time.zone.today >= kill_app_on_or_after
  end

  private

  def _valid?
    (Time.zone.today >= not_valid_before) &&
      (not_valid_on_or_after? ? not_valid_on_or_after > Time.zone.today : true) &&
      (kill_app_on_or_after? ? kill_app_on_or_after > Time.zone.today : true)
  end
end
