
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

class EhSession
  include RedisClient
  extend RedisClient

  NAMESPACE = DEFAULT_NAMESPACE + ":eh:session:"
  TIMEOUT = (APP_CONFIG.dig("eh_session", "ttl-minutes") || 15) * 60

  def self.create(session_id: SecureRandom.uuid, **values)
    session = new(session_id: session_id, values: values.stringify_keys)
    session.save!
    session.reset_session_timeout!
    session
  end

  def self.find(session_id)
    session_key = NAMESPACE + session_id
    values = redis.hgetall(session_key)
    new(session_id: session_id, values: values)
  end

  def save!
    redis.mapped_hmset(@session_key, @values)
  end

  def initialize(session_id: nil, values: nil)
    raise "session id is nil" if session_id.nil?

    @session_id = session_id
    @session_key = NAMESPACE + session_id
    @values = values
  end

  def id
    @session_id
  end

  def reset_session_timeout!(ttl: TIMEOUT)
    expire(ttl)
  end

  def expire(ttl)
    redis.expire(@session_key, ttl)
  end

  def reload
    @values = redis.hgetall(@session_key)
    @values
  end

  def account_id=(account_id)
    @values["account_id"] = account_id
    set("account_id", account_id)
  end

  def account_id
    get("account_id")
  end

  def account
    account_id = get("account_id")
    return Account.find(account_id) if account_id
  end

  def ttl
    redis.ttl(@session_key)
  end

  def active?
    get("status") == "active"
  end

  private

  def set(key, value)
    @values[key] = value
    redis.hset(@session_key, key, value)
  end

  def get(key)
    redis.hget(@session_key, key)
  end
end
