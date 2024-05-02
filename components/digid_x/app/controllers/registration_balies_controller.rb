
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

class RegistrationBaliesController < RegistrationBaseController
  before_action :cancel_registration?,  only: [:create]
  before_action :check_session_time,    except: [:new]
  before_action :update_session,        except: [:new]

  def new
    @registration = build_registration_balie
    start_session "registration"
    balie_session!
    Log.instrument("422")
    @news_items = news_items("Aanvraag Balie")
    @page_name = ::Configuration.get_boolean("balie_aanvraag_voor_rni") ? "A1E" : "A1B"

    render_partial_or_return_json("#registration-form", :form, :new)
  end

  # POST /registrations_balie
  # try to save a submitted form;
  # if validation errors -> render the form again + error msgs
  # if ok move on to GBA
  def create
    @news_items = news_items("Aanvraag Balie")
    @registration = RegistrationBalie.new(registration_balie_params)

    if @registration.invalid?
      @registration.gba_status = nil
      Log.instrument("474")
      @page_name = ::Configuration.get_boolean("balie_aanvraag_voor_rni") ? "A1E" : "A1B"
      render_partial_or_return_json("#registration-form", :form, :new)
    elsif show_warning_less_than_one_month_left?
      flash.now[:notice] = t("valid_until_is_less_than_one_month_away", valid_until: l(@registration.valid_until, format: :date_european), valid_until_min_1: l(@registration.valid_until - 1.day, format: :date_european)).html_safe
      flash[:remembered_form_values_while_switching_process] = registration_balie_params
      @registration_unsuccessful = true
      session[:registration_valid] = true
      render_message(button_to: save_registration_url(registration_balie: registration_balie_params),
                     button_to_options: { remote: true },
                     cancel_to: new_balie_registration_url,
                    )
    else
      session[:registration_valid] = true
      save_registration
    end
  end

  def save_registration
    redirect_via_js_or_html(new_balie_registration_url) unless session[:registration_valid]
    session.delete(:registration_valid)
    current_flow.transition_to!(:registration_verified)
    @registration = RegistrationBalie.new(registration_balie_params)
    @registration.gba_status = "request"
    @registration.baliecode = if Rails.application.config.performance_mode
                                "B" + @registration.burgerservicenummer[0..7]
                              else
                                VerificationCode.generate("B")
                              end
    Log.instrument("450", registration_id: @registration.id)

    if @registration.save

      Log.instrument("504", registration_id: @registration.id)
      Log.instrument("6", registration_id: @registration.id, hidden: true)

      if @registration.no_gba_info_leakage?
        # since we were able to save a registration,
        # the user entered valid data to do a GBA check
        # so we must show screen "message/index" with a timer and
        # a msg "please hold on", and do a call to GBA using DelayedJob
        # then go to any of the following screen according to the GBA outcome.
        session[:registration_id] = @registration.id
        # REFACTOR - magic "registration" string
        registered_successful("registration_balie", cancel_label: :cancel_back_to_home)
      else
        errors = @registration.errors
        @registration = RegistrationBalie.new
        @registration.errors.add(:leakage, errors[:leakage])
        @page_name = ::Configuration.get_boolean("balie_aanvraag_voor_rni") ? "A1E" : "A1B"
        render_partial_or_return_json("#registration-form", :form, :new)
      end
    else
      render_partial_or_return_json("#registration-form", :form, :new)
    end
  end

  private
  def show_warning_less_than_one_month_left?
    ::Configuration.get_int("minimale_geldigheid_ID").days.from_now > @registration.valid_until if @registration.valid_until
  end

  def cancel_registration?
    cancel_button(return_to: new_balie_registration_url) if clicked_cancel?
  end

  def build_registration_balie
    if redirected_from_regular_aanvraag_proces_with_form_values_to_remember?
      RegistrationBalie.new(valid_remembered_form_values_while_switching_process)
    elsif params[:registration_balie].present?
      RegistrationBalie.new(valid_registration_params_process_switch)
    else
      registration_reconstructed_from_session || RegistrationBalie.new
    end
  end

  def redirected_from_regular_aanvraag_proces_with_form_values_to_remember?
    remembered_form_values_while_switching_process.present?
  end

  def remembered_form_values_while_switching_process
    # result of reading from flash needs to be memoized in controller instance variable, flash can only be read once!
    @remembered_form_values_while_switching_process ||= (flash[:remembered_form_values_while_switching_process] || {})
  end

  def valid_remembered_form_values_while_switching_process
    # according to specs we only remember valid fields from the previous form
    # so test validity on a dummy registration object and remove form values that cause errors
    # on the corresponding fields
    dummy_registration = RegistrationBalie.new(remembered_form_values_while_switching_process.permit!)
    remembered_form_values_while_switching_process.reject { |field, _| dummy_registration.errors[field].present? }
  end

  def valid_registration_params_process_switch
    dummy_registration = RegistrationBalie.new(
      burgerservicenummer: registration_balie_params[:burgerservicenummer],
      geboortedatum_dag: registration_balie_params[:geboortedatum_dag],
      geboortedatum_maand: registration_balie_params[:geboortedatum_maand],
      geboortedatum_jaar: registration_balie_params[:geboortedatum_jaar]
    )
    dummy_registration.valid?
    remembered_form_values_while_switching_process.reject { |field, _| dummy_registration.errors[field].present? }
    registration_balie_params.reject do |field, _|
      dummy_registration.errors[field].present?
    end
  end

  def registration_balie_params
    params.require(:registration_balie).permit(
      :burgerservicenummer,
      :geboortedatum_dag, :geboortedatum_maand, :geboortedatum_jaar,
      :nationality_id, :id_number,
      :valid_until_day, :valid_until_month, :valid_until_year
    )
  end

  def registration_retrieved_from_session
    @registration_balie_from_session ||= RegistrationBalie.find_by(id: session[:registration_id])
  end
end
