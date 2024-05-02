
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

module SmsConcern
  extend ActiveSupport::Concern

  included do
    helper_method :gesproken_sms?
    helper_method :gesproken_sms_for_session
    helper_method :translation_key_check_code
  end

  def choose_sms
    if !current_account || !(current_account.blocking_manager.blocked? && !session[:session].eql?("recover_account"))
      if params[:sms_keuze]
        Log.instrument("1052", account_id: session[:account_id])
        return render_sms_keuze
      end
    end
  end

  def set_sms_choice
    session[:sms_options] = {} unless session[:sms_options]
    if params[:confirm]
      if params[:confirm][:value] == "resend_spoken_sms"
        Log.instrument("1054", account_id: session[:account_id])
        session[:sms_options][:gesproken_sms] = "1"
      elsif params[:confirm][:value] == "resend_text_sms"
        Log.instrument("1053", account_id: session[:account_id])
        session[:sms_options][:gesproken_sms] = "0"
      end
    end
  end

  def render_sms_keuze
    @choice_to_proceed = Confirm.new(value: "resend_spoken_sms")
    @page_name = "C14"
    render "shared/sms_keuze"
  end

  def gesproken_sms_for_session
    if session[:sms_options].present? && session[:sms_options][:gesproken_sms]
      ["1", true].include?(session[:sms_options][:gesproken_sms]) # from the session
    elsif current_account
      ["registration","activation"].include?(session[:session]) ? current_account.pending_gesproken_sms : current_account.gesproken_sms # from the active sms_tool (via account)
    end
  end

  def new_number_for_session
    if session[:sms_options].present? && session[:sms_options][:new_number]
      session[:sms_options][:new_number] # from the session
    elsif current_account
      ["registration","activation"].include?(session[:session]) ? current_account.pending_phone_number : current_account.phone_number # from the active sms_tool (via account)
    end
  end

  def gesproken_sms?
    gesproken_sms_for_session
  end

  def set_page_name_for_sms_not_received
    if params[:sms_keuze]
      if session[:session] == "activation"
        @page_name      = "C2B"
      elsif session[:session] == "sign_in"
        @page_name      = "C2C"
      elsif session[:session] == "mijn_digid"
        @page_name      = "C2D"
      elsif session[:session] == "recover_account"
        @page_name      = "C2E"
      else
        @page_name      = "C2A"
      end
    else
      @page_name      = "C2"
    end
  end

  def translation_key_check_code
    session[:current_flow] == :registration ? 'check_code_change_phone_number' : 'check_code'
  end
end
