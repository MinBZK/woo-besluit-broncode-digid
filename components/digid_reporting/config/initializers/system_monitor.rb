
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

# Available log entries
# =====================

BURGER_LOG_MAPPINGS = {

  # General
  'general.ping'                                          => -10_000,
  'general.sessie_verlopen'                               => 100_001,
  'general.annuleren_gelukt'                              => 100_002,
  'general.mobiel_te_vaak'                                => 100_003,
  'general.mobiel_formaat'                                => 100_004,
  'general.account_geblokkeerd'                           => 100_005,

  # UC1
  'uc1.account_aanvragen_start_gelukt'                    => 101_000,
  'uc1.gegevens_ingevoerd_validatie_gelukt'               => 101_001,
  'uc1.gegevens_ingevoerd_validatie_mislukt'              => 101_002,
  'uc1.gba_start_gelukt'                                  => 101_003,
  'uc1.gba_raadplegen_gelukt'                             => 101_004,
  'uc1.gba_raadplegen_mislukt'                            => 101_005,
  'uc1.gba_bevraging_mislukt'                             => 101_059,
  'uc1.aanvraag_mislukt_not_found'                        => 101_006,
  'uc1.gba_bsn_aanwezig_geen_a_nummer'                    => 101_007,
  'uc1.aanvraag_mislukt_a_nummer_bestaat'                 => 101_008,
  'uc1.aanvraag_mislukt_investigate'                      => 101_009,
  'uc1.aanvraag_mislukt_investigate_address'              => 101_010,
  'uc1.aanvraag_mislukt_deceased'                         => 101_011,
  'uc1.aanvraag_mislukt_emigrated'                        => 101_012,
  'uc1.gba_te_snel'                                       => 101_013,
  'uc1.gba_teveel_aanvragen'                              => 101_014,
  'uc1.aanvraag_bestaat'                                  => 101_015,
  'uc1.aanvraag_vervangen'                                => 101_016,
  'uc1.aanvraag_afgebroken_aanvraag_bestaat'              => 101_017,
  'uc1.account_bestaat'                                   => 101_018,
  'uc1.account_vervangen'                                 => 101_019,
  'uc1.aanvraag_afbroken_account_bestaat'                 => 101_020,
  'uc1.registration_without_sms_messages'                 => 101_021,
  'uc1.registration_with_sms_messages'                    => 101_022,
  'uc1.gesproken_sms_aanvragen_gelukt'                    => 101_023,
  'uc1.aanmaken_account_gelukt'                           => 101_024,
  'uc1.aanmaken_account_mislukt'                          => 101_025,
  'uc1.aanmaken_mislukt_lengte_gebruikersnaam'            => 101_026,
  'uc1.aanmaken_mislukt_verplicht_veld'                   => 101_027,
  'uc1.aanmaken_mislukt_wachtwoord'                       => 101_028,
  'uc1.aanmaken_mislukt_gebruikersnaam'                   => 101_029,
  'uc1.aanmaken_mislukt_email_formaat'                    => 101_030,
  'uc1.aanmaken_mislukt_wachtwoordherstel_zonder_email'   => 101_032,
  'uc1.aanmaken_mislukt_serviceberichtent_zonder_email'   => 101_033,
  'uc1.aanmaken_mislukt_gebruikersvoorwaarden'            => 101_034,
  'uc1.aanmaken_mislukt_gebruikersnaam_bestaat'           => 101_035,
  'uc1.controle_email_verzonden'                          => 101_037,
  'uc1.controle_email_niet_verzonden'                     => 101_038,
  'uc1.code_invoeren_mislukt'                             => 101_039,
  'uc1.code_invoeren_overgeslagen'                        => 101_040,
  'uc1.code_verlopen'                                     => 101_041,
  'uc1.code_tevaak_ingevoerd'                             => 101_042,
  'uc1.code_invoeren_geslaagd'                            => 101_043,
  'uc1.aanvraag_aanmaken_mislukt'                         => 101_044,
  'uc1.aanvraag_email_niet_verzonden'                     => 101_045,
  'uc1.aanvraag_account_gelukt'                           => 101_046,
  'uc1.heraanvraag_account_gelukt'                        => 101_047,
  'uc1.aanvraag_annuleren_gelukt'                         => 101_048,
  'uc1.aanvraag_sessie_verlopen'                          => 101_049,
  'uc1.aanvraag_afgebroken_verwijderen_gelukt'            => 101_050,
  'uc1.aanvraag_afgebroken_verwijderen_mislukt'           => 101_051,
  'uc1.account_opgeschort'                                => 101_052,
  'uc1.aanvraag_opgeheven_brief_verstuurd'                => 101_053,
  'uc1.aanvraag_opgeheven_brief_niet_verstuurd'           => 101_054,
  'uc1.account_aanvragen_webdienst_start_gelukt'          => 101_055,
  'uc1.account_aanvragen_webdienst_start_mislukt'         => 101_056,
  'uc1.gba_block_bsn'                                     => 101_057,
  'uc1.gba_block_adres'                                   => 101_058,
  'uc1.aanvraag_mislukt_bsn_afgemeld'                     => 101_060,
  'uc1.account_aanvragen_balie_start_gelukt'              => 101_061,
  'uc1.gegevens_balie_ingevoerd_validatie_gelukt'         => 101_062,
  'uc1.gegevens_balie_ingevoerd_validatie_mislukt'        => 101_063,
  'uc1.balie_gba_bevraging_mislukt'                       => 101_064,
  'uc1.aanvraag_mislukt_not_emigrated'                    => 101_065,
  'uc1.balie_ongeldige_baliecode_verlopen'                => 101_066,
  'uc1.balie_ongeldige_baliecode_onbekend'                => 101_067,
  'uc1.balie_ongeldige_baliecode_gebruikt'                => 101_068,
  'uc1.aanmaken_account_balie_gelukt'                     => 101_069,
  '428'                                                   => 101_069,
  'uc1.aanvraag_account_balie_gelukt'                     => 101_070,
  'uc1.gba_block_baliecode'                               => 101_071,
  'uc1.aanvraag_account_baliecode_gebruikt'               => 101_072,
  'uc1.standaard_inlogmethode_ingesteld'                  => 101_073,
  'uc1.balie_baliecode_verzonden_via_sms'                 => 101_074,
  'uc1.front_desk_code_created'                           => 101_075,
  'uc1.aanvraag_buitenland_email_niet_verzonden'          => 101_076,
  'uc1.gba_block_geboortedatum_en_id_nummer'              => 101_077,
  'uc1.gba_te_snel_balie'                                 => 101_078,
  'uc1.aanvraag_buitenland_bevestigings_email_verstuurd'  => 101_079,
  'uc1.aanvraag_buitenland_bevestigings_email_niet_verstuurd' => 101_080,
  'uc1.aanvraag_buitenland_heraanvraag_gelukt'            => 101_081,
  'uc1.aanmaken_mislukt_mobielnummer_formaat'             => 101_082,
  'uc1.aanmaken_mislukt_mobielnummer_te_vaak'             => 101_083,
  'uc1.sectoraal_nummer_is_afgemeld'                      => 101_084,
  'uc1.aanvraag_mislukt_not_dutch'                        => 101_085,
  'uc1.aanvraag_buitenland_mislukt_brp'                   => 101_087,
  'uc1.aanvraag_buitenland_teveel_aanvragen_maand'        => 101_088,

  # UC2
  'uc2.authenticeren_start'                               => 102_001,
  'uc2.authenticeren_start_basis'                         => 102_002,
  'uc2.authenticeren_start_midden'                        => 102_003,
  'uc2.authenticeren_mislukt_niet_ingevuld'               => 102_004,
  'uc2.authenticeren_mislukt_geblokkeerd'                 => 102_005,
  'uc2.authenticeren_mislukt_geen_match'                  => 102_006,
  'uc2.authenticeren_mislukt_wachtwoord'                  => 102_007,
  'uc2.authenticeren_mislukt_sms_te_snel'                 => 102_008,
  'uc2.authenticeren_mislukt_afgebroken'                  => 102_009,
  'uc2.authenticeren_mislukt_sessie_verlopen'             => 102_010,
  'uc2.authenticeren_mislukt_geen_midden'                 => 102_011,
  'uc2.authenticeren_mislukt_geannuleerd'                 => 102_012,
  'uc2.sms_send_successful'                               => 102_038,
  'uc2.sms_send_failed'                                   => 102_039,
  'uc2.sms_afleveren_gelukt'                              => 102_013,
  'uc2.sms_afleveren_mislukt'                             => 102_014,
  'uc2.authenticeren_midden_mislukt_sms_code_onjuist'     => 102_015,
  'uc2.authenticeren_midden_mislukt_sms_ongeldig'         => 102_016,
  'uc2.authenticeren_basis_gelukt'                        => 102_017,
  'uc2.authenticeren_midden_gelukt'                       => 102_018,
  'uc2.sso_sessie_verlengt'                               => 102_019,
  'uc2.sso_authenticatie_gelukt'                          => 102_020,
  'uc2.sso_sessie_verlopen_overall_sessie'                => 102_021,
  'uc2.sso_sessie_verlopen_inactiviteit_timeout'          => 102_022,
  'uc2.sso_sessie_verlopen_inactiviteit_graceperiod'      => 102_023,
  'uc2.uitloggen_gelukt'                                  => 102_024,
  'uc2.logoff_bericht_verstuurd'                          => 102_025,
  'uc2.saml_request_authentication'                       => 102_026,
  'uc2.sessie_verlengt'                                   => 102_027,
  'uc2.vraag_om_wijziging_zwak_wachtwoord'                => 102_028,
  'uc2.wijzigen_zwak_wachtwoord_overgeslagen'             => 102_029,
  'uc2.wijzigen_zwak_naar_sterk_wachtwoord_gelukt'        => 102_030,
  'uc2.wijzigen_zwak_naar_sterk_wachtwoord_mislukt'       => 102_031,
  'uc2.authenticeren_light_gelukt'                        => 102_032,
  'uc2.authenticeren_mislukt_middel_geblokkeerd'          => 102_068,
  'uc2.authenticate_app_failed_blocked'                   => 102_069,
  'uc2.authenticeren_digid_app_gelukt'                    => 102_070,
  'uc2.authenticeren_light_gelukt_sms_code'               => 102_071,
  'uc2.authenticeren_preference_midden'                       => 102_072,
  'uc2.authenticeren_digid_app_to_app_gelukt'                 => 102_073,
  'uc2.authenticeren_digid_app_failed_session_not_found'      => 102_074,
  'uc2.authenticeren_digid_app_failed_app_not_found'          => 102_075,
  'uc2.authenticeren_digid_app_deactivated_app'               => 102_076,
  'uc2.authenticeren_digid_app_failed_no_match'               => 102_077,
  'uc2.authenticeren_digid_app_failed_invalid_user_app_id'    => 102_078,
  'uc2.authenticeren_digid_app_failed_invalid_instance_id'    => 102_079,
  'uc2.authenticeren_digid_app_failed_invalid_public_key'     => 102_080,
  'uc2.authenticeren_digid_app_failed_invalid_signature'      => 102_081,
  'uc2.authenticeren_digid_app_failed_invalid_pincode'        => 102_082,
  'uc2.authenticeren_digid_app_cancelled'                     => 102_083,
  'uc2.authenticeren_digid_app_choice'                        => 102_084,
  'uc2.authenticeren_digid_app_failed_switch_disabled'        => 102_085,
  'uc2.authenticeren_digid_app_deactivated_app_mobile_only'   => 102_086,
  'uc2.authenticeren_no_test_account_for_gbav'                => 102_087,
  'uc2.authenticeren_test_login'                              => 102_088,
  'uc2.authenticeren_test_login_success'                      => 102_089,
  'uc2.authenticeren_substantieel_gelukt'                     => 102_090,
  'uc2.authenticeren_lowering_substantial_to_midden'          => 102_091,
  'uc2.authenticeren_hoog_gelukt'                             => 102_045,
  'uc2.authenticeren_digid_app_to_app_start'                  => 102_100,
  'uc2.authenticeren_digid_app_to_app_substantieel_gelukt'    => 102_101,
  'uc2.authenticeren_digid_app_to_app_mislukt_geen_sessie'    => 102_102,
  'uc2.authenticeren_digid_app_to_app_hoog_gelukt'            => 102_103,
  'uc2.authenticeren_digid_app_to_app_wid_upgrade'            => 102_104,

  # Digid Hoog

  'digid_hoog.activate.started'                                        => 145_000,
  'digid_hoog.activate.app_chosen'                                     => 145_001,
  'digid_hoog.activate.abort.no_bsn'                                   => 145_002,
  'digid_hoog.activate.interrupted_no_email'                           => 145_004,
  'digid_hoog.activate.abort.no_nfc'                                   => 145_006,
  'digid_hoog.activate.abort.nfc_forbidden'                            => 145_007,
  'digid_hoog.activate.abort.no_app_session'                           => 145_008,
  'digid_hoog.activate.success'                                        => 145_009,
  'digid_hoog.activate.abort.eid_timeout'                              => 145_010,
  'digid_hoog.activate.abort.mu_error'                                 => 145_011,
  'digid_hoog.activate.abort.false_bsn'                                => 145_012,
  'digid_hoog.activate.abort.wrong_doctype_sequence_no'                => 145_013,
  'digid_hoog.activate.abort.msc_not_issued'                           => 145_014,
  'digid_hoog.authenticate.chose_app'                                  => 145_015,
  'digid_hoog.activate.usb_chosen'                                     => 145_017,
  'digid_hoog.activate.abort.desktop_client_timeout'                   => 145_018,
  'digid_hoog.authenticate.chose_desktop_app'                          => 145_019,
  'digid_hoog.activate.abort.desktop_clients_ip_address_not_equal'     => 145_021,
  'digid_hoog.revoke.success'                                          => 145_054,
  'digid_hoog.revoke.app_midden_start'                                 => 145_056,
  'digid_hoog.revoke.app_substantieel_start'                           => 145_057,
  'digid_hoog.unblock.app_chosen'                                      => 145_177,
  'digid_hoog.unblock.unblocked'                                       => 145_179,
  'digid_hoog.unblock.usb_chosen'                                      => 145_187,

  'digid_kiosk.missing_headers'                                        => 146_002,
  'digid_kiosk.update_warning'                                         => 146_003,
  'digid_kiosk.force_update'                                           => 146_004,
  'digid_kiosk.kill_app'                                               => 146_005,
  'digid_kiosk.unknown_version'                                        => 146_006,


  # UC3
  'uc3.activeren_start_gelukt'                            => 103_001,
  'uc3.activeren_mislukt_drie_maal_fout'                  => 103_002,
  'uc3.activeren_mislukt_code_onjuist'                    => 103_003,
  'uc3.activeren_mislukt_geen_code'                       => 103_004,
  'uc3.activeren_mislukt_code_ongeldig'                   => 103_005,
  'uc3.activeren_gelukt_bestaande_opgeheven'              => 103_006,
  'uc3.activeren_gelukt'                                  => 103_007,
  'uc3.activeren_mislukt_proces_afgebroken'               => 103_008,
  'uc3.activeren_mislukt_sessie_verlopen'                 => 103_009,
  'uc3.activeren_mislukt_geannuleerd'                     => 103_010,
  'uc3.activeren_mislukt_reeds_geactiveerd'               => 103_011,
  'uc3.activeren_uitbreiding_gelukt'                      => 103_012,
  'uc3.activeren_gelukt_bestaande_opgeheven_gegevens'     => 103_013,
  'uc3.activeren_uitbreiding_start'                       => 103_014,
  'uc3.activeren_sessie_verlengd'                         => 103_015,
  'uc3.activeren_authenticeren_gelukt'                    => 103_016,
  'uc3.aangevraagde_opgeheven'                            => 103_017,
  'uc3.activeren_app_start'                               => 103_021,

  # UC4
  'uc4.webdienst_authenticatie_cgi_gelukt'                => 104_001,
  'uc4.webdienst_authenticatie_cgi_mislukt'               => 104_101,
  'uc4.webdienst_authenticatie_soap-1-1_gelukt'           => 104_002,
  'uc4.webdienst_authenticatie_soap-1-1_mislukt'          => 104_102,
  'uc4.webdienst_authenticatie_soap-1-2_gelukt'           => 104_003,
  'uc4.webdienst_authenticatie_soap-1-2_mislukt'          => 104_103,
  'uc4.webdienst_authenticatie_wsdl_gelukt'               => 104_004,
  'uc4.webdienst_authenticatie_wsdl_mislukt'              => 104_104,
  'uc4.authenticatie_mislukt'                             => 104_005,
  'uc4.herauthenticatie_forceauth'                        => 104_006,
  'uc4.nieuwe_sso_sessie'                                 => 104_007,
  'uc4.afnemer_bestaande_sso_sessie'                      => 104_008,
  'uc4.authenticatie_mislukt_account_niet_actief'         => 104_009,
  'uc4.authenticatie_mislukt_geen_sectorale_nummers'      => 104_010,
  'uc4.zekerheidsniveau_niet_hoog_genoeg'                 => 104_011,
  'uc4.authenticatie_sessie_stop'                         => 104_012,
  'uc4.authenticatiebericht_verstuurd'                    => 104_013,
  'uc4.sso_sessie_verlopen'                               => 104_014,
  'uc4.sso_voorkeur_instellen_gelukt'                     => 104_015,
  'uc4.sso_voorkeur_instellen_mislukt'                    => 104_016,
  'uc4.authenticatie_gelukt_niveau_inloggen'              => 104_017,
  'uc4.activeren_mislukt_sessie_verlopen'                 => 104_019,
  'uc4.activeren_mislukt_geannuleerd'                     => 104_020,
  'uc4.zeker_weten_bevestigd'                             => 104_021,
  'uc4.redirect_url_invalid'                              => 104_022,
  'uc4.webdienst_authenticatie_verificatie_cgi_gelukt'                => 104_023,
  'uc4.webdienst_authenticatie_verificatie_cgi_mislukt'               => 104_123,
  'uc4.webdienst_authenticatie_verificatie_soap-1-1_gelukt'           => 104_024,
  'uc4.webdienst_authenticatie_verificatie_soap-1-1_mislukt'          => 104_124,
  'uc4.webdienst_authenticatie_verificatie_soap-1-2_gelukt'           => 104_025,
  'uc4.webdienst_authenticatie_verificatie_soap-1-2_mislukt'          => 104_125,
  'uc4.webdienst_authenticatie_verificatie_wsdl_gelukt'               => 104_026,
  'uc4.webdienst_authenticatie_verificatie_wsdl_mislukt'              => 104_126,
  'uc4.aselect_sessie_verlopen' => 104_027,
  'uc4.webdienst_authenticatie_app_to_app_mislukt' => 104_028,

  # UC5 / UC7
  'uc5.bekijkt_gebruiksgeschiedenis'                      => 105_001,
  'uc5.wijzig_emailinstellingen_start_gelukt'             => 105_002,
  'uc5.serviceberichten_activeren_gelukt'                 => 105_003,
  'uc5.serviceberichten_deactiveren_gelukt'               => 105_004,
  'uc5.wachtwoordherstel_activeren_gelukt'                => 105_005,
  'uc5.wachtwoordherstel_deactiveren_gelukt'              => 105_006,
  'uc5.wijzigen_wachtwoord_start'                         => 105_007,
  'uc5.wijzigen_wachtwoord_mislukt_wachtwoord_onjuist'    => 105_008,
  'uc5.wijzigen_wachtwoord_mislukt_reden_onbekend'        => 105_009,
  'uc5.wijzigen_wachtwoord_mislukt'                       => 105_010,
  'uc5.wijzigen_wachtwoord_gelukt'                        => 105_011,
  'uc5.email_controle_start'                              => 105_012,
  'uc5.controle_email_verstuurd'                          => 105_013,
  'uc5.controle_email_teveel'                             => 105_014,
  'uc5.controle_email_verzonden'                          => 105_015,
  'uc5.controle_email_niet_verzonden'                     => 105_016,
  'uc5.controle_code_mislukt_onjuiste_code'               => 105_017,
  'uc5.controle_code_gelukt'                              => 105_018,
  'uc5.wijzigen_email_start'                              => 105_019,
  'uc5.wijzigen_emailadres_mislukt_ongeldig'              => 105_020,
  'uc5.wijzigen_emailinstellingen_gelukt'                 => 105_021,
  'uc5.verwijderen_email_gelukt'                          => 105_022,
  'uc5.wijzigen_mobiel_start'                             => 105_023,
  'uc5.wijzigen_mobiel_mislukt'                           => 105_024,
  'uc5.wijzigen_mobiel_mislukt_geen_geldig_formaat'       => 105_025,
  'uc5.mobiel_mislukt_nummers_teveel'                     => 105_026,
  'uc5.wijzigen_mobiel_nummer_gelukt'                     => 105_027,
  'uc5.uitbreidingsaanvraag_start'                        => 105_028,
  'uc5.gba_controle_start'                                => 105_029,
  'uc5.gba_controle_gelukt'                               => 105_030,
  'uc5.gba_controle_mislukt'                              => 105_031,
  'uc5.uitbreiding_mislukt_niet_geaccordeerd'             => 105_032,
  'uc5.uitbreiding_adresgegevens_geaccordeerd'            => 105_033,
  'uc5.persoonsgegevens_no_match_gba'                     => 105_034,
  'uc5.snelheid_aanvragen_te_snel'                        => 105_035,
  'uc5.teveel_aanvragen_deze_maand'                       => 105_036,
  'uc5.mislukt_mobiel_te_vaak'                            => 105_037,
  'uc5.uitbreidingsaanvraag_intrekken_gelukt'             => 105_038,
  'uc5.uitbreidingsaanvraag_gelukt'                       => 105_039,
  'uc5.opheffen_sms_start'                                => 105_040,
  'uc5.opheffen_sms_gelukt'                               => 105_041,
  'uc5.wijzigen_voorkeur_inlog_start'                     => 105_042,
  'uc5.wijzigen_voorkeur_inlog_gelukt'                    => 105_043,
  'uc5.opheffen_digid_start'                              => 105_044,
  'uc5.opheffen_digid_gelukt'                             => 105_045,
  'uc5.displays_single_sign_on_preference'                => 105_046,
  'uc5.updated_single_sign_on_preference'                 => 105_047,
  'uc5.account_uitloggen_gelukt'                          => 105_048,
  'uc5.account_uitloggen_mislukt'                         => 105_049,
  'uc5.activeren_mislukt_sessie_verlopen'                 => 105_051,
  'uc5.activeren_mislukt_geannuleerd'                     => 105_052,
  'uc5.geannuleerd_change_password_post'                  => 105_053,
  'uc5.geannuleerd_email_configuration_post'              => 105_054,
  'uc5.geannuleerd_my_digid_configuration_post'           => 105_056,
  'uc5.geannuleerd_change_mobile_post'                    => 105_057,
  'uc5.geannuleerd_confirm_cancel_digid'                  => 105_058,
  'uc5.geannuleerd_ask_mobile_post'                       => 105_059,
  'uc5.geannuleerd_password_check_post'                   => 105_060,
  'uc5.uitbreidingsaanvraag_brief_verstuurd'              => 105_061,
  'uc5.uitbreidingsaanvraag_brief_niet_verstuurd'         => 105_062,
  'uc5.uitbreiding_activeren_gelukt'                      => 105_063,
  'uc5.opheffen_digid_foutief_wachtwoord'                 => 105_064,
  'uc5.wijzigen_wachtwoord_mislukt_foutief_huidig'        => 105_065,
  'uc5.code_tevaak_ingevoerd'                             => 105_066,
  'uc5.gesproken_sms_aanvragen_gelukt'                    => 105_067,
  'uc5.geannuleerd_email'                                 => 105_068,
  'uc5.spoken_text_messages_on'                           => 105_069,
  'uc5.spoken_text_messages_off'                          => 105_070,
  'uc5.password_recovery_by_email_on'                     => 105_071,
  'uc5.password_recovery_by_email_off'                    => 105_072,
  'uc5.wijzigen_wachtwoord_mislukt_andere_reden'          => 105_073,
  'uc5.start_request_authenticator_app'                   => 105_077,
  'uc5.cancel_request_totp_verification'                  => 105_078,
  'uc5.completed_request_authenticator_app'               => 105_083,
  'uc5.start_remove_authenticator_app'                    => 105_085,
  'uc5.app_activation_start'                              => 105_104,
  'uc5.app_switch_inactive_activation'                    => 105_105,
  'uc5.app_not_in_pilot_group'                            => 105_106,
  'uc5.app_already_activated'                             => 105_108,
  'uc5.app_deactivate_start'                              => 105_109,
  'uc5.app_deactivate_done'                               => 105_110,
  'uc5.app_activation_done'                               => 105_111,
  'uc5.app_activation_canceled'                           => 105_112,
  'uc5.app_activation_invalid_app_session'                => 105_113,
  'uc5.app_activation_invalid_app_user_id'                => 105_114,
  'uc5.app_activation_letter_canceled'                    => 105_115,
  'uc5.app_activation_invalid_session_id'                 => 105_116,
  'uc5.idensys_card_pin_change'                           => 105_117,
  'uc5.idensys_card_pin_unlock'                           => 105_118,
  'uc5.app_switch_inactive_deactivation'                  => 105_119,
  'uc5.wijzigen_mobiel_mislukt_hetzelfde'                 => 105_120,
  'uc5.app_activation_invalid_activationcode'             => 105_121,
  'uc5.app_activation_too_many_invalid_activationcodes'   => 105_122,
  'uc5.view_login_method_settings'                        => 105_123,
  'uc5.wijzigen_mobiel_mislukt_geannuleerd'               => 105_124,
  'uc5.wijzigen_emailadres_mislukt_annuleren'             => 105_125,
  'uc5.toevoegen_emailadres_mislukt_annuleren'            => 105_126,
  'uc5.app_activation_activationcodes_expired'            => 105_127,
  'uc5.app_activation_invalid_signature'                  => 105_129,
  'uc5.app_activation_invalid_public_key'                 => 105_130,
  'uc5.app_activation_temporarily_unavailable'            => 105_131,
  'uc5.app_deactivate_failed_no_activated_app'            => 105_132,
  'uc5.app_activation_start_again'                        => 105_133,
  'uc5.app_to_substantial'                                => 105_134,
  'uc5.app_to_substantial_no_app'                         => 105_135,
  'uc5.app_to_substantial_chosen_wid_document_type'       => 105_136,
  'uc5.app_to_substantial_no_nfc'                         => 105_137,
  'uc5.app_to_substantial_nfc_forbidden'                  => 105_138,
  'uc5.app_to_substantial_no_bsn'                         => 105_139,
  'uc5.app_to_substantial_brp_deceased'                   => 105_140,
  'uc5.app_to_substantial_brp_investigate'                => 105_141,
  'uc5.app_to_substantial_brp_not_found'                  => 105_142,
  'uc5.app_to_substantial_brp_no_documents'               => 105_143,
  'uc5.app_to_substantial_brp_no_valid_documents'         => 105_144,
  'uc5.app_to_substantial_brp_error'                      => 105_145,
  'uc5.app_to_substantial_redirect_to_cis'                => 105_146,
  'uc5.app_to_substantial_rda_url_ready'                  => 105_147,
  'uc5.app_to_substantial_cant_connect_to_rda_server'     => 105_148,
  'uc5.app_to_substantial_rda_generic_error'              => 105_149,
  'uc5.app_to_substantial_crb_check_start'                => 105_150,
  'uc5.app_to_substantial_active_authentication_error'    => 105_151,
  'uc5.app_to_substantial_cant_open_chip'                 => 105_152,
  'uc5.app_to_substantial_rda_timeout'                    => 105_153,
  'uc5.app_to_substantial_rda_no_match'                   => 105_154,
  'uc5.app_to_substantial_switch_app_disabled'            => 105_155,
  'uc5.app_to_substantial_successful'                     => 105_156,
  'uc5.app_to_substantial_cancel'                         => 105_157,
  'uc5.app_to_substantial_crb_check_success'              => 105_158,
  'uc5.app_to_substantial_authn_response_error'           => 105_159,
  'uc5.app_to_substantial_failed_crb_302'                 => 105_160,
  'uc5.app_to_substantial_failed_timeout'                 => 105_161,
  'uc5.app_to_substantial_failed_no_driving_licence'      => 105_162,
  'uc5.app_to_substantial_rda_cancel'                     => 105_163,
  'uc5.app_to_substantial_failed_crb_error'               => 105_164,
  'uc5.recovery_codes_destroyed_due_to_changed_account'   => 105_165,
  'uc5.app_activation_by_letter_chosen_letter'            => 105_183,
  'uc5.app_activation_by_letter_activation_code_requested'                   => 105_185,
  'uc5.app_activation_by_letter_pending_request_revoked_letter_already_sent' => 105_189,
  'uc5.app_activation_by_letter_pending_request_revoked_letter_not_yet_sent' => 105_190,
  'uc5.app_activation_by_letter_activationcode_success'                      => 105_193,
  'uc5.app_activation_no_active_sms_tool'                                    => 105_226,
  'uc5.app_activation_by_letter_enter_activation_code'                       => 105_230,
  'uc5.kiosk_app_to_substantial_start'                                       => 105_244,
  'uc5.kiosk_app_to_substantial_successful'                                  => 105_246,
  'uc5.kiosk_app_to_substantial_kiosk_not_active'                            => 105_247,
  'uc5.kiosk_app_to_substantial_kiosk_unknown'                               => 105_248,
  'uc5.kiosk_app_to_substantial_authentication_failed'                       => 105_249,
  'uc5.kiosk_app_to_substantial_authentication_failed_no_bsn'                => 105_250,
  'uc5.kiosk_app_to_substantial_switch_kiosk_disabled'                       => 105_251,
  'uc5.kiosk_app_to_substantial_failed_cancelled'                            => 105_252,


  # UC6
  'uc6.herstellen_wachtwoord_start'                       => 106_001,
  'uc6.herstellen_wachtwoord_mislukt_geblokkeerd'         => 106_002,
  'uc6.herstellen_wachtwoord_mislukt_account_niet_actief' => 106_003,
  'uc6.herstellen_wachtwoord_mislukt_no_match'            => 106_004,
  'uc6.herstellen_account_sessie_start'                   => 106_005,
  'uc6.sms_herstellen'                                    => 106_006,
  'uc6.herstelcode_opnieuw_versturen'                     => 106_007,
  'uc6.herstelcode_invoeren_gelukt'                       => 106_008,
  'uc6.herstellen_wachtwoord_via_brief'                   => 106_009,
  'uc6.herstellen_brief_mislukt_te_snel'                  => 106_010,
  'uc6.gba_controle_start'                                => 106_011,
  'uc6.gba_controle_gelukt'                               => 106_012,
  'uc6.gba_controle_mislukt_timeout'                      => 106_013,
  'uc6.gba_status_niet_juist'                             => 106_014,
  'uc6.wachtwoordherstel_via_email'                       => 106_015,
  'uc6.herstellen_email_mislukt_te_snel'                  => 106_016,
  'uc6.herstellen_email_gelukt'                           => 106_017,
  'uc6.herstellen_email_mislukt'                          => 106_018,
  'uc6.invoeren_herstelcode_gelukt'                       => 106_019,
  'uc6.invoeren_herstelcode_mislukt_code_onjuist'         => 106_020,
  'uc6.invoeren_herstelcode_mislukt_code_ongeldig'        => 106_021,
  'uc6.invoeren_nieuw_wachtwoord_mislukt_voldoet_niet'    => 106_022,
  'uc6.wijzigen_wachtwoord_succesvol'                     => 106_023,
  'uc6.invoeren_nieuw_mobiel_niet_juist'                  => 106_024,
  'uc6.herstellen_sms_mislukt_mobiel_formaat_niet_juist'  => 106_025,
  'uc6.herstellen_sms_mislukt_mobiel_te_vaak'             => 106_026,
  'uc6.wijzigen_mobiel_gelukt'                            => 106_027,
  'uc6.opheffen_sms_uitbreiding_start'                    => 106_028,
  'uc6.opheffen_sms_uitbreiding_gelukt'                   => 106_029,
  'uc6.wachtwoord_herstellen_mislukt_afgebroken'          => 106_030,
  'uc6.wachtwoord_herstellen_mislukt_sessie_verlopen'     => 106_031,
  'uc6.wachtwoord_herstellen_mislukt_geannuleerd'         => 106_032,
  'uc6.herstellen_wachtwoord_brief_gelukt'                => 106_033,
  'uc6.wachtwoord_herstellen_mislukt_andere_reden'          => 106_034,
  'uc6.wachtwoordherstel_via_email_gekozen'                 => 106_039,
  'uc6.wachtwoordherstel_via_brief_gekozen'                 => 106_040,
  'uc6.cancel_herstel_wachtwoord'                           => 106_041,
  'uc6.invoeren_herstelcode_mislukt_te_vaak_code_onjuist'   => 106_042,
  'uc6.herstellen_brief_mislukt_te_vaak'                    => 106_043,
  'uc6.aanvraag_mislukt_a_nummer_bestaat'                   => 106_044,
  'uc6.aanvraag_mislukt_investigate'                        => 106_045,
  'uc6.aanvraag_mislukt_investigate_address'                => 106_046,
  'uc6.aanvraag_mislukt_deceased'                           => 106_047,
  'uc6.aanvraag_mislukt_emigrated'                          => 106_048,
  'uc6.aanvraag_mislukt_not_found'                          => 106_049,
  'uc6.gba_raadplegen_mislukt'                              => 106_050,
  'uc6.herstellen_wachtwoord_brief_gegenereerd'             => 106_051,
  'uc6.invoeren_herstelcode_mislukt_geen_code'              => 106_052,
  'uc6.invoeren_herstelcode_start'                          => 106_053,

  # KV Brief
  'kvbrief.briefbestanden_aanmaken_gelukt'                => 140_001,
  'kvbrief.briefbestanden_aanmaken_mislukt'               => 140_002,
  'kvbrief.briefbestanden_verzenden_gelukt'               => 140_003,
  'kvbrief.briefbestanden_verzenden_mislukt'              => 140_004,
  'kvbrief.verwerkingbestand_start'                       => 140_005,
  'kvbrief.verwerkingbestand_gelukt'                      => 140_006,
  'kvbrief.verwerkingbestand_geen_relatie'                => 140_007,
  'kvbrief.verwerkingbestand_mislukt'                     => 140_008,

  # KV E-mail
  'kvemail.account_aanvraag_mail_versturen_gelukt'        => 150_001,
  'kvemail.account_aanvraag_mail_versturen_mislukt'       => 150_002,
  'kvemail.account_herstellen_mail_versturen_gelukt'      => 150_003,
  'kvemail.account_herstellen_mail_versturen_mislukt'     => 150_004,

  'kvemail.notify_heraanvraag_gelukt'                     => 150_005,
  'kvemail.notify_heraanvraag_mislukt'                    => 150_006,

  'kvemail.notify_email_wijziging_gelukt'                 => 150_007,
  'kvemail.notify_email_wijziging_mislukt'                => 150_008,

  'kvemail.notify_wachtwoord_wijziging_gelukt'            => 150_009,
  'kvemail.notify_wachtwoord_wijziging_mislukt'           => 150_010,

  'kvemail.notify_activation_gelukt'                      => 150_011,
  'kvemail.notify_activation_mislukt'                     => 150_012,

  'kvemail.notify_activation_heraanvraag_gelukt'          => 150_013,
  'kvemail.notify_activation_heraanvraag_mislukt'         => 150_014,
  'kvemail.account_buitenland_aanvraag_mail_versturen_gelukt'   => 150_015,
  'kvemail.account_buitenland_aanvraag_mail_versturen_mislukt'  => 150_016,
  'kvemail.notify_heraanvraag_niet_verzonden_ongewijzigd_adres' => 150_017,

  # Cronjobs
  'cronjobs.account_status_vervallen_gelukt'              => 190_001,
  'cronjobs.aanvraaggegevens_opschonen_gelukt'            => 190_002,
  'cronjobs.brief_genereren_gelukt'                       => 190_003,

  # KV SMS
  'kvsms.sms_aangeboden_leverancier'                      => 160_001,
  'kvsms.sms_reactie_ontvangen_leverancier'               => 160_002,
  'kvsms.sms_versturen_gelukt'                            => 160_003,

  # UC19
  'uc19.testaccount_aanmaken_gelukt'                      => 170_001,
  'uc19.testaccount_aanmaken_mislukt'                     => 170_002,

  # UC20
  'uc20.testaccount_revoceren_gelukt'                     => 180_001,
  'uc20.testaccount_revoceren_gelukt_opgeschort'          => 180_002,
  'uc20.testaccount_revoceren_mislukt_onbekende_fout'     => 180_003,

  # UC16
  'uc16.account_driving_licence_block_gelukt'             => 216_014,

  # UC30
  'uc30.front_desk_activation_code_email_sent'            => 230_049,
  'uc30.front_desk_activation_code_email_failed'          => 230_050,

  # Metrics
  'aanvraag_gba.duration'                                 => 300_101,
  'sms.duration'                                          => 300_102,
  'email.duration'                                        => 300_103,
  'authentications.duration'                              => 300_104
}

