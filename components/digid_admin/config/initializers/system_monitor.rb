
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
SYSTEM_MONITOR_MAPPINGS = {
  'pilot_group.listed_successfully'                           => 400_000,
  'pilot_group.viewed_successfully'                           => 400_001,
  'pilot_group.updated_successfully'                          => 400_002,
  'pilot_group.update_failed'                                 => 400_003,

  'pilot_switch.pilot_switches_inzien_gelukt'                 => 400_100,
  'pilot_switch.inzien_gelukt'                                => 400_101,
  'pilot_switch.wijzigen_gelukt'                              => 400_102,
  'pilot_switch.wijzigen_mislukt'                             => 400_103,
  '602'                                                       => 400_104,
  '1541'                                                      => 400_105,
  '1542'                                                      => 400_106,
  '1544'                                                      => 400_107,
  'pilot_switch.inzien_aanpassen_gelukt'                      => 400_108,
  'pilot_switch.wijzigen_via_script'                          => 400_109,
  '1543'                                                      => 400_110,

  'subscriber.added_successfully'                             => 241_000,
  'subscriber.deleted_successfully'                           => 241_001,
  'subscriber.all_deleted_successfully'                       => 241_002,

  'app_version.inzien'                                        => 500_001,
  'app_version.created'                                       => 500_002,
  'app_version.updated'                                       => 500_003,
  'app_version.edit'                                          => 500_004,
  '776'                                                       => 500_005,

  '823'                                                       => 500_006,
  '825'                                                       => 500_007,
  '826'                                                       => 500_008,
  '827'                                                       => 500_009,

  'third_party_app.inzien'                                    => 510_001,
  'third_party_app.created'                                   => 510_002,
  'third_party_app.updated'                                   => 510_003,
  'third_party_app.edit'                                      => 510_004,

  'blacklisted_phone_number.inzien'                           => 600_001,
  'blacklisted_phone_number.created'                          => 600_002,
  'blacklisted_phone_number.updated'                          => 600_003,
  'blacklisted_phone_number.edit'                             => 600_004,
  'blacklisted_phone_number.destroyed'                        => 600_005,

  'four_eyes_review.inzien'                                   => 700_001,
  '1215'                                                      => 700_002,
  '1216'                                                      => 700_003,

  'kiosk.inzien'                                              => 800_001,
  'kiosk.updated'                                             => 800_002,
  'kiosk.edit'                                                => 800_003,
  'kiosk.destroyed'                                           => 800_004,

  'kiosk.four_eyes.inzien'                                    => 800_005,
  'kiosk.four_eyes.accept'                                    => 800_006,
  'kiosk.four_eyes.reject'                                    => 800_007,
  'kiosk.four_eyes.withdraw'                                  => 800_008,


  # digid_hoog from digid_x including code!
  #
  'digid_hoog.request_unblock_letter.abort.invalid_pl'          => 145_156,
  'digid_hoog.request_unblock_letter.abort.invalid_deceased'    => 145_157,
  'digid_hoog.request_unblock_letter.abort.invalid_emigrated'   => 145_158,
  'digid_hoog.request_unblock_letter.abort.invalid_suspended'   => 145_159,
  'digid_hoog.request_unblock_letter.abort.invalid_not_found'   => 145_160,
  'digid_hoog.request_unblock_letter.abort.invalid_investigate' => 145_161,
  'digid_hoog.request_unblock_letter.abort.invalid_error'       => 145_198,
  'digid_hoog.status_rijbewijs_rdw_fault'                       => 145_199,
  'digid_hoog.status_identity_card_rvig_fault'                  => 145_200,

  # UC8
  #
  'uc8.inloggen_beheer_gelukt'                                => 208_001,
  'uc8.inloggen_beheer_mislukt_niet_geauthoriseerd'           => 208_002,
  'uc8.inloggen_beheer_mislukt_onbekende_fout'                => 208_003,
  'uc8.uitloggen_beheer_gelukt'                               => 208_004,
  'uc8.uitloggen_beheer_mislukt'                              => 208_005,
  'uc8.uitloggen_beheersessie_verlopen_inactive'              => 208_006,
  '807'                                                       => 208_007,

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
  '373'                                                       => 213_002,
  '374'                                                       => 213_003,
  '761'                                                       => 213_004,
  '798'                                                       => 213_005,

  # UC14
  #
  'uc14.webdienst_aanmaken_gelukt'                            => 214_001,
  'uc14.webdienst_verwijderen_gelukt'                         => 214_002,
  'uc14.webdienst_inactief_zetten_gelukt'                     => 214_003,
  'uc14.webdienst_actief_zetten_gelukt'                       => 214_004,
  'uc14.webdienst_wijzigen_gelukt'                            => 214_005,
  'uc14.wijziging_webdienst_inzien'                           => 214_006, # Log#808
  'uc14.wijziging_webdienst_geaccordeerd'                     => 214_007, # Log#810
  'uc14.wijziging_webdienst_afgekeurd'                        => 214_008, # Log#811
  'uc14.wijziging_webdienst_ingetrokken'                      => 214_009, # Log#812

  # UC15
  #
  'uc15.transacties_inzien_gelukt'                            => 215_001,
  'uc15.transacties_zoeken_gelukt'                            => 215_002,
  'uc15.inzien_overzicht_omgeving_gelukt'                     => 215_003,

  # UC16
  #
  'uc16.account_overzicht_inzien_gelukt'                   => 216_001,
  'uc16.account_inzien_gelukt'                             => 216_002,
  'uc16.account_wijzigen_gelukt'                           => 216_003,
  'uc16.account_opschorten_gelukt'                         => 216_004,
  'uc16.account_opschorten_ongedaan_maken_gelukt'          => 216_005,
  'uc16.account_omzetten_sofi_bsn_gelukt'                  => 216_006,
  'uc16.account_verwijderen_gelukt'                        => 216_008,
  'uc16.opgeschort_account_verwijderen_gelukt'             => 216_009,
  '933'                                                    => 216_010,
  '928'                                                    => 216_011,
  'uc16.account_rvig_raadplegen_rijbewijs_mislukt'         => 216_012,
  'uc16.account_mu_block_gestart'                          => 216_013,
  'uc16.account_driving_licence_block_gelukt'              => 216_014,
  'uc16.account_mu_block_mislukt'                          => 216_015,
  'uc16.account_mu_block_mislukt_reden'                    => 216_016,
  'uc16.account_mu_block_onmogelijk_switch_uit'            => 216_017,
  'uc16.sms_versturen_mislukt'                             => 216_018,
  'uc16.account_mu_pin_onmogelijk_switch_uit'              => 216_019,
  '1038'                                                   => 216_020,
  '1043'                                                   => 216_021,
  'uc16.account_mu_pin_mislukt'                            => 216_022,
  'uc16.account_mu_pin_mislukt_reden'                      => 216_023,
  'uc16.account_iapi_deblock_gestart'                      => 216_024,
  'uc16.account_iapi_deblock_timeout'                      => 216_025,
  '963'                                                    => 216_026,
  '934'                                                    => 216_027,
  '929'                                                    => 216_028,
  '1126'                                                   => 216_029,
  '1174'                                                   => 216_030,
  'uc16.account_id_card_block_gelukt'                      => 216_031,

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
  '280'                                                         => 221_000,
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
  '376'                                                       => 222_009,

  # UC23
  #
  'uc23.organisatie_overzicht_inzien_gelukt'                  => 223_001,
  'uc23.organisatie_inzien_gelukt'                            => 223_002,
  'uc23.organisatie_aanmaken_gelukt'                          => 223_003,
  'uc23.organisatie_verwijderen_gelukt'                       => 223_004,
  'uc23.organisatie_wijzigen_gelukt'                          => 223_005,

  # UC24
  #
  'uc24.email_reminder_account_activation_send'               => 224_003,
  'uc24.email_block_versturen_gelukt'                         => 224_004,
  'uc24.email_block_versturen_mislukt'                        => 224_005,
  '1039'                                                      => 224_006,
  'uc24.email_pin_versturen_mislukt'                          => 224_007,
  'uc24.email_deblock_versturen_gelukt'                       => 224_008,
  'uc24.email_deblock_versturen_mislukt'                      => 224_009,


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
  'uc25.wijziging_beheeraccount_inzien_gelukt'                => 225_009,
  'uc25.wijziging_beheeraccount_geaccordeerd'                 => 225_010,
  'uc25.wijziging_beheeraccount_afgekeurd'                    => 225_011,
  'uc25.wijziging_beheeraccount_ingetrokken'                  => 225_012,

  # UC26
  #
  'uc26.eigen_beheeraccount_wijzigen_gelukt'                  => 226_001,
  'uc26.eigen_beheeraccount_inzien_gelukt'                    => 226_002,
  'uc26.eigen_beheeraccount_inzien_wijzigen_gelukt'           => 226_003,

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
  'uc27.beheerrol_inzien_wijzigen_gelukt'                     => 227_011,
  'uc27.beheerrol_gewijzigd_ter_accorderen'                   => 227_012,
  'uc27.wijziging_beheerrol_inzien_gelukt'                    => 227_013,
  'uc27.wijziging_beheerrol_geaccordeerd'                     => 227_014,
  'uc27.wijziging_beheerrol_afgekeurd'                        => 227_015,
  'uc27.wijziging_beheerrol_ingetrokken'                      => 227_016,

  # UC28
  #
  'uc28.rapportage_download_gelukt'                           => 228_001, # 612
  'uc28.rapportage_aanvraag_logging_onderzoek_gelukt'         => 228_002, # old
  'uc28.rapportage_aanvraag_account_onderzoek_gelukt'         => 228_003, # old
  'uc28.rapportage_aanvraag_account_zip_onderzoek_gelukt'     => 228_004,
  '613'                                                       => 228_005,
  'uc28.rapportage_aanvraag_account_bsn_onderzoek_gelukt'     => 228_006,
  'uc28.rapportage_aanvraag_account_ip_onderzoek_gelukt'      => 228_007,
  'uc28.rapportage_aanvraag_logging_bsn_onderzoek_gelukt'     => 228_008,
  'uc28.rapportage_aanvraag_logging_ip_onderzoek_gelukt'      => 228_009,
  'uc28.rapportage_aanvraag_logging_zip_onderzoek_gelukt'     => 228_010,

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
  '454'                                                                 => 230_029,
  'uc30.baliemdw_identificatie_gelukt_activatiecode'                    => 230_030,

  'uc30.baliemdw_identificatie_mislukt_vier_ogen'                       => 230_031,
  'uc30.baliemdw_sessie_verlopen'                                       => 230_032,

  '426'                                                                 => 230_033,
  '431'                                                                 => 230_034,
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
  'uc30.front_desk_employee_log_in_successful'                          => 230_058,

  'uc30.baliemdw_niet_gemachtigd'                                       => 230_059,


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
  'uc31.front_desk_audit_confirmed'                           => 231_032,
  'uc31.front_desk_audit_negated'                             => 231_033,

  # UC32
  #
  'uc32.afmeldlijst_inzien_gelukt'                            => 232_001,
  'uc32.afmelding_aanmaken_gelukt'                            => 232_002,
  'uc32.afmelding_verwijderen_gelukt'                         => 232_003,

  # UC33
  #
  '358'                                  => 233_001,
  'uc33.vraag_verwijderen_gelukt'                             => 233_002,
  'uc33.vraag_wijzigen_gelukt'                                => 233_003,
  'uc33.vraag_aanmaken_gelukt'                                => 233_004,

  # UC38
  #
  "375"                                                       => 238_001,

  # UC40
  #
  'uc40.beveiligde_bezorging_postcodes_inzien_gelukt'         => 240_001,
  'uc40.beveiligde_bezorging_postcode_verwijderen_gelukt'     => 240_002,
  'uc40.beveiligde_bezorging_postcode_aanmaken_gelukt'        => 240_003,

  # UC42
  #
  '608'                                                       => 242_001,
  '609'                                                       => 242_002,
  '610'                                                       => 242_003,
  '611'                                                       => 242_004,
  '612'                                                       => 242_005,

  # UC46
  #
  'uc46.bulk_order_overview_shown'                          => 246_001,
  'uc46.bulk_order_show'                                    => 246_002,
  'uc46.bulk_order_approved'                                => 246_003,
  'uc46.bulk_order_rejected'                                => 246_004,
  'uc46.bulk_order_destroyed'                               => 246_005,
  'uc46.bulk_order_created'                                 => 246_006,
  'uc46.bulk_order_download_invalid_bsn'                    => 246_007,
  'uc46.bulk_order_download_account_status'                 => 246_008,
  'uc46.bulk_order_download_address_list'                   => 246_009,
  '1237'                                                    => 246_010,
  'uc46.bulk_order_delete_accounts_finished'                  => 246_011,
  'uc46.bulk_order_account_deleted'                           => 246_012,
  'uc46.bulk_order_deleting_accounts_not_completed'           => 246_013,
  'uc46.bulk_order_add_command_failed'                      => 246_014,
  'uc46.bulk_order_csv_amount'                              => 246_015,
  'uc46.bulk_order_download_overview'                       => 246_016,
  'uc46.bulk_order_created_invalid'                         => 246_017,
  'uc46.bulk_order_brp_details_complete'                    => 246_018,
  'uc46.bulk_order_brp_started'                             => 246_019,
  'uc46.bulk_order_brp_finished'                            => 246_020,
  'uc46.bulk_order_brp_not_completed'                       => 246_021,
  'uc46.bulk_order_brn_request_started'                     => 246_022,
  'uc46.bulk_order_brn_request_finished'                    => 246_023,
  'uc46.bulk_order_brn_request_not_completed'               => 246_024,
  'uc46.bulk_order_change_status_accounts_finished'           => 246_025,
  'uc46.bulk_order_account_status_changed'                    => 246_026,
  'uc46.bulk_order_changing_status_accounts_not_completed'    => 246_027,


  # UC54
  #
  'uc54.eid_certificate_import_error'                         => 254_001,
  'uc54.eid_certificate_overview_shown'                       => 254_002,
  '1117'                                                      => 254_003,
  'uc54.eid_certificate_imported'                             => 254_004,
  'uc54.eid_certificate_downloaded'                           => 254_005,

  # UC55
  #
  '1103'                                                      => 255_001,
  '1104'                                                      => 255_002,
  '1105'                                                      => 255_003,
  '1106'                                                      => 255_004,
  '1107'                                                      => 255_005,
  'uc55.eid_at_request_rejected'                              => 255_006,
  '1109'                                                      => 255_007,
  '1110'                                                      => 255_008,
  '1111'                                                      => 255_009,
  'uc55.eid_at_request_aborted'                               => 255_010,

  # UC56
  #
  'uc56.eid_crl_import_error'                                 => 256_001,
  '1112'                                                      => 256_002,
  '1113'                                                      => 256_003,
  '1114'                                                      => 256_004,
  'uc56.eid_crl_downloaded'                                   => 256_005,

  # UC58
  #
  'uc58.rda_certificate_import_error'                         => 258_001,
  'uc58.rda_certificate_overview_shown'                       => 258_002,
  'uc58.rda_certificate_shown'                                => 258_003,
  'uc58.rda_certificate_imported'                             => 258_004,
  'uc58.rda_certificate_downloaded'                           => 258_005,

  # UC59
  #
  'uc59.rda_crl_import_error'                                 => 259_001,
  '1192'                                                      => 259_002,
  'uc59.rda_crl_shown'                                        => 259_003,
  'uc59.rda_crl_imported'                                     => 259_004,
  'uc59.rda_crl_downloaded'                                   => 259_005,

  # UC62
  '1347'                                                      => 259_006,

  # CRON
  #
  'cron.aanvraaggegevens_opschonen_gelukt'                    => 300_004,

  'admin_report.overview_success'                             => 302_001,
  'admin_report.filter_overview_success'                      => 302_002,

  "412"                                                       => 103_013,

}.freeze

