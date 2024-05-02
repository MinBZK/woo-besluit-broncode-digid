
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

# Non persistent helper model.
# Used in ReportAuthWebDisp* to aggregate the ip addresses.
class AuthWebDisp
  #~ include Comparable

  attr_accessor :dispensary
  attr_accessor :dispensary_code
  attr_accessor :webservice_id
  attr_accessor :webservice_name
  attr_accessor :attempts
  attr_accessor :authentications
  attr_accessor :sms
  attr_accessor :ip_address_netherlands
  attr_accessor :ip_address_foreign
  attr_accessor :total_attempts
  attr_accessor :total_authentications
  attr_accessor :total_sms
  attr_accessor :total_ip_address_netherlands
  attr_accessor :total_ip_address_foreign

  class Types
    FAILURE  = "failure"
    AUTHENTICATED = "authenticated"
    SMS = "sms"
  end

  def initialize
    @class_name = 'AuthWebDisp'
    @logger = Rails.logger
    @dispensary = ""
    @dispensary_code = 0
    @webservice_id = 0
    @webservice_name = ""
    @attempts = 0
    @authentications = 0
    @sms = 0
    @ip_address_netherlands = 0
    @ip_address_foreign = 0
    @total_attempts = 0
    @total_authentications = 0
    @total_sms = 0
    @total_ip_address_netherlands = 0
    @total_ip_address_foreign = 0
    @logger.debug "DEBUG AuthWebDisp.#{__method__} -> initialized AuthWebDisp instance."
  end

  #~ def <=>(other)
    #~ result = nil
    #~ me = "#{@class_name}.#{__method__}"
    #~ @logger.debug "DEBUG #{me} -> Compare to #{other.to_s}."
    #~ if other.dispensary < @dispensary
      #~ result = 1
    #~ elsif other.dispensary > @dispensary
      #~ result = -1
    #~ elsif other.dispensary == @dispensary
    #~ elsif other.webservice_id < @webservice_id
      #~ result = 1
    #~ elsif other.webservice_id > @webservice_id
      #~ result = -1
    #~ elsif other.webservice_id == @webservice_id
      #~ result = 0
    #~ end
    #~ return result
  #~ end

  # Increment one of the type counters.
  # QQQ Use constant or enum to check input parameter
  def increment(type, is_netherlands)
    me = "#{@class_name}.#{__method__}"
    @logger.debug "DEBUG #{me} -> Increment periodical counter of type #{type}, is_netherlands is #{is_netherlands} ."
    begin
      case type
        when :attempt then
          #@attemps += 1
          @logger.error "ERROR #{me} -> :attempts is an invalid type."
        when :failure then
          @attempts += 1
          @logger.debug "DEBUG #{me} -> incremented counter of type attempts to #{@attempts}"
        when :authenticated then
          @authentications += 1
          @attempts += 1
          @logger.debug "DEBUG #{me} -> incremented counters of type authentications to #{@authentications} and attempts to#{@attempts}"
        when :sms then
          @sms += 1
          @logger.debug "DEBUG #{me} -> incremented counter of type sms to #{@sms}"
        else @logger.error "ERROR -> #{me}. ===> Unexpected else in case type block."
      end
      is_netherlands ? @ip_address_netherlands = 1 : @ip_address_foreign = 1
    rescue Exception => e
      @logger.error "ERROR AuthWebDisp.#{me}  ===> Increment for #{type.inspect} caused #{e.message}"
    end
  end

   # Increment one of the type counters.
  # QQQ Use constant or enum to check input parameter
  def increment_totals(type, is_netherlands)
    me = "#{@class_name}.#{__method__}"
    @logger.debug "DEBUG #{me} ->  of type #{type} is_netherlands is #{is_netherlands}."
    begin
      case type
        when :attempt then
          @logger.error "ERROR #{me} -> :attempts is an invalid type." #total_@attemps += 1
        when :failure then
          @total_attempts += 1
          @logger.debug "DEBUG #{me} -> incremented counter of type attempts to #{@total_attempts}"
        when :authenticated then
          @total_authentications += 1
          @total_attempts += 1
          @logger.debug "DEBUG #{me} -> incremented counters of type authentications to #{@total_authentications} and attempts to#{@total_attempts}"
        when :sms then
          @total_sms += 1
          @logger.debug "DEBUG #{me} -> incremented counter of type sms to #{@total_sms}"
        else @logger.warn "WARNING -> #{me}. ===> Unexpected else in case type block."
      end
      is_netherlands ? @total_ip_address_netherlands = 1 : @total_ip_address_foreign = 1

    rescue Exception => e
      @logger.error "ERROR #{me}  ===> Increment for #{type.inspect} caused #{e.message}"
    end
  end

  # Compare to AuthWebDisp objects,
  # if dispensary and webservice are equal, the demands
  # of this report are met.
  def equals?(a_row)
    me = "#{@class_name}.#{__method__}"
    result = false
    if a_row.dispensary == @dispensary and a_row.webservice_id == @webservice_id
      result = true
      @logger.debug "DEBUG #{me} -> row has equal dispensary and webservice."
    else
      @logger.debug "DEBUG #{me} -> row has unequal dispensary and webservice!"
    end
    return result
  end

  # Add the counters of an equal object
  def add(a_row)
    me = "#{@class_name}.#{__method__}"
    #@logger.debug "DEBUG AuthWebDisp.#{me} -> Enter function"
    if a_row.present?
      @attempts += a_row.attempts
      @authentications += a_row.authentications
      @sms += a_row.sms
      @ip_address_netherlands += a_row.ip_address_netherlands
      @ip_address_foreign += a_row.ip_address_foreign
      @total_attempts += a_row.total_attempts
      @total_authentications += a_row.total_authentications
      @total_sms += a_row.total_sms
      @total_ip_address_netherlands += a_row.total_ip_address_netherlands
      @total_ip_address_foreign += a_row.total_ip_address_foreign
      @logger.debug "DEBUG #{me} -> incremented counters to attempts: #{@attempts},
        authentications: #{@authentications},sms: #{@sms}, ip_address_netherlands: #{@ip_address_netherlands},
        ip_address_foreign: #{@ip_address_foreign} total_attempts: #{@total_attempts},
        total_authentications: #{@total_authentications},total_sms: #{@total_sms},
        total_ip_address_netherlands: #{@total_ip_address_netherlands},
        total_ip_address_foreign: #{@total_ip_address_foreign}"
    end
  end

  # Return an array of values
  def to_a
    [@dispensary, @dispensary_code,
    @webservice_id, @webservice_name,
    @attempts, @authentications, @sms,
    @ip_address_netherlands, @ip_address_foreign,
    @total_attempts, @total_authentications, @total_sms,
    @total_ip_address_netherlands, @total_ip_address_foreign]
  end

  # Return the string representation
  def to_s
    "#{@dispensary}, #{@dispensary_code},
    #{@webservice_id}, #{@webservice_name},
    #{@attempts}, #{@authentications}, #{@sms},
    #{@ip_address_netherlands}, #{@ip_address_foreign},
    #{@total_attempts}, #{@total_authentications}, #{@total_sms},
    #{@total_ip_address_netherlands}, #{@total_ip_address_foreign}"
  end

  def serialize
    to_s
  end

  def self.deserialize(str)
    me = "AuthWebDisp.#{__method__}"
    Rails.logger.debug "DEBUG #{me} -> importing string: #{str}"
    result = nil
    if str.present?
      the_row = self.new
      strings = str.split(',')
      the_row.dispensary = strings[0]
      the_row.dispensary_code = strings[1].to_i
      the_row.webservice_id = strings[2].to_i
      the_row.webservice_name = strings[3]
      the_row.attempts = strings[4].to_i
      the_row.authentications = strings[5].to_i
      the_row.sms = strings[6].to_i
      the_row.ip_address_netherlands = strings[7].to_i
      the_row.ip_address_foreign = strings[8].to_i
      the_row.total_attempts = strings[9].to_i
      the_row.total_authentications = strings[10].to_i
      the_row.total_sms = strings[11].to_i
      the_row.total_ip_address_netherlands = strings[12].to_i
      the_row.total_ip_address_foreign = strings[13].to_i
      result = the_row
      Rails.logger.debug "DEBUG #{me} -> imported into row object: #{the_row.inspect}"
    end
    return result
  end

end
