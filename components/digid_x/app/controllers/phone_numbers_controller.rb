
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
class PhoneNumbersController < ApplicationController
  include FlowBased
  include SmsConcern

  before_action :find_sms_tool
  before_action :verify_session

  def edit
    if @sms_tool.times_changed > ::Configuration.get_int("max_changes_phone_number")
      Log.instrument("1448", account_id: current_account.id)
      flash.now[:notice] = I18n.t("phone_number_changed_too_many_times")
      render_simple_message(ok: authenticators_sms_controleren_url)
    else
      Log.instrument("147", account_id: current_account.id)
      @page_name = current_account.issuer_type == IssuerType::DESK ? "A10B" : "A10"
    end
  end

  def update
    if @sms_tool.update(sms_tool_params)
      redirect_to authenticators_check_mobiel_url
    else
      @page_name = current_account.issuer_type == IssuerType::DESK ? "A10B" : "A10"
      render "edit"
    end
  end

  private
  def verify_session
    return render_not_found unless session[:session] == "registration"
  end

  def sms_tool_params
    params.require(:authenticators_sms_tool).permit(:phone_number, :gesproken_sms).merge(current_phone_number: current_account&.sms_challenge&.mobile_number || @sms_tool.phone_number, in_registration_flow: true).to_h.symbolize_keys
  end

  def find_sms_tool
    if current_account
      @sms_tool = current_account.pending_sms_tool
    else
      raise SessionExpired
    end
  end
end
