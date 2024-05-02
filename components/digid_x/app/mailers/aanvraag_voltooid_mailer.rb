
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

# mailer for confirm email at the end of a registration
class AanvraagVoltooidMailer < ApplicationMailer

  # mail the user a confirmation email (ED003/ED018)
  def aanvraag_voltooid(kwargs = {})
    account_id = kwargs[:account_id]
    recipient = kwargs[:recipient]
    beveiligde_bezorging = kwargs[:beveiligde_bezorging] || false

    @body = beveiligde_bezorging ? "email.body.aanvraag_voltooid_beveiligde_postcode" : "email.body.aanvraag_voltooid"

    begin
      i18n_mail(account_id: account_id, to: recipient, subject: "email.subject.aanvraag_voltooid")
    rescue
      Log.instrument("338", account_id: account_id)
    else
      Log.instrument("337", account_id: account_id)
    end

    SentEmail.create(account_id: account_id, reason: beveiligde_bezorging ? ::SentEmail::Reason::NOTIFY_SECURE_REQUEST : ::SentEmail::Reason::REGISTRATION)
  end

  # mail the user a confirmation email (ED028)
  def aanvraag_voltooid_svb(kwargs = {})
    account_id = kwargs[:account_id]
    recipient = kwargs[:recipient]
    beveiligde_bezorging = kwargs[:beveiligde_bezorging] || false
    @body = beveiligde_bezorging ? "email.body.aanvraag_voltooid_beveiligde_postcode" : "email.body.aanvraag_voltooid_svb"

    begin
      i18n_mail(account_id: account_id, to: recipient, subject: "email.subject.aanvraag_voltooid")
    rescue
      Log.instrument("338", account_id: account_id)
    else
      Log.instrument("337", account_id: account_id)
    end

    SentEmail.create(account_id: account_id, reason: beveiligde_bezorging ? ::SentEmail::Reason::NOTIFY_SECURE_REQUEST : ::SentEmail::Reason::REGISTRATION_SVB)
  end

  # mail the user a confirmation email (EDB001)
  def aanvraag_voltooid_buitenland(kwargs = {})
    account_id = kwargs[:account_id]
    recipient = kwargs[:recipient]
    baliecode = kwargs[:baliecode]
    @code = baliecode

    begin
      i18n_mail(account_id: account_id, to: recipient, subject: "email.subject.aanvraag_voltooid")
    rescue
      Log.instrument("411", account_id: account_id)
    else
      Log.instrument("410", account_id: account_id)
    end
    SentEmail.create(account_id: account_id, reason: ::SentEmail::Reason::REGISTRATION_BALIE)
  end

  # Bevestigingsmail aanvraag KOOP (ED012)
  # Deze brief mag niet in het engels
  def aanvraag_voltooid_oep(kwargs = {})
    account_id = kwargs[:account_id]
    recipient = kwargs[:recipient]

    begin
      mail(to: recipient, subject: t("email.subject.oep_request_completed"))
    rescue
      Log.instrument("338", account_id: account_id)
    else
      Log.instrument("337", account_id: account_id)
    end
    SentEmail.create(account_id: account_id, reason: ::SentEmail::Reason::REGISTRATION_OEP)
  end
end
