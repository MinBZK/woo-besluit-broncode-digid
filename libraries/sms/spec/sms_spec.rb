
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

require File.expand_path(File.dirname(__FILE__) + "/spec_helper")

require "sms"

describe "Sms" do
  before(:each) do
    Sms.configure do |c|
      c.spoken_token = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
      c.regular_token = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
      c.conversion_token = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
      c.from_number = "PPPPPPPPPPPP"
      c.anonymous = Sms.configuration.anonymous
    end
  end

  describe ".conversion" do
    let(:gateway) { "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" }
    let(:options) do
      {
        gateway: gateway,
        phone_number: "PPPPPPPPPPPP",
        reference: "ref-1"
      }
    end

    let!(:stub_regular_request) do
      stub_request(:post, gateway)
    end

    context "input validation" do
      it "raises error if phone_number is missing" do
        expect { Sms.conversion options.merge(phone_number: nil) }.to raise_error(Sms::Gateway::ValidationError)
      end

      it "raises error if gateway is missing" do
        expect { Sms.conversion options.merge(gateway: nil) }.to raise_error(Sms::Gateway::ValidationError)
      end

      it "raises error if reference is missing" do
        expect { Sms.conversion options.merge(reference: nil) }.to raise_error(Sms::Gateway::ValidationError)
      end

      it "raises no error if nothing is missing" do
        expect { Sms.conversion(options) }.not_to raise_error
      end
    end
  end

  describe ".deliver" do
    let(:gateway) { "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" }
    let(:options) do
      {
        sender: "DigiD",
        gateway: gateway,
        message: "Dit is een test",
        phone_number: "PPPPPPPPPPPP",
        reference: "ref-1"
      }
    end

    let!(:stub_regular_request) do
      stub_request(:post, gateway).
        with(
          body: "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS",
          headers: { "Content-Type"=>"application/json; charset=utf-8" }
        )
    end

    context "input errors" do
      it "raises error if sender is missing" do
        expect { Sms.deliver options.merge(sender: nil) }.to raise_error(Sms::Gateway::ValidationError)
      end

      it "raises error if gateway is missing" do
        expect { Sms.deliver options.merge(gateway: nil) }.to raise_error(Sms::Gateway::ValidationError)
      end

      it "raises error if phone_number is missing" do
        expect { Sms.deliver options.merge(phone_number: nil) }.to raise_error(Sms::Gateway::ValidationError)
      end

      it "raises error if message is missing" do
        expect { Sms.deliver options.merge(message: nil) }.to raise_error(Sms::Gateway::ValidationError)
      end

      it "raises no error if nothing is missing" do
        expect { Sms.deliver(options) }.not_to raise_error
      end
    end

    context "regular SMS" do
      context "network errors" do
        it "returns false if timeout occurs" do
          stub_regular_request.to_timeout
          expect(Sms.deliver(options)).to eq(false)
        end

        it "returns false if server closes connection" do
          stub_regular_request.to_raise(EOFError.new)
          expect(Sms.deliver(options)).to eq(false)
        end

        it "returns false if dns lookup fails" do
          stub_regular_request.to_raise(SocketError.new)
          expect(Sms.deliver(options)).to eq(false)
        end
      end

      it "returns false if gateway responds with error body" do
        stub_regular_request.to_return(status: 200, body: "Error: ERROR")
        expect(Sms.deliver(options)).to eq(false)
      end

      it "returns false if gateway responds with error status" do
        stub_regular_request.to_return(status: 400)
        expect(Sms.deliver(options)).to eq(false)
      end

      it "returns true if response is 000" do
        stub_regular_request.to_return(status: 200, body: "000")
        expect(Sms.deliver(options)).to eq(true)
      end
    end

    context "spoken SMS" do
      let(:request_headers) do
         {
          "Content-Type"=>"application/json; charset=utf-8",
          "X-Cm-Producttoken"=>"SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
        }
      end

      let(:request_body) do
        {
          "callee"=>"PPPPPPPPPPPPP",
          "caller"=>"PPPPPPPPPPPP",
          "intro-prompt"=>"/prompts/intro.wav",
          "intro-prompt-type"=>"File",
          "code-prompt"=>"/prompts/code.wav",
          "code-prompt-type"=>"File",
          "code"=>"Dit is een test",
          "max-replays"=>3,
          "replay-prompt"=>"/prompts/replay.wav",
          "replay-prompt-type"=>"File",
          "anonymous"=>Sms.configuration.anonymous,
          "voice"=>{"language"=>"nl-NL"}
         }
      end

      let(:spoken_options) { options.merge! spoken: true }

      it "returns false if gateway responds with error status" do
        stub_request(:post, gateway).with( body: request_body.to_json, headers: request_headers ).to_return(body: { success: false }.to_json)

        expect(Sms.deliver(spoken_options)).to eq(false)
      end

      it "returns true if gateway responds with ok status and result code is 0" do
        stub_request(:post, gateway).with( body: request_body.to_json, headers: request_headers ).to_return(body: { success:  true }.to_json)

        expect(Sms.deliver(spoken_options)).to eq(true)
      end

      it "uses the correct language" do
        request_body["voice"]["language"] = "en-EN"
        stub_request(:post, gateway).with( body: request_body.to_json, headers: request_headers ).to_return(body: { success: true }.to_json)

        expect(Sms.deliver(spoken_options.merge(locale: "en"))).to eq(true)
      end
    end
  end
