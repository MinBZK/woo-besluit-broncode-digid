
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

describe DigidUtils::BSN do
  describe "valid?" do
    it "should return false for nil" do
      expect(subject.valid?(nil)).to eq(false)
    end

    it "should return false for empty string" do
      expect(subject.valid?("")).to eq(false)
    end

    it "should return true for valid bsn" do
      expect(subject.valid?("900095192")).to eq(true)
    end

    it "should return true for invalid bsn" do
      expect(subject.valid?("900095190")).to eq(false)
    end
  end

  describe "first_valid" do
    it "should return first valid bsn including start if not specified" do
      expect(subject.first_valid("900095271")).to eq("900095271")
    end

    it "should return first valid bsn including start if specified" do
      expect(subject.first_valid("912345640", false)).to eq("912345640")
    end

    it "should return first valid bsn excluding start if specified" do
      expect(subject.first_valid("912345639", true)).to eq("912345640")
    end
  end

  describe "generate" do
    it "should return count if block is given" do
      expect(subject.generate(1, 4) {}).to eq(4)
    end

    it "should return Enumerator if block is not given" do
      expect(subject.generate(1, 4)).to be_a(Enumerator)
    end

    it "should generate number of bsn in order" do
      expect(subject.generate("399999970", 3).to_a).to eq(%w[399999978 399999991 400000003])
    end
  end

  describe "generate_to" do
    it "should return nil if block is given" do
      expect(subject.generate_to(12, 48) {}).to eq(nil)
    end

    it "should return Enumerator if block is not given" do
      expect(subject.generate_to(12, 48)).to be_a(Enumerator)
    end

    it "should generate bsn from start to stop inclusive" do
      expect(subject.generate_to("399999970", "400000003").to_a).to eq(%w[399999978 399999991 400000003])
    end

    it "should generate bsn from start to stop, but not more" do
      expect(subject.generate_to("399999970", "400000004").to_a).to eq(%w[399999978 399999991 400000003])
    end

    it "should generate bsn from start to stop" do
      expect(subject.generate_to("502749994", "502750017").to_a).to eq(%w[502749994 502750005 502750017])
    end
  end
end
