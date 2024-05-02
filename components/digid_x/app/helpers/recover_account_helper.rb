
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

module RecoverAccountHelper
  def recover_authentication_steps_helper(url)

    case url
    when "/herstellen/persoonsgegevens", "/en/herstellen/persoonsgegevens"
      steps(1, 5, t("personal_details"))
    end
  end

  def recover_authentication_info_helper(flow)
    case flow
    when "herstellen_wachtwoord"
      content_tag :p, t(".password_recover_info")
    when "herstellen_wachtwoord_code_invoeren"
      content_tag :p, t(".submit_code_info")
    end
  end

  def recover_code_field_label_helper
    if starting_point.eql?("herstellen_wachtwoord_code_invoeren") && !account_has_herstelcode_email?
      t("enter_code_received_by_post")
    else
      t("enter_code_received_by_email")
    end
  end

  def recover_code_example_helper
    return unless starting_point.eql?("herstellen_wachtwoord_code_invoeren_via_email") || account_has_herstelcode_email?

    t("examples.no_code_received_with_url", url: link_to(t("send_again"), new_request_recover_password_again_url))
  end
end