ADMIN_LOG_MAPPINGS = {

  'general.ping'                                              => -20_000,

  # TOTP pre_release
  'pilot_group.listed_successfully'                           => 400_000,
  'pilot_group.viewed_successfully'                           => 400_001,
  'pilot_group.updated_successfully'                          => 400_002,
  'pilot_group.update_failed'                                 => 400_003,

  'pilot_switch.pilot_switches_inzien_gelukt'                 => 400_100,
  'pilot_switch.inzien_gelukt'                                => 400_101,
  'pilot_switch.wijzigen_gelukt'                              => 400_102,
  'pilot_switch.wijzigen_mislukt'                             => 400_103,

  'subscriber.added_successfully'                             => 241_000,
  'subscriber.deleted_successfully'                           => 241_001,
  'subscriber.all_deleted_successfully'                       => 241_002,

  # UC8
  #
  'uc8.inloggen_beheer_gelukt'                                => 208_001,
  'uc8.inloggen_beheer_mislukt_niet_geauthoriseerd'           => 208_002,
  'uc8.inloggen_beheer_mislukt_onbekende_fout'                => 208_003,
  'uc8.uitloggen_beheer_gelukt'                               => 208_004,
  'uc8.uitloggen_beheer_mislukt'                              => 208_005,

  # UC9
  #
  'uc9.beheer_overzicht_bekijken_gelukt'                      => 209_001,
  'uc9.beheer_zoeken_gelukt'                                  => 209_002,
  'uc9.beheer_inzien_gelukt'                                  => 209_003,
  'uc9.beheer_inzien_wijzigen_gelukt'                         => 209_004,

  # UC10
  #
  'uc10.beheerhandelingen_inzien_gelukt'                      => 210_001,

  # UC11
  #
  'uc11.nieuws_inzien_gelukt'                                 => 211_001,
  'uc11.nieuws_verwijderen_gelukt'                            => 211_002,
  'uc11.nieuws_wijzigen_gelukt'                               => 211_003,
  'uc11.nieuws_aanmaken_gelukt'                               => 211_004,

  # UC12
  #
  'uc12.sectoren_inzien_gelukt'                               => 212_001,
  'uc12.sector_inzien_gelukt'                                 => 212_002,
  'uc12.sector_wijzigen_gelukt'                               => 212_003,
  'uc12.sector_wijzigen_mislukt'                              => 212_004,
  'uc12.sector_aanmaken_gelukt'                               => 212_005,
  'uc12.sector_aanmaken_mislukt'                              => 212_006,

  # UC13
  #
  'uc13.webdienst_inzien_gelukt'                              => 213_001,

  # UC14
  #
  'uc14.webdienst_aanmaken_gelukt'                            => 214_001,
  'uc14.webdienst_verwijderen_gelukt'                         => 214_002,
  'uc14.webdienst_inactief_zetten_gelukt'                     => 214_003,
  'uc14.webdienst_actief_zetten_gelukt'                       => 214_004,
  'uc14.webdienst_wijzigen_gelukt'                            => 214_005,

  # UC15
  #
  'uc15.transacties_inzien_gelukt'                            => 215_001,
  'uc15.transacties_zoeken_gelukt'                            => 215_002,

  # UC16
  #
  'uc16.account_overzicht_inzien_gelukt'                      => 216_001,
  'uc16.account_inzien_gelukt'                                => 216_002,
  'uc16.account_wijzigen_gelukt'                              => 216_003,
  'uc16.account_opschorten_gelukt'                            => 216_004,
  'uc16.account_opschorten_ongedaan_maken_gelukt'             => 216_005,
  'uc16.account_omzetten_sofi_bsn_gelukt'                     => 216_006,
  'uc16.brief_data_inzien_gelukt'                             => 216_007,
  'uc16.account_verwijderen_gelukt'                           => 216_008,
  'uc16.opgeschort_account_verwijderen_gelukt'                => 216_009,

  # UC17
  #
  'uc17.gba_raadplegen_gelukt'                                => 217_001,
  'uc17.gba_raadplegen_gelukt_no_match'                       => 217_002,
  'uc17.gba_raadplegen_mislukt_onbereikbaar'                  => 217_003,
  'uc17.gba_geen_anummer'                                     => 217_004,

  # UC18
  #
  'uc18.grafieken_raadplegen_gelukt'                          => 218_001,

  # UC19
  #
  'uc19.testaccount_aanmaken_gelukt'                          => 219_001,
  'uc19.testaccount_aanmaken_mislukt_onbekende_fout'          => 219_002,

  # UC20
  #
  'uc20.testaccount_revoceren_gelukt'                         => 220_001,
  'uc20.testaccount_revoceren_mislukt_onbekende_fout'         => 220_002,
  'uc20.beheeraccount_alarmering_wijzigen_gelukt:'            => 220_003,

  # UC21
  #
  'uc21.alarmering_verzonden'                                 => 221_000,
  # "uc21.alarmering_verzonden_gba_onbereikbaar"                => 221001,
  # "uc21.alarmering_verzonden_sms_provider_onbereikbaar"       => 221002,
  # "uc21.alarmering_verzonden_geen_verzonden_briefbestand"     => 221003,
  # "uc21.alarmering_verzonden_lange_responsetijden"            => 221004,
  # "uc21.alarmering_verzonden_systeemfouten"                   => 221005,
  # "uc21.alarmering_verzonden_overbelasting"                   => 221006,
  # "uc21.alarmering_verzonden_abnormaal_verkeer"               => 221007,

  # UC22
  #
  'uc22.inzien_post_gelukt'                                   => 222_001,
  'uc22.inzien_email_gelukt'                                  => 222_002,
  'uc22.inzien_sms_gelukt'                                    => 222_003,
  'uc22.downloaden_briefbestand_gelukt'                       => 222_004,
  'uc22.zoeken_sms_gelukt'                                    => 222_005,
  'uc22.zoeken_email_gelukt'                                  => 222_006,
  'uc22.post_wijzigen_gelukt'                                 => 222_007,
  'uc22.email_wijzigen_gelukt'                                => 222_008,

  # UC23
  #
  'uc23.organisatie_overzicht_inzien_gelukt'                  => 223_001,
  'uc23.organisatie_inzien_gelukt'                            => 223_002,
  'uc23.organisatie_aanmaken_gelukt'                          => 223_003,
  'uc23.organisatie_verwijderen_gelukt'                       => 223_004,
  'uc23.organisatie_wijzigen_gelukt'                          => 223_005,

  # UC24
  #
  'uc24.email_verlopen_aanvraag_versturen_gelukt'             => 224_001,
  'uc24.email_verlopen_aanvraag_versturen_mislukt'            => 224_002,
  'uc24.email_reminder_account_activation_send'               => 224_003,

  # UC25
  #
  'uc25.beheeraccount_aanmaken_gelukt'                        => 225_001,
  'uc25.beheeraccount_wijzigen_gelukt'                        => 225_002,
  'uc25.beheeraccount_verwijderen_gelukt'                     => 225_003,
  'uc25.beheeraccount_opschorten_gelukt'                      => 225_004,
  'uc25.beheeraccount_opschorten_ongedaan_maken_gelukt'       => 225_005,
  'uc25.beheeraccount_authenticatiemiddel_toevoegen_gelukt'   => 225_006,
  'uc25.beheeraccount_authenticatiemiddel_verwijderen_gelukt' => 225_007,
  'uc25.beheeraccount_rol_toekennen_gelukt'                   => 225_008,

  # UC26
  #
  'uc26.eigen_beheeraccount_wijzigen_gelukt'                  => 226_001,

  # UC27
  #
  'uc27.beheerrollen_overzicht_inzien_gelukt'                 => 227_001,
  'uc27.beheerrol_inzien_gelukt'                              => 227_002,
  'uc27.beheerder_beheerrol_verwijderen_gelukt'               => 227_003,
  'uc27.beheerder_beheerrol_toevoegen_gelukt'                 => 227_004,
  'uc27.beheerrol_aanmaken_gelukt'                            => 227_005,
  'uc27.beheerrol_verwijderen_gelukt'                         => 227_006,
  'uc27.beheerrol_verwijderen_mislukt'                        => 227_007,
  'uc27.beheerprivilege_toevoegen_beheerrol_gelukt'           => 227_008,
  'uc27.beheerprivilege_verwijderen_beheerrol_gelukt'         => 227_009,
  'uc27.beheerprivilege_toevoegen_beheerrol_mislukt'          => 227_010,

  # UC28
  #
  'SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS'                           => 228_001,
  'SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS'         => 228_002, # old
  'SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS'         => 228_003, # old
  'SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS'     => 228_004,
  'SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS'      => 228_005,
  'SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS'     => 228_006,
  'SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS'      => 228_007,
  'SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS'     => 228_008,
  'SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS'      => 228_009,
  'SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS'     => 228_010,

  # UC29
  #
  'uc29.sso_domein_overzicht_inzien_gelukt'                   => 229_001,
  'uc29.sso_domein_inzien_gelukt'                             => 229_002,
  'uc29.sso_domein_wijzigen_gelukt'                           => 229_003,
  'uc29.sso_domein_wijzigen_mislukt'                          => 229_004,
  'uc29.sso_domein_aanmaken_gelukt'                           => 229_005,
  'uc29.sso_domein_aanmaken_mislukt'                          => 229_006,
  'uc29.sso_domein_verwijderen_gelukt'                        => 229_007,
  'uc29.sso_domein_verwijderen_mislukt'                       => 229_008,

  # UC30
  #
  'uc30.front_desk_employee_assigned_to_front_desk'                     => 230_001,
  'uc30.front_desk_employee_log_in_fail_unknown_front_desk'             => 230_002,
  'uc30.front_desk_employee_log_in_fail_unknown_autorisation'           => 230_003,
  'uc30.baliemdw_inloggen_zekerheidsniveau_te_laag'                     => 230_004,
  'uc30.baliemdw_inloggen_mislukt_balie_geblokkeerd'                    => 230_005,
  'uc30.front_desk_employee_log_out'                                    => 230_006,
  'uc30.baliemdw_identificatie_gestart'                                 => 230_007,
  'uc30.front_desk_id_check_successful'                                 => 230_008,
  'uc30.baliemdw_identificatie_mislukt'                                 => 230_009,
  'uc30.front_desk_id_check_fail_not_unique_for_citizen_service_number' => 230_010,

  'uc30.front_desk_id_check_empty_fields'                               => 230_011,
  'uc30.front_desk_activation_letter_shown'                             => 230_013,
  'uc30.front_desk_activation_code_activated'                           => 230_015,
  'uc30.baliemdw_baliecode_aanmaken_start'                              => 230_017,
  'uc30.baliemdw_baliecode_aanmaken_gelukt'                             => 230_018,
  'uc30.baliemdw_baliecode_aanmaken_mislukt_gebruikt'                   => 230_019,

  'uc30.front_desk_audit_one_case_shown'                                => 230_021,
  'uc30.front_desk_audit_case_marked_as_fraud'                          => 230_022,
  'uc30.baliemdw_identificatie_bsn_gevonden'                            => 230_023,
  'uc30.baliemdw_identificatie_bsn_niet_gevonden'                       => 230_024,
  'uc30.baliemdw_identificatie_bsn_niet_gevonden_geen_aanvraag'         => 230_025,
  'uc30.baliemdw_identificatie_bsn_niet_gevonden_aanvraag_klaar'        => 230_026,
  'uc30.baliemdw_identificatie_bsn_niet_gevonden_andere_balie'          => 230_027,
  'uc30.baliemdw_baliecode_aanmaken_mislukt_quota'                      => 230_028,
  'uc30.front_desk_audit_case_marked_as_checked'                        => 230_029,
  'uc30.baliemdw_identificatie_gelukt_activatiecode'                    => 230_030,

  'uc30.baliemdw_identificatie_mislukt_vier_ogen'                       => 230_031,
  'uc30.baliemdw_sessie_verlopen'                                       => 230_032,

  'uc30.front_desk_code_already_used'                                   => 230_033,
  'uc30.front_desk_code_successfully_used'                              => 230_034,
  'uc30.front_desk_employee_log_in_fail_front_desk_blocked'             => 230_035,
  'uc30.front_desk_employee_log_in_fail'                                => 230_036,
  'uc30.front_desk_activation_letter_printed'                           => 230_037,
  'uc30.front_desk_activation_letter_marked_as_sent'                    => 230_038,
  'uc30.front_desk_audit_started'                                       => 230_039,
  'uc30.front_desk_employee_front_desk_selected'                        => 230_040,

  'uc30.front_desk_start_activation_process'                            => 230_041,
  'uc30.front_desk_start_activation_process_session'                    => 230_042,
  'uc30.front_desk_employee_start_selection_of_front_desk'              => 230_043,
  'uc30.front_desk_employee_no_front_desk_found_for_identifier'         => 230_044,
  'uc30.front_desk_id_check_correction'                                 => 230_045,
  'uc30.front_desk_maximum_per_day_reached'                             => 230_046,
  'uc30.front_desk_code_no_longer_valid'                                => 230_047,
  'uc30.front_desk_code_combination_invalid'                            => 230_048,

  'uc30.front_desk_id_check_correction_empty_fields'                    => 230_051,
  'uc30.front_desk_id_check_cancelled'                                  => 230_052,
  'uc30.front_desk_employee_session_expired'                            => 230_053,
  'uc30.front_desk_id_check_id_signaled'                                => 230_054,
  'uc30.front_desk_employee_session_ended_front_desk_blocked'           => 230_055,
  'uc30.front_desk_id_check_failed_no_changes'                          => 230_056,
  'uc30.front_desk_activation_process_cancelled'                        => 230_057,

  # UC31
  #
  'uc31.front_desk_show'                                      => 231_001,
  'uc31.front_desk_update_success'                            => 231_002,
  'uc31.front_desk_update_fail'                               => 231_003,
  'uc31.front_desk_create_success'                            => 231_004,
  'uc31.front_desk_create_fail'                               => 231_005,
  'uc31.front_desk_destroy_success'                           => 231_006,
  'uc31.front_desk_destroy_fail'                              => 231_007,
  'uc31.front_desk_block_success'                             => 231_008,
  'uc31.front_desk_block_fail'                                => 231_009,
  'uc31.front_desk_unblock_success'                           => 231_010,
  'uc31.front_desk_unblock_fail'                              => 231_011,
  'uc31.balie_emigratie_aan'                                  => 231_012,
  'uc31.balie_emigratie_uit'                                  => 231_013,
  'uc31.balie_baliecode_quota_wijziging'                      => 231_014,
  'uc31.balie_baliecode_geldigheid_wijziging'                 => 231_015,
  'uc31.balie_baliecode_blokkeren_gelukt'                     => 231_016,
  'uc31.balie_baliecode_blokkeren_mislukt'                    => 231_017,
  'uc31.front_desk_audit_case_shown'                          => 231_020,
  'uc31.front_desk_audit_case_marked_as_finished'             => 231_021,
  'uc31.front_desk_activation_code_duration_changed'          => 231_022,
  'uc31.balie_functie_scheiding_op_4ogen'                     => 231_023,
  'uc31.balie_functie_scheiding_op_controle_achteraf'         => 231_024,
  'uc31.front_desk_overview_shown'                            => 231_025,
  'uc31.front_desk_relation_destroyed'                        => 231_026,
  'uc31.front_desk_relation_destruction_failed'               => 231_027,
  'uc31.front_desk_maximum_per_day_adjusted'                  => 231_028,
  'uc31.front_desk_show_edit_successful'                      => 231_029,
  'uc31.front_desk_warning_unaudited_changed'                 => 231_030,
  'uc31.front_desk_warning_fraud_suspicions_changed'          => 231_031,

  # UC32
  #
  'uc32.afmeldlijst_inzien_gelukt'                            => 232_001,
  'uc32.afmelding_aanmaken_gelukt'                            => 232_002,
  'uc32.afmelding_verwijderen_gelukt'                         => 232_003,

  # UC33
  #
  'uc33.vraag_inzien_gelukt'                                  => 233_001,
  'uc33.vraag_verwijderen_gelukt'                             => 233_002,
  'uc33.vraag_wijzigen_gelukt'                                => 233_003,
  'uc33.vraag_aanmaken_gelukt'                                => 233_004,

  # UC40
  #
  'uc40.beveiligde_bezorging_postcodes_inzien_gelukt'         => 240_001,
  'uc40.beveiligde_bezorging_postcode_verwijderen_gelukt'     => 240_002,
  'uc40.beveiligde_bezorging_postcode_aanmaken_gelukt'        => 240_003,

  # CRON
  #
  'cron.send_activation_reminders_start'                      => 300_000,
  'cron.send_expiry_notifications_start'                      => 300_001,
  'cron.set_accounts_to_expired_start'                        => 300_002,
  'cron.clean_up_expired_start'                               => 300_003,
  'cron.aanvraaggegevens_opschonen_gelukt'                    => 300_004,
  'cron.account_status_naar_vervallen'                        => 300_005,
  'cron.clean_up_expired_finished'                            => 300_006,
  'cron.account_expiry_notification_send'                     => 300_007

}

RESULT_TYPES = {
  :success        => "gelukt",
  :failure        => "mislukt",
  :attempt        => "poging"
}
