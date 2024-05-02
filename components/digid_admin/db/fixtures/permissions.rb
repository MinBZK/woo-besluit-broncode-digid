
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

Permission.seed(:name, [
                  { name: 'Accorderen eigen wijzigingen',                       identifier: [:accept_own_change,   :four_eyes_review] },

                  { name: 'Beheeraccounts inzien',                              identifier: [:read,   :manager] },
                  { name: 'Beheeraccounts beheren',                             identifier: [:manage, :manager] },

                  { name: 'Beheerrollen inzien',                                identifier: [:read,   :role] },
                  { name: 'Beheerrollen beheren',                               identifier: [:manage, :role] },

                  { name: 'Nieuws- en onderhoudsberichten inzien',              identifier: [:read,   :news_item] },
                  { name: 'Nieuws- en onderhoudsberichten beheren',             identifier: [:manage, :news_item] },

                  { name: 'Sectoren inzien',                                    identifier: [:read,   :sector] },
                  { name: 'Sectoren beheren',                                   identifier: [:manage, :sector] },

                  { name: 'Organisaties inzien',                                identifier: [:read,   :organization] },
                  { name: 'Organisaties beheren',                               identifier: [:manage, :organization] },

                  { name: 'Webdienstaansluitingen inzien',                      identifier: [:read,   :webservice] },
                  { name: 'Webdienstaansluitingen beheren',                     identifier: [:manage, :webservice] },

                  { name: 'SSO domeinen inzien',                                identifier: [:read,   :sso_domain] },
                  { name: 'SSO domeinen beheren',                               identifier: [:manage, :sso_domain] },

                  { name: 'Transactielog inzien',                               identifier: [:read,   :log] },

                  { name: 'Overzicht omgevingen inzien',                        identifier: [:read,   :omgeving] },

                  { name: 'DigiD accounts inzien',                              identifier: [:read,   :account] },
                  { name: 'DigiD accounts beheren',                             identifier: [:manage, :account] },
                  { name: 'Raadplegen basisregistratie',                        identifier: [:gba_request, :account] },

                  { name: 'eID middelen bij DigiD account inzien',              identifier: [:view, :eid__mu] },
                  { name: 'eID middelen bij DigiD account beheren',             identifier: [:update, :eid__mu] },

                  { name: 'SMS kanaal inzien',                                  identifier: [:read,   :sms_challenge] },
                  { name: 'Postkanaal inzien',                                  identifier: [:read,   :activation_letter_file] },
                  { name: 'Email kanaal inzien',                                identifier: [:read,   :sent_email] },
                  { name: 'Postkanaal wijzigen',                                identifier: [:manage, :activation_letter_file] },

                  { name: 'Pilotgroepen inzien',                                identifier: [:read,   :pilot_group] },
                  { name: 'Pilotgroepen beheren',                               identifier: [:manage, :pilot_group] },

                  { name: 'Pilot switches inzien',                              identifier: [:read,   :switch] },
                  { name: 'Pilot switches beheren',                             identifier: [:manage, :switch] },

                  { name: 'Bulkopdrachten inzien',                              identifier: [:read,   :bulk_order] },
                  { name: 'Bulkopdrachten beheren',                             identifier: [:manage, :bulk_order] },

                  { name: 'DigiD app-versies inzien',                           identifier: [:read, :app_version] },
                  { name: 'DigiD app-versies beheren',                          identifier: [:manage, :app_version] },

                  { name: 'Third-party app(s) inzien',                           identifier: [:read, :third_party_app] },
                  { name: 'Third-party app(s) beheren',                          identifier: [:manage, :third_party_app] },

                  { name: 'Blacklist telefoonnummers inzien',                   identifier: [:read, :blacklisted_phone_number] },
                  { name: 'Blacklist telefoonnummers beheren',                  identifier: [:manage, :blacklisted_phone_number] },

                  { name: 'Kiosks inzien',                                      identifier: [:read, :kiosk] },
                  { name: 'Kiosks beheren',                                     identifier: [:manage, :kiosk] },

                  # Deelprivileges

                  # Nieuws
                  { name: 'Nieuws- en onderhoudsberichten toevoegen',           identifier: [:create,   :news_item] },
                  { name: 'Nieuws- en onderhoudsberichten wijzigen',            identifier: [:update,   :news_item] },
                  { name: 'Nieuws- en onderhoudsberichten verwijderen',         identifier: [:destroy,  :news_item] },
                  { name: 'Nieuws- en onderhoudsberichten activeren',           identifier: [:activate, :news_item] },

                  # Organisaties
                  { name: 'Organisaties toevoegen',                             identifier: [:create,   :organization] },
                  { name: 'Organisaties wijzigen',                              identifier: [:update,   :organization] },
                  { name: 'Organisaties verwijderen',                           identifier: [:destroy,  :organization] },
                  { name: 'Organisaties opschorten',                            identifier: [:suspend,  :organization] },

                  # Webservices
                  { name: 'Webdienstaansluitingen toevoegen',                   identifier: [:create,   :webservice] },
                  { name: 'Webdienstaansluitingen wijzigen',                    identifier: [:update,   :webservice] },
                  { name: 'Webdienstaansluitingen opschorten',                  identifier: [:suspend,  :webservice] },
                  { name: 'Webdienstaansluitingen accorderen',                  identifier: [:accorderen, :webservice] },

                  # Accounts
                  { name: 'DigiD accounts opschorten',                          identifier: [:suspend,               :account] },
                  { name: 'DigiD accounts sofi naar BSN zetten',                identifier: [:convert_sofi,          :account] },
                  { name: 'DigiD accounts wijzigen',                            identifier: [:update,                :account] },
                  { name: 'DigiD accounts verwijderen',                         identifier: [:destroy,               :account] },
                  { name: "DigiD accounts vervangen account blokkeren",         identifier: [:block_account_replace, :account] },

                  { name: 'Raadplegen basisregistratie - inzien status',        identifier: [:view_gba_status,  :account] },
                  { name: 'Raadplegen basisregistratie - inzien gegevens',      identifier: [:view_gba_data,    :account] },

                  # Letters
                  { name: 'Postkanaal adressen inzien',                         identifier: [:view_addresses,       :activation_letter_file] },
                  { name: 'Postkanaal alle briefgegevens inzien',               identifier: [:last_activation_data, :account] },

                  # SMS
                  { name: 'SMS kanaal: telefoonnummers inzien',                 identifier: [:view_phone_numbers, :sms_challenge] },
                  { name: 'SMS kanaal: SMS codes inzien',                       identifier: [:view_sms_codes,     :sms_challenge] },

                  # Email
                  { name: 'Email kanaal: emailadressen inzien',                 identifier: [:view_email_addressess,  :sent_email] },
                  { name: 'Email kanaal: Email codes inzien',                   identifier: [:view_email_codes,       :sent_email] },

                  # Rapportages
                  { name: 'Frauderapportages inzien',                           identifier: [:read_fraud,     :admin_report] },
                  { name: 'Integriteitsrapportages inzien',                     identifier: [:read_integrity, :admin_report] },
                  { name: 'Maandrapportages inzien',                            identifier: [:read_monthly,   :admin_report] },
                  { name: 'Weekrapportages inzien',                             identifier: [:read_weekly,    :admin_report] },
                  { name: 'Securityrapportages inzien',                         identifier: [:read_sec,       :admin_report] },
                  { name: 'Standaardrapportages inzien',                        identifier: [:read_std,       :admin_report] },

                  # Balies
                  { name: 'Balies inzien',                                      identifier: [:read,       :front_desk] },
                  { name: 'Balies beheren',                                     identifier: [:manage,     :front_desk] },
                  { name: 'Balies toevoegen',                                   identifier: [:create,     :front_desk] },
                  { name: 'Balies wijzigen',                                    identifier: [:update,     :front_desk] },
                  { name: 'Balies verwijderen',                                 identifier: [:destroy,    :front_desk] },
                  { name: 'Balies blokkeren',                                   identifier: [:block,      :front_desk] },
                  { name: 'Balies fraudevermoeden beheren',                     identifier: [:audit,      :verification] }, # PRIV45

                  # UC32 afmeldlijst
                  { name: 'Afmeldlijst inzien',                                 identifier: [:read,       :afmeldlijst] },
                  { name: 'Afmeldlijst beheren',                                identifier: [:manage,     :afmeldlijst] },
                  { name: 'Afmeldlijst toevoegen',                              identifier: [:create,     :afmeldlijst] },
                  { name: 'Afmeldlijst verwijderen',                            identifier: [:destroy,    :afmeldlijst] },

                  # FAQ
                  { name: 'Vragen inzien',                                      identifier: [:read,     :question] },
                  { name: 'Vragen beheren',                                     identifier: [:manage,   :question] },
                  { name: 'Vragen toevoegen',                                   identifier: [:create,   :question] },
                  { name: 'Vragen wijzigen',                                    identifier: [:update,   :question] },
                  { name: 'Vragen verwijderen',                                 identifier: [:destroy,  :question] },

                  # Fraude onderzoek
                  { name: 'Fraude - Loggegevens opvragen',                      identifier: [:create_log, :fraud_report] },
                  { name: 'Fraude - Accountgegevens opvragen',                  identifier: [:create_gba, :fraud_report] },

                  { name: 'Fraude Lograpportage inzien',                        identifier: [:read_adhoc_log, :admin_report] },
                  { name: 'Fraude Accountrapportage inzien',                    identifier: [:read_adhoc_gba, :admin_report] },

                  # nieuwe zoek permissies
                  { name: 'Het BSN nummer op basis van het emailadres',         identifier: [:search_email,   :account] },
                  { name: 'Het BSN nummer op basis van het telefoonnummer',     identifier: [:search_mobile,  :account] },
                  { name: 'Het BSN nummer op basis van de gebruikersnaam',      identifier: [:search_username,   :account] },

                  # Fraude onderzoek (Transactiegegevens)
                  { name: 'Fraude - Transactiegegevens opvragen',               identifier: [:create_tx,      :fraud_report] },
                  { name: 'Fraude Transactiegegevens inzien',                   identifier: [:read_adhoc_tx,  :admin_report] },

                  # Postcodegebieden voor beveiligde bezorging
                  { name: 'Postcodegebieden voor beveiligde bezorging inzien',  identifier: [:read,   :beveiligde_bezorging_postcode] },
                  { name: 'Postcodegebieden voor beveiligde bezorging beheren', identifier: [:manage, :beveiligde_bezorging_postcode] },

                  # Bulkopdrachten
                  { name: 'Bulkopdrachten toevoegen',                           identifier: [:create,   :bulk_order] },
                  { name: 'Bulkopdrachten verwijderen',                         identifier: [:_destroy, :bulk_order] }, # see ability.rb
                  { name: 'Bulkopdrachten accorderen',                          identifier: [:_approve, :bulk_order] },

                  # DigiD app versies
                  { name: 'DigiD app-versies toevoegen',                        identifier: [:create,  :app_version] },
                  { name: 'DigiD app-versies wijzigen',                         identifier: [:update,  :app_version] },
                  { name: 'DigiD app-versies verwijderen',                      identifier: [:destroy, :app_version] },
                  { name: 'DigiD app-versies accorderen',                       identifier: [:accorderen, :app_version] },

                  # Telefoonnummers blacklist
                  { name: 'Blacklist telefoonnummers toevoegen',                identifier: [:create,  :blacklisted_phone_number] },
                  { name: 'Blacklist telefoonnummers wijzigen',                 identifier: [:update,  :blacklisted_phone_number] },
                  { name: 'Blacklist telefoonnummers verwijderen',              identifier: [:destroy, :blacklisted_phone_number] },

                  # Kiosks
                  { name: 'Kiosks wijzigen',                                    identifier: [:update, :kiosk] },
                  { name: 'Kiosks verwijderen',                                 identifier: [:destroy, :kiosk] },
                  { name: 'Kiosks accorderen',                                  identifier: [:accorderen, :kiosk] },

                  # eID AT-verzoeken
                  { name: 'eID AT-verzoeken inzien',                            identifier: [:read, :eid__at_request] },
                  { name: 'eID AT-verzoeken beheren',                           identifier: [:manage, :eid__at_request] },

                  # eID certificaten
                  { name: 'eID certificaten inzien',                            identifier: [:read, :eid__certificate] },
                  { name: 'eID certificaten beheren',                           identifier: [:manage, :eid__certificate] },

                  # eID CRL
                  { name: 'eID CRL inzien',                                     identifier: [:read, :eid__crl] },
                  { name: 'eID CRL beheren',                                    identifier: [:manage, :eid__crl] },

                  # RDA certificaten
                  { name: 'RDA certificaten inzien',                            identifier: [:read, :rda__certificate] },
                  { name: 'RDA certificaten beheren',                           identifier: [:manage, :rda__certificate] },

                  # RDA CRL
                  { name: 'RDA CRL inzien',                                     identifier: [:read, :rda__crl] },
                  { name: 'RDA CRL beheren',                                    identifier: [:manage, :rda__crl] },

                  # Certificaatmeldingen
                  { name: 'Certificaatmeldingen',                               identifier: [:view_cert_alerts, :ApplicationController] },

                  # 4-ogen raportage
                  { name: 'Controle-overzicht beheerders beheren',              identifier: [:manage, :four_eyes_report] },
                  { name: 'Controle-overzicht beheerders inzien',               identifier: [:read, :four_eyes_report] },

                  # Microservices health
                  { name: 'Microservices inzien',                               identifier: [:read, :microservice] },

                  { name: 'Dc::Organisaties inzien',                            identifier: [:read,   :dc__organization] },
                  { name: 'Dc::Organisaties beheren',                           identifier: [:manage, :dc__organization] },

                  { name: 'Dc::Aansluitingen inzien',                           identifier: [:read,   :dc__connection] },
                  { name: 'Dc::Aansluitingen beheren',                          identifier: [:manage, :dc__connection] },

                  { name: 'Dc::Diensten inzien',                                identifier: [:read,   :dc__service] },
                  { name: 'Dc::Diensten beheren',                               identifier: [:manage, :dc__service] },

                  { name: 'Dc::Verwerkingsresultaten inzien',                   identifier: [:read,   :dc__local_metadata_process_result] },

                  # Nationalities
                  { name: 'Nationaliteit inzien',                               identifier: [:read, :nationality] },
                  { name: 'Nationaliteit beheren',                              identifier: [:manage, :nationality] },

                  { name: 'Whitelist telefoonnummers inzien',                   identifier: [:read, :whitelisted_phone_number] },
                  { name: 'Whitelist telefoonnummers beheren',                  identifier: [:manage, :whitelisted_phone_number] }
            ])

