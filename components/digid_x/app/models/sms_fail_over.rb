
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

# sms failover
#
# time_block(int) | regular_failures(int) | spoken_failures(int) |
# regular_active_time_block(int) | spoken_active_time_block(int) |

# NOTICE: This klass is going to be properly replaced in the redis branch

class SmsFailOver < ActiveRecord::Base
  MUTEX = Mutex.new

  class << self
    include RedisClient
  end

  def self.time_range
    ::Configuration.get_int("sms_failover_time_range") || 300
  end

  def self.quarantine
    ::Configuration.get_int("sms_failover_quarantine") || 3600
  end

  def self.threshold
    ::Configuration.get_int("sms_failover_treshold") || 6
  end

  def self.time_block
    Time.zone.now.to_i / time_range
  end

  def self.time_block_quarantine
    Time.zone.now.to_i / quarantine
  end

  # registers failed sms in MySQL
  #
  def self.register_failed_sms(sms_type, gateway)
    MUTEX.synchronize do
      @sms_type      = sms_type
      @gateway       = gateway
      @failover_data = failover_data(gateway)
      activate_failover? ? activate : update_failover
    end
  end

  # check if failover is active at this moment
  #
  def self.active?(sms_type, gateway)
    MUTEX.synchronize do
      @failover_data = failover_data(gateway)
      if sms_type == "regular"
        return true unless redis.get("regular_active_time_block_#{gateway}").nil? # first check local regular (Redis) datastore
        return same_regular_active_time_block? # check central regular (MySQL) datastore
      elsif sms_type == "spoken"
        return true unless redis.get("spoken_active_time_block_#{gateway}").nil? # first check Redis spoken datastore
        return same_spoken_active_time_block? # check central spoken (MySQL) datastore
      end
    end
  end

  # get sms failover data
  #
  def self.failover_data(gateway)
    SmsFailOver.where(gateway: gateway).first_or_create
  end

  # activate failover if counter reached treshold
  #
  # time_block is time in seconds since 1-1-1970 / time range
  #
  # returns boolean
  #
  def self.activate_failover?
    if @sms_type == "regular"
      same_time_block? && (@failover_data.regular_failures + 1) > threshold ? true : false
    elsif @sms_type == "spoken"
      same_time_block? && (@failover_data.spoken_failures + 1) > threshold ? true : false
    end
  end

  # checks if time_blocks are the same
  #
  # returns boolean
  #
  def self.same_time_block?
    @failover_data.time_block == Time.zone.now.to_i / time_range
  end

  # checks if regular active time_block exists and is the same as this moment
  #
  # returns boolean
  #
  def self.same_regular_active_time_block?
    if !@failover_data.regular_active_time_block.nil? && @failover_data.regular_active_time_block == time_block_quarantine
      true
    else
      unless @failover_data.regular_active_time_block.nil?
        @failover_data.regular_active_time_block = nil
        @failover_data.save!
        Rails.logger.warn "SmsFailOver deactivated for regular"
      end
      false
    end
  end

  # checks if spoken active time_block exists and is the same as this moment
  #
  # returns boolean
  #
  def self.same_spoken_active_time_block?
    if !@failover_data.spoken_active_time_block.nil? && @failover_data.spoken_active_time_block == time_block_quarantine
      true
    else
      unless @failover_data.spoken_active_time_block.nil?
        @failover_data.spoken_active_time_block = nil
        @failover_data.save!
        Rails.logger.warn "SmsFailOver deactivated for spoken"
      end
      false
    end
  end

  # activates failover (updated MySQL (central) and Redis (local))
  #
  def self.activate
    set_central_failover_active
    set_local_failover_active
    Rails.logger.warn "SmsFailOver activated for #{@sms_type}"
  end

  # set MySQL (central) to active
  #
  def self.set_central_failover_active
    @failover_data.time_block                = nil
    @failover_data.regular_failures          = nil if @sms_type == "regular"
    @failover_data.spoken_failures           = nil if @sms_type == "spoken"
    @failover_data.regular_active_time_block = time_block_quarantine if @sms_type == "regular"
    @failover_data.spoken_active_time_block  = time_block_quarantine if @sms_type == "spoken"
    @failover_data.gateway                   = @gateway
    @failover_data.save!
  end

  # set Redis (local) to active
  #
  def self.set_local_failover_active
    if @sms_type == "regular"
      redis.setex("regular_active_time_block_#{@gateway}", quarantine, "active")
    elsif @sms_type == "spoken"
      redis.setex("spoken_active_time_block_#{@gateway}", quarantine, "active")
    end
  end

  # update failover counter
  #
  def self.update_failover
    if @sms_type == "regular" && same_time_block?
      @failover_data.regular_failures = @failover_data.regular_failures + 1
    elsif @sms_type == "regular" && !same_time_block?
      @failover_data.time_block       = Time.zone.now.to_i / time_range
      @failover_data.regular_failures = 1
    elsif @sms_type == "spoken" && same_time_block?
      @failover_data.spoken_failures = @failover_data.spoken_failures + 1
    elsif @sms_type == "spoken" && !same_time_block?
      @failover_data.time_block      = Time.zone.now.to_i / time_range
      @failover_data.spoken_failures = 1
    end
    @failover_data.save!
  end
end
