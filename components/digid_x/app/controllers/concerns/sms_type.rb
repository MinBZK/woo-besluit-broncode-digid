
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

module SmsType
  extend ActiveSupport::Concern

  def sms_type
    @sms_type ||= begin
      action = session[:session]
      if action.eql?("sign_in")
        "SMS05"
      elsif action.eql?("registration")
        "SMS02"
      elsif (action.eql?("activation") && current_account.status == "active") || session[:current_flow].eql?(:activate_sms_authenticator)
        "SMS04"
      elsif action.eql?("activation")
        "SMS03"
      elsif action.eql?("recover_account")
        "SMS10"
      elsif session[:current_flow].eql?("App")
        "SMS09"
      elsif session[:current_flow].eql?("SMS_controle")
        "SMS08"
      elsif session[:request_new_mobile] == true || session[:new_new_number].present?
        "SMS07"
      elsif session[:current_flow].eql?(:change_phone_number)
        "SMS06"
      else #  Should never happen
        "SMS00"
      end
    end
  end

  def replace_special_characters(name)
    name.tr("ÀÁÂÃÄÅàáâãäåÈÉÊËèéêëÌÍÎÏìíîïÒÓÔÕÖòóôõöÙÚÛÜùúûüç", "AAAAAAaaaaaaEEEEeeeeIIIIiiiiOOOOOoooooUUUUuuuuc")
  end

  def sms_message_params
    if sms_type == "SMS05"
      { webservice: replace_special_characters(short_webservice_name) }
    else
      {}
    end
  end

  def short_webservice_name
    @webservice ||= Webservice.from_authentication_session(session[:authentication])
    @webservice ? @webservice.short_name(108) : nil
  end
end
