
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

describe DigidUtils::Hsm do
  subject do
    described_class.new(url: "http://localhost:82", timeout: 1)
  end

  describe "polymorph_to_encrypted_identity" do
    let!(:stub) do
      stub_request(:post, "http://localhost:82/iapi/bsnk/transform/single")
        .with(body: { polymorph: "PIP", oin: "digid-x", ksv: "20180123", identity: true, pseudonym: false, targetMsgVersion: 1 })
        .to_return(headers: { "Content-Type" => "application/json" },
                   body: { identity: "IDENTITY", pseudonym: nil }.to_json)
    end

    it "should return encrypted identity" do
      result = subject.polymorph_to_encrypted_identity(polymorph: "PIP", oin: "digid-x", ksv: "20180123")
      expect(result).to eq("IDENTITY")
    end
  end

  describe "polymorph_to_encrypted_pseudonym" do
    let!(:stub) do
      stub_request(:post, "http://localhost:82/iapi/bsnk/transform/single")
        .with(body: { polymorph: "PIP", oin: "digid-x", ksv: "20180123", identity: false, pseudonym: true, targetMsgVersion: 1 })
        .to_return(headers: { "Content-Type" => "application/json" },
                   body: { identity: nil, pseudonym: "PSEUDONYM" }.to_json)
    end

    it "should return encrypted pseudonym" do
      result = subject.polymorph_to_encrypted_pseudonym(polymorph: "PIP", oin: "digid-x", ksv: "20180123")
      expect(result).to eq("PSEUDONYM")
    end
  end

  describe "transform_single" do
    let!(:stub) do
      stub_request(:post, "http://localhost:82/iapi/bsnk/transform/single")
        .with(body: { polymorph: "PIP", oin: "digid-x", ksv: "20180123", identity: true, pseudonym: true, targetMsgVersion: 1  })
        .to_return(headers: { "Content-Type" => "application/json" },
                   body: { identity: "IDENTITY", pseudonym: "PSEUDONYM" }.to_json)
    end

    it "should return encrypted identity and pseudonym" do
      result = subject.transform_single(polymorph: "PIP", oin: "digid-x", ksv: "20180123")
      expect(result).to eq("identity" => "IDENTITY", "pseudonym" => "PSEUDONYM")
    end
  end

  describe "transform_multiple" do
    let!(:stub) do
      stub_request(:post, "http://localhost:82/iapi/bsnk/transform/multiple")
        .with(body: { polymorph: "PIP", requests: {
          "digid-x" => { ksv: 1, identity: true, polymorph: false },
          "digid-msc" => { ksv: 2 }
        }, "targetMsgVersion" => 1 })
        .to_return(headers: { "Content-Type" => "application/json" },
                   body: {
                     "digid-x" => { identity: "IDENTITY", pseudonym: nil },
                     "digid-msc" => { identity: nil, pseudonym: "PSEUDONYM" }
                   }.to_json)
    end

    it "should return encrypted identities and pseudonyms" do
      result = subject.transform_multiple(polymorph: "PIP", requests: {
        "digid-x" => { ksv: 1, identity: true, polymorph: false },
        "digid-msc" => { ksv: 2 }
      })
      expect(result).to eq(
        "digid-x" => { "identity" => "IDENTITY", "pseudonym" => nil },
        "digid-msc" => { "identity" => nil, "pseudonym" => "PSEUDONYM" }
      )
    end
  end

  describe "activate" do
    let!(:stub) do
      stub_request(:post, "http://localhost:82/iapi/bsnk/activate")
        .with(body: { identifier: "11111111", type: "PP", signed: true, status_provider_oin: nil, status_provider_ksv: nil, activator: nil, authorized_party: nil })
        .to_return(headers: { "Content-Type" => "application/json" }, body: { polymorph: "SIGNED-PP" }.to_json)
    end

    it "should return PI/PP/PIP" do
      expect(subject.activate(bsn: "11111111", type: "PP", signed: true)).to eq("SIGNED-PP")
    end
  end

  describe "decrypt_keys" do
    let!(:stub) do
      stub_request(:post, "http://localhost:82/iapi/bsnk/service-provider-keys")
        .with(body: { certificate: "CERT", closing_key_version: 1, identity: true, pseudonym: true })
        .to_return(headers: { "Content-Type" => "application/json" }, body: { ID: "ID", PD: "PD", PC: "PC" }.to_json)
    end

    it "should return requested decrypt keys" do
      result = subject.decrypt_keys(certificate: "CERT")
      expect(result).to eq("ID" => "ID", "PD" => "PD", "PC" => "PC")
    end
  end
end
