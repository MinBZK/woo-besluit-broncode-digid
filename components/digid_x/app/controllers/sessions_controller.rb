
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

class SessionsController < ApplicationController
  # Needed for setting verification token status to cancelled, discuss if necessary with FO
  include AppSessionConcern
  include AppAuthenticationSession

  before_action(:check_session_time, except: :destroy)
  before_action(:update_session)

  # GET /uitloggen
  def destroy
    Saml.current_store = :database

    if current_provider.present? && current_federation.present?
      saml_request = Saml::LogoutRequest.new(
        issuer: current_provider.entity_id,
        name_id: current_federation.subject
      )

      saml_request.destination = saml_request_logout_url
      saml_request.provider.signing_key = OpenSSL::PKey::RSA.new(File.read(Saml::Config.my_digid_private_key_file), Saml::Config.private_key_password)
      url = Saml::Bindings::HTTPRedirect.create_url(saml_request)
      render_message_or_redirect(url)
    else
      render_message_or_redirect
    end
  end

  # PUT /sessions (only possible via xhr)
  #
  # This url is used to keep the session alive when it is nearly expired. There
  # is a maximum number of "keep-alive" attempts and when that maximum is
  # reached, a http status code of 401 (unauthorized) is returned. If there are
  # still attempts left, status 200 is returned. In case the session is already
  # expired, the before filter has redirected the user.
  def update
    raise unless request.xhr?

    session[:keep_alive_retries] ||= ::Configuration.get_int("session_keep_alive_retries")
    session[:keep_alive_retries] -= 1
    # send http status 200 if there are keep-alive retries left, else 401

    if session[:keep_alive_retries] > 0
      case session[:session]
      when "activation"
        Log.instrument("414", account_id: session[:account_id])
      when "registration"
        Log.instrument "413", account_id: session[:account_id]
      end
    end

    head(
      ((session[:keep_alive_retries] > 0) ? :ok : :unauthorized),
      next_timeout_in: wrapped_session.show_popup_after_seconds,
      popup_text: popup_text,
      popup_buttons: popup_buttons
    )
  end

  private

  def render_message_or_redirect(url = nil)
    reset_session if url.nil?

    if app_session&.error == "invalid_pin"
      reset_session if url
      flash.now[:notice] = blocked_message.html_safe
      @link_back = true
      render_simple_message
    elsif current_provider.present? && current_federation.present?
      redirect_to(url)
    else
      redirect_to(APP_CONFIG["urls"]["external"]["digid_home"])
    end
  end

  def popup_text
    "<p>#{if wrapped_session.timeout_warning > Time.zone.now
            if wrapped_session.timeout_warning_is_extendible?
              t('session_almost_expires')
            elsif wrapped_session.timeout_warning_is_absolute?
              t('session_maximum_time')
            end
          else
            session_expired_message
          end}</p>"
  end

  def popup_buttons
    "<div class=\"actions\">#{if wrapped_session.timeout_warning > Time.zone.now
                                if wrapped_session.timeout_warning_is_extendible?
                                  view_context.action_button(:yeah, sessions_path, { method: :put, remote: true, class: 'extend-session' }) + \
                                    view_context.action_button(:nope, '#', { method: :get })
                                elsif wrapped_session.timeout_warning_is_absolute?
                                  view_context.action_button(:annie_are_you_okay, sessions_path, { method: :put, remote: true })
                                end
                              else
                                ''
                              end}</div>"
  end
end
