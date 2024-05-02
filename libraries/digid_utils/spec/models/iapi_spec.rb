
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

describe DigidUtils::Iapi do
  subject do
    described_class::Client.new(url: "http://localhost:81/iapi/", timeout: 1)
  end

  let(:logger_io) do
    StringIO.new
  end

  before do
    @original_logger = DigidUtils.logger
    DigidUtils.logger = Logger.new(logger_io)
  end

  after do
    DigidUtils.logger = @original_logger
  end

  it "should add authorization token to request" do
    DigidUtils::Iapi.token = "test"
    stub = stub_request(:get, "localhost:81/iapi/test").with(headers: { "X-Auth-Token" => "test" })
    subject.get("test")
    expect(stub).to have_been_requested
  end

  it "should decode json if content type is json" do
    stub_request(:get, "localhost:81/iapi/test")
      .to_return(headers: { "Content-Type" => "application/json; charset=utf-8" }, body: '{"abc":"def"}')
    expect(subject.get("test").result).to eq("abc" => "def")
  end

  it "should decode json with symbolized keys if requested" do
    stub_request(:get, "localhost:81/iapi/test")
      .to_return(headers: { "Content-Type" => "text/json; charset=utf-8" }, body: '{"abc":"def"}')
    expect(subject.get("test").result).to eq("abc" => "def")
  end

  it "should not decode json if header is missing" do
    subject = described_class::Client.new(url: "http://localhost:81/iapi/", timeout: 1, symbolize_keys: true)
    stub_request(:get, "localhost:81/iapi/test")
      .to_return(body: '{"abc":"def"}')
    expect(subject.get("test").result).to eq(nil)
  end

  it "should not log if request success" do
    stub_request(:get, "localhost:81/iapi/test")
    subject.get("test")
    expect(logger_io.string).to eq("")
  end

  it "should log on info if request success and verbose" do
    subject = described_class::Client.new(url: "http://localhost:81/iapi/", timeout: 1, verbose: true)
    stub_request(:get, "localhost:81/iapi/test")
    subject.get("test")
    expect(logger_io.string).to include("INFO -- : Request GET on http://localhost:81/iapi/test")
    expect(logger_io.string).to include("INFO -- :  base url: http://localhost:81/iapi/")
    expect(logger_io.string).to include("INFO -- : Response status 200")
  end

  it "should log on error status code" do
    stub_request(:post, "localhost:81/iapi/test")
      .to_return(status: 500, headers: { "Content-Type" => "application/json" }, body: '{"abc": "def"}')

    expect do
      subject.post("test", header: { "Other" => "Key" }, body: { "abc" => "def" })
    end.to raise_error(DigidUtils::Iapi::StatusError)

    expect(logger_io.string).to include("ERROR -- : Request POST on http://localhost:81/iapi/test")
    expect(logger_io.string).to include("ERROR -- :  base url: http://localhost:81/iapi/")
    expect(logger_io.string).to include(
      'ERROR -- :  header: {"Other"=>"Key", "X-Request-Token"=>nil, "Content-Type"=>"application/json; charset=utf-8"}'
    )
    expect(logger_io.string).to include('ERROR -- :  body: {"abc":"def"}')
    expect(logger_io.string).to include("ERROR -- : Response status 500")
    expect(logger_io.string).to include("ERROR -- :  Content-Type: application/json")
    expect(logger_io.string).to include('ERROR -- : {"abc": "def"}')
    expect(logger_io.string).to include("ERROR -- : Unexpected status 500 when doing POST on http://localhost:81/iapi/test")
  end

  it "should log on timeout" do
    stub_request(:get, "localhost:81/iapi/test?q=search").to_timeout

    expect do
      subject.get("test", q: "search")
    end.to raise_error(DigidUtils::Iapi::TimeoutError)

    expect(logger_io.string).to include("ERROR -- : Request GET on http://localhost:81/iapi/test")
    expect(logger_io.string).to include("ERROR -- :  base url: http://localhost:81/iapi/")
    expect(logger_io.string).to include('ERROR -- :  query: {:q=>"search"}')
    expect(logger_io.string).to include("ERROR -- : Timeout when doing GET on http://localhost:81/iapi/test")
    expect(logger_io.string).to include("ERROR -- : HTTPClient::TimeoutError (HTTPClient::TimeoutError)")
  end

  it "should parse response and not log on additional ok code" do
    subject = described_class::Client.new(url: "http://localhost:81/iapi/", timeout: 1, ok_codes: [404])
    stub_request(:post, "localhost:81/iapi/test")
      .to_return(status: 404, headers: { "Content-Type" => "application/json" }, body: '{"abc": "def"}')

    response = subject.post("test")
    expect(response.status).to eq(404)
    expect(response.result).to eq("abc" => "def")
  end

  it "should log on json decode error" do
    stub_request(:post, "localhost:81/iapi/test")
      .to_return(headers: { "Content-Type" => "application/json" }, body: '{"abc" => "def"}')

    expect do
      subject.post("test", body: { "abc" => "def" })
    end.to raise_error(DigidUtils::Iapi::ParseError)

    expect(logger_io.string).to include("ERROR -- : Request POST on http://localhost:81/iapi/test")
    expect(logger_io.string).to include("ERROR -- :  base url: http://localhost:81/iapi/")
    expect(logger_io.string).to include('ERROR -- :  body: {"abc":"def"}')
    expect(logger_io.string).to include("ERROR -- : Response status 200")
    expect(logger_io.string).to include("ERROR -- :  Content-Type: application/json")
    expect(logger_io.string).to include('ERROR -- : {"abc" => "def"}')
    expect(logger_io.string).to include("ERROR -- : Could not parse response when doing POST on http://localhost:81/iapi/test")
    expect(logger_io.string).to include('unexpected token at \'{"abc" => "def"}\' (JSON::ParserError)')
  end
end
