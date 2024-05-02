
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

class NotificatieMailer < ApplicationMailer

  def send_by_email_ref(kwargs = {})
    email_ref = kwargs[:email_ref]
    account_id = kwargs[:account_id]
    recipient = kwargs[:recipient]

    if respond_to?(email_ref)
      send(email_ref, account_id: account_id, recipient: recipient)
    else
      Rails.logger.error("Unknown email ref #{email_ref} #{e.message}")
    end
  end


  # ED007 - Notificatiemail wachtwoord wijziging
  def notify_wachtwoord_wijziging(kwargs = {})
    account_id = kwargs[:account_id]
    recipient = kwargs[:recipient]

    notify_email(
      template: __method__,
      account_id: account_id,
      subject: "email.subject.notify_wachtwoord_wijziging",
      recipient: recipient,
      fail_log: "409",
      success_log: "408",
      reason: ::SentEmail::Reason::NOTIFY_PASSWORD_CHANGE
    )
  end

  # ED008 - Notificatiemail email wijziging
  def notify_email_wijziging(kwargs = {})
    account_id = kwargs[:account_id]
    recipient = kwargs[:recipient]

    notify_email(
      template: __method__,
      account_id: account_id,
      subject: "email.subject.notify_email_wijziging",
      recipient: recipient,
      fail_log: "405",
      success_log: "404",
      reason: ::SentEmail::Reason::NOTIFY_EMAIL_CHANGE
    )
  end

  # ED009 - Notificatiemail heraanvraag
  def notify_heraanvraag(kwargs = {})
    account_id = kwargs[:account_id]
    recipient = kwargs[:recipient]

    notify_email(
      template: __method__,
      account_id: account_id,
      subject: "email.subject.notify_heraanvraag",
      recipient: recipient,
      fail_log: "398",
      success_log: "397",
      reason: ::SentEmail::Reason::NOTIFY_REGISTRATION
    )
  end

  # ED010 - Notificatiemail activatie aanvraag
  def notify_activatie(kwargs = {})
    account_id = kwargs[:account_id]
    recipient = kwargs[:recipient]

    notify_email(
      template: __method__,
      account_id: account_id,
      subject: "email.subject.notify_activation",
      recipient: recipient,
      fail_log: "401",
      success_log: "400",
      reason: ::SentEmail::Reason::NOTIFY_ACTIVATION_REQ
    )
  end

  # ED011 - Notificatiemail activatie heraanvraag
  def notify_activatie_heraanvraag(kwargs = {})
    account_id = kwargs[:account_id]
    recipient = kwargs[:recipient]

    notify_email(
      template: __method__,
      account_id: account_id,
      subject: "email.subject.notify_activation_heraanvraag",
      recipient: recipient,
      fail_log: "403",
      success_log: "402",
      reason: ::SentEmail::Reason::NOTIFY_ACTIVATION_REREQ
    )
  end

  # ED013 - Notificatiemail telefoonnummer wijziging
  def notify_telefoonnummer_wijziging(kwargs = {})
    account_id = kwargs[:account_id]
    recipient = kwargs[:recipient]

    notify_email(
      template: __method__,
      account_id: account_id,
      subject: "email.subject.notify_telefoonnummer_wijziging",
      recipient: recipient,
      fail_log: "kvemail.notify_telefoonnummer_wijziging_mislukt",
      success_log: "911",
      reason: ::SentEmail::Reason::NOTIFY_PHONE_CHANGE
    )
  end

  # ED014 - Notificatiemail eID geactiveerd
  def notify_activatie_rijbewijs(kwargs = {})
    account_id = kwargs[:account_id]
    recipient = kwargs[:recipient]

    notify_email(
      template: __method__,
      account_id: account_id,
      subject: "email.subject.notify_activation_eid",
      subject_options: { wid_type: "document.driving_licence" },
      recipient: recipient,
      fail_log: "kvemail.notify_activation_licence_mislukt",
      success_log: "kvemail.notify_activation_licence_gelukt",
      reason: ::SentEmail::Reason::NOTIFY_WID_ACTIVATED
    )
  end

  def notify_activatie_identiteitskaart(kwargs = {})
    account_id = kwargs[:account_id]
    recipient = kwargs[:recipient]

    notify_email(
      template: __method__,
      account_id: account_id,
      subject: "email.subject.notify_activation_eid",
      subject_options: { wid_type: "document.id_card" },
      recipient: recipient,
      fail_log: "kvemail.notify_activation_id_card_mislukt",
      success_log: "970",
      reason: ::SentEmail::Reason::NOTIFY_WID_ACTIVATED
    )
  end

  # ED015 - Notificatiemail rijbewijs ingetrokken
  def notify_intrekking_rijbewijs(kwargs = {})
    account_id = kwargs[:account_id]
    recipient = kwargs[:recipient]

    notify_email(
      template: __method__,
      account_id: account_id,
      subject: "email.subject.notify_intrekking_wid",
      subject_options: { wid_type: "document.driving_licence" },
      recipient: recipient,
      fail_log: "kvemail.notify_intrekking_driving_licence_mislukt",
      success_log: "kvemail.notify_intrekking_driving_licence_gelukt",
      reason: ::SentEmail::Reason::NOTIFY_WID_REVOKED
    )
  end

  def notify_intrekking_identiteitskaart(kwargs = {})
    account_id = kwargs[:account_id]
    recipient = kwargs[:recipient]

    notify_email(
      template: __method__,
      account_id: account_id,
      subject: "email.subject.notify_intrekking_wid",
      subject_options: { wid_type: "document.id_card" },
      recipient: recipient,
      fail_log: "kvemail.notify_intrekking_id_card_mislukt",
      success_log: "951",
      reason: ::SentEmail::Reason::NOTIFY_WID_REVOKED
    )
  end

  # ED016 - Notificatiemail rijbewijs gedeblokkeerd
  def notify_rijbewijs_gedeblokkeerd(kwargs = {})
    account_id = kwargs[:account_id]
    recipient = kwargs[:recipient]

    notify_email(
      template: __method__,
      account_id: account_id,
      subject: "email.subject.notify_gedeblokkeerd_wid",
      subject_options: { wid_type: "document.driving_licence" },
      recipient: recipient,
      fail_log: "990",
      success_log: "989",
      reason: ::SentEmail::Reason::NOTIFY_WID_BLOCKED
    )
  end

  def notify_identiteitskaart_gedeblokkeerd(kwargs = {})
    account_id = kwargs[:account_id]
    recipient = kwargs[:recipient]

    notify_email(
      template: __method__,
      account_id: account_id,
      subject: "email.subject.notify_gedeblokkeerd_wid",
      subject_options: { wid_type: "document.id_card" },
      recipient: recipient,
      fail_log: "kvemail.notify_id_card_unblocked_mislukt",
      success_log: "kvemail.notify_id_card_unblocked_gelukt",
      reason: ::SentEmail::Reason::NOTIFY_WID_BLOCKED
    )
  end

  # ED017 – Notificatiemail rijbewijs deblokkeringscode aangevraagd
  def notify_aanvraag_deblokkeringscode_rijbewijs(kwargs = {})
    account_id = kwargs[:account_id]
    recipient = kwargs[:recipient]

    notify_email(
      template: __method__,
      account_id: account_id,
      subject: "email.subject.notify_unblockingscode_requested",
      subject_options: { wid_type: "document.driving_licence" },
      recipient: recipient,
      fail_log: "1015",
      success_log: "1014",
      reason: ::SentEmail::Reason::DEBLOCK_WID_MY_DIGID
    )
  end

  def notify_aanvraag_deblokkeringscode_identiteitskaart(kwargs = {})
    account_id = kwargs[:account_id]
    recipient = kwargs[:recipient]

    notify_email(
      template: __method__,
      account_id: account_id,
      subject: "email.subject.notify_unblockingscode_requested",
      subject_options: { wid_type: "document.id_card" },
      recipient: recipient,
      fail_log: "kvemail.notify_unblocking_id_card_requested_mislukt",
      success_log: "kvemail.notify_unblocking_id_card_requested_gelukt",
      reason: ::SentEmail::Reason::DEBLOCK_WID_MY_DIGID
    )
  end

  # ED019 – Notificatiemail pincode aangevraagd
  def notify_request_pin_rijbewijs(kwargs = {})
    account_id = kwargs[:account_id]
    recipient = kwargs[:recipient]

    notify_email(
      template: __method__,
      account_id: account_id,
      subject: "email.subject.notify_request_pin",
      subject_options: { wid_type: "document.driving_licence" },
      recipient: recipient,
      fail_log: "kvemail.notify_licence_pin_requested_mislukt",
      success_log: "kvemail.notify_licence_pin_requested_gelukt",
      reason: ::SentEmail::Reason::REQUESTED_PIN
    )
  end

  # ED020
  def notify_set_pin_rijbewijs(kwargs = {})
    account_id = kwargs[:account_id]
    recipient = kwargs[:recipient]

    notify_email(
      template: __method__,
      account_id: account_id,
      subject: "email.subject.notify_set_pin",
      subject_options: { wid_type: "document.driving_licence"},
      recipient: recipient,
      fail_log: "kvemail.notify_licence_pin_set_mislukt",
      success_log: "kvemail.notify_licence_pin_set_gelukt",
      reason: ::SentEmail::Reason::SET_PIN
    )
  end

  # ED021
  def notify_sms_controle_activated(kwargs = {})
    account_id = kwargs[:account_id]
    recipient = kwargs[:recipient]

    notify_email(
      template: __method__,
      account_id: account_id,
      subject: "email.subject.notify_sms_controle_activated",
      recipient: recipient,
      fail_log: "1329",
      success_log: "1328",
      reason: ::SentEmail::Reason::SMS_ACTIVATED_WITH_APP
    )
  end

  # ED022
  def notify_digid_app_activated(kwargs = {})
    account_id = kwargs[:account_id]
    recipient = kwargs[:recipient]

    notify_email(
      template: __method__,
      account_id: account_id,
      subject: "email.subject.notify_digid_app_activated",
      recipient: recipient,
      fail_log: "1331",
      success_log: "1330",
      reason: ::SentEmail::Reason::APP_ACTIVATED
    )
  end

  # ED023
  def notify_digid_app_id_check_activated(kwargs = {})
    account_id = kwargs[:account_id]
    recipient = kwargs[:recipient]

    notify_email(
      template: __method__,
      account_id: account_id,
      subject: "email.subject.notify_digid_app_id_check_activated",
      recipient: recipient,
      fail_log: "1333",
      success_log: "1332",
      reason: ::SentEmail::Reason::APP_ACTIVATED_WITH_ID
    )
  end

  # ED024
  def notify_digid_app_upgraded(kwargs = {})
    account_id = kwargs[:account_id]
    recipient = kwargs[:recipient]

    notify_email(
      template: __method__,
      account_id: account_id,
      subject: "email.subject.notify_digid_app_upgraded",
      recipient: recipient,
      fail_log: "1335",
      success_log: "1334",
      reason: ::SentEmail::Reason::APP_UPGRADED_WITH_ID
    )
  end

  # ED025 - Notificatiemail email verwijderen
  def notify_email_verwijderen(kwargs = {})
    account_id = kwargs[:account_id]
    recipient = kwargs[:recipient]

    notify_email(
      template: __method__,
      account_id: account_id,
      subject: "email.subject.notify_email_verwijderd",
      recipient: recipient,
      fail_log: "407",
      success_log: "406",
      reason: ::SentEmail::Reason::DELETE_EMAIL
    )
  end

  # ED026 - Notificatiemail activeren DigiD app en account
  def notify_digid_app_and_account_activated(kwargs = {})
    account_id = kwargs[:account_id]
    recipient = kwargs[:recipient]

    notify_email(
      template: __method__,
      account_id: account_id,
      subject: "email.subject.notify_digid_app_and_account_activated",
      recipient: recipient,
      fail_log: "1331",
      success_log: "1330",
      reason: ::SentEmail::Reason::APP_AND_ACCOUNT_ACTIVATED
    )
  end

  # ED030
  def notify_sms_controle_deactivated(kwargs = {})
    account_id = kwargs[:account_id]
    recipient = kwargs[:recipient]

    notify_email(
      template: __method__,
      account_id: account_id,
      subject: "email.subject.notify_sms_controle_deactivated",
      recipient: recipient,
      fail_log: "1478",
      success_log: "1477",
      reason: ::SentEmail::Reason::SMS_DEACTIVATED
    )
  end

  # ED031
  def notify_account_removed(kwargs = {})
    account_id = kwargs[:account_id]
    recipient = kwargs[:recipient]

    notify_email(
      template: __method__,
      account_id: account_id,
      subject: "email.subject.account_deleted",
      recipient: recipient,
      fail_log: "1499",
      success_log: "1498",
      reason: ::SentEmail::Reason::ACCOUNT_REMOVED
    )
  end

  # ED032
  def notify_digid_app_deactivated(kwargs = {})
    account_id = kwargs[:account_id]
    recipient = kwargs[:recipient]

    notify_email(
      template: __method__,
      account_id: account_id,
      subject: "email.subject.notify_digid_app_deactivated",
      recipient: recipient,
      fail_log: "1508",
      success_log: "1507",
      reason: ::SentEmail::Reason::APP_DEACTIVATED
    )
  end

  alias_method :ED007, :notify_wachtwoord_wijziging
  alias_method :ED008, :notify_email_wijziging
  alias_method :ED009, :notify_heraanvraag
  alias_method :ED010, :notify_activatie
  alias_method :ED011, :notify_activatie_heraanvraag
  alias_method :ED013, :notify_telefoonnummer_wijziging
  alias_method :ED014_dl, :notify_activatie_rijbewijs
  alias_method :ED014_id, :notify_activatie_identiteitskaart
  alias_method :ED015_dl, :notify_intrekking_rijbewijs
  alias_method :ED015_id, :notify_intrekking_identiteitskaart
  alias_method :ED016_dl, :notify_rijbewijs_gedeblokkeerd
  alias_method :ED016_id, :notify_identiteitskaart_gedeblokkeerd
  alias_method :ED017_dl, :notify_aanvraag_deblokkeringscode_rijbewijs
  alias_method :ED017_id, :notify_aanvraag_deblokkeringscode_identiteitskaart
  alias_method :ED019, :notify_request_pin_rijbewijs
  alias_method :ED020, :notify_set_pin_rijbewijs
  alias_method :ED021, :notify_sms_controle_activated
  alias_method :ED022, :notify_digid_app_activated
  alias_method :ED023, :notify_digid_app_id_check_activated
  alias_method :ED024, :notify_digid_app_upgraded
  alias_method :ED025, :notify_email_verwijderen
  alias_method :ED026, :notify_digid_app_and_account_activated
  alias_method :ED030, :notify_sms_controle_deactivated
  alias_method :ED031, :notify_account_removed
  alias_method :ED032, :notify_digid_app_deactivated

  private

  def notify_email(kwargs = {})
    subject = kwargs[:subject]
    subject_options = kwargs[:subject_options] || {}
    account_id = kwargs[:account_id]
    recipient = kwargs[:recipient]
    fail_log = kwargs[:fail_log]
    success_log = kwargs[:success_log]
    reason = kwargs[:reason]
    template = kwargs[:template]

    begin
      i18n_mail(account_id: account_id, to: recipient, subject: subject, subject_options: subject_options, template: template)
    rescue
      Log.instrument(fail_log, account_id: account_id)
    else
      Log.instrument(success_log, account_id: account_id)
    end

    SentEmail.create(account_id: account_id, reason: reason)
  end
end
