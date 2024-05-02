
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

class VerificationsController < ApplicationController
  load_and_authorize_resource
  before_action :check_duration, except: %i[create new]
  around_action :set_time_zone, only: %i[show create update]

  def show
    @page_id = { false => 'BA.30', true => 'BA.50.1' }[verification_correction?]
    if ::Configuration.get_boolean "balie_aanvraag_voor_rni"
      flash.now[:notice] = t('info_activation_code')
      @activation_form = ActivationForm.new
      render :show_rni
    else
      front_desk_log('front_desk_activation_letter_shown', account_id: verification.front_desk_account_id)
    end
  end

  def show_letter
    pdf = Pdf.generate_letter(verification)
    send_data pdf.render, filename: 'activerings_brief.pdf', type: 'application/pdf'
  end

  def sms_received
    @activation_form = ActivationForm.new(sms_activation_params)
    if @activation_form.submit
      @sms_received_form = SmsReceivedForm.new
      response = iapi_x_client.post("/iapi/sms", sms: {
        gateway: verification.sms_gateway(@activation_form.spoken == "true" ? "spoken" : "regular"),
        timeout: ::Configuration.get_string_without_cache('sms_timeout'),
        sender: "DigiD",
        account_id: verification.front_desk_account_id,
        message: verification.activation_code,
        code_gid: nil,
        spoken: sms_activation_params[:spoken]
      })
      front_desk_log('1585', account_id: verification.front_desk_account_id)
    else
      render :show_rni
    end
  end

  def finish
    @sms_received_form = SmsReceivedForm.new(sms_received_params)
    if !@sms_received_form.submit
      render :sms_received
    elsif @sms_received_form.send_received?
      front_desk_log('1587', account_id: verification.front_desk_account_id)
      redirect_to root_path
    else
      front_desk_log('1586', account_id: verification.front_desk_account_id)
      redirect_to verification_path
    end
  end

  def new
    @form = CreateVerificationForm.new
    @news_items = FrontDeskNewsItem.find_all('Balie Home')
    @page_id = 'BA.10'
  end

  # rubocop:disable MethodLength
  def create
    front_desk_log('front_desk_start_activation_process')
    @form = CreateVerificationForm.new(create_verification_params)

    unless @form.valid?
      front_desk_log('front_desk_id_check_empty_fields')
      @news_items = FrontDeskNewsItem.find_all('Balie Home')
      @page_id = 'BA.10'
      return render :new
    end

    result = StartVerification.call(create_verification_params.merge(current_front_desk: current_front_desk))

    if result.failure?
      front_desk_log(result.fail_reason)
      case result.fail_reason
      when :front_desk_code_already_used
        flash[:alert] = t(result.fail_reason)
        front_desk_log('426', account_id: verification.front_desk_account_id)
        return redirect_to action: :new
      when :front_desk_maximum_per_day_reached
        flash.now[:alert] = t(result.fail_reason)
        @news_items = FrontDeskNewsItem.find_all('Balie Home')
        @page_id = 'BA.10'
        return render :new
      end
    end

    response = iapi_x_client.post("/iapi/front_desk_registrations/search", front_desk_registration: {
      baliecode: create_verification_params[:front_desk_code],
      burgerservicenummer: create_verification_params[:citizen_service_number],
      status: 'requested'
    })
    front_desk_registration = HashWithIndifferentAccess.new(JSON.parse(response.body))

    if !front_desk_registration[:front_desk_registration]
      flash.now[:notice] = t('front_desk_code_not_found')
      front_desk_log('front_desk_code_combination_invalid')
      @news_items = FrontDeskNewsItem.find_all('Balie Home')
      @page_id = 'BA.10'
      render :new
    else

      verification = Verification.new(front_desk_registration[:front_desk_registration])

      if verification.front_desk_code_expires_at < Time.zone.now
        flash[:alert] = t(
          'front_desk_code_is_expired',
          created_at: l(verification.front_desk_registration_created_at, format: :date_at_time_hour),
          days: verification.expires_in
        )
        front_desk_log('front_desk_code_no_longer_valid', account_id: verification.front_desk_account_id)
        redirect_to action: :new
      else
        front_desk_log('front_desk_start_activation_process_session', account_id: verification.front_desk_account_id)
        verification.assign_attributes(user: current_user, front_desk: current_front_desk)
        verification.save!

        redirect_to edit_verification_path(verification)
      end
    end
  end

  def edit
    @form = EditVerificationForm.new(verification: verification)
    @page_id = { false => 'BA.20', true => 'BA.40.1' }[verification_correction?]
  end

  # OPTIMIZE: refactor nested if branches
  def update
    @form = EditVerificationForm.new(edit_verification_params.merge(verification: verification))
    result = MaximumIssues.call(current_front_desk: current_front_desk)

    if params[:correct]
      @form.submit!
      return redirect_to new_verification_verification_correction_path(verification)
    end

    if result.failure?
      front_desk_log(result.fail_reason)
      flash.now[:alert] = t(result.fail_reason)
      @page_id = { false => 'BA.20', true => 'BA.40.1' }[verification_correction?]
      return render :edit
    end

    if @form.submit

      if verification.id_established? && 
        (
          (!::Configuration.get_boolean("balie_aanvraag_voor_rni") && !verification.id_signaled?) ||
          ::Configuration.get_boolean("balie_aanvraag_voor_rni")
        )

        unless ::Configuration.get_boolean("balie_aanvraag_voor_rni")
          if id_number_already_in_use?
            @form.errors.add(:id_number, t('id_number_already_in_use'))
            front_desk_log('front_desk_id_check_fail_not_unique_for_citizen_service_number', account_id: verification.front_desk_account_id)
            @page_id = { false => 'BA.20', true => 'BA.40.1' }[verification_correction?]
            return render :edit
          end
        end

        response = iapi_x_client.post("/iapi/front_desk_registrations/update", front_desk_registration: {
            front_desk_registration_id: verification.front_desk_registration_id,
            front_desk_id: verification.front_desk_id
        })

        if response.code == 200

          verification.update(activated_at: Time.zone.now, state: Verification::State::VERIFIED)

          front_desk_log('front_desk_activation_code_activated', account_id: verification.front_desk_account_id)
          front_desk_log('431', account_id: verification.front_desk_account_id)
          front_desk_log('front_desk_id_check_successful', account_id: verification.front_desk_account_id)

          redirect_to verification_path(verification)

        end
      else
        options = {
          pop_up_text: t(::Configuration.get_boolean("balie_aanvraag_voor_rni") ? "you_signalled_that_identity_could_not_be_established" : "you_signalled_that_identity_could_not_be_established_or_was_signaled"),
          ok_button_to: reject_verification_path(verification),
          cancel_button_to: edit_verification_path(verification)
        }

          render_pop_up(options)
      end
    else
      if verification.errors[:front_desk_code].empty?
        front_desk_log('front_desk_id_check_empty_fields', account_id: verification.front_desk_account_id)
      else
        flash[:alert] = t('front_desk_code_already_used')
        front_desk_log('426', account_id: verification.front_desk_account_id)
      end
      @page_id = { false => 'BA.20', true => 'BA.40.1' }[verification_correction?]
      render :edit
    end
  end

  def destroy
    front_desk_log('front_desk_activation_process_cancelled', account_id: verification.front_desk_account_id)
    verification.destroy

    redirect_to root_path
  end

  def reject
    verification.update(state: Verification::State::REJECTED)
    if verification.id_signaled
      front_desk_log('front_desk_id_check_id_signaled', account_id: verification.front_desk_account_id)
    end
    unless verification.id_established
      front_desk_log('front_desk_id_check_cancelled', account_id: verification.front_desk_account_id)
    end
    redirect_to root_path
  end

  private

  def id_number_already_in_use?
    Verification.activated.where(id_number: verification.id_number, nationality: verification.nationality).where.not(citizen_service_number: verification.citizen_service_number).present?
  end

  def create_verification_params
    params.require(:create_verification_form).permit(:citizen_service_number, :front_desk_code)
  end

  def edit_verification_params
    params.require(:edit_verification_form).permit(:id_established, :id_signaled, :suspected_fraud)
  end

  def check_duration
    return if verification.created_at >= 60.minutes.ago

    flash[:alert] = t('verification_process_too_slow')
    redirect_to root_path
  end

  def verification
    @verification ||= Verification.find(params[:id])
  end

  def verification_correction?
    verification = Verification.find_by_id(params[:id])
    return false unless verification
    session[:verification_correction_for] == verification.id
  end

  def iapi_x_client
    @iapi_x_client ||= DigidUtils::Iapi::Client.new(url: APP_CONFIG['urls']['internal']['x'] , timeout: 15)
  end

  def sms_received_params
    params.require(:sms_received_form).permit(:sms_received)
  end

  def sms_activation_params
    params.require(:activation_form).permit(:spoken)
  end

end
