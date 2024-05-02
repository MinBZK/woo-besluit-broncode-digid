
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

require "spec_helper"

RSpec.describe Letter::Salutation do
  GBA_FIELD_MAP = {
    :geslachtsaanduiding => "010410",
    :voornamen => "010210",
    :adellijke_titel => "010220",
    :aanduiding_naamgebruik => "016110",
    :voorvoegsel_geslachtsnaam => "010230",
    :geslachtsnaam => "010240",
    :voorvoegsel_geslachtsnaam_partner => "050230",
    :geslachtsnaam_partner => "050240",
  }.freeze

  let(:saluation_wrapper) { Class.new { extend Letter::Salutation } }
  let(:gba) {{
    GBA_FIELD_MAP[:voornamen] => "PPPPPPPPPP",
    GBA_FIELD_MAP[:voorvoegsel_geslachtsnaam] => "PP",
    GBA_FIELD_MAP[:geslachtsnaam] => "PPPPP",
    GBA_FIELD_MAP[:aanduiding_naamgebruik] => nil,
    GBA_FIELD_MAP[:voorvoegsel_geslachtsnaam_partner] => "PP",
    GBA_FIELD_MAP[:geslachtsnaam_partner] => "PPPPP",
    GBA_FIELD_MAP[:geslachtsaanduiding] => "M",
    GBA_FIELD_MAP[:adellijke_titel] => nil,
  }}

  describe "With geslachtsaanduiding O" do
    before(:each) do
      gba[GBA_FIELD_MAP[:geslachtsaanduiding]] = "O"
    end

    it "With defaults" do
      expect(saluation_wrapper.naam_aanhef(gba)).to eq("PPPPPPPPPPPPPPPPPPPPPP")
      expect(saluation_wrapper.naam(gba)).to eq("PPPPPPPPPPPPPP")
    end

    it "With adelijk JH" do
      gba[GBA_FIELD_MAP[:adellijke_titel]] = "JH"

      expect(saluation_wrapper.naam_aanhef(gba)).to eq("PPPPPPPPPPPPPPPPPPPPPP")
      expect(saluation_wrapper.naam(gba)).to eq("PPPPPPPPPPPPPP")
    end

    it "With adelijk B" do
      gba[GBA_FIELD_MAP[:adellijke_titel]] = "B"

      expect(saluation_wrapper.naam_aanhef(gba)).to eq("PPPPPPPPPPPPPPPPPPPPPP")
      expect(saluation_wrapper.naam(gba)).to eq("PPPPPPPPPPPPPP")
    end

    it "With voorkeur V" do
      gba[GBA_FIELD_MAP[:aanduiding_naamgebruik]] = "V"

      expect(saluation_wrapper.naam_aanhef(gba)).to eq("PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP")
      expect(saluation_wrapper.naam(gba)).to eq("PPPPPPPPPPPPPPPPPPPPPPP")
    end

    it "With voorkeur P" do
      gba[GBA_FIELD_MAP[:aanduiding_naamgebruik]] = "P"

      expect(saluation_wrapper.naam_aanhef(gba)).to eq("PPPPPPPPPPPPPPPPPPPPPP")
      expect(saluation_wrapper.naam(gba)).to eq("PPPPPPPPPPPPPP")
    end

    it "With voorkeur N" do
      gba[GBA_FIELD_MAP[:aanduiding_naamgebruik]] = "N"

      expect(saluation_wrapper.naam_aanhef(gba)).to eq("PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP")
      expect(saluation_wrapper.naam(gba)).to eq("PPPPPPPPPPPPPPPPPPPPPPP")
    end
  end

  describe "With geslachtsaanduiding M" do
    before(:each) do
      gba[GBA_FIELD_MAP[:geslachtsaanduiding]] = "M"
    end

    it "With defaults" do
      expect(saluation_wrapper.naam_aanhef(gba)).to eq("PPPPPPPPPPPPPPPPPPPPP")
      expect(saluation_wrapper.naam(gba)).to eq("PPPPPPPPPPPPPPPPPPPPPP")
    end

    it "With adelijk JH" do
      gba[GBA_FIELD_MAP[:adellijke_titel]] = "JH"

      expect(saluation_wrapper.naam_aanhef(gba)).to eq("PPPPPPPPPPPPPPPPPPPPPPPPP")
      expect(saluation_wrapper.naam(gba)).to eq("PPPPPPPPPPPPPPPPPPPPPPP")
    end

    it "With adelijk B" do
      gba[GBA_FIELD_MAP[:adellijke_titel]] = "B"

      expect(saluation_wrapper.naam_aanhef(gba)).to eq("PPPPPPPPPPPPPPPPPPPPPPPPPPP")
      expect(saluation_wrapper.naam(gba)).to eq("PPPPPPPPPPPPPPPPPPPPPPPPPPPP")
    end

    it "With voorkeur V" do
      gba[GBA_FIELD_MAP[:aanduiding_naamgebruik]] = "V"

      expect(saluation_wrapper.naam_aanhef(gba)).to eq("PPPPPPPPPPPPPPPPPPPPPPPPPPPPPP")
      expect(saluation_wrapper.naam(gba)).to eq("PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP")
    end

    it "With voorkeur P" do
      gba[GBA_FIELD_MAP[:aanduiding_naamgebruik]] = "P"

      expect(saluation_wrapper.naam_aanhef(gba)).to eq("PPPPPPPPPPPPPPPPPPPPP")
      expect(saluation_wrapper.naam(gba)).to eq("PPPPPPPPPPPPPPPPPPPPPP")
    end

    it "With voorkeur N" do
      gba[GBA_FIELD_MAP[:aanduiding_naamgebruik]] = "N"

      expect(saluation_wrapper.naam_aanhef(gba)).to eq("PPPPPPPPPPPPPPPPPPPPPPPPPPPPPP")
      expect(saluation_wrapper.naam(gba)).to eq("PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP")
    end
  end

  describe "With invalid gba" do
    it "With nil geslachtsaanduiding" do
      gba[GBA_FIELD_MAP[:geslachtsaanduiding]] = nil

      expect(saluation_wrapper.naam(gba)).to eq("PPPPPPPPPPPPPP")
    end
  end
end
