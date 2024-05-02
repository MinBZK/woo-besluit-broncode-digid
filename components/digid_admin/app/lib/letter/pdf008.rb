
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

module Letter::Pdf008
  include Letter::Template

  def generate_letter_008
    pdf = generate_template('recover_letter', '008')

    add_letter_008_info(pdf)

    pdf
  end

  private

  def add_letter_008_info(pdf)
    I18n.with_locale(@locale) do
      pdf.bounding_box([60, 380], width: 400) do
        pdf.font('text', size: 9) do
          pdf.text I18n.t('letters.recover_letter.008.recover_digid_steps')
        end
      end

      pdf.bounding_box([60, 360], width: 400) do
        pdf.font('text', size: 9) do
          pdf.text I18n.t('letters.recover_letter.008.recover_step_1')
          pdf.text I18n.t('letters.recover_letter.008.recover_step_2')
          pdf.text I18n.t('letters.recover_letter.008.recover_step_3')
          pdf.text I18n.t('letters.recover_letter.008.recover_step_4')
          pdf.text I18n.t('letters.recover_letter.008.recover_step_5')
          pdf.text I18n.t('letters.recover_letter.008.recover_step_6')
          pdf.text I18n.t('letters.recover_letter.008.recover_step_7')
          pdf.text I18n.t('letters.recover_letter.008.recover_step_8')
          pdf.text I18n.t('letters.recover_letter.008.recover_step_9')
          pdf.text I18n.t('letters.recover_letter.008.recover_step_10')
          pdf.indent(12) { pdf.text I18n.t('letters.recover_letter.008.recover_step_10_a')}
          pdf.indent(12) { pdf.text I18n.t('letters.recover_letter.008.recover_step_10_b')}
          pdf.text I18n.t('letters.recover_letter.008.recover_step_11')
        end
      end
    end
  end
end
