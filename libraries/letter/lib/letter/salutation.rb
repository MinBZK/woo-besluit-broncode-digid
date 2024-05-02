
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

require 'letter/translation'

module Letter
  module Salutation
    include Letter::Translation
    # if 01.02.20 [JH|JV] then "Jonkheer | Jonkvrouw" else
    # if '01.04.10 [M|V|O] then
    #   "Mevrouw | De heer | Geachte heer/mevrouw"
    # + eerste letter van de voornamen in 01.02.10 in hoofdletter overnemen gevolgd door een punt
    # + 01.02.20 [B|BS|G|GI|H|HI|M|MI|P|PS|R]
    # + achternaam
    def naam_aanhef(gba)
      if ["M", "V"].include?(gba['010410'].to_s.upcase)
        upcase_first_letter("#{geachte} \
          #{meneer_mevrouw(gba['010220'], gba['010410'])} \
          #{adellijk(gba['010220'])} \
          #{upcase_first_letter(achternaam(gba))}".strip.gsub(/\s+/, ' '))
      else
        upcase_first_letter("#{geachte} \
          #{voorletters(gba['010210'])} \
          #{achternaam(gba)}".strip.gsub(/\s+/, ' '))
      end
    end

    # only used in aanhef
    def geachte
      t('geachte')
    end

    # slightly different from naam_aanhef (adds voorletters, deletes "geachte")
    def naam(gba)
      if ["M", "V"].include?(gba['010410'].to_s.upcase)
        upcase_first_letter("#{add_de(meneer_mevrouw(gba['010220'], gba['010410']))} \
          #{voorletters(gba['010210'])} \
          #{adellijk(gba['010220'])} \
          #{achternaam(gba)}".strip.gsub(/\s+/, ' '))
      else
        upcase_first_letter("#{voorletters(gba['010210'])} \
          #{achternaam(gba)}".strip.gsub(/\s+/, ' '))
      end
    end

    # add "de" if str starts with "heer"
    def add_de(str)
      if str.start_with?('heer')
        "de #{str}"
      else
        str
      end
    end

    def upcase_first_letter(str)
      str && str.length > 0 ? str[0,1].upcase + str[1..-1] : str
    end

    # Jonkheer/Jonkvrouw of De Heer/Mevrouw
    def meneer_mevrouw(titel, geslacht)
      if !titel.nil? && (['JH','JV'].include? titel.upcase)
        {'JH' => t('jonkheer'),
         'JV' => t('jonkvrouw')
        }[titel.upcase]
      else
        { 'M' => t('heer'),
          'V' => t('mevrouw'),
          'O' => t('heer/mevrouw'),
          '' => t('heer/mevrouw')
         }[geslacht.to_s.upcase]
      end
    end

    # creates a string of first letters from the string namen
    # since UTF8 may take more than one byte, we cannot simply slice,
    # so we use scan(/./mu) to split into correct characters
    def voorletters(namen)
      namen.strip.split(' ').map {|naam| naam.scan(/./mu)[0] + '.' }.join(' ') unless namen.nil?
    end

    # try to  map titel on one of the adellijke titels
    def adellijk(titel)
      { 'B' => t('baron'),
        'BS' => t('barones'),
        'G' => t('graaf'),
        'GI' => t('gravin'),
        'H' => t('hertog'),
        'HI' => t('hertogin'),
        'M' => t('markies'),
        'MI' => t('markiezin'),
        'P' => t('prins'),
        'PS' => t('prinses'),
        'R' => t('ridder')
      }[titel.upcase] unless titel.nil?
    end

    # creates a lastname, which is composed of multiple fields in case of marriage
    def achternaam(gba)
      case gba['016110']
      when 'E' then eigennaam(gba)
      when 'P' then partnernaam(gba)
      when 'V' then "#{partnernaam(gba)}-#{eigennaam(gba)}"
      when 'N' then "#{eigennaam(gba)}-#{partnernaam(gba)}"
      else eigennaam(gba)
      end
    end

    # voorvoegsel (010230) en geslachtsnaam (010240)
    def eigennaam(gba)
      "#{gba['010230']} #{gba['010240']}".strip
    end

    # voorvoegsel (050230) en geslachtsnaam (050240) van meest recente huwelijk
    def partnernaam(gba)
      "#{gba['050230']} #{gba['050240']}".strip
    end
  end
end
