
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

class LetterPresenter < BasePresenter
  presents :activation_letter

  def letter_code
    default_info['Briefcode'] if default_info
  end

  def letter_name
    LetterBuilder::BRIEFCODE_MAPPING.key(letter_code)
  end

  def registration_date
    default_info['Aanvraagdatum'] if default_info
  end

  def expiry_date
    activation_details['Vervaldatum'] if activation_details
  end

  def annotation
    default_info['Aanhef'] if default_info
  end

  def name
    address_details['Naamregel1'] if address_details
  end

  def address
    address_details['Adresregel1'] if address_details
  end

  def address_addition
    address_details['Adresregel2'] if address_details
  end

  def more_address_additions
    address_details['Adresregel3'] if address_details
  end

  def even_more_address_additions
    address_details['Adresregel4'] if address_details
  end

  def country
    address_details['Landcode'] if address_details
  end

  def activation_code
    activation_details['Activeringscode'] if activation_details
  end

  def printable?
    %w(008 018 019).include?(letter_code)
  end

  private

  def letter
    letters = activation_letter.fetch('Brieven', nil)
    letters['Brief'] if letters
  end

  def default_info
    letter['BriefStandaardinhoud'] if letter
  end

  def address_details
    letter['ActiveringsBrief']['Adresgegevens'] if letter && letter['ActiveringsBrief']
  end

  def activation_details
    letter['ActiveringsBrief']['ActiveringsBericht'] if letter && letter['ActiveringsBrief']
  end
end
