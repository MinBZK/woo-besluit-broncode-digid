
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

module LogConcern
  extend ActiveSupport::Concern

  def log_webservice_authentication_succeed(options)
    log_webservice_authentication(options, :success)
  end

  def log_webservice_authentication_error(options)
    log_webservice_authentication(options, :fail)
  end

  def log_fields(account)
    if account
      level = if account.substantieel_aanvraag?
                "substantieel"
              elsif account.midden_aanvraag?
                "midden"
              else
                "basis"
              end
      [account.password_authenticator&.username || "-", level].join(", ")
    end
  end

  private

  def log_webservice_authentication(options, status)
    account_id = if options[:aselect]
                   account = Account.active.with_bsn(options[:aselect].sectoraal_nummer).first
                   account ||= Account.active.with_oeb(options[:aselect].sectoraal_nummer).first
                   account&.id
                 elsif current_account
                   current_account.id
                 elsif options[:sp_session]
                   options[:sp_session].federation.account_id
    end

    level_number = if options[:sp_session]
                     Saml::Config.authn_context_levels.find { |_k, v| v == options[:sp_session].federation.authn_context_level }&.first
                   elsif options[:aselect]
                     options[:aselect].betrouwbaarheidsniveau
    end

    level = Account::LEVELS[level_number]
    webservice = if options[:sp_session]
                   options[:sp_session].provider.webservice
                 elsif options[:aselect]
                   options[:aselect].webservice.webservice
    end

    if level.present?
      Log.instrument(log_mapping[level.to_sym][status],
                     webservice_name: webservice&.name, webservice_id: webservice&.id, account_id: account_id, hidden: true)
    end
  end

  def log_mapping
    { basis: { success: "1016", fail: "1017" },
      midden: { success: "1018", fail: "1019" },
      substantieel: { success: "1022", fail: "1023" },
      hoog: { success: "1024", fail: "1025" } }
  end
end
