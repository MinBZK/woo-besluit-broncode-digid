
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

describe DigidUtils::FlatHash do
  let(:nested_hash) do
    { "a" => { "b" => 1 } }
  end
  let(:other_flat_hash) do
    described_class.new({"a/d" => 3})
  end
  let(:other_flat_hash_with_hash) do
    described_class.new.tap { |h| h["a/b"] = { "c" => 1 } }
  end

  describe "new" do
    it "should initialize empty flat hash with no arguments" do
      expect(described_class.new.size).to eq(0)
    end

    it "should merge hash into flattened hash" do
      expect(described_class.new(nested_hash).to_a).to eq([["a/b", 1]])
    end

    it "should merge flatten hash into new flat hash" do
      expect(described_class.new(other_flat_hash_with_hash).to_a).to eq([["a/b", { "c" => 1 }]])
    end

    it "should set separator in hash" do
      expect(described_class.new(nested_hash, separator: ".").to_a).to eq([["a.b", 1]])
    end
  end

  describe "merge" do
    subject do
      described_class.new({"a/c" => 2})
    end

    it "should merge flatten hash into new flat hash" do
      expect(subject.merge(other_flat_hash).to_a).to eq([["a/c", 2], ["a/d", 3]])
      expect(subject.size).to eq(1)
    end

    it "should merge hash into new flat hash" do
      expect(subject.merge(nested_hash).to_a).to eq([["a/c", 2], ["a/b", 1]])
      expect(subject.size).to eq(1)
    end
  end

  describe "merge!" do
    subject do
      described_class.new({"a/c" => 2})
    end

    it "should merge flatten hash into new flat hash" do
      expect(subject.merge!(other_flat_hash).to_a).to eq([["a/c", 2], ["a/d", 3]])
      expect(subject.size).to eq(2)
    end

    it "should merge hash into new flat hash" do
      expect(subject.merge!(nested_hash).to_a).to eq([["a/c", 2], ["a/b", 1]])
      expect(subject.size).to eq(2)
    end
  end

  describe "to_h" do
    it "should unflatten hash" do
      nested = described_class.new({"a/b" => 1, "b/c/d" => 2}).to_h
      expect(nested.class).to be(Hash)
      expect(nested).to eq("a" => { "b" => 1 }, "b" => { "c" => { "d" => 2 } })
    end

    it "should raise exception if hash cannot be flattened" do
      expect { described_class.new({"a/b" => 1, "a/b/c" => 2}).to_h }.to raise_error(
        ArgumentError, "a/b collides on b with 1"
      )
    end
  end
end
