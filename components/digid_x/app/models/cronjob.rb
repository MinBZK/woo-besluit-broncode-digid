
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

# DigiD 4.0 Cronjobs
#
# Usage:
# bundle exec rails runner -e <environment> Cronjob.new.clean_tables
# bundle exec rails runner -e <environment> Cronjob.new.clean_up_initial_accounts

class Cronjob
  BATCH_SIZE = 1000

  def clean_up_aselect
    clean_up_aselect_chunked(Aselect::Session)
    clean_up_aselect_chunked(Aselect::Request)
  end

  def clean_up_aselect_chunked(aselect_table, timestamp = 20.minutes.ago)
    clean_up_chunked(aselect_table.where("updated_at < ?", timestamp)) do |ids|
      sleep 1
    end
  end

  def clean_up_saml
    # While in mijn.digid the session/cookies stays alive. But the User doesn't update the federation/sp_session table anymore.
    # Theoretically the user could be acive until the max session time is reached
    mijn_digid_name = SamlProvider.where("entity_id = ?", "MijnDigid").first&.federation_name || "provider_1"
    inactivity_timeout = ::Configuration.get_int("session_expires_in_absolute").minutes.ago.utc
    # Saml sessions we can clean earlier
    # redis sessions and cookies have a TTL of APP_CONFIG["cache_expire_after"].
    # After a succesful authentication the Saml::SpSession and Saml::Federation get their updated_at field updated
    # Which means if the user at the last second of their 40 minute session timeout starts an new authentication and thus keeping their session alive;
    # We give that user 40 minutes to succesfully finish that authentication
    inactivity_timeout_saml = (APP_CONFIG["cache_expire_after"].to_i * 2).minutes.ago.utc


    # This also destroys the child Saml::SpSessions
    Saml::Federation.where(["updated_at < ?", inactivity_timeout_saml]).where("federation_name != ?", mijn_digid_name).delete_all
    Saml::Federation.where(["updated_at < ?", inactivity_timeout])
  end

  # verwijder niet afgemaakte aanvragen na het verlopen van 2 maal de sessie verlooptijd
  def clean_up_initial_accounts
    ids = Account.initial.stale.delete_with_associations
    Account.where(id: ids).delete_all # let's not call this on the scope
  end

  # verwijder niet geactiveerde aanvragen na het verlopen van de activatiecode (+ 1 dag safety)
  def clean_up_expired_activations
    ids = (Account.expired_activation.expired_password_tool.delete_with_associations + Account.expired_activation.expired_app.delete_with_associations)
    ids.each do |account_id|
      Log.instrument("352", account_id: account_id, hidden: true)
    end
    Account.where(id: ids).delete_all # let's not call this on the scope
  end

  # verwijder registraties waarvan
  # 0. status is NULL en sessie is verlopen of,
  def clean_up_null_registrations
    clean_up_registrations(Registration.status_null)
  end

  # 1. status is initieel en sessie is verlopen of,
  def clean_up_initial_registrations
    clean_up_registrations(Registration.initial.abandoned)
  end

  # 2. status is aangevraagd en langer dan 6 weken,
  def clean_up_aangevraagd_registrations
    clean_up_registrations(Registration.requested.expired)
  end

  # 3. status is afgebroken.
  def clean_up_afgebroken_registrations
    clean_up_registrations(Registration.aborted)
  end

  # 5. state of registration is completed and updated at is more than 6 weeks ago.
  def clean_up_completed_registrations
    clean_up_registrations(Registration.completed.expired)
  end

  def clean_up_web_registrations
    clean_up_registrations(WebRegistration.where("created_at < ?", 42.days.ago))
  end

  def clean_up_inactive_app_authenticators
    Authenticators::AppAuthenticator.inactive.where(["updated_at < ?", 1.day.ago]).delete_all
  end

  # Verwijder alle app authenticators waarvan de activatie code 1 dag is verlopen
  def clean_up_app_authenticators_with_expired_activationcode
    Authenticators::AppAuthenticator.expired.clean_up
  end

  # Verwijder alle sms tools waarvan de activatie code 1 dag is verlopen
  def clean_up_sms_tools_with_expired_activationcode
    Authenticators::SmsTool.expired.clean_up
  end

  # Verwijder alle user ghosts die niet meer relevant zijn
  def clean_up_user_ghosts
    UserGhost.expired.clean_up
  end

  def clean_up_email_deliveries
    EmailDelivery.where("created_at < ?", APP_CONFIG["email_delivery_clean_after"].day.ago).delete_all
  end

  # verwijder alle log entries die ouder zijn dan 18 maanden
  #
  def clean_up_logs
    clean_log(Log)
  end

  # verwijder alle account_log entries die ouder zijn dan 18 maanden
  #
  def clean_up_account_logs
    clean_log(AccountLog)
  end

  def clean_up_manager_logs
    clean_log(ManagerLog)
  end

  private

  def clean_up_registrations(relation)
    clean_up_chunked(relation) do |ids|
      ActivationLetter.where(registration_id: ids).delete_all
    end
  end

  def clean_up_chunked(relation)
    # Get klass of relation, if none, we have already the klass
    klass = relation.respond_to?(:klass) ? relation.klass : relation

    amount = relation.count
    chunks = amount / BATCH_SIZE + (amount % BATCH_SIZE == 0 ? 0 : 1) # for the last batch smaller than BATCH_SIZE
    chunks.times do
      ids = relation.limit(BATCH_SIZE).pluck(:id)
      yield ids
      klass.where(id: ids).delete_all
    end
  end

  def clean_log(log, timestamp = 18.months.ago)
    minmax = log.where("created_at < ?", timestamp).select("min(id) as min_id").select("max(id) as max_id")[0]
    min_id = minmax.min_id.to_i
    max_id = minmax.max_id.to_i
    return if max_id.zero?

    stepsize = 1000
    (min_id..max_id).step(stepsize) do |start_id|
      log.where("id >= ? AND id <= ? AND created_at < ?", start_id, start_id + stepsize - 1, timestamp).delete_all
      sleep(1.second)
    end
  end
end
