
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

class RegistrationBaseController < ApplicationController
  include FlowBased

  def timeout
    @registration = Registration.find(session[:registration_id])
    @registration.update_attribute(:gba_status, "error")
    @registration_unsuccessful = true

    flash.now[:notice] = brp_message("time_out")

    if session[:registration_type].end_with?("_extension") || session[:registration_type] == "unblock_letter"
      render_message( button_to: my_digid_url, button_to_options: { method: :get }, no_cancel_to: true )
    else
      render_message(no_cancel_to: true)
    end
  end

  private

  def brp_message(brp_status, digid_process = nil)
    if digid_process == "registration_balie" && %w(valid not_emigrated).include?(brp_status)
      t("brp_messages.registration_balie.general", registration_url: new_registration_url).html_safe
    elsif digid_process == "registration_balie" && %w(investigate_address not_found suspended_error suspended_unknown).include?(brp_status)
      t("brp_messages.registration_balie.incorrect", rijksoverheid: view_context.rijksoverheid_link_to).html_safe
    elsif digid_process == "registration" && %w(emigrated rni ministerial_decree).include?(brp_status)
      t("brp_messages.registration.general", registration_balie_url: new_balie_registration_url).html_safe
    elsif digid_process == "registration" && %w(not_found suspended_error suspended_unknown).include?(brp_status)
      t("brp_messages.registration.incorrect").html_safe
    else
      t(brp_status, scope: ["brp_messages", digid_process], rijksoverheid: view_context.rijksoverheid_link_to, mijnoverheid: view_context.mijnoverheid_link_to, digid_aanvraag: registration_link).html_safe
    end
  end

  # cancel from the first page
  def cancel_registration?
    cancel_button(return_to: new_registration_url) if clicked_cancel?
  end

  # registered_successful:
  # User filled out a valid form and has no other exceptions;
  #   update the gba status to 'request',
  #   then make the call so delayed job does its thing,
  #   then redirect to the wait screen
  #  type: the type of registration:
  #    aanvraag => "registration",
  #    aanvraag_balie => "registration_balie",
  #    uitbreiding => "sms_extension" of "app_extension"
  #    herstel => "recovery",
  def registered_successful(type, options = {})
    session[:registration_type] = type
    @registration.update_attribute(:gba_status, "request")

    # make a call to BRP
    if @registration.retrieve_brp!(type)
      if type == "recovery"
        Log.instrument("uc6.gba_start_gelukt", registration_id: @registration.id, hidden: true)
      elsif type.end_with?("_extension")
        Log.instrument("155", registration_id: @registration.id, hidden: true)
      elsif type == "unblock_letter"
        Log.instrument("155", registration_id: @registration.id, hidden: true)
      else
        Log.instrument("6", registration_id: @registration.id, hidden: true)
      end
      flash.now[:notice] = t("one_moment_please")
      render_message({ registration_id: @registration.id,
                       page_name: "A2",
                       check_gba: true }.merge(options))
    else

      if type == "recovery" || type == "unblock_letter"
        Log.instrument("202", registration_id: @registration.id, hidden: true)
      elsif type.end_with?("_extension")
        Log.instrument("158", registration_id: @registration.id, hidden: true)
      else
        Log.instrument("9", registration_id: @registration.id, hidden: true)
      end

      redirect_via_js_or_http(registration_timeout_path)
    end
  end

  def registration_reconstructed_from_session
    return nil unless registration_retrieved_from_session
    reconstructed_registration = registration_retrieved_from_session.dup
    reconstructed_registration.assign_attributes(gba_status: "init", status: "")
    reconstructed_registration
  end

  private

  def registration_link
    view_context.link_to(t("brp_messages.registration_balie.not_emigrated_digid_aanvraag_link"), new_registration_url)
  end
end
