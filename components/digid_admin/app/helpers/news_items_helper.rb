
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

module NewsItemsHelper
  def strong text
    content_tag :strong, text
  end

  def green(text)
    content_tag :span, text, class: 'green'
  end

  def red(text)
    content_tag :span, text, class: 'red'
  end

  def sanitized_news_body_for(news_item, locale)
    Redcarpet::Markdown.new(Redcarpet::Render::HTML, safe_links_only: true, escape_html: true).render(news_item.send("body_#{locale}") || "").html_safe
  end

  def news_locations(news_item)
    if news_item.news_locations.empty?
      'Geen locatie toegewezen.'
    else
      html = '<ul>'
      news_item.news_locations.map(&:name).each do |location_name|
        html << "<li>#{location_name}</li>"
      end
      html << '<ul>'
      html.html_safe
    end
  end

  def user_configuration(news_item)
    if news_item.os_versions.flatten.empty? && news_item.browser_versions.flatten.empty?
      'Geen configuratie toegewezen.'
    else
      html = '<ul>'
      news_item.os_versions.map {|os, version| html << "<li>#{NewsItem.os_list.map(&:reverse).to_h[os.to_sym]}: #{version}</li>" if os}
      news_item.browser_versions.map {|b, version| html << "<li>#{NewsItem.browser_list.map(&:reverse).to_h[b.to_sym]}: #{version}</li>" if b}
      html << '<ul>'
      html.html_safe
    end
  end

  def news_item_active(news_item)
    html = '<strong>'
    if news_item.active?
      html << "Bericht is #{green('actief')} en zichtbaar van "
      html << '</strong>'
      html << l(news_item.active_from, format: :default)
      html << ' <strong>tot</strong> '
      html << l(news_item.active_until, format: :default)
    else
      html << "Bericht is #{red('niet actief')} en hierdoor niet zichtbaar</strong>"
    end
    html.html_safe
  end
end