end

describe Sms::Message::Spoken do
  describe "#body" do
    let(:message_nl) { described_class.new(phone_number: "PPPPPPPPPPPPP", message: "COOL123", locale: "nl") }
    let(:message_en) { described_class.new(phone_number: "PPPPPPPPPPPPP", message: "COOL123", locale: "en") }

    it "returns the correct json body with nl voice" do
      expect(message_nl.body).to eq({
        "anonymous" => Sms.configuration.anonymous,
        "callee" => "PPPPPPPPPPPPP",
        "caller" => "PPPPPPPPPPPP",
        "code" => "COOL123",
        "code-prompt" => "/prompts/code.wav",
        "code-prompt-type" => "File",
        "intro-prompt" => "/prompts/intro.wav",
        "intro-prompt-type" => "File",
        "max-replays" => 3,
        "replay-prompt" => "/prompts/replay.wav",
        "replay-prompt-type" => "File",
        "voice" => {"language" => "nl-NL"}
      })
    end

    it "returns the correct json body with en voice" do
      expect(message_en.body).to eq({
        "anonymous" => Sms.configuration.anonymous,
        "callee" => "PPPPPPPPPPPPP",
        "caller" => "PPPPPPPPPPPP",
        "code" => "COOL123",
        "code-prompt" => "/prompts/code.wav",
        "code-prompt-type" => "File",
        "intro-prompt" => "/prompts/intro.wav",
        "intro-prompt-type" => "File",
        "max-replays" => 3,
        "replay-prompt" => "/prompts/replay.wav",
        "replay-prompt-type" => "File",
        "voice" => {"language" => "en-EN"}
      })
    end
  end
end

describe Sms::Message::Regular do
  describe "#body" do
    let(:message) { described_class.new(phone_number: "PPPPPPPPPPPPP", message: "COOL123", sender: "DigiD", reference: "REF-X") }

    it "returns json body" do
      expect(message.body).to eq({
        "messages" => {
          "authentication"=>{ "producttoken"=>"SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"},
          "msg"=>[{
            "body"=>{"content"=>"COOL123"},
            "from"=> "DigiD",
            "reference"=> "REF-X",
            "to"=>[{"number"=>"PPPPPPPPPPPPP"}]
          }]
        }
      })
    end
  end
end

describe Sms::Message::Conversion do
  describe "#body" do
    let(:message) { described_class.new(phone_number: "PPPPPPPPPPPPP", message: "COOL123", sender: "DigiD", reference: "REF-X") }

    it "returns json body" do
      expect(message.body).to eq({
        "Msisdn" => "PPPPPPPPPPPPP",
        "ProductToken" => "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS",
        "Reference" => "REF-X"})
    end
  end
end


