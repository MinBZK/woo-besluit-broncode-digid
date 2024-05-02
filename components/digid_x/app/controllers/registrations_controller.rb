
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

class RegistrationsController < RegistrationBaseController
  include RdwClient
  include RvigClient
  include AppSessionConcern
  include EmailConcern

  before_action :cancel_registration?,     only:   [:create]
  before_action :aanvraag_present?,        only:   [:webdienst]
  before_action :check_session_time,       except: [:new, :webdienst]
  before_action :update_session,           except: [:new, :webdienst]
  before_action :check_cookie_blocked?, only: [:new]

  # GET /registrations/new
  # setup a brand new form for starting a registration request for a DigiD
  def new
    @registration = build_registration

    start_session("registration")
    Log.instrument("3")

    @news_items = news_items("Aanvraagpagina")
    @page_name = "A1"

    render_partial_or_return_json("#registration-form", :form, :new)
  end

  # POST /registrations
  # try to save a submitted form;
  # if validation errors -> render the form again + error msgs
  # if ok move on to GBA
  def create
    current_flow.transition_to!(:registration_verified)

    @news_items = news_items("Aanvraagpagina")
    @registration = Registration.new(registration_params)
    @registration.gba_status = "request"

    if @registration.save
      Log.instrument("4", registration_id: @registration.id)
      session[:registration_id] = @registration.id

      if @registration.no_gba_info_leakage?
        # since we were able to save a registration,
        # the user entered valid data to do a GBA check
        # so we must show screen "message/index" with a timer and
        # a msg "please hold on", and do a call to GBA using DelayedJob
        # then go to any of the following screen according to the GBA outcome.
        session[:registration_id] = @registration.id
        # REFACTOR - magic "registration" string
        registered_successful("registration", cancel_label: :cancel_back_to_home)
      else
        errors = @registration.errors
        @registration = Registration.new
        @registration.errors.add(:leakage, errors[:leakage])
        render_partial_or_return_json("#registration-form", :form, :new)
      end

    else
      @page_name = "A1"
      @registration.gba_status = nil
      Log.instrument("5")
      render_partial_or_return_json("#registration-form", :form, :new)
    end
  end

  # Aanvraag deblokkeringsbrief voor rijbewijs en identiteitskaart
  def request_unblock_letter
    return render_not_found if params[:card_type].blank?

    # switches
    switch = check_hoog_switches(params[:card_type])
    return redirect_via_js_or_http(switch[:redirect_url]) if switch[:status] == false && switch[:redirect_url].present?

    switch[:check_tonen] if switch[:status] == false && switch[:check_tonen].present?

    session[:sequence_no] = params[:sequence_no]
    session[:card_type] = params[:card_type]

    if params[:card_type] == "NL-Rijbewijs"
      session[:flow] = ::RequestUnblockLetterDrivingLicenceFlow.new
      current_flow.transition_to!(:pre_confirm) if session[:existing_unblock_letter_request_licence]
      current_flow.transition_to!(:confirm) if session[:existing_unblock_letter_request_licence]
      session[:current_flow] = "request_unblock_letter_licence"

      unless session[:existing_unblock_letter_request_licence]
        begin
          @driving_licence = rdw_client.get(bsn: current_account.bsn, sequence_no: session[:sequence_no]).first
          return render_not_found unless @driving_licence.deblockable?
        rescue DigidUtils::DhMu::RdwError => e
          return redirect_via_js_or_html my_digid_url
        end
        Log.instrument("982", wid_type: t("document.driving_licence", locale: :nl), account_id: current_account.id)
      end
    else
      session[:flow] = ::RequestUnblockLetterIdentityCardFlow.new
      current_flow.transition_to!(:pre_confirm) if session[:existing_unblock_letter_request_id_card]
      current_flow.transition_to!(:confirm) if session[:existing_unblock_letter_request_id_card]
      session[:current_flow] = "request_unblock_letter_id_card"

      unless session[:existing_unblock_letter_request_id_card]
        begin
          @identity_card = rvig_client.get(bsn: current_account.bsn, sequence_no: session[:sequence_no]).first
          return render_not_found unless @identity_card.deblockable?
        rescue DigidUtils::DhMu::RvigError => e
          return redirect_via_js_or_html my_digid_url
        end
        Log.instrument("982", wid_type: t("document.id_card", locale: :nl), account_id: current_account.id)
      end
    end

    # create a normal registration so we can follow through with GBA the process normally
    sectorcode = current_account.sectorcodes.find_by(sector_id: Sector.get("bsn"))

    if sectorcode
      @registration = Registration.create_fake_aanvraag(sectorcode.sectoraalnummer)
      @registration.create_wid(sequence_no: session[:sequence_no], card_type: session[:card_type], action: "unblock")
      session[:registration_id] = @registration.id # so we can set aanvraag/letter to final when done
      request_unblock_letter_checks
    else
      flash[:notice] = I18n.t("messages.my_digid.error.verkeerde_sector")
      redirect_via_js("/")
    end
  end

  # Aanvraag APP uitbreiding, goto GBA and if succesful move on to D17
  def request_app
    session[:current_flow] = "App_by_letter"

    Log.instrument("886", account_id: current_account.id)

    # create a normal registration so we can follow through with GBA the process normally
    sectorcode = current_account.sectorcodes.find_by(sector_id: Sector.get("bsn"))

    if sectorcode
      @registration = Registration.create_fake_aanvraag sectorcode.sectoraalnummer
      session[:registration_id] = @registration.id # so we can set aanvraag/letter to final when done
      request_app_checks
    else
      flash[:notice] = I18n.t("messages.my_digid.error.verkeerde_sector")
      redirect_via_js("/")
    end
  end

  # Aanvraag SMS uitbreiding, goto GBA and if succesful move on to D17
  def request_sms
    if current_account.deceased?
      Log.instrument("1445", account_id: current_account.id, hidden: true)
      return render_not_found_if_account_deceased
    end

    session[:current_flow] = "SMS_controle"
    if current_account.present? && current_account.password_authenticator.present? &&
       (current_account.sms_tools.empty? || current_account.sms_tools.pending? || current_account.mobiel_kwijt_in_progress? || session[:change_mobile_while_old_number_not_usable])

      session[:sms_extension] = true
      session[:request_new_mobile] = true unless session[:change_mobile_while_old_number_not_usable]

      Log.instrument("154", account_id: current_account.id)

      # create a normal registration so we can follow through with GBA the process normally
      sectorcode = current_account.sectorcodes.where("sector_id=? or sector_id=?", Sector.get("bsn"), Sector.get("sofi")).first

      if sectorcode
        @registration = Registration.create_fake_aanvraag sectorcode.sectoraalnummer
        request_sms_checks
      elsif current_account.oeb.present?
        flash.now[:notice] = t("activation_sms_by_letter_with_oeb_not_possible")
        Log.instrument("1075", account_id: current_account.id)
        render_simple_message(ok: my_digid_url)
      else
        flash[:notice] = I18n.t("messages.my_digid.error.verkeerde_sector")
        redirect_via_js("/")
      end
    elsif current_account.password_authenticator.blank?
      render_not_found
    else
      flash[:notice] = I18n.t("your_digid_already_has_a_sms_function")
      redirect_via_js_or_http(my_digid_url)
    end
  end

  def request_recovery_code
    account = Account.find(session[:recovery_account_id])
    return unless account

    @registration = Registration.create_fake_aanvraag(
      account.sectorcodes.where("sector_id=? or sector_id=?", Sector.get("bsn"), Sector.get("sofi")).first.sectoraalnummer
    )
    session[:registration_id] = @registration.id
    registered_successful("recovery", button_to: request_recover_password_url, cancel_label: :cancel_back_to_home)
  end

  # gba_status:
  # Callback function for polling mechanism to see if GBA is done.
  # GBA handler communicates through the registration field 'gba_status'
  # so we check the contents of this field to see if anything happened yet.
  def gba_status
    @registration_unsuccessful = true
    @registration = Registration.find(session[:registration_id])

    if @registration.gba_status == "request"
      @registration_unsuccessful = nil
      flash.now[:notice] = t("one_moment_please")

      if session[:session].eql?("mijn_digid")
        render_message(cancel_label: :cancel_back_to_my_digid, cancel_to: my_digid_url, check_gba: true)
      else
        render_message(cancel_label: :cancel_back_to_home, cancel_to: home_url, check_gba: true)
      end
    else
      redirect_via_js_or_http(registrations_gba_result_path)
    end
  end

  def gba_result
    @registration_unsuccessful = true
    @registration = Registration.find(session[:registration_id])

    case @registration.gba_status
    when "valid", "investigate_address"
      balie_aanvraag_status_valid
    when "valid_sms_extension", "valid_app_extension"
      dispatch_on_extension
    when "valid_recovery"
      dispatch_on_recovery
    when "emigrated", "rni", "ministerial_decree"
      balie_aanvraag_status_emigrated_or_rni_or_ministerial_decree
    when "valid_unblock_letter"
      dispatch_on_unblock_letter
    else
      dispatch_on_gba_errors
    end
  end


  def extension_confirmation
    # End of app_extension and sms_extension flow
    return redirect_to my_digid_url if current_registration.nil?

    postcode = current_registration.activation_letters.last.postcode
    @secure_delivery = BeveiligdeBezorgingPostcodeCheck.new(postcode).positive?

    @page_name = "D41"

    if session[:current_flow] == "App_by_letter"
      @page_title = t("titles.D41.app")
    elsif session[:current_flow] == "request_unblock_letter_licence"
      @page_header = t("confirm_unblock_wid")
      @page_name = "D49"
      @page_title = t("titles.D49")
      @document_type = "driving_licence"
      flash[:notice] = t("you_requested_wid_unblockingscode_letter_sent", wid_type: t("document.driving_licence")).html_safe
    elsif session[:current_flow] == "request_unblock_letter_id_card"
      @page_header = t("confirm_unblock_wid")
      @page_name = "D49"
      @page_title = t("titles.D49")
      @document_type = "id_card"
      flash[:notice] = t("you_requested_wid_unblockingscode_letter_sent", wid_type: t("document.id_card")).html_safe
    else
      Log.instrument("550", account_id: current_account.id)
      @page_title = t("titles.D41.sms")
    end

    session.delete(:registration_id)
  end

  # entry point for svb users
  def webdienst
    if (days = @aanvraag_webdienst.expired?)
      flash[:notice] = t("messages.aanvraag.expired", count: days)
      render_message
    elsif (datum = @aanvraag_webdienst.web_registration_too_often?)
      Log.instrument("17", registration_id: @aanvraag_webdienst.id)
      options = {}
      options[:count] = ::Configuration.get_int("blokkering_aanvragen")
      options[:wait_until] = I18n.l(datum, format: :date)
      flash[:notice] = I18n.t("you_requested_an_account_too_often", **options)
      render_message(button_to: WebRegistration.get_webdienst_url(session), no_cancel_to: true)
    elsif @aanvraag_webdienst.web_registration_too_soon?
      Log.instrument("16", registration_id: @aanvraag_webdienst.id)
      flash[:notice] = I18n.t("you_have_made_a_new_request_too_soon")
      render_message(button_to: WebRegistration.get_webdienst_url(session), no_cancel_to: true)
    else
      # find the matching registration
      @registration = Registration.where(burgerservicenummer: @aanvraag_webdienst.sectoraalnummer).last
      preserve_flash = flash[:notice]
      start_session("registration")
      flash[:notice] = preserve_flash
      session[:webdienst] = true
      session[:web_registration_id] = @aanvraag_webdienst.id
      session[:registration_id] = @registration.id

      web_reg = WebRegistration.find(@aanvraag_webdienst.id) # getting the sector

      current_flow.transition_to!(:registration_verified)
      dispatch_on_account @aanvraag_webdienst.sectoraalnummer, web_reg.sector_id
    end
  end

  # cancel the cancel request
  def cancel_cancel
    if session[:registration_id]
      @registration = Registration.find(session[:registration_id])
      if session[:web_registration_id]
        web_reg = WebRegistration.find(session[:web_registration_id])
        sector_id = web_reg.sector_id
      else
        sector_id = Sector.get("bsn")
      end
      dispatch_on_account @registration.burgerservicenummer, sector_id
    else
      session[:session] = "registration"
      session_expired
    end
  end

  # send another email on request of the user
  def repeat_email
    if current_account.max_emails_per_day?(::Configuration.get_int("aantal_controle_emails_per_dag"))
      Log.instrument("1096")

      if session[:session] == "registration"
        max_emails_per_day_error
      else
        flash[:notice] = t("email_max_verification_mails", max_number_emails: ::Configuration.get_int("aantal_controle_emails_per_dag"), date: l(Time.zone.now.next_day, format: :date).strip)
        redirect_to(check_email_url)
      end
    else
      EmailControlCodeMailer.new(current_account, balie_session? ? "balie" : "mijn").perform
      session[:check_email_options] = {
        return_to: confirm_account_url,
        cancel_to: confirm_account_url,
        instant_cancel: false
      }
      redirect_to(check_email_url)
    end
  end

  def request_unblock_letter_status
    @registration = Registration.find(session[:registration_id])
    @current_wid = @registration.wid
    wid_type = @current_wid.card_type == "NL-Rijbewijs" ? t("document.driving_licence", locale: :nl) : t("document.id_card", locale: :nl)

    if @registration.registration_too_soon?("snelheid_aanvragen_deblokkeringscode_identiteitsbewijs", ["valid_unblock_letter"], @current_wid.id)
      flash.now[:notice] = t("digid_hoog.request_unblock_letter.abort.too_soon")
      @registration_unsuccessful = true
      Log.instrument("981", wid_type: wid_type, account_id: current_account.id)
      session.delete(:existing_unblock_letter_request_licence) if session[:existing_unblock_letter_request_licence]
      session.delete(:existing_unblock_letter_request_id_card) if session[:existing_unblock_letter_request_id_card]
      render_message(button_to: my_digid_url, button_to_options: { method: :get }, no_cancel_to: true)
    elsif (next_possible_registration =
             @registration.registration_too_often?("blokkering_aanvragen_deblokkeringscode_identiteitsbewijs", "valid_unblock_letter", @current_wid.id)
          )
      flash.now[:notice] = t(
        "digid_hoog.request_unblock_letter.abort.too_often",
        wid_type: @current_wid.card_type == "NL-Rijbewijs" ? t("document.driving_licence") : t("document.id_card"),
        count: ::Configuration.get_int("blokkering_aanvragen_deblokkeringscode_identiteitsbewijs"),
        date: l(next_possible_registration, format: :date)
      )
      session.delete(:existing_unblock_letter_request_licence) if session[:existing_unblock_letter_request_licence]
      session.delete(:existing_unblock_letter_request_id_card) if session[:existing_unblock_letter_request_id_card]
      @registration_unsuccessful = true

      Log.instrument("983", wid_type: wid_type, account_id: current_account.id)
      render_message(button_to: my_digid_url, button_to_options: { method: :get }, no_cancel_to: true)
    end
  end

  def fail
    @registration = registration_retrieved_from_session

    if @registration.registration_too_soon?("snelheid_uitbreidingsaanvragen", ["valid_sms_extension"])
      flash.now[:notice] = t("you_need_to_activate_sms_code_verification_first")
      @registration_unsuccessful = true
      Log.instrument("162", account_id: current_account.id)
      render_message(button_to: my_digid_url, button_to_options: { method: :get }, no_cancel_to: true)
    elsif (next_possible_registration = @registration.registration_too_often?("blokkering_uitbreidingsaanvragen", "valid_sms_extension"))
      flash.now[:notice] = t(
        "you_requested_sms_code_verification_too_often",
        count: ::Configuration.get_int("blokkering_uitbreidingsaanvragen"),
        date: l(next_possible_registration, format: :date)
      )
      @registration_unsuccessful = true
      Log.instrument("163", account_id: current_account.id)
      render_message(button_to: my_digid_url, button_to_options: { method: :get }, no_cancel_to: true)
    end
  end


  private
  def check_hoog_switches(document)
    response = {}

    if document == "NL-Rijbewijs"
      if request.referer
        response[:status] = driving_licence_enabled? && show_driving_licence?(current_account.bsn)
        response[:redirect_url] = my_digid_licence_unblocking_switch_off_message_url
      else
        response[:status] = false
        response[:check_tonen] = check_tonen_rijbewijs_switch
      end
    else
      if request.referer
        response[:status] = identity_card_enabled? && show_identity_card?(current_account.bsn)
        response[:redirect_url] = my_digid_id_card_unblocking_switch_off_message_url
      else
        response[:status] = false
        response[:check_tonen] = check_tonen_identiteitskaart_switch
      end
    end

    return response
  end

  def registration_params
    params.require(:registration).permit(
      :burgerservicenummer, :geboortedatum_dag, :geboortedatum_maand,
      :geboortedatum_jaar, :postcode, :huisnummer, :huisnummertoevoeging
    ).transform_values { |v| v.gsub(/\s+/, "") } # no spaces
  end

  def request_sms_checks
    session[:registration_id] = @registration.id # so we can set aanvraag/letter to final when done

    if @registration.registration_too_soon?("snelheid_uitbreidingsaanvragen", ["valid_sms_extension"]) || @registration.registration_too_often?("blokkering_uitbreidingsaanvragen", "valid_sms_extension")
      return redirect_via_js_or_http(registrations_fail_url)
    end

    registered_successful("sms_extension", button_to: my_digid_url, cancel_to: my_digid_url,
                                           cancel_to_options: { method: :get }, cancel_label: :cancel_back_to_my_digid)
  end

  def request_app_checks
    if @registration.registration_too_soon?("snelheid_uitbreidingsaanvragen", ["valid_app_extension"])
      i18n_key = if session[:existing_app_request]
                   "you_need_to_activate_app_code_verification_first_existing_request"
                 else
                   "you_need_to_activate_app_code_verification_first"
                 end
      flash.now[:notice] = t(i18n_key)
      @registration_unsuccessful = true
      Log.instrument("758", account_id: current_account.id)
      session.delete(:existing_app_request)
      render_message(button_to: my_digid_url, button_to_options: { method: :get }, no_cancel_to: true)
    elsif (next_possible_registration = @registration.registration_too_often?("blokkering_digid_app_aanvragen", "valid_app_extension"))
      flash.now[:notice] = t(
        "you_requested_app_code_verification_too_often",
        count: ::Configuration.get_int("blokkering_digid_app_aanvragen"),
        date: l(next_possible_registration, format: :date)
      )
      session.delete(:existing_app_request)
      @registration_unsuccessful = true
      Log.instrument("906", account_id: current_account.id)
      render_message(button_to: my_digid_url, button_to_options: { method: :get }, no_cancel_to: true)
    else
      clean_pending_app_activation if session[:existing_app_request]
      session[:registration_id] = @registration.id # so we can set aanvraag/letter to final when done
      registered_successful("app_extension", button_to: my_digid_url, cancel_to: my_digid_url, cancel_label: :cancel_back_to_my_digid)
    end
  end

  def request_unblock_letter_checks
    @current_wid = @registration.wid
    session[:registration_id] = @registration.id # so we can set aanvraag/letter to final when done

    if @registration.registration_too_soon?("snelheid_aanvragen_deblokkeringscode_identiteitsbewijs", ["valid_unblock_letter"], @current_wid.id) ||
        @registration.registration_too_often?("blokkering_aanvragen_deblokkeringscode_identiteitsbewijs", "valid_unblock_letter", @current_wid.id)
      return redirect_via_js_or_http(request_unblock_letter_status_path)
    end

    registered_successful("unblock_letter", button_to: my_digid_url, cancel_to: my_digid_url, cancel_label: :cancel_back_to_my_digid)
  end

  def clean_pending_unblock_letter_requests(current_wid)
    previous_registration_ids = Wid.where.not(id: current_wid.id)
                                   .where(sequence_no: current_wid.sequence_no, card_type: current_wid.card_type, action: "unblock").pluck(:registration_id)
    previous_registration = Registration.where(id: previous_registration_ids, gba_status: "valid_unblock_letter").last
    if previous_registration
      wid_type = current_wid.card_type == "NL-Rijbewijs" ? t("document.driving_licence", locale: :nl) : t("document.id_card", locale: :nl)
      if previous_registration.activation_letters.last.try(:state) == ActivationLetter::Status::SENT
        Log.instrument("1008", wid_type: wid_type, registration_id: previous_registration.id)
      else
        Log.instrument("1009", wid_type: wid_type, registration_id: previous_registration.id)
      end
    end

    Wid.where.not(id: current_wid.id).where(
      sequence_no: current_wid.sequence_no, card_type: current_wid.card_type, action: "unblock"
    ).update_all(unblock_code: nil)
  end

  def clean_pending_app_activation
    previous_registration = @registration.get_previous_registration("valid_app_extension")
    if previous_registration
      if previous_registration.activation_letters.last.try(:state) == ActivationLetter::Status::SENT
        Log.instrument("908", registration_id: previous_registration.id)
      else
        Log.instrument("909", registration_id: previous_registration.id)
      end
    end
    current_account.app_authenticators.pending.try(:first).try(:destroy)
    session.delete(:existing_app_request)
  end

  # Check if this is a DigiD balie aanvraag.
  def balie_aanvraag_status_emigrated_or_rni_or_ministerial_decree
    if @registration.balie_request?
      balie_session!
      dispatch_on_registration
    else
      dispatch_on_gba_errors
    end
  end

  # something is wrong when we have a baliecode but no answer from balie
  def balie_aanvraag_status_valid
    if @registration.gba_status.eql?("investigate_address")
      dispatch_on_gba_errors
    elsif @registration.balie_request?
      @registration.gba_status = "not_emigrated"
      dispatch_on_gba_errors
    else
      # regular registrations exit through here
      dispatch_on_registration
    end
  end

  # handle the case that gba either:
  # - has not responded yet
  # - has responded with some case of error
  def dispatch_on_gba_errors
    # Mogelijke BRP statussen: 'valid', 'not_found', 'ministerial_decree', 'deceased', 'emigrated', 'rni', 'suspended_error', 'suspended_unknown'
    # Mogelijke eigen statussen: 'not_emigrated', 'error', 'request', 'investigate_address', 'invalid'
    if %w[not_eer not_found error invalid investigate_address suspended_error suspended_unknown].include?(@registration.gba_status)
      @registration.update_attribute(:status, ::Registration::Status::ABORTED)
    end

    flash.now[:notice] = if @registration.gba_status == "error"
                           brp_message("time_out")
                         elsif ::Configuration.get_boolean("balie_aanvraag_voor_rni")
                           brp_message(@registration.gba_status == "invalid" ? "invalid_rni" : @registration.gba_status, session[:registration_type])
                         else
                           brp_message(@registration.gba_status, session[:registration_type])
                         end

    log_gba_error(@registration.gba_status)

    render_message(
      button_to: determine_buttons(session[:registration_type]),
      button_to_options: { method: :get },
      no_cancel_to: true
    )
  end

  # give the previous button a url
  def determine_buttons(registration_type)
    case registration_type
    when "registration"
      new_registration_url
    when "registration_balie"
      new_balie_registration_url
    when /_extension$/
      my_digid_url
    when "recovery"
      request_recover_password_url
    when "unblock_letter"
      my_digid_url
    end
  end

  # before filter for method webdienst, check if there is one
  def aanvraag_present?
    @aanvraag_webdienst = WebRegistration.find_by(aanvraagnummer: params[:id])
    return unless @aanvraag_webdienst.blank?

    # there was a problem finding this aanvraag id
    flash[:notice] = I18n.t("messages.aanvraag.not_found")
    render_message
  end

  # dispatch_on_registration checks on timing issues concerning registrations:
  # go to either
  #  message screen: if something is wrong
  #  method dispatch_on_account: if all is good
  def dispatch_on_registration
    Log.instrument("7", gba_status: @registration.gba_status, registration_id: @registration.id, hidden: true)
    Log.instrument("8", registration_id: @registration.id, hidden: true) unless @registration.a_number?
    button_to = balie_session? ? new_balie_registration_url : new_registration_url
    match = RegistrationChecks::RegistrationChecksQueue.new(@registration, button_to).registration_checks(session)
    if match
      flash[:notice] = match.flash_msg.html_safe
      match.log
      render_message(match.buttons.merge(no_cancel_to: true))
    else
      bsn = @registration.burgerservicenummer
      dispatch_on_account bsn, Sector.get("bsn")
    end
  end

  # dispatch_on_account checks on issues concerning existing accounts:
  # go to either:
  #  - lopende_aanvraag: when there is another DigiD in request mode
  #  - bestaande_digid: when a DigiD already exists
  #  - render_message: when a DigiD exists which is 'opgeschort'
  def dispatch_on_account(bsn, sector_id)
    sector_ids = Sector.fetch(sector_id)
    button_to = balie_session? ? new_balie_registration_url : new_registration_url
    match = RegistrationChecks::RegistrationChecksQueue.new(@registration, button_to).account_checks(bsn, sector_ids)

    if match
      flash[:notice] = match.flash_msg.html_safe
      match.log
      render_message(match.buttons.merge(no_cancel_to: true))
    elsif Account.count_registration_currently_in_progress(bsn, sector_ids) > 0
      redirect_via_js_or_html(existing_request_url)
    elsif Account.count_account_already_exists(bsn, sector_ids) > 0
      redirect_via_js_or_html(existing_account_url)
    else
      redirect_via_js_or_http(new_account_url)
    end
  end

  # dispatch_on_extension checks on timing issues concerning extension:
  # go to either
  #  message screen: if something is wrong
  def dispatch_on_extension
    Log.instrument("156", gba_status: @registration.gba_status, registration_id: @registration.id, hidden: true)

    if current_account.app_authenticator_active?
      redirect_via_js_or_http(new_my_digid_request_two_factor_authentications_url)
    elsif current_account.sms_in_uitbreiding? || current_account.mobiel_kwijt_in_progress? # 'valid_sms_extension'
      redirect_via_js_or_http(existing_sms_request_url)
    else
      redirect_via_js_or_http(index_my_digid_request_two_factor_authentications_url)
    end
  end

  def dispatch_on_unblock_letter
    Log.instrument("156", gba_status: @registration.gba_status, registration_id: @registration.id, hidden: true)
    if session[:existing_unblock_letter_request_licence]
      @current_wid = @registration.wid
      clean_pending_unblock_letter_requests(@current_wid)
      session.delete(:existing_unblock_letter_request_licence)
      redirect_via_js_or_http(my_digid_licence_unblocking_finalize_request_code_url)
    elsif session[:existing_unblock_letter_request_id_card]
      @current_wid = @registration.wid
      clean_pending_unblock_letter_requests(@current_wid)
      session.delete(:existing_unblock_letter_request_id_card)
      redirect_via_js_or_http(my_digid_id_card_unblocking_finalize_request_code_url)
    else
      current_flow.transition_to!(:pre_confirm)

      if session[:current_flow] == "request_unblock_letter_licence"
        redirect_via_js_or_http(my_digid_licence_unblocking_request_code_url)
      else
        redirect_via_js_or_http(my_digid_id_card_unblocking_request_code_url)
      end
    end
  end

  def dispatch_on_recovery
    Log.instrument("156", registration_id: @registration.id, hidden: true)
    if session[:recovery_account_type].eql?("midden")
      @current_account = Account.find(session[:recovery_account_id]) if session[:recovery_account_id]
      session[:sms_options] = {
        cancel_to: home_url,
        page_name: "C2",
        return_to: letter_sent_for_password_url,
        step: :request_recover_password
      }
      redirect_via_js_or_http(authenticators_check_mobiel_url(url_options))
    else
      redirect_via_js_or_http letter_sent_for_password_url
    end
  end

  # Log GBA status
  def log_gba_error(gba_status)
    unless %w[deceased emigrated invalid investigate_address not_emigrated not_found not_eer rni
            ministerial_decree invalid suspended_unknown suspended_error error].include?(gba_status)
      return
    end

    if gba_status == "error"
      case session[:registration_type]
      when "recovery", "unblock_letter"
        Log.instrument("202", hidden: true)
      when /_extension$/
        Log.instrument("158", hidden: true)
      else
        Log.instrument("9", hidden: true)
      end
      return
    end

    suffix = case gba_status
             when "rni", "ministerial_decree"
               "emigrated"
             when /^suspended_/
               "suspended"
             else
               gba_status
    end

    if @registration.balie_request?
      case gba_status
      when "not_found"
        Log.instrument("11", hidden: true)
      when "invalid"
        Log.instrument("507", hidden: true)
      when "not_emigrated"
        Log.instrument("505", hidden: true) if suffix == "not_emigrated"
      else
        log_suffix(suffix)
      end
    elsif session[:registration_type] == "recovery"
      case suffix
      when "deceased" then Log.instrument("587", hidden: true)
      when "investigate_address" then Log.instrument("223", hidden: true)
      when "emigrated", "not_found", "suspended" then Log.instrument("586", hidden: true)
      else Log.instrument("uc6.aanvraag_mislukt_#{suffix}", hidden: true)
      end
    elsif session[:registration_type] == "sms_extension"
      Log.instrument(suffix == "deceased" ? "559" : "558", hidden: true)
    elsif session[:registration_type] == "app_extension"
      Log.instrument("558", hidden: true)
    elsif session[:registration_type] == "unblock_letter"
      code = case suffix
             when "deceased" then "559"
             when "emigrated" then "558"
             else "digid_hoog.request_unblock_letter.abort.invalid_#{suffix}"
      end
      Log.instrument(code, hidden: true)
    else
      log_suffix(suffix)
    end
  end

  def log_suffix(suffix)
    if ["suspended", "not_found"].include?(suffix)
      Log.instrument("11", hidden: true)
    elsif suffix == "invalid"
      Log.instrument("10", hidden: true)
    elsif suffix == "deceased"
      Log.instrument("13", hidden: true)
    elsif suffix == "emigrated"
      Log.instrument("14", hidden: true)
    elsif suffix == "investigate_address"
      Log.instrument("12", hidden: true)
    elsif suffix == "not_eer"
      Log.instrument("506", hidden: true)
    else
      Log.instrument("uc1.aanvraag_mislukt_#{suffix}", hidden: true)
    end
  end

  def build_registration
    if wrapped_session.timed_out?
      Registration.new
    elsif params[:registration].present?
      Registration.new(valid_registration_params_process_switch)
    else
      registration_reconstructed_from_session || Registration.new
    end
  end

  def registration_retrieved_from_session
    @registration_from_session ||= Registration.find_by(id: session[:registration_id])
  end

  def valid_registration_params_process_switch
    dummy_registration = Registration.new(
      burgerservicenummer: registration_params[:burgerservicenummer],
      geboortedatum_dag: registration_params[:geboortedatum_dag],
      geboortedatum_maand: registration_params[:geboortedatum_maand],
      geboortedatum_jaar: registration_params[:geboortedatum_jaar]
    )
    dummy_registration.valid?
    remembered_form_values_while_switching_process.reject { |field, _| dummy_registration.errors[field].present? }
    registration_params.reject do |field, _|
      dummy_registration.errors[field].present?
    end
  end

  def remembered_form_values_while_switching_process
    # result of reading from flash needs to be memoized in controller instance variable, flash can only be read once!
    @remembered_form_values_while_switching_process ||= (flash[:remembered_form_values_while_switching_process] || {})
  end

end
