
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

class RedisSessionBase
  include ActiveModel::Model

  DEFAULT_NAMESPACE = "digid_x"

  def self.create(session_id: SecureRandom.uuid, **values)
    session = self.new(session_id: session_id, values: values)
    session.save!
    session.reset_session_timeout!
    session
  end

  def self.find(session_id:)
    session_key = self::NAMESPACE + session_id
    values = RedisSessionBase::redis.hgetall(session_key)
    self.new(session_id: session_id, values: values)
  end

  def save!
    RedisSessionBase::redis.del(@_session_key) # remove old data from session
    RedisSessionBase::redis.mapped_hmset(@_session_key, self.instance_values.slice!('_session_id', '_session_key').stringify_keys)
  end

  def reset_session_timeout!(ttl: self.class::TIMEOUT)
    expire(ttl)
  end

  def session_id
    @_session_id
  end

  def update_values(**values)
    assign_attributes values
    save!
    reset_session_timeout!
  end

  private

  def initialize(session_id: nil, values: nil)
    raise "session id is nil" if session_id.nil?
    @_session_id = session_id
    @_session_key = self.class::NAMESPACE + session_id
    assign_attributes values
  end

  def self.redis
    Thread.current[:redis] ||= Redis.new(APP_CONFIG["dot_environment"] ? DigidUtils.redis_options.merge(logger: Rails.logger) : DigidUtils.redis_options)
  end

  def expire(ttl)
    RedisSessionBase::redis.expire(@_session_key, ttl)
  end

end
