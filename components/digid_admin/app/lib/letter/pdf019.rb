
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

module Letter::Pdf019
  include Letter::Template

  def generate_letter_019
    pdf = generate_template('activation_letter', '019')

    add_letter_19_info(pdf)

    pdf
  end

  private

  def add_letter_19_info(pdf)
    I18n.with_locale(@locale) do
      pdf.bounding_box([60, 380], width: 400) do
        pdf.font('text', size: 9) do
          pdf.text I18n.t('letters.activation_letter.019.activate_digid_steps_multiple'), style: :bold
        end
      end

      pdf.bounding_box([60, 360], width: 200) do
        pdf.font('text', size: 9) do
          pdf.text I18n.t('letters.activation_letter.019.app_header'), style: :bold
          pdf.move_down 5

          pdf.text I18n.t('letters.activation_letter.019.app_step_1_1')
          pdf.indent(8) { pdf.text I18n.t('letters.activation_letter.019.app_step_1_2') }
          pdf.text I18n.t('letters.activation_letter.019.app_step_2')
        end
      end

      pdf.bounding_box([260, 360], width: 200) do
        pdf.font('text', size: 9) do
          pdf.text I18n.t('letters.activation_letter.019.site_header'), style: :bold
          pdf.move_down 5

          pdf.text I18n.t('letters.activation_letter.019.site_step_1')
          pdf.text I18n.t('letters.activation_letter.019.site_step_2')
          pdf.text I18n.t('letters.activation_letter.019.site_step_3')
          pdf.text I18n.t('letters.activation_letter.019.site_step_4')
          pdf.text I18n.t('letters.activation_letter.019.site_step_5_1')
          pdf.indent(8) { pdf.text I18n.t('letters.activation_letter.019.site_step_5_2')}
          pdf.text I18n.t('letters.activation_letter.019.site_step_6')
          pdf.text I18n.t('letters.activation_letter.019.site_step_7')
        end
      end

      pdf.bounding_box([60, 200], width: 340) do
        pdf.font('text', size: 9) do
          pdf.text I18n.t('letters.activation_letter.019.request_digid'), inline_format: true
        end
      end
    end
  end
end
