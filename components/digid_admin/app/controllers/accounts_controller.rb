
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

class AccountsController < ApplicationController
  include Concerns::Gba
  load_and_authorize_resource except: [:view_gba_popup]

  prepend_before_action :verified_request?, only: [:gba_request, :last_activation_data]

  # GET /accounts
  def index
    @search       = Account.ransack(params[:q])
    if params[:q] && params[:q].values.any? { |q| !q.empty? && !q.match(/\s/) }
      instrument_logger("1470", query: params[:q].values.join(','), manager_id: session[:current_user_id])
      @accounts = @search.result(distinct: true).page(params[:page]).includes(:sectorcodes)
    else
      @accounts = []
    end

    instrument_logger('uc16.account_overzicht_inzien_gelukt')
  end

  # GET /accounts/1
  def show
    current_user.account_viewed(@account)
    registrations = Registration.where(burgerservicenummer: @account.sectorcodes.first.sectoraalnummer).requested.all
    @letters = ActivationLetter.all.where(status: %w(finished sent), registration_id: registrations)
    @briefcode = LetterBuilder::BRIEFCODE_MAPPING
    instrument_logger('uc16.account_inzien_gelukt', account_id: @account.id)
  end

  # POST /accounts/1
  def suspend
    if @account.state.suspended?
      instrument_logger('uc16.account_opschorten_ongedaan_maken_gelukt', account_id: @account.id)
      @account.update_attribute(:status, ::Account::Status::ACTIVE)
    elsif @account.state.active?
      instrument_logger('uc16.account_opschorten_gelukt', account_id: @account.id)
      @account.update_attribute(:status, ::Account::Status::SUSPENDED)
    end
    redirect_to account_path, notice: t(@account.reload.state, scope: 'accounts.notifications.state_changed')
  end

  # POST /accounts/1/sector_code
  def convert_sofi
    sectoraalnummer = @account.sectorcodes.first.sectoraalnummer
    gba_data        = gba_data(sectoraalnummer)
    case gba_data['status']
    when 'valid', 'emigrated', 'rni'
      # TODO: cleanup or remove add_a_nummer? method
      if @account.add_a_nummer?(gba_data['010110'])
        @account.convert_sofi_to_bsn
        instrument_logger('uc16.account_omzetten_sofi_bsn_gelukt', account_id: @account.id)
        redirect_to account_path, notice: I18n.t('accounts.notifications.sofi_set_to_bsn')
      else
        redirect_to account_path, alert: I18n.t('accounts.notifications.sofi_not_set_to_bsn_anummer')
      end
    else
      redirect_to account_path, alert: I18n.t('accounts.notifications.sofi_not_set_to_bsn')
    end
  end

  def block_account_replace
    replace_blocked = params[:commit]&.exclude?("ongedaan")

    @account.update(replace_account_blocked: replace_blocked)
    instrument_logger("1557", type: replace_blocked ? "blokkeren" : "blokkeren ongedaan maken", account_id: @account.id)

    redirect_to(account_path)
  end

  # DELETE /accounts/1
  def destroy
    account = Account.find(params[:id])
    # Log first
    if account.state.suspended?
      instrument_logger('uc16.opgeschort_account_verwijderen_gelukt', account_id: account.id)
    else
      instrument_logger('uc16.account_verwijderen_gelukt', account_id: account.id)
    end
    # Then destroy account
    account.destroy
    redirect_to accounts_path, notice: I18n.t('accounts.notifications.account_deleted'), deleted: true
  end

  # GET /accounts/:id/gba_request
  # Do a request to the GBA en receive data for given BSN
  def gba_request
    if request.format.html?
      head 404
    else
      @zoek     = @account.sectorcodes.first.sectoraalnummer
      @gba_data = gba_data(@zoek)
      if @gba_data['status'].eql?('not_found')
        instrument_logger('uc17.gba_raadplegen_gelukt_no_match', manager_id: session[:current_user_id], account_id: @account.id)
      elsif @gba_data
        instrument_logger('uc17.gba_raadplegen_gelukt', manager_id: session[:current_user_id], account_id: @account.id)
        instrument_logger('uc17.gba_geen_anummer', manager_id: session[:current_user_id], account_id: @account.id) unless @gba_data['010110'].present?
      end
    end

    if request.xhr?
      if !@gba_data || @gba_data['status'].eql?('not_found')
        render json: { target: "#brp_error", body:  not_found_json_body }
      else
        render json: { target: "#brp_response", body:  "#{render_to_string(partial: 'accounts/gba_request_form')}" }
      end
    end

  rescue => e
    Rails.logger.error e.message
    Rails.logger.error e.backtrace.first(3).join("\n")
    instrument_logger('uc17.gba_raadplegen_mislukt_onbereikbaar', manager_id: session[:current_user_id], account_id: @account.id)

    render json: { target: "#brp_error", body:  not_found_json_body }
  end

  # GET /view_gba_popup
  # Do a request to the GBA en receive data for given BSN
  def view_gba_popup
    return unless can?(:view_gba_status, Account)
    begin
      @zoek     = params[:nummer]
      @gba_data = gba_data(@zoek)
      @account_id = Sectorcode.find_by(sectoraalnummer: @zoek)&.account_id

      if @gba_data['status'].eql?('not_found')
        instrument_logger('uc17.gba_raadplegen_gelukt_no_match', manager_id: session[:current_user_id], account_id: @account_id)
      else
        instrument_logger('uc17.gba_raadplegen_gelukt', manager_id: session[:current_user_id], account_id: @account_id)
        instrument_logger('uc17.gba_geen_anummer', manager_id: session[:current_user_id], account_id: @account_id) unless @gba_data['010110'].present?
      end

      if !@gba_data || @gba_data['status'].eql?('not_found')
        render json: {
          dialog_body: not_found_json_body,
          dialog_title: t('brp_data_of_bsn', bsn: @zoek)
        }
      else
        render json: {
          dialog_body: render_to_string(partial: "accounts/gba_request_form"),
          dialog_title: t('brp_data_of_bsn', bsn: @zoek)
        }
      end
    rescue => e
      Rails.logger.error e.message
      Rails.logger.error e.backtrace.first(3).join("\n")
      instrument_logger('uc17.gba_raadplegen_mislukt_onbereikbaar', manager_id: session[:current_user_id], account_id: @account_id)

      render json: {
        dialog_body: not_found_json_body,
        dialog_title: t('brp_data_of_bsn', bsn: @zoek)
      }
    end
  end

  # GET /accounts/:id/last_activation_data
  # Get last letter data from an account
  def last_activation_data
    sectoraalnummer = @account.sectorcodes.first.sectoraalnummer
    registration    = Registration.where(burgerservicenummer: sectoraalnummer).requested.first
    letters         = registration && ActivationLetter.where(id: params[:letter_id])
    if letters.present?
      first_letter        = letters.first
      @geldigheidstermijn = first_letter.geldigheidsduur.to_i.days
      @vervaltijd         = first_letter.created_at + @geldigheidstermijn
      letters_xml         = LetterBuilder.new(APP_CONFIG["csv_codes_file"]&.path).create_xml_from(letters)
      @letters_hash       = Hash.from_xml(letters_xml.xml)
      @baliecode          = first_letter.registration.baliecode
      @balie_expire_date  = first_letter.registration.created_at + ::Configuration.get_int("balie_default_geldigheidsduur_baliecode").days
    end
    instrument_logger('963', briefcode: LetterBuilder::BRIEFCODE_MAPPING[first_letter.letter_type], aanvraagdatum: I18n.l(first_letter.created_at), account_id: @account.id)

    if request.xhr?
      render json: { dialog_content: render_to_string(partial: "last_activation_data"), title: "Gegevens brief" }
    else
      render :last_activation_data
    end
  end

  # GET /accounts/:id/generate_pdf_letter
  def generate_pdf_letter
    letters = ActivationLetter.where(id: params[:letter_id])
    pdf = Pdf.new letters: letters, locale: @account.locale

    instrument_logger('1174', briefcode: LetterBuilder::BRIEFCODE_MAPPING[letters.first.letter_type], aanvraagdatum: I18n.l(letters.first.created_at), account_id: @account.id)
    send_data pdf.render, filename: letters.first.letter_type + '_copy.pdf', type: 'application/pdf'
  end

  def resend_letter_registered
    letter = ActivationLetter.where(id: params[:letter_id])&.first.dup
    letter.aangetekend = true
    letter.status = ActivationLetter::Status::FINISHED
    letter.save

    instrument_logger('1580', briefcode: LetterBuilder::BRIEFCODE_MAPPING[letter.letter_type], aanvraagdatum: I18n.l(letter.created_at), account_id: letter.account&.id)

    redirect_to(account_path, notice: "Brief is aangetekend verstuurd")
  end

  # GET /accounts/:id/histories
  def histories
    @account_histories  = @account.account_histories.page(params[:account_histories_page])

    if request.xhr?
      render json: {
        dialog_content: render_to_string(partial: "histories"), title: "Historische Gegevens (#{@account.gebruikersnaam}/#{@account.bsn})"
      }
    else
      render :histories
    end
  end

  # GET /accounts/:id/sent_emails
  def sent_emails
    @account_emails  = @account.sent_emails.page(params[:account_emails_page])

    if request.xhr?
      render json: {
        dialog_content: render_to_string(partial: "sent_emails"), title: "Verzonden e-mails (#{@account.gebruikersnaam}/#{@account.bsn})"
      }
    else
      render :sent_emails
    end
  end

  private

  def not_found_json_body
    ActionController::Base.helpers.content_tag(:div, ActionController::Base.helpers.content_tag(:span, t('not_found')), id: 'accounts')
  end
end
