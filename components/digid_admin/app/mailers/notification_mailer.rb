
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

class NotificationMailer < ApplicationMailer
  default from: APP_CONFIG['mailer_default_email_address'] || "noreply@#{APP_CONFIG['hosts']['digid']}"

  # EB003 – Notificatiemail rijbewijs/identiteitskaart geblokkeerd
  def notify_block(account_id:, doc_type:)
    @document_type = doc_type
    account = Account.find(account_id)
    subject = "mailer.subject.notify_geblokkeerd_wid"
    subject_options = { document_type: doc_type }

    begin
      i18n_mail(account_id: account_id, to: account.adres, subject: subject, subject_options: subject_options)
     rescue
      Log.instrument("uc24.email_block_versturen_mislukt", account_id: account.id, account_transactie: true, mydigid: true, doc_type: t(doc_type))
     else
      Log.instrument("uc24.email_block_versturen_gelukt", account_id: account.id, account_transactie: true, mydigid: true, doc_type: t(doc_type))
    end

    SentEmail.create(account_id: account.id, reason: SentEmail::Reason::BLOCK_WID)
  end

  # EB004 – Notificatiemail rijbewijs deblokkeringscode aangevraagd
  def notify_deblock(account_id:, doc_type:)
    @document_type = doc_type
    account = Account.find(account_id)
    subject = "mailer.subject.notify_deblokkeringscode"
    subject_options = { document_type: doc_type }

    begin
      i18n_mail(account_id: account_id, to: account.adres, subject: subject, subject_options: subject_options)
     rescue
      Log.instrument("uc24.email_deblock_versturen_mislukt", account_id: account.id, account_transactie: true, mydigid: true, doc_type: t(doc_type))
     else
      Log.instrument("uc24.email_deblock_versturen_gelukt", account_id: account.id, account_transactie: true, mydigid: true, doc_type: t(doc_type))
    end

    SentEmail.create(account_id: account.id, reason: SentEmail::Reason::DEBLOCK_WID)
  end

  # EB005 – Notificatiemail rijbewijs pin aangevraagd
  def notify_pin(account_id:, doc_type:)
    @document_type = doc_type
    account = Account.find(account_id)
    subject = "mailer.subject.notify_pin_wid"
    subject_options = { document_type: doc_type }

    begin
      i18n_mail(account_id: account_id, to: account.adres, subject: subject, subject_options: subject_options)
     rescue
      Log.instrument("uc24.email_pin_versturen_mislukt", account_id: account.id, account_transactie: true, mydigid: true, doc_type: t(doc_type))
     else
      Log.instrument("1039", account_id: account.id, account_transactie: true, mydigid: true, doc_type: t(doc_type))
    end

    SentEmail.create(account_id: account.id, reason: SentEmail::Reason::PIN_WID)
  end
end
