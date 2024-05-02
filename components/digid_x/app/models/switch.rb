
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

class Switch < ActiveRecord::Base
  module Status
    INACTIVE = 0
    ALL = 1
    PILOT_GROUP = 2
    PARTLY = 3
  end

  belongs_to :pilot_group, required: false

  validates :name, presence: true, uniqueness: { case_sensitive: true }, length: { maximum: 255 }
  validates :description, length: { maximum: 1024 }

  class << self
    def digid_app_enabled?
      switch_on?("Koppeling met DigiD app")
    end

    def rda_enabled?
      switch_on?("Koppeling met RDA server")
    end

    def digid_rda_enabled?
      switch_on?("Koppeling met DigiD RDA server")
    end

    def wid_checker_enabled?
      switch_on?("Koppeling met ID-checker")
    end

    def kiosk_enabled?
      switch_on?("Koppeling met DigiD kiosk")
    end

    def request_station_enabled?
      switch_on?("Koppeling met RvIG-Aanvraagstation")
    end

    def inlogniveau_verhogen_via_mydigid_enabled?
      switch_off?("Uitschakelen verhogen betrouwbaarheidsniveau DigiD app vanuit Mijn DigiD")
    end

    def show_digid_hoog?(bsn)
      show_driving_licence?(bsn) || show_identity_card?(bsn)
    end

    def show_identity_card?(bsn)
      switch_on?("Tonen DigiD Hoog - Identiteitskaart", bsn)
    end

    def identity_card_enabled?
      switch_on?("Koppeling met DigiD Hoog - Identiteitskaart")
    end

    def identity_card_partly_enabled?
      switch_partly_on?("Koppeling met DigiD Hoog - Identiteitskaart")
    end

    def identity_card_pin_reset_enabled?
      switch_on?("PIN-reset identiteitskaart")
    end

    def show_driving_licence?(bsn)
      switch_on?("Tonen DigiD Hoog - Rijbewijs", bsn)
    end

    def driving_licence_enabled?
      switch_on?("Koppeling met DigiD Hoog - Rijbewijs")
    end

    def driving_licence_partly_enabled?
      switch_partly_on?("Koppeling met DigiD Hoog - Rijbewijs")
    end

    def driving_licence_pin_reset_enabled?
      switch_on?("PIN-(re)set rijbewijs")
    end

    def driving_licence_pin_reset_partly_enabled?
      find_by(name: "PIN-(re)set rijbewijs").partly?
    end

    def eenvoudige_herauthenticatie_enabled?
      switch_on?("Eenvoudige Herauthenticatie")
    end

    #
    # WARNING!
    # This switch is NEVER allowed to be active on 'productie' environments.
    # It enables a test login feature, giving users the ability to login with every possible security level
    #
    def show_test_betrouwbaarheidsniveau?
      switch_on?("Tonen testen betrouwbaarheidsniveau") && APP_CONFIG["test_betrouwbaarheidsniveau"]
    end

    # List all methods ending on a ? and designating a Switch
    def test_methods
      @test_methods ||= (Switch.singleton_methods(false) - ActiveRecord::Base.singleton_methods).select { |method| method.to_s.ends_with?("?") }
    end

    private

    def switch_on?(name, bsn = nil)
      switch = find_by(name: name)
      return nil unless switch
      switch.on? || switch.on_for_pilot_group?(bsn)
    end

    def switch_off?(name)
      switch = find_by(name: name)
      return nil unless switch
      switch.off?
    end

    def switch_partly_on?(name, bsn = nil)
      switch = find_by(name: name)
      return nil unless switch
      switch.on? || switch.on_for_pilot_group?(bsn) || switch.partly?
    end
  end

  def off?
    status == Status::INACTIVE
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
