
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

class RememberLogin
  constructor: (form) ->
    @form = $(form)
    @form.find(".remember_option").show()
    if @findElements()
      @load()
      @attachHandlers()

  findElements: ->
    @form.find('input[type="text"]').each (i, el) =>
      @username = $(el) if /username\]$/.test(el.name)
    @form.find('input[type="checkbox"]').each (i, el) =>
      @checkbox = $(el) if /\[remember_login\]$/.test(el.name)
    @username and @checkbox

  attachHandlers: ->
    @form.on 'submit', (event) => @save(event)

  load: ->
    login = window.localStorage.getItem('login')
    @username.val(login) if login
    @checkbox.prop('checked', login)

  save: (event) ->
    return unless @username.is(':visible')
    if @checkbox.prop('checked')
      window.localStorage.setItem('login', @username.val()) if @username.val()
    else
      window.localStorage.removeItem('login')

supportsLocalStorage = ->
  try
    window.localStorage.setItem('test', 'test')
    result = window.localStorage.getItem('test') == 'test'
    window.localStorage.removeItem('test')
    result
  catch err
    false

if supportsLocalStorage()
  $ ->
    $(".remember_login").each (i, el) ->
      new RememberLogin(el)
