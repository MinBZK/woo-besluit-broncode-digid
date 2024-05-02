
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

class Sms::Message::Spoken < Sms::Message
  def body
    {
      "callee" => phone_number,
      "caller" => Sms.configuration.from_number,
      "intro-prompt" => "/prompts/intro.wav",
      "intro-prompt-type" => "File",
      "code-prompt" => "/prompts/code.wav",
      "code-prompt-type" => "File",
      "code" => message,
      "max-replays" => 3,
      "replay-prompt" => "/prompts/replay.wav",
      "replay-prompt-type" => "File",
      "anonymous" => Sms.configuration.anonymous,
      "voice" => { "language" => { "en" => "en-EN", "nl" => "nl-NL" }[locale.to_s] || "nl-NL" }
    }
  end

  def headers
    super.merge({ 'X-CM-PRODUCTTOKEN' => Sms.configuration.spoken_token} )
  end
end
