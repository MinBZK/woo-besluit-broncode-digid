
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

RSpec.shared_examples "ping" do |url|
  before do
    $redis.del("load-balancer-off:#{DigidUtils.rails_project}")
  end

  after(:all) do
    $redis.del("load-balancer-off:#{DigidUtils.rails_project}")
  end

  it "responds to a ping with load_balancer true" do
    get url
    expect(response.status).to eq(200)
    expect(JSON.parse(response.body)).to eq("load_balancer" => true)
  end

  it "responds to a ping with load_balancer false when Redis key set" do
    $redis.hset("load-balancer-off:#{DigidUtils.rails_project}", DigidUtils.hostname, Time.zone.now.round)
    get url
    expect(response.status).to eq(200)
    expect(JSON.parse(response.body)).to eq(
     "load_balancer" => false
    )
  end

  it "responds to a ping with load_balancer true with error if error reading Redis" do
    allow_any_instance_of(DigidUtils::HealthCheck::Middleware).to(
      receive_message_chain(:load_balancer?).and_raise("Fault!")
    )
    get url
    expect(response.status).to eq(200)
    expect(JSON.parse(response.body)).to eq(
      "load_balancer" => true, "load_balancer_message" => "Fault!"
    )
  end

  it "gives an error if the active record connection fails" do
    allow(ActiveRecord::Base.connection).to receive_message_chain(:query).and_raise("Fault!")
    get url
    expect(response.body).to eq({load_balancer: true}.to_json)
  end
end

describe "/ping" do
  include_examples "ping", "/ping"
end

describe "/core/loadbalancer/ping" do
  include_examples "ping", "/core/loadbalancer/ping"
end
