
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

# NOTICE: other seeds need to run first to be able to create the Role 'Manage everything'

# create Managers to be able to use admin within development environments
if %w(development o1 o2 t1 t2 t3).include?(Rails.env)
  certificate =
    %(-----BEGIN CERTIFICATE-----
    SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    -----END CERTIFICATE-----
    )

  def find_permissions(*args)
    Permission.where(name: args)
  end

  def find_roles(*args)
    Role.where(name: args)
  end

  roles = Role.seed(:name, [
                      { name: 'Manage everything', permissions: Permission.all },
                      {
                        name: '* FRD DigiD',
                        permissions: find_permissions('Afmeldlijst beheren',
                                                      'Afmeldlijst inzien',
                                                      'Afmeldlijst toevoegen',
                                                      'Balies inzien',
                                                      'Balies wijzigen',
                                                      'Bulkopdrachten accorderen',
                                                      'Bulkopdrachten beheren',
                                                      'Bulkopdrachten inzien',
                                                      'Bulkopdrachten toevoegen',
                                                      'Bulkopdrachten verwijderen',
                                                      'DigiD accounts beheren',
                                                      'DigiD accounts inzien',
                                                      'DigiD accounts opschorten',
                                                      'DigiD accounts sofi naar BSN zetten',
                                                      'DigiD accounts verwijderen',
                                                      'DigiD accounts wijzigen',
                                                      'eID middelen bij DigiD account beheren',
                                                      'eID middelen bij DigiD account inzien',
                                                      'Fraude - Accountgegevens opvragen',
                                                      'Fraude - Loggegevens opvragen',
                                                      'Fraude - Transactiegegevens opvragen',
                                                      'Fraude Accountrapportage inzien',
                                                      'Fraude Lograpportage inzien',
                                                      'Fraude Transactiegegevens inzien',
                                                      'Frauderapportages inzien',
                                                      'Het BSN nummer op basis van het emailadres',
                                                      'Het BSN nummer op basis van het telefoonnummer',
                                                      'Integriteitsrapportages inzien',
                                                      'Maandrapportages inzien',
                                                      'Nieuws- en onderhoudsberichten inzien',
                                                      'Organisaties beheren',
                                                      'Organisaties inzien',
                                                      'Organisaties opschorten',
                                                      'Organisaties toevoegen',
                                                      'Organisaties wijzigen',
                                                      'Postcodegebieden voor beveiligde bezorging beheren',
                                                      'Postcodegebieden voor beveiligde bezorging inzien',
                                                      'Postkanaal alle briefgegevens inzien',
                                                      'Postkanaal wijzigen',
                                                      'Raadplegen basisregistratie',
                                                      'Raadplegen basisregistratie - inzien gegevens',
                                                      'Raadplegen basisregistratie - inzien status',
                                                      'Securityrapportages inzien',
                                                      'SSO domeinen inzien',
                                                      'Transactielog inzien',
                                                      'Webdienstaansluitingen beheren',
                                                      'Webdienstaansluitingen inzien',
                                                      'Webdienstaansluitingen opschorten',
                                                      'Webdienstaansluitingen toevoegen',
                                                      'Webdienstaansluitingen wijzigen',
                                                      'Weekrapportages inzien')
                      },
                      {
                        name: '* FRD DigiD+',
                        permissions: find_permissions('Beheeraccounts inzien')
                      },
                      {
                        name: '* KB DigiD',
                        permissions: find_permissions('Afmeldlijst beheren',
                                                      'Afmeldlijst inzien',
                                                      'Afmeldlijst toevoegen',
                                                      'Balies inzien',
                                                      'Balies wijzigen',
                                                      'Bulkopdrachten accorderen',
                                                      'Bulkopdrachten beheren',
                                                      'Bulkopdrachten inzien',
                                                      'Bulkopdrachten toevoegen',
                                                      'Bulkopdrachten verwijderen',
                                                      'DigiD accounts beheren',
                                                      'DigiD accounts inzien',
                                                      'DigiD accounts opschorten',
                                                      'DigiD accounts sofi naar BSN zetten',
                                                      'DigiD accounts verwijderen',
                                                      'DigiD accounts wijzigen',
                                                      'Fraude - Accountgegevens opvragen',
                                                      'Fraude - Loggegevens opvragen',
                                                      'Fraude - Transactiegegevens opvragen',
                                                      'Fraude Accountrapportage inzien',
                                                      'Fraude Lograpportage inzien',
                                                      'Fraude Transactiegegevens inzien',
                                                      'Frauderapportages inzien',
                                                      'Het BSN nummer op basis van het emailadres',
                                                      'Het BSN nummer op basis van het telefoonnummer',
                                                      'Integriteitsrapportages inzien',
                                                      'Maandrapportages inzien',
                                                      'Nieuws- en onderhoudsberichten inzien',
                                                      'Organisaties beheren',
                                                      'Organisaties inzien',
                                                      'Organisaties opschorten',
                                                      'Organisaties toevoegen',
                                                      'Organisaties wijzigen',
                                                      'Postcodegebieden voor beveiligde bezorging beheren',
                                                      'Postcodegebieden voor beveiligde bezorging inzien',
                                                      'Postkanaal alle briefgegevens inzien',
                                                      'Postkanaal wijzigen',
                                                      'Raadplegen basisregistratie',
                                                      'Raadplegen basisregistratie - inzien gegevens',
                                                      'Raadplegen basisregistratie - inzien status',
                                                      'Securityrapportages inzien',
                                                      'SSO domeinen inzien',
                                                      'Transactielog inzien',
                                                      'Webdienstaansluitingen beheren',
                                                      'Webdienstaansluitingen inzien',
                                                      'Webdienstaansluitingen opschorten',
                                                      'Webdienstaansluitingen toevoegen',
                                                      'Webdienstaansluitingen wijzigen',
                                                      'Weekrapportages inzien')
                      },
                      {
                        name: '* KB DigiD Outtakerechten (tijdelijk)',
                        permissions: find_permissions('Beheeraccounts beheren',
                                                      'Beheeraccounts inzien',
                                                      'Beheerrollen beheren',
                                                      'Beheerrollen inzien',
                                                      'Fraude - Accountgegevens opvragen',
                                                      'Fraude - Loggegevens opvragen',
                                                      'Fraude - Transactiegegevens opvragen',
                                                      'Fraude Accountrapportage inzien',
                                                      'Fraude Lograpportage inzien',
                                                      'Fraude Transactiegegevens inzien',
                                                      'Frauderapportages inzien',
                                                      'Het BSN nummer op basis van het emailadres',
                                                      'Het BSN nummer op basis van het telefoonnummer',
                                                      'Integriteitsrapportages inzien',
                                                      'Nieuws- en onderhoudsberichten activeren',
                                                      'Nieuws- en onderhoudsberichten beheren',
                                                      'Nieuws- en onderhoudsberichten inzien',
                                                      'Nieuws- en onderhoudsberichten toevoegen',
                                                      'Nieuws- en onderhoudsberichten verwijderen',
                                                      'Nieuws- en onderhoudsberichten wijzigen',
                                                      'Pilot switches beheren',
                                                      'Pilot switches inzien',
                                                      'Pilotgroepen beheren',
                                                      'Pilotgroepen inzien',
                                                      'Postcodegebieden voor beveiligde bezorging beheren',
                                                      'Postcodegebieden voor beveiligde bezorging inzien',
                                                      'Postkanaal wijzigen')
                      },
                      {
                        name: '* Pilot groep beheer (tijdelijk)',
                        permissions: find_permissions('Pilot switches beheren',
                                                      'Pilot switches inzien',
                                                      'Pilotgroepen beheren',
                                                      'Pilotgroepen inzien')
                      },
                      {
                        name: '* SC DigiD aansluitingen',
                        permissions: find_permissions('Organisaties beheren',
                                                      'Organisaties inzien',
                                                      'Organisaties toevoegen',
                                                      'Organisaties verwijderen',
                                                      'Organisaties wijzigen',
                                                      'SSO domeinen inzien',
                                                      'Webdienstaansluitingen beheren',
                                                      'Webdienstaansluitingen inzien',
                                                      'Webdienstaansluitingen opschorten',
                                                      'Webdienstaansluitingen toevoegen',
                                                      'Webdienstaansluitingen wijzigen')
                      },
                      {
                        name: '* SC DigiD Account verwijderen (tijdelijk)',
                        permissions: find_permissions('DigiD accounts verwijderen',
                                                      'Vragen beheren',
                                                      'Vragen inzien',
                                                      'Vragen toevoegen',
                                                      'Vragen verwijderen',
                                                      'Vragen wijzigen')
                      },
                      {
                        name: '* SC DigiD Balie',
                        permissions: find_permissions('Balies inzien',
                                                      'Balies toevoegen',
                                                      'Balies wijzigen')
                      },
                      {
                        name: '* SC DigiD Burger',
                        permissions: find_permissions('Afmeldlijst inzien',
                                                      'Afmeldlijst toevoegen',
                                                      'Balies inzien',
                                                      'DigiD accounts inzien',
                                                      'DigiD accounts opschorten',
                                                      'DigiD accounts sofi naar BSN zetten',
                                                      'Nieuws- en onderhoudsberichten inzien',
                                                      'Organisaties inzien',
                                                      'Postkanaal alle briefgegevens inzien',
                                                      'Raadplegen basisregistratie',
                                                      'Raadplegen basisregistratie - inzien gegevens',
                                                      'Raadplegen basisregistratie - inzien status',
                                                      'Webdienstaansluitingen inzien')
                      },
                      {
                        name: '* SC DigiD+',
                        permissions: find_permissions('Balies blokkeren',
                                                      'Balies inzien',
                                                      'Balies toevoegen',
                                                      'Balies wijzigen',
                                                      'Nieuws- en onderhoudsberichten inzien',
                                                      'Vragen inzien',
                                                      'Vragen toevoegen',
                                                      'Vragen verwijderen',
                                                      'Vragen wijzigen')
                      } # , { name: '..', permissions: find_permissions(..) }
                    ])

  defaults = { active: true, mobile_number: '1' * 8, roles: [roles.first] }

  # added users for default roles
  Manager.seed(:account_name, [
                 { account_name: '! root',
                   active: true,
                   certificate: certificate,
                   name: 'super',
                   surname: 'user',
                   mobile_number: '0' * 8,
                   superuser: true
                 },
                 { account_name: '* FRD DigiD',
                   active: true,
                   name: 'FRD',
                   surname: 'DigiD',
                   mobile_number: '0' * 8,
                   roles: find_roles('* FRD DigiD')
                 },
                 { account_name: '* FRD DigiD+',
                   active: true,
                   name: 'FRD',
                   surname: 'DigiD+',
                   mobile_number: '0' * 8,
                   roles: find_roles('* FRD DigiD+')
                 },
                 { account_name: '* KB DigiD',
                   active: true,
                   name: 'KB',
                   surname: 'DigiD',
                   mobile_number: '0' * 8,
                   roles: find_roles('* KB DigiD')
                 },
                 { account_name: '* KB DigiD Outtakerechten (tijdelijk)',
                   active: true,
                   name: 'KB',
                   surname: 'DigiD Outtakerechten (tijdelijk)',
                   mobile_number: '0' * 8,
                   roles: find_roles('* KB DigiD Outtakerechten (tijdelijk)')
                 },
                 { account_name: '* Pilot groep beheer (tijdelijk)',
                   active: true,
                   name: 'Pilot',
                   surname: 'groep beheer (tijdelijk)',
                   mobile_number: '0' * 8,
                   roles: find_roles('* Pilot groep beheer (tijdelijk)')
                 },
                 { account_name: '* SC DigiD aansluitingen',
                   active: true,
                   name: 'SC',
                   surname: 'DigiD aansluitingen',
                   mobile_number: '0' * 8,
                   roles: find_roles('* SC DigiD aansluitingen')
                 },
                 { account_name: '* SC DigiD Account verwijderen (tijdelijk)',
                   active: true,
                   name: 'SC',
                   surname: 'DigiD Account verwijderen (tijdelijk)',
                   mobile_number: '0' * 8,
                   roles: find_roles('* SC DigiD Account verwijderen (tijdelijk)')
                 },
                 { account_name: '* SC DigiD Balie',
                   active: true,
                   name: 'SC',
                   surname: 'DigiD Balie',
                   mobile_number: '0' * 8,
                   roles: find_roles('* SC DigiD Balie')
                 },
                 { account_name: '* SC DigiD Burger',
                   active: true,
                   name: 'SC',
                   surname: 'DigiD Burger',
                   mobile_number: '0' * 8,
                   roles: find_roles('* SC DigiD Burger')
                 },
                 { account_name: '* SC DigiD+',
                   active: true,
                   name: 'SC',
                   surname: 'DigiD+',
                   mobile_number: '0' * 8,
                   roles: find_roles('* SC DigiD+')
                 },
                 defaults.merge(account_name: 'PPPPPPPPPP', active: true, name: 'PPPP', surname: 'PPPPPPPPP'),
                 defaults.merge(account_name: 'PPPPPPPP', active: true, name: 'PPPP', surname: 'PPPPPPPPPPPPPP'),
                 defaults.merge(account_name: 'PPPPPP', name: 'PPPPPPP', surname: 'PPPPP'),
                 defaults.merge(account_name: 'PPPPPP', name: 'PPPPP', surname: 'PPPPPP'),
                 defaults.merge(account_name: 'PPPPPPP', name: 'PPPP', surname: 'PPPPPP'),
                 defaults.merge(account_name: 'PPPPPPP', name: 'PPPPP', surname: 'PPPPPP'),
                 defaults.merge(account_name: 'PPPPPPP', name: 'PPPPP', surname: 'PPPPPP'),
                 defaults.merge(account_name: 'PPPPPPPP', name: 'PPPPPPP', surname: 'PPPPPPP'),
                 defaults.merge(account_name: 'PPPPPPP', name: 'PPP', surname: 'PPPPPPPPPP'),
                 defaults.merge(account_name: 'PPPPPP', name: 'PPPPPPP', surname: 'PPPPP'),
                 defaults.merge(account_name: 'PPPPPP', name: 'PPPPPP', surname: 'PPPPP'),
                 defaults.merge(account_name: 'PPPPP', name: 'PPPPPPP', surname: 'PPPPP'),
                 defaults.merge(account_name: 'PPPPPP', name: 'PPPPPPP', surname: 'PPPPP'),
                 defaults.merge(account_name: 'PPPPPP', name: 'PPP', surname: 'PPPPP'),
                 defaults.merge(account_name: 'PPPPPPPPP', name: 'PPPPP', surname: 'PPPPPPPP'),
                 defaults.merge(account_name: 'PPPPP', name: 'PPPPP', surname: 'PPPPPPPP'),
                 defaults.merge(account_name: 'PPPPP', name: 'PPPPPP', surname: 'PPPP'),
                 defaults.merge(account_name: 'PPPPPP', name: 'PPPP', surname: 'PPPPP'),
                 defaults.merge(account_name: 'PPPP', name: 'PPPPP', surname: 'PPP'),
                 defaults.merge(account_name: 'PPPPPPPPP', name: 'PPPP', surname: 'PPPPPPPP'),
                 defaults.merge(account_name: 'PPPPPP', name: 'PPPPPPP', surname: 'PPPPP'),
                 defaults.merge(account_name: 'PPPPPPPPP', name: 'PPPPPPP', surname: 'PPPPPPPP'),
                 defaults.merge(account_name: 'PPPPPP', name: 'PPPP', surname: 'PPPPPPPPPPPP'),
                 defaults.merge(account_name: 'PPPPPP', name: 'PPPPPPPP', surname: 'PPPPPPPPPPPPP'),
                 defaults.merge(account_name: 'PPPPPPPP', name: 'PPP', surname: 'PPPPPPP'),
               ])
end
