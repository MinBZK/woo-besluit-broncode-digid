
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

module Activations
  class LettersController < ApplicationController

    before_action :find_account
    before_action :check_session_time
    before_action :update_session

    def request_new_letter_activation_start
      @page_name = "B5"

      return redirect_via_js_or_http(new_letter_gba_error_url) unless @account.bsn.present?

      registration = Registration.where(burgerservicenummer: @account.bsn).last
      registration.update_attribute(:gba_status, "request")
      registration.retrieve_brp!("registration_letter")

      Log.instrument("155", registration_id: registration.id, hidden: true)

      session[:registration_id] = registration.id


      flash.now[:notice] = t("one_moment_please")
      render_message(registration_id: registration.id, page_name: @page_name, check_gba: true, custom_gba_path: request_new_letter_activation_gba_status_path(locale: nil), cancel_label: :cancel_back_to_home)
    end

    def request_new_letter_activation_gba_status
      @page_name = "B5"
      registration = Registration.find(session[:registration_id])

      if registration.gba_status == "valid"
        Log.instrument("156", registration_id: registration.id, hidden: true)
        redirect_via_js_or_http(request_new_letter_activation_url)
      elsif registration.gba_status == "request"
        flash.now[:notice] = t("one_moment_please")
        render_message(registration_id: registration.id, page_name: @page_name, check_gba: true, custom_gba_path: request_new_letter_activation_gba_status_path(locale: nil), cancel_label: :cancel_back_to_home)
      else
        redirect_via_js_or_http(new_letter_gba_error_url)
      end
    end

    def new_letter_gba_error
      registration = Registration.find(session[:registration_id]) if session[:registration_id]

      flash.now[:notice] = if !@account.bsn
        flash.now[:notice] = t("notice_new_letter_no_bsn")
        return render_simple_message(ok: activationcode_url)
      elsif registration.gba_status == "deceased"
        Log.instrument("559", hidden: true)
        brp_message(registration.gba_status)
      elsif registration.gba_status == "error"
        Log.instrument("158", hidden: true)
        brp_message("time_out")
      elsif registration.gba_status != "request"
        Log.instrument("558", hidden: true)
        brp_message(registration.gba_status)
      end

      render_simple_message(ok: request_new_letter_activation_url)
    end

    def request_new_letter_activation
      @page_name = "B5"
      registration = Registration.find(session[:registration_id])

      Log.instrument("1549", registration_id: registration.id)

      letters = registration.activation_letters.where(letter_type: ActivationLetter::LetterType::AANVRAAG_WITH_SMS, status: [ ActivationLetter::Status::SENT, ActivationLetter::Status::FINISHED ])
      total_days = ::Configuration.get_int("snelheid_aanvragen")

      if letters.where("created_at > ?", total_days.days.ago).any?
        Log.instrument("16")
        flash.now[:notice] = t("requested_but_not_activated", days: total_days == 1 ? I18n.t("day") : "#{total_days} #{I18n.t("days")}")
        render_simple_message(ok: activationcode_url)
      elsif letters.count >= 2
        flash.now[:notice] = t("new_activation_letter_too_soon").html_safe
        render_simple_message(yes: cancel_request_new_letter_url, cancel: activationcode_url )
      else
        Log.instrument("1551", registration_id: registration.id)
        flash.now[:notice] = t("we_will_send_you_a_letter_to_known_adres", postcode: registration.postcode&.to_i).html_safe
        session[:allowed_to_receive_new_letter?] = true
        render(:request_new_letter_activation)
      end
    end

    def cancel_request_new_letter
      Log.instrument("1550")
      session.delete(:account_id)

      redirect_to(new_registration_url)
    end

    def confirm_receive_new_letter
      return unless session.delete(:allowed_to_receive_new_letter?)

      @page_name = "B6"
      registration = Registration.find(session[:registration_id])

      Log.instrument("1552", registration_id: registration.id)

      activation_letter = registration.activation_letters.created.where(letter_type: ActivationLetter::LetterType::AANVRAAG_WITH_SMS).last
      activation_letter.update(status: ActivationLetter::Status::FINISHED)

      @account.password_authenticator.update(activation_code: activation_letter.controle_code)
      @secure_delivery = BeveiligdeBezorgingPostcodeCheck.new(registration.postcode).positive?
    end

    private

    def brp_message(brp_status)
      if %w(emigrated rni ministerial_decree).include?(brp_status)
        t("brp_messages.registration.general", registration_balie_url: new_balie_registration_url).html_safe
      elsif %w(not_found suspended_error suspended_unknown).include?(brp_status)
        t("brp_messages.registration.incorrect").html_safe
      else
        t(brp_status, scope: ["brp_messages", "registration"], rijksoverheid: view_context.rijksoverheid_link_to, mijnoverheid: view_context.mijnoverheid_link_to, digid_aanvraag: registration_link).html_safe
      end
    end

    def registration_link
      view_context.link_to(t("brp_messages.registration_balie.not_emigrated_digid_aanvraag_link"), new_registration_url)
    end
  end
end
