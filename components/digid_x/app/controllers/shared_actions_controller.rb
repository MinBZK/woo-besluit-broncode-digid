
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

class SharedActionsController < ApplicationController
  SESSION_TYPE = {
    "registration"    => "Aanvragen",
    "sign_in"         => "Inloggen",
    "mijn_digid"      => "Mijn DigiD",
    "activation"      => "Activeren",
    "recover_account" => "Herstellen account"
  }.freeze

  # handles confirmation of cancellation
  def confirm_cancel
    webservice = Webservice.from_authentication_session(session[:authentication]) if session[:authentication]

    type = session[:session] ? SESSION_TYPE[session[:session]] : "Actie"

    account_id = nil
    account_id = @account.id if @account.present?
    account_id = session[:account_id] if session[:account_id].present?
    account_id = session[:recovery_account_id] if session[:recovery_account_id].present?

    if account_id && webservice
      Log.instrument("96", account_id: account_id, webservice_id: webservice.id, session: type)
    elsif webservice
      Log.instrument("96", webservice_id: webservice.id, session: type)
    elsif account_id
      Log.instrument("96", account_id: account_id, session: type)
    else
      Log.instrument("96", session: type)
    end

    Log.instrument(session.delete(:cancel_log), account_id: account_id, hidden: true) if session[:cancel_log].present?

    if session[:session] == "sign_in" && session[:authentication] && session[:authentication][:return_to]
      session[:authentication].delete(:confirmed_level)
      session[:authentication].delete(:confirmed_sector_code)
      session[:authentication].delete(:confirmed_sector_number)
      redirect_to session[:authentication][:return_to]
    else
      remove_registrations
      remove_session
      redirect_to APP_CONFIG["urls"]["external"]["digid_home"]
    end
  end

  def remove_registrations
    WebRegistration.where(id: session[:web_registration_id]).delete_all if session[:web_registration_id]
    Registration.where(id: session[:registration_id]).delete_all if session[:registration_id]
  end

  def new_authentication_request
    reset_session
    redirect_to my_digid_url
  end
end