# in case the permissions for toevoegen and verwijderen had been created, remove them
# use a destroy_all instead of delete_all to make sure the links with administrators are removed as well
Permission.where(name: 'Postcodegebieden voor beveiligde bezorging toevoegen').destroy_all
Permission.where(name: 'Postcodegebieden voor beveiligde bezorging verwijderen').destroy_all

# in case the permissions for balies (oud) had been created, remove them
Permission.where(name: 'Balies inzien (oud)').destroy_all
Permission.where(name: 'Balies beheren (oud)').destroy_all
Permission.where(name: 'Balies toevoegen (oud)').destroy_all
Permission.where(name: 'Balies wijzigen (oud)').destroy_all
Permission.where(name: 'Balies verwijderen (oud)').destroy_all
Permission.where(name: 'Balies blokkeren (oud)').destroy_all
Permission.where(name: 'Baliecodes blokkeren (oud)').destroy_all
Permission.where(name: 'Baliecodes blokkeren').destroy_all
Permission.where(name: 'Registratie eIDs inzien').destroy_all
Permission.where(name: 'Uitvoeren registratie eIDs').destroy_all
Permission.where(name: 'Raadplegen MU').destroy_all
Permission.where(name: 'Raadplegen MU data - inzien gegevens').destroy_all
Permission.where(name: 'Postkanaal laatste briefgegevens inzien').destroy_all
Permission.where(name: 'Postkanaal briefgegevens inzien').destroy_all
Permission.where(name: 'Webdienstaansluitingen verwijderen').destroy_all
Permission.where(name: 'Bulkverwijdering inzien').destroy_all
Permission.where(name: 'Bulkverwijdering beheren').destroy_all
Permission.where(name: 'Bulkverwijdering toevoegen').destroy_all
Permission.where(name: 'Bulkverwijdering verwijderen').destroy_all
Permission.where(name: 'Dc::Dienstdefinities inzien').destroy_all
Permission.where(name: 'Dc::Dienstdefinities beheren').destroy_all
Permission.where(name: 'Dc::Dienst definities inzien').destroy_all
Permission.where(name: 'Dc::Dienst definities beheren').destroy_all
