
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

# Validates that an attribute starts with a specific letter. If this is so, it
# checks if a class method "#{attribute}_correct?" returns true as well.
class CodeValidator < ActiveModel::EachValidator
  def initialize(options)
    raise(ArgumentError, "Invalid starts_with option") if options[:starts_with] && (!options[:starts_with].is_a?(String) || (options[:starts_with] !~ /\A[A-Z]\z/))
    super
  end

  def validate_each(object, attribute, value)
    return if value.nil?
    if value.upcase !~ Regexp.new("\\A#{options[:starts_with]}")
      object.errors.add(attribute, :invalid)
    elsif !object.send("#{attribute}_correct?")
      if object.is_a?(Activationcode)
        object.errors.add(attribute, :incorrect, 
          letter_sent_at: object.letter_sent_at)
      else
        object.errors.add(attribute, :incorrect)
      end
    end
  end
end
