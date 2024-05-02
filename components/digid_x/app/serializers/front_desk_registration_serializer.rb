
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

class FrontDeskRegistrationSerializer < ActiveModel::Serializer
  include Letter::Salutation
  include Letter::Translation

  attributes :front_desk_code, :citizen_service_number, :front_desk_registration_created_at, :front_desk_registration_id, :id_number
  attributes :front_desk_account_id, :front_desk_code_expires_at, :expires_in, :id_expires_at, :birthday, :activation_code, :activation_code_end_date, :first_names, :full_name, :salutation, :surname
  attributes :locale, :nationality

  def front_desk_code
    object.baliecode
  end

  def citizen_service_number
    object.burgerservicenummer
  end

  def front_desk_registration_id
    object.id
  end

  def front_desk_registration_created_at
    object.created_at
  end

  def front_desk_account_id
    Account.requested.with_bsn(object.burgerservicenummer).first&.id
  end

  def locale
    Account.find(front_desk_account_id)&.locale if front_desk_account_id
  end

  def front_desk_code_expires_at
    object.created_at + ::Configuration.get_int("balie_default_geldigheidsduur_baliecode").days
  end

  def expires_in
    ::Configuration.get_int("balie_default_geldigheidsduur_baliecode")
  end

  def id_expires_at
    object.valid_until
  end

  def birthday
    object.geboortedatum
  end

  def first_names
    gba.voornamen
  end

  def activation_code
    activation_letter.controle_code
  end

  def activation_code_end_date
    Date.current + ::Configuration.get_int("balie_default_geldigheidsduur_activatiecode")
  end

  def full_name
    naam(gba)
  end

  def salutation
    upcase_first_letter(
      condense_spaces(
        naam_aanhef(gba)
      )
    )
  end

  def surname
    "#{gba.voorvoegsel_geslachtsnaam} #{gba.geslachtsnaam}".strip
  end

  def nationality
    nationality = Nationality.find(object.nationality_id || Nationality.dutch_id)

    if I18n.locale == :nl
      "#{nationality.description_nl}"
    elsif I18n.locale == :en
      "#{nationality.description_en}"
    end
  end

  private

  def activation_letter
    @activation_letter ||= object.activation_letters.where(letter_type: ActivationLetter::LetterType::BALIE).first # There should only be one letter on a registration
  end

  def gba
    activation_letter.gba
  end

  def condense_spaces(string)
    string.strip.gsub(/\s+/, " ")
  end
end