ActiveSupport::Notifications.subscribe(/^digid_admin\.(.*)/) do |*args|
  event             = ActiveSupport::Notifications::Event.new(*args)
  translation       = I18n.t(event.name, event.payload)
  key               = event.name.gsub(/^digid_admin\./, '')
  code              = SYSTEM_MONITOR_MAPPINGS[key]
  code              = key if code.nil? && key.to_i > 0
  raise('Expected a log code in SystemMonitor!') if code.nil?
  account_id        = event.payload[:account_id] if event.payload.include?(:account_id)

  log               = LogWrite.new(name: translation, transaction_id: event.transaction_id, code: code)
  log.session_id    = event.payload.delete(:session_id) if event.payload.include?(:session_id)
  log.ip_address    = ''
  log.ip_address    = event.payload.delete(:ip_address) if event.payload.include?(:ip_address)
  manager_id        = event.payload.delete(:manager_id) || -1
  log.manager_id    = manager_id
  log.webservice_id = event.payload.delete(:webservice_id) if event.payload.include?(:webservice_id)
  log.account_id    = event.payload.delete(:account_id) if event.payload.include?(:account_id)
  log.pseudoniem    = event.payload.delete(:pseudonym) if event.payload.include?(:pseudonym)
  log.subject_type  = event.payload.delete(:subject_type) if event.payload.include?(:subject_type)
  log.subject_id    = event.payload.delete(:subject_id) if event.payload.include?(:subject_id)
  log.sector_number = event.payload.delete(:sector_number) if event.payload.include?(:sector_number)
  log.sector_name   = event.payload.delete(:sector_name) if event.payload.include?(:sector_name)

  unless log.sector_number
    sectorcode = Sectorcode.where(account_id: log.account_id).first
    if sectorcode
      log.sector_number = sectorcode.sectoraalnummer
      log.sector_name   = sectorcode.sector.name
    end
  end

  # Find the subject. This should be the only <model>_id left in the payload
  subject_key = event.payload.keys.find { |key| key.to_s =~ /[\w|_]*_id/ }
  if log.subject_type.blank? && subject_key
    log.subject_type = subject_key.to_s.gsub(/_id$/, '').split('__').map{ |part| part.camelize }.join('::')
    log.subject_id   = event.payload.delete(subject_key.to_sym)
  end

  if account_id && event.payload[:account_transactie]
    log.manager_id = nil
  else
    ManagerLogWrite.create(log.attributes)
  end
  log.save!

  if account_id && event.payload[:mydigid]
    AccountLogWrite.create(name: translation, code: code, account_id: account_id, ip_address: '')
  end
end
