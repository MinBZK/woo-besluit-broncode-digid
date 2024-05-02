
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

describe "/iapi/lb" do
  before do
    $redis.del("load-balancer-off:#{DigidUtils.rails_project}")
  end

  after(:all) do
    $redis.del("load-balancer-off:#{DigidUtils.rails_project}")
  end

  describe "/iapi/lb/off" do
    it "should set redis load balancer key of hostname" do
      post "/iapi/lb/off"
      expect(response.status).to eq(200)
      date = $redis.hget("load-balancer-off:#{DigidUtils.rails_project}", DigidUtils.hostname)
      expect(date).to_not eq(nil)
      expect(Time.zone.parse(date)).to be_within(1.second).of(Time.zone.now)
    end
  end

  describe "/iapi/lb/off" do
    it "should delete redis load balancer key of hostname" do
      $redis.hset("load-balancer-off:#{DigidUtils.rails_project}", DigidUtils.hostname, Time.zone.now.round)
      post "/iapi/lb/on"
      expect(response.status).to eq(200)
      expect($redis.hget("load-balancer-off:#{DigidUtils.rails_project}", DigidUtils.hostname)).to eq(nil)
    end
  end
end
