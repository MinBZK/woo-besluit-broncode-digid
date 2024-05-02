
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

describe DigidUtils::Crypto::Password do
  describe "create" do
    it "should generate salt and password of length 64" do
      password, salt = subject.create("welkom123")
      expect(salt.length).to be(64)
      expect(password.length).to be(64)
    end

    it "should generate salt and password that verify" do
      expect(subject.verify("welkom123", *subject.create("welkom123")))
    end
  end

  describe "verify" do
    it "should verify empty password to false" do
      expect(subject.verify("", "hash", "salt")).to eq(false)
    end

    it "should verify empty hash to false" do
      expect(subject.verify("password", "", "salt")).to eq(false)
    end

    it "should verify old style SHA1 hash without salt" do
      expect(subject.verify("welkom123", "f6c7baa98b19f4914ff842408a11cb2c8a83d3d4", nil))
    end

    it "should verify old style SHA1 hash with short salt" do
      expect(subject.verify("welkom", "f6c7baa98b19f4914ff842408a11cb2c8a83d3d4", "123"))
    end

    it "should verify new style PKCS5 pbkdf2_hmac with SHA256 when salt is larger or equal to 32" do
      expect(subject.verify("welkom123", "eeaf60e9faa340a31f940c7d75a33e4ea57350e1f9f92c6aace24749782c8874",
                            "0123456789abcdef0123456789abcdef"))
    end
  end
end
