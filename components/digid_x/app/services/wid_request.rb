
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

class WidRequest
  include RedisClient
  extend RedisClient

  NAMESPACE =  DEFAULT_NAMESPACE + ":wid_request:"
  TIMEOUT = 15.minutes

  def self.create(account: nil, session_id: SecureRandom.uuid)
    session = self.new(account: account, session_id: session_id)
    session.save
    session
  end

  def self.find(session_id)
    session_key = NAMESPACE + session_id
    keys = redis.hgetall(session_key)
    self.new(session_id: session_id, keys: keys)
  end

  def initialize(account: nil, session_id: SecureRandom.uuid, keys: nil)
    @session_id = session_id
    @session_key = NAMESPACE + session_id
    if keys.nil?
      @account_id = account.id
      @crb_request_id = get_driving_licenses(bsn: account.bsn, account_id: @account_id)
      @brp_request_id = get_travel_documents(account.bsn, account.id)
    else
      @crb_request_id = keys["crb_request_id"]
      @brp_request_id = keys["brp_request_id"]
    end
  end

  def account_id
    @account_id
  end

  def id
    @session_id
  end

  def valid?
    crb_response.valid? && brp_response.valid?
  end

  def not_found_in_brp?
    brp_response.not_found?
  end

  def person_deceased_in_brp?
    brp_response.status == "deceased"
  end

  def ready?
    crb_response.present? && brp_response.present?
  end

  def completed_with_errors?
    crb_response.error? || brp_response.error?
  end

  def save
    redis.mapped_hmset(@session_key, {crb_request_id: @crb_request_id, brp_request_id: @brp_request_id})
    reset_session_timeout!
  end

  def reset_session_timeout!(ttl: TIMEOUT)
    expire(ttl)
  end

  def expire(ttl)
    redis.expire(@session_key, ttl)
  end

  def documents
    travel_documents + driving_licences
  end

  def travel_documents
    response = brp_response
    valid_documents = response.valid_documents("passport") + response.valid_documents("id_card")
    rda_documents = []
    for document in valid_documents
      next if response.birthday.empty?

      rda_document = {
        "documentNumber": document.id_number,
        "dateOfBirth": response.birthday.slice(2,8).gsub("-", ""),
        "dateOfExpiry": document.valid_until.strftime("%y%m%d")
      }
      rda_documents << rda_document
    end
    rda_documents
  end

  def driving_licences
    driving_licenses = crb_response.machine_readable_zones
  end

  def brp_response
    Brp::Response.find(@brp_request_id)
  end

  def crb_response
    Crb::Response.find(@crb_request_id)
  end

  private

  def get_driving_licenses(bsn:, account_id:)
    Log.instrument("871", account_id: account_id, hidden: true)
    @crb_request_id = SecureRandom.uuid
    return @crb_request_id if CrbJob.perform_async(@crb_request_id, bsn, account_id)
  end

  def get_travel_documents(bsn, account_id)
    Log.instrument("155", account_id: account_id, hidden: true)
    @brp_request_id = SecureRandom.uuid
    return @brp_request_id if BrpRequestJob.schedule(@brp_request_id, bsn)
    Log.instrument("158", account_id: account_id, hidden: true)
    false
  end
end
