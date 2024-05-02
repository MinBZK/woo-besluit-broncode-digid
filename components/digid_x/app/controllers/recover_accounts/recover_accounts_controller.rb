
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

module RecoverAccounts
  class RecoverAccountsController < ApplicationController
    before_action :check_session_time
    before_action :update_session
    before_action :check_account_type

    private

    def starting_point
      @starting_point ||= session[:recover_account_entry_point]
    end
    helper_method :starting_point

    def current_account
      @current_account ||= Account.find(session[:recovery_account_id]) || raise(SessionExpired)
    rescue ActiveRecord::RecordNotFound
      raise SessionExpired
    end
    helper_method :current_account

    # prevent tinkering with authentication levels during recovery
    def check_account_type
      return unless session[:recovery_account_type].present?
      recovery_account_type = current_account.sms_tools.active? ? "midden" : "basis"
      return if recovery_account_type.eql?(session[:recovery_account_type])

      raise SessionExpired
    end

    def recovery_account_type
      return session[:recovery_account_type] if session.key?(:recovery_account_type)
      return unless session.key?(:recovery_account_id)
      current_account.sms_tools.active? ? "midden" : "basis"
    end

    def recovery_by_letter?
      return session[:recovery_by_letter] if session.key?(:recovery_by_letter)
      return unless session.key?(:recovery_account_id)
      session[:recovery_by_letter] = current_account.recovery_codes.by_letter.not_used.not_expired.exists?
    end

    def recovery_by_choice?
      return session[:recovery_by_choice] if session.key?(:recovery_by_choice)
      false
    end

    def store_recovery_method
      @recovery_method = nil
      recovery_method
    end

    def recovery_method
      return @recovery_method if @recovery_method
      by_letter = recovery_by_letter? ? "per_brief" : nil
      by_choice = recovery_by_choice? ? "als_keuze" : nil
      starting_point_regex = /\A(.*?)(_code_invoeren_via_email)?\z/
      start = starting_point.match(starting_point_regex).to_a[1]
      method = [start, recovery_account_type, by_letter, by_choice].compact
      session[:recovery_method] = @recovery_method = method.join("_").to_sym
    end

    def add_flow_step(flow, step)
      if flow.index(step)
        flow[0..flow.index(step) - 1] + step
      else
        flow + step
      end
    end
  end
end
