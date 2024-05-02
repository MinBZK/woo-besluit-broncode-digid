
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

class Pdf
  def self.generate_letter(verification)
    stationary_root = "#{Rails.root}/app/assets/stationary/"
    font_root = stationary_root
    images_root = stationary_root
    heading_font_name = 'RijksoverheidSansHeadingTT'
    default_font_name = 'RijksoverheidSansTextTT'

    I18n.with_locale(verification.locale) do
      info = {
        Title: I18n.t('letters.activation_letter.title'),
        Author: I18n.t('letters.activation_letter.author'),
        Subject: I18n.t('letters.activation_letter.subject'),
        Keywords: I18n.t('letters.activation_letter.keywords'),
        Creator: I18n.t('letters.activation_letter.creator'),
        Producer: I18n.t('letters.activation_letter.producer'),
        CreationDate: Time.now
      }

      Prawn::Document.new(page_size: 'A4', margin: 0, info: info) do
        font_families.update(
          'heading' => {
            normal: { file: font_root + "#{heading_font_name}-Regular_2_0.ttf", font: heading_font_name },
            italic: { file: font_root + "#{heading_font_name}-Italic_2_0.ttf", font: heading_font_name },
            bold: { file: font_root + "#{heading_font_name}-Bold_2_0.ttf", font: heading_font_name },
            bold_italic: { file: font_root + "#{heading_font_name}-BoldItalic_2_0.ttf", font: heading_font_name }
          },
          'text' => {
            normal: { file: font_root + "#{default_font_name}-Regular_2_0.ttf", font: default_font_name },
            italic: { file: font_root + "#{default_font_name}-Italic_2_0.ttf", font: default_font_name },
            bold: { file: font_root + "#{default_font_name}-Bold_2_0.ttf", font: default_font_name },
            bold_italic: { file: font_root + "#{default_font_name}-BoldItalic_2_0.ttf", font: default_font_name }
          }
        )

        font 'text'
        default_leading 3
        margin_bottom = 15

        image images_root + 'rijksoverheid.png', at: [275, 850], fit: [100, 100]

        bounding_box([465, 700], width: 120) do
          text 'DigiD', style: :bold, size: 8
          font('text', size: 7) do
            text I18n.t('letters.activation_letter.postal')
            text I18n.t('letters.activation_letter.zip')
            text I18n.t('letters.activation_letter.website')
            move_down 5
            text I18n.t('letters.activation_letter.helpdesk'), style: :bold, size: 8
            text I18n.t('letters.activation_letter.phonenumber')
            text I18n.t('letters.activation_letter.contact_form')
            move_down 5
            text I18n.t("letters.activation_letter.reach_us_1"), style: :italic
            text I18n.t("letters.activation_letter.reach_us_2"), style: :italic
            text I18n.t("letters.activation_letter.reach_us_3"), style: :italic
            move_down 5
            text I18n.t('letters.activation_letter.reference'), style: :bold, size: 8
            text 'B001'
          end
        end

        bounding_box([60, 700], width: 340) do
          font('text', size: 9) do
            text I18n.t('letters.activation_letter.return_address'), size: 7
            move_down 25
            text I18n.t('letters.activation_letter.confidental'), size: 7, style: :bold
            text verification.full_name
          end
        end

        text_box I18n.t('letters.activation_letter.date'), at: [60, 575], size: 9
        text_box I18n.l(Time.zone.today, format: "%e %B, %Y").strip, at: [100, 575], size: 9
        text_box I18n.t('letters.activation_letter.re'), at: [60, 560], size: 9
        text_box I18n.t('letters.activation_letter.letter_title'), at: [100, 560], size: 9

        bounding_box([60, 520], width: 340) do
          font('text', size: 9) do
            text "#{verification.salutation},"
            move_down margin_bottom
            text I18n.t('letters.activation_letter.request')
          end
        end

        bounding_box([50, 460], width: 345) do
          indent 10 do
            move_down 10
            font('heading', size: 9, style: :bold) do
              text "#{I18n.t("letters.activation_letter.your_activation_code")}  <font name='Helvetica' size='9'><b>#{verification.activation_code}</b></font>", inline_format: true
            end
            move_down 10
            text I18n.t("letters.activation_letter.activate_before" , date: I18n.l(verification.activation_code_end_date, format: :long)), size: 9, inline_format: true
            move_down 5
          end

          stroke_color '000000'
          stroke_bounds
        end

        bounding_box([60, 380], width: 400) do
          font('text', size: 9) do
            text I18n.t('letters.activation_letter.activate_digid_steps'), style: :bold
          end
        end

        bounding_box([60, 360], width: 200) do
          font('text', size: 9) do
            text I18n.t('letters.activation_letter.app_header'), style: :bold
            move_down 5

            text I18n.t('letters.activation_letter.app_step_1_1')
            indent(8) { text I18n.t('letters.activation_letter.app_step_1_2') }
            text I18n.t('letters.activation_letter.app_step_2')
          end
        end

        bounding_box([260, 360], width: 200) do
          font('text', size: 9) do
            text I18n.t('letters.activation_letter.site_header'), style: :bold
            move_down 5

            text I18n.t('letters.activation_letter.site_step_1')
            text I18n.t('letters.activation_letter.site_step_2')
            text I18n.t('letters.activation_letter.site_step_3')
            text I18n.t('letters.activation_letter.site_step_4')
            text I18n.t('letters.activation_letter.site_step_5_1')
            indent(8) { text I18n.t('letters.activation_letter.site_step_5_2') }
            text I18n.t('letters.activation_letter.site_step_6')
            text I18n.t('letters.activation_letter.site_step_7_1')
            indent(8) { text I18n.t('letters.activation_letter.site_step_7_2') }
            text I18n.t('letters.activation_letter.site_step_8')

            move_down 20
          end
        end

        bounding_box([60, 200], width: 340) do
          font('text', size: 9) do
            text I18n.t('letters.activation_letter.request_digid'), inline_format: true
          end
        end

        bounding_box([60, 165], width: 340) do
          font('text', size: 9) do
            text I18n.t('letters.activation_letter.contact'), inline_format: true
            move_down 10
            text I18n.t('letters.activation_letter.greetings')
            move_down 10
            text 'Helpdesk DigiD'
            move_down 15
            text I18n.t('letters.activation_letter.digid_private'), inline_format: true
          end
        end

        text_box I18n.t('letters.activation_letter.confidental'), at: [60, 30], size: 7, style: :bold_italic
      end
    end
  end
end
