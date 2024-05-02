
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

class Question < AccountBase
  validates :answer, presence: true
  validates :page, presence: true
  validates :position, presence: true
  validates :question, presence: true

  default_scope { order(:page, :position, :id) }

  def self.for(page, locale = :nl)
    where(page: page, locale: locale)
  end

  def name
    "#{full_page_name}: #{question}"
  end

  def full_page_name
    if Question.pages[page]
      "#{page} - #{Question.pages[page]}"
    else
      page
    end
  end

  def self.options
    existing_pages.map { |page, description| ["#{page} - #{description}", page] }
  end

  def self.select_existing_pages
    pluck(:page)
  end

  def self.existing_pages
    new_pages = pages
    select_existing_pages.each do |page|
      new_pages[page.strip] = page.strip unless pages.key?(page.strip)
    end
    new_pages
  end

  def self.pages
    {
      'A1'   => 'Persoonsgegevens',
      'A1B'  => 'Persoonsgegevens via balie',
      'A3'   => 'Type account',
      'A4'   => 'Accountgegevens Midden',
      'A6'   => 'Accountgegevens Basis',
      'A7'   => 'Controle e-mailadres',
      'A8'   => 'Bevestiging aanvraag',
      'B1'   => 'Accountgegevens',
      'B3'   => 'Activeren',
      'B4'   => 'Bevestiging',
      'C1'   => 'Inloggegevens - keuze',
      'C1x1' => 'Inloggegevens - Keuze - webdienst vereist basis zonder sms optie',
      'C1x2' => 'Inloggegevens - Keuze - webdienst vereist basis met sms optie',
      'C1x3' => 'Inloggegevens - Keuze - webdienst vereist midden',
      'C1x4' => 'Inloggegevens - Keuze - webdienst vereist substantieel',
      'C1A'  => 'Invoeren gebruikersnaam en wachtwoord',
      'C1B'  => 'DigiD app (keuze device)',
      'C1D' => "Identiteitsbewijs",
      'C1Dx1' => "Rijbewijs (keuze device)",
      'C1Dx2' => "Identiteitskaart (keuze device)",
      'C2'   => 'Sms code',
      'C3'   => 'SSO voorkeur',
      'C4'   => 'SSO tussenscherm',
      'C5'   => 'Uitgelogd',
      'C6'   => 'Wijzigen wachtwoord conform policy',
      'C9'   => 'Inloggegevens DigiD app',
      'C10'  => 'Keuze inlogmethode DigiD app',
      'D1'   => 'Mijn Gegevens',
      'D2'   => 'Mijn geschiedenis',
      'D7'   => 'Wijzigen wachtwoord',
      'D9'   => 'Wijzigen e-mailinstellingen',
      'D10'  => 'Verwijderen e-mailadres',
      'D11'  => 'Wachtwoord',
      'D12'  => 'Wijzigen telefoonnummer',
      'D16'  => 'Aanvragen sms-functie',
      'D17'  => 'Extra sms-controle aanvragen - Telefoonnummer',
      'D20'  => 'Opheffen sms',
      'D22'  => 'Mijn DigiD instellingen',
      'D24'  => 'Opheffen DigiD',
      'D28'  => 'Hoe wilt u de DigiD app activeren',
      'D37'  => 'Scannen QR-code (Mijn DigiD)',
      'D41'  => 'Bevestiging aanvraag uitbreiding',
      'E1'   => 'Persoonsgegevens',
      'E2'   => 'Keuze herstelwijze',
      'E3'   => 'Bevestiging versturen brief',
      'E4'   => 'Herstelcode invoeren',
      'E6'   => 'Nieuw wachtwoord',
      'E7'   => 'Bevestiging',
      'E8'   => 'Bevestiging versturen e-mail',
      'E9'   => 'Nieuw nummer',
      'E11'  => 'Wachtwoord',
    }
  end
end
