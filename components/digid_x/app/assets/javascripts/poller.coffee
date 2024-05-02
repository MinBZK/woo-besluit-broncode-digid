
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

class Poller
  constructor: (el) ->
    el = $(el)
    @url = el.data('url')
    @timeout = parseInt(el.data('timeout')) || 2500
    @_start() unless el.data('suspended') == 'data-suspended'

  _start: () =>
    @timeoutId = window.setTimeout(@_poll, @timeout)

  _stop: () =>
    if (@timeoutId)
      window.clearTimeout(@timeoutId)
      @timeoutId = null

  _poll: () =>
    $.ajax
      type: "GET",
      cache: false,
      url: @url,
      statusCode:
        200: @_success
        202: @_start
        500: @_error
      # Retry unhandled errors
      error: @_start

  _success: (data) =>
    return unless data || data.stop
    if data.html || data.popup
      handle_message(data)
    else if data.dialog
      window.message_dialog(data.dialog)
      if data.url || data.redirect_url
        @url = data.url || data.redirect_url
        @_start()
    else if data.url || data.redirect_url
      window.location.href = data.url || data.redirect_url
    else if data.reload
      window.location.reload()

  _error: ->
    console.log("Error!")

$ ->
  pollers = []
  $(".poller").each (i, el) ->
    pollers.push(new Poller(el))

  window.stopPolling = () ->
    for poller in pollers
      poller._stop()

  window.doPoll = () ->
    for poller in pollers
      poller._poll()

  $(".waiting_poller").click (e) ->
    if this.type == "submit"
      $(this).closest("form").one "submit", (f) ->
        pollers.push(new Poller(e.currentTarget))
      return
    pollers.push(new Poller(e.currentTarget))
    return
  return
