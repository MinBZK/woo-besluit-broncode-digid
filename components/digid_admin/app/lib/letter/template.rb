
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

module Letter::Template
  def generate_template(letter_type, letter_number)
    stationary_root = "#{Rails.root}/app/assets/stationary/"
    font_root = stationary_root
    images_root = stationary_root
    heading_font_name = 'RijksoverheidSansHeadingTT'
    default_font_name = 'RijksoverheidSansTextTT'
    parent = self

    I18n.with_locale(@locale) do
      info = {
        Title: I18n.t("letters.#{letter_type}.title"),
        Author: I18n.t("letters.author"),
        Subject: I18n.t("letters.#{letter_type}.subject"),
        Keywords: I18n.t("letters.#{letter_type}.keywords"),
        Creator: I18n.t("letters.creator"),
        Producer: I18n.t("letters.producer"),
        CreationDate: Time.now
      }

      Prawn::Document.new(page_size: 'A4', margin: 0, info: info) do |pdf|
        pdf.font_families.update(
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

        pdf.font 'text'
        pdf.default_leading 3
        margin_bottom = 15

        pdf.image "#{images_root}rijksoverheid.png", at: [275, 850], fit: [100, 100]

        pdf.bounding_box([465, 700], width: 110) do
          pdf.text 'DigiD', style: :bold, size: 8
          pdf.font('text', size: 7) do
            pdf.text I18n.t('letters.postal')
            pdf.text I18n.t('letters.zip')
            pdf.text I18n.t('letters.website')
            pdf.move_down 5
            pdf.text I18n.t('letters.helpdesk'), style: :bold, size: 8
            pdf.text I18n.t('letters.phonenumber')
            pdf.text I18n.t('letters.contact_form')
            pdf.move_down 5
            pdf.text I18n.t("letters.reach_us_1"), style: :italic
            pdf.text I18n.t("letters.reach_us_2"), style: :italic
            pdf.text I18n.t("letters.reach_us_3"), style: :italic
            pdf.move_down 5
            pdf.text I18n.t('letters.reference'), style: :bold, size: 8
            pdf.text parent.brief_code
          end
        end

        pdf.bounding_box([60, 735], width: 340) do
          pdf.font('text', size: 9) do
            pdf.text I18n.t('letters.return_address'), size: 7
            pdf.move_down 25
            pdf.text I18n.t('letters.confidental'), size: 7, style: :bold
            pdf.text parent.address_details['Naamregel1']
            pdf.text parent.address_details['Adresregel1']
            pdf.text parent.address_details['Adresregel2']
            pdf.text parent.address_details['Adresregel3']
            pdf.text parent.address_details['Adresregel4']
          end
        end

        pdf.bounding_box([360, 700], width: 20) do
          pdf.font('text', size: 9) do
            pdf.text I18n.t("letters.#{letter_type}.reference_type")
          end
        end

        pdf.text_box I18n.t('letters.date'), at: [60, 575], size: 9
        pdf.text_box I18n.l(Time.zone.today, format: :long), at: [100, 575], size: 9
        pdf.text_box I18n.t('letters.re'), at: [60, 560], size: 9
        pdf.text_box I18n.t("letters.#{letter_type}.letter_title"), at: [100, 560], size: 9

        pdf.bounding_box([60, 520], width: 340) do
          pdf.font('text', size: 9) do
            pdf.text "#{parent.letter["BriefStandaardinhoud"]["Aanhef"]},"
            pdf.move_down margin_bottom
            pdf.text I18n.t("letters.#{letter_type}.#{letter_number}.request")
          end
        end

        pdf.bounding_box([50, 460], width: 345) do
          pdf.indent 10 do
            pdf.move_down 10
            pdf.font('heading', size: 9, style: :bold) do
              pdf.text "#{I18n.t("letters.#{letter_type}.your_code")}  <font name='Helvetica' size='9'><b>#{parent.activation_details['Activeringscode']}</b></font>", inline_format: true
            end
            pdf.move_down 10
            pdf.text I18n.t("letters.#{letter_type}.use_before", date: parent.activation_details['Vervaldatum']), size: 9, inline_format: true
            pdf.move_down 5
          end

          pdf.stroke_color '000000'
          pdf.stroke_bounds
        end

        pdf.bounding_box([60, 165], width: 340) do
          pdf.font('text', size: 9) do
            pdf.text I18n.t('letters.contact_1'), inline_format: true
            pdf.text I18n.t('letters.contact_2'), inline_format: true
            pdf.move_down 10
            pdf.text I18n.t('letters.greetings')
            pdf.move_down 10
            pdf.text I18n.t('letters.helpdesk')
            pdf.move_down 15
            pdf.text I18n.t('letters.digid_private'), inline_format: true
          end
        end

        pdf.text_box I18n.t('letters.confidental'), at: [60, 30], size: 7, style: :bold_italic
        pdf.text_box I18n.t('letters.page'), at: [465, 30], size: 7
      end
    end
  end
end
