
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

# InfoBoxes are used on the MyDigiD page. The [I] button is connected to the infobox
# open/close animation.
window.InfoBox = (elements) ->
  ariaAttributeName = 'aria-label'
  $(elements).click (e) ->
    e.stopPropagation()
    infoButton = $(this)
    if infoButton.closest('.wrap-main').find('.info-box').length > 1
      infoBox = infoButton.closest('.multiple-info-boxes-wrapper').find('.info-box')
    else
      infoBox = infoButton.closest('.wrap-main').find('.info-box')
    if infoBox.is(':empty')
      infoBox.html '<p>' + infoButton.attr('title') + '</p>'
    # Stop animations
    infoButton.stop(true, true)
    infoBox.stop(true, true)
    infoButtonAriaLabelValue = infoButton.attr(ariaAttributeName)
    # We ease the scrolling of the other content (see: http://stackoverflow.com/questions/10092794/jquery-ui-slide-ease-sibling-push)
    if infoBox.is(':visible')
      with_prefix = infoButtonAriaLabelValue.replace(constants.information_boxes.aria_labels_close_prefix + " ", "")
      infoButton.attr(ariaAttributeName, with_prefix.charAt(0).toUpperCase() + with_prefix.slice(1))
      infoButton.switchClass 'close', 'info'
      infoBox.toggle 'slide', direction: 'up', 500 # jquery-ui/effects-slide
      infoBox.parent(".ui-effects-wrapper").slideUp(500)
      infoButton.closest('.wrap-main').find('.col.highlight').removeClass('info-box-active')
      infoButton.closest('.wrap-main.yellow-highlight').removeClass('info-box-active')
    else
      infoButton.attr(ariaAttributeName, constants.information_boxes.aria_labels_close_prefix + " " + (infoButtonAriaLabelValue.charAt(0).toLowerCase() + infoButtonAriaLabelValue.slice(1)))
      infoButton.switchClass 'info', 'close'
      infoBox.toggle 'slide', direction: 'up', 500 # jquery-ui/effects-slide
      infoBox.parent(".ui-effects-wrapper").hide().slideDown(500)
      infoButton.closest('.wrap-main').find('.col.highlight').addClass('info-box-active')
      infoButton.closest('.wrap-main.yellow-highlight').addClass('info-box-active')

(($) ->
  init = ->
    window.InfoBox('.info-button')
  window.InfoBoxes = init: init
) window.jQuery
