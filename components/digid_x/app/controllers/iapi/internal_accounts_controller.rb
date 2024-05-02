
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

module Iapi
    class InternalAccountsController < ApplicationController
      skip_before_action :verify_authenticity_token
      authorize_with_token :iapi_token

      def account_logs
        @account_id = params[:account_id]

        log_action(t("process_names.log.get_logs", locale: :nl), params[:app_code], params[:device_name]) if params[:page_id].to_i.zero?

        subquery = Sectorcode.select(:sectoraalnummer).where(account_id: @account_id).map(&:sectoraalnummer)
        all_account_ids = Sectorcode.select("DISTINCT account_id").where(sectoraalnummer: subquery).map(&:account_id)
        all_account_ids << SectorcodesHistory.select("DISTINCT account_id").where(sectoraalnummer: subquery).map(&:account_id)
        all_account_ids = all_account_ids.flatten.uniq

        page_size = params[:page_size].presence&.to_i || logs_per_page
        transactions_scope = params[:query].present? && !APP_CONFIG["skip_log_search"] ? AccountLog.history_search(all_account_ids, params[:query]) : AccountLog.history(all_account_ids)

        @transactions = transactions_scope.page(params[:page_id].to_i + 1).per(page_size)

        render json: {
          status: "OK",
          results: @transactions,
          total_items: transactions_scope.size,
          total_pages: (transactions_scope.size / page_size.to_f).ceil
        }
      end


      def account_data
        @account = Account.find(params[:account_id])

        render json: {
          status: "OK",
          email_status: (@account.email&.status || "NONE").upcase,
          current_email_address: @account.email&.adres,
          setting_2_factor: @account.login_level_two_factor?,
          classified_deceased: @account.deceased?,
          app_only: @account.password_authenticator.nil?
        }
      end

      def two_factor_status
        @account = Account.find(params[:account_id])
        log_action(t("process_names.log.get_two_factor", locale: :nl), params[:app_code], params[:device_name])

        unless @account.password_authenticator_active?
          return render(json: { status: "NOK", error: "no_username_password"}, status: 200)
        end

        render(json: { status: "OK", setting_2_factor: @account.zekerheidsniveau.to_i >= ::Account::LoginLevel::TWO_FACTOR }, status: 200)
      end

      def change_two_factor
        @account = Account.find(params[:account_id])

        two_factor = params[:setting_2_factor].to_s == "true"

        Log.instrument("502", account_id: @account.id)

        unless setting_changed?(two_factor, @account.login_level_two_factor?)
          Log.instrument("573", account_id: @account.id)
          return render(json: { status: "OK" }, status: 200)
        end

        @account.update!(zekerheidsniveau: two_factor ? ::Account::LoginLevel::TWO_FACTOR : ::Account::LoginLevel::PASSWORD,
                        last_change_security_level_at: Time.zone.now)

        Log.instrument("618", account_id: @account.id, active: two_factor ? "Actief" : "Niet actief")
        render(json: { status: "OK" }, status: 200)
      end

      def email_status
        @account = Account.find(params[:account_id])
        if false || !::Configuration.get_boolean("update_email_address_in_authentication_process")
            return render(json: {status: "OK", email_status: "NONE", user_action_needed: FALSE}, status: 200)
        end

        status = @account&.email&.status
        status ||= "NONE"

        result = { status: "OK", email_status: status.to_s.upcase, email_address: @account&.email&.adres, user_action_needed:  user_action_required? }

        render(json: result, status: 200)
      end

      def email_register
        @account = Account.find(params[:account_id])
        new_email_address = params[:email_address].to_s.strip.downcase

        if new_email_address.empty? # user skips email
          @account.touch(:email_requested)
          return render(json: { status: "OK" }, status: 200)
        end

        new_email = Email.new(adres: new_email_address)

        if new_email_address == @account.email&.adres && @account&.email&.status == ::Email::Status::NOT_VERIFIED
          return send_notification
        elsif new_email_address == @account.email&.adres && @account&.email&.status == ::Email::Status::VERIFIED
          return render(json: { status: "NOK", error: "email_already_verified" }, status: 200)
        end

        return render_invalid_email(new_email) unless new_email.valid?

        @account.email = new_email
        @account.save

        send_notification
      end

      def email_verify
        @account = Account.find(params[:account_id])
        verification_code = params[:verification_code].to_s

        Log.instrument("139", account_id: @account.id)

        if @account.email.reached_attempts_limit?
          return render(json: { status: "NOK", error: "code_blocked" }, status: 200)
        end


        unless EmailCode.new(account: @account, code: verification_code).valid?
          if @account.email.expired?
            Log.instrument("557", account_id: @account.id)
            return render(json: { status: "NOK", error: "code_invalid" }, status: 200)
          end
          remaining_attempts = @account.email.max_number_of_failed_attempts - @account.email.attempts.count
          # code_incorrect
          return render(json: { status: "NOK", error: "code_incorrect", remaining_attempts: remaining_attempts }, status: 200)
        end

        @account.email.update(status: ::Email::Status::VERIFIED, confirmed_at: Time.zone.now)
        Log.instrument("145", account_id: @account.id)

        # success
        render(json: { status: "OK" }, status: 200)
      end

      def email_confirm
        @account = Account.find(params[:account_id])
        if params[:email_address_confirmed].to_s.downcase == "true"
          @account.email.touch(:confirmed_at)
          Log.instrument("1401", account_id: @account.id)
        else
          Log.instrument("1402", account_id: @account.id)
        end

        render(json: { status: "OK" }, status: 200)
      end

      private

      def log_action(process_name, app_code = "", device_name = "")
        Log.instrument("1468",
          human_process: process_name,
          account_id: @account_id || @account.id,
          app_code: app_code,
          device_name: device_name
        )
      end

      def setting_changed?(two_factor, two_factor_active)
        two_factor != two_factor_active
      end

      def logs_per_page
        ::Configuration.get_int("maximum_number_of_logs_DigiD_app")
      end

      def register_again_threshold
        ::Configuration.get_int("maximum_period_unregistered_email").months.ago
      end

      def user_action_required?
        action_required = (( @account.email_activated? && @account.email.confirmation_expired? ) ||
        ( !@account.email_activated? && (@account.email_requested.to_i < register_again_threshold.to_i ))) &&
        !@account.deceased?

        Log.instrument("1400", account_id: @account&.id) if action_required
        action_required
      end

      def render_invalid_email(email)
        if email.email_adress_maximum?
          # email_address_maximum_reached
          render(json: { status: "NOK", error: "email_address_maximum_reached" }, status: 200)
        else
          # invalid_email_address
          render(json: { status: "NOK", error: "invalid_email_address" }, status: 200)
        end
      end

      def send_notification
        max_amount_emails = ::Configuration.get_int("aantal_controle_emails_per_dag")
        if @account.max_emails_per_day?(max_amount_emails)
          # too_many_emails
          return render(json: { status: "NOK", error: "too_many_emails", max_amount_emails: max_amount_emails }, status: 200)
        end

        EmailControlCodeMailer.new(@account, "app").perform

        # succes_verification_code_send
        render(json: { status: "OK", email_address: @account.email.adres }, status: 200)
      end

    end
  end
