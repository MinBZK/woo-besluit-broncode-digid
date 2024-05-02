
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

class SentEmail < AccountBase
  # possible value of attribute reason
  module Reason
    EMAIL                   = "Controle e-mail" # ED001, ED002
    EMAIL_BALIE             = "Controle e-mail (balie)" # EDB002

    REGISTRATION            = "Nieuwe aanvraag voltooid" # ED003
    REGISTRATION_BALIE      = "Nieuwe aanvraag voltooid (balie)" # EDB001
    REGISTRATION_OEP        = "Nieuwe aanvraag voltooid (OEP sector)" # ED012
    REGISTRATION_SVB        = "Nieuwe aanvraag SVB voltooid" # ED028

    REMINDER_ACTIVATION     = "Herinnering activatie" # ED004
    RECOVERY                = "Aanvraag herstelcode" # ED006
    REMINDER_EXPIRED        = "Herinnering vervallen DigiD" # ED005
    NOTIFY_PASSWORD_CHANGE  = "Notificatie wachtwoord wijziging" # ED007
    NOTIFY_EMAIL_CHANGE     = "Notificatie e-mail wijziging" # ED008
    NOTIFY_REGISTRATION     = "Notificatie heraanvraag" # ED009
    NOTIFY_ACTIVATION_REQ   = "Notificatie activatie aanvraag" # ED010
    NOTIFY_ACTIVATION_REREQ = "Notificatie activatie heraanvraag" # ED011
    SENT_EMAIL_BALIE_CODE   = "E-mail verzonden om ophalen activatiecode bij balie te bevestigen" # EDB003
    NOTIFY_PHONE_CHANGE     = "Notificatie telefoonnummer gewijzigd" # ED013
    NOTIFY_WID_ACTIVATED    = "Notificatie inloggen met identiteitsbewijs geactiveerd" # ED014
    NOTIFY_WID_REVOKED      = "Notificatie inlogfunctie identiteitsbewijs ingetrokken" # ED015
    NOTIFY_WID_BLOCKED      = "Notificatie inlogfunctie identiteitsbewijs gedeblokkeerd" # ED016

    DEBLOCK_WID_MY_DIGID    = "Notificatie deblokkeringscode inlogfunctie identiteitsbewijs aangevraagd (vanuit Mijn DigiD)" # ED017
    NOTIFY_SECURE_REQUEST   = "Notificatie nieuwe aanvraag beveiligde bezorging" # ED018
    BLOCK_WID               = "Notificatiemail identiteitsbewijs geblokkeerd" # EB003

    DEBLOCK_WID             = "Notificatie deblokkeringscode inlogfunctie identiteitsbewijs aangevraagd (vanuit Beheer)" #  EB004
    PIN_WID                 = "Notificatie PIN-brief identiteitsbewijs aangevraagd" # EB005

    REQUESTED_PIN           = "Notificatie pincode aangevraagd voor identiteitskaart of rijbewijs" # ED019
    SET_PIN                 = "Notificatie pincode ingesteld voor identiteitskaart of rijbewijs" # ED020
    SMS_ACTIVATED_WITH_APP  = "Notificatie Sms-controle geactiveerd met DigiD app" # ED021
    APP_ACTIVATED           = "Notificatie DigiD app geactiveerd" # ED022
    APP_ACTIVATED_WITH_ID   = "Notificatie DigiD app met ID-check geactiveerd" # ED023
    APP_UPGRADED_WITH_ID    = "Notificatie DigiD app verhoogd met ID-check" # ED024
    DELETE_EMAIL            = "Notificatie e-mail verwijderen" # ED025

    ALL = [EMAIL, EMAIL_BALIE, REGISTRATION, REGISTRATION_BALIE, REGISTRATION_OEP, REGISTRATION_SVB, REMINDER_ACTIVATION, RECOVERY, REMINDER_EXPIRED, NOTIFY_PASSWORD_CHANGE, NOTIFY_EMAIL_CHANGE, NOTIFY_REGISTRATION, NOTIFY_ACTIVATION_REQ, NOTIFY_ACTIVATION_REREQ, SENT_EMAIL_BALIE_CODE, NOTIFY_PHONE_CHANGE, NOTIFY_WID_ACTIVATED, NOTIFY_WID_REVOKED, NOTIFY_WID_BLOCKED, DEBLOCK_WID_MY_DIGID, NOTIFY_SECURE_REQUEST, BLOCK_WID, DEBLOCK_WID, PIN_WID, REQUESTED_PIN, SET_PIN, SMS_ACTIVATED_WITH_APP, APP_ACTIVATED, APP_ACTIVATED_WITH_ID, APP_UPGRADED_WITH_ID, DELETE_EMAIL].freeze
  end

  default_scope { order(created_at: :desc) }
  paginates_per 10
end
