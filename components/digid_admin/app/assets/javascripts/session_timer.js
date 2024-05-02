
/*
 * Deze broncode is openbaar gemaakt vanwege een Woo-verzoek zodat deze
 * gericht is op transparantie en niet op hergebruik. Hergebruik van 
 * de broncode is toegestaan onder de EUPL licentie, met uitzondering 
 * van broncode waarvoor een andere licentie is aangegeven.
 * 
 * Het archief waar dit bestand deel van uitmaakt is te vinden op:
 *   https://github.com/MinBZK/woo-besluit-broncode-digid
 * 
 * Eventuele kwetsbaarheden kunnen worden gemeld bij het NCSC via:
 *   https://www.ncsc.nl/contact/kwetsbaarheid-melden
 * onder vermelding van "Logius, openbaar gemaakte broncode DigiD" 
 * 
 * Voor overige vragen over dit Woo-besluit kunt u mailen met open@logius.nl
 * 
 * This code has been disclosed in response to a request under the Dutch
 * Open Government Act ("Wet open Overheid"). This implies that publication 
 * is primarily driven by the need for transparence, not re-use.
 * Re-use is permitted under the EUPL-license, with the exception 
 * of source files that contain a different license.
 * 
 * The archive that this file originates from can be found at:
 *   https://github.com/MinBZK/woo-besluit-broncode-digid
 * 
 * Security vulnerabilities may be responsibly disclosed via the Dutch NCSC:
 *   https://www.ncsc.nl/contact/kwetsbaarheid-melden
 * using the reference "Logius, publicly disclosed source code DigiD" 
 * 
 * Other questions regarding this Open Goverment Act decision may be
 * directed via email to open@logius.nl
*/

var DigiD = DigiD || {};

DigiD.SessionTimer = function (options) {
  this.options = {
    server_session_start      : 0     // time session started in seconds
    ,inactive_expiration      : 40    // timeout in seconds
    ,absolute_expiration      : 10800 // timeout in seconds
    ,warning_inactive_time    : 30    // show dialog xx seconds before session timeout
    ,warning_absolute_time    : 120   // timeout in seconds  
    ,sign_out_path            : '/destroy_session'  // path to redirect to when the session is expired
    ,session_ping_path        : '/session_ping'     // path to update the inactive session serverside
    ,dialog_title             : 'title'
    ,dialog_inactive_content  : 'inactive_content'
    ,dialog_absolute_content  : 'absolute_content'
    ,update_timer             : true
    ,warning_absolute_disable : 5 // in seconds
  };

  options = options || {};
  for(var x in options) {
    this.options[x] = options[x];
  }

  var now_s = (new Date()).getTime() / 1000 // in seconds

  this.dialog = null;
  this.disabled = false;
  this.server_time_used = now_s - this.options.server_session_start; // used server time in seconds
};

DigiD.SessionTimer.prototype.startTimer = function() {
  this.resetAbsoluteTimer();
  this.resetInactiveTimer();

  // timeout for session in miliseconds
  var that = this; // set scope of this to use inside timer
  this.timer = window.setInterval(function() {
    that.intervalTimer((new Date()).getTime()); // send current time
  }, 1000);

  // reset inactive timer on each ajax request
  $(document).ajaxComplete(function () {
    that.resetInactiveTimer();
  });

  // ping the server when using the back button of the browser to sync the inactive timer
  this.pingServer();
};

DigiD.SessionTimer.prototype.resetInactiveTimer = function() {
  this.inactive_start_time = (new Date()).getTime(); // in miliseconds
  this.inactive_expiration_ms = (this.options.inactive_expiration) * 1000; // in miliseconds
};

DigiD.SessionTimer.prototype.resetAbsoluteTimer = function() {
  this.absolute_start_time = (new Date()).getTime(); // in miliseconds
  this.absolute_expiration_ms = (this.options.absolute_expiration - this.server_time_used) * 1000; // in miliseconds

  // Disable warnings if absolute time left is smaller than the warning_absolute_time
  var warning_disable_time_ms = (this.options.warning_absolute_time - this.options.warning_absolute_disable) * 1000
  if ( this.absolute_expiration_ms < warning_disable_time_ms ) {
    this.disabled = true
  }
};

DigiD.SessionTimer.prototype.intervalTimer = function (now) {
  var inactive_time_left = this.inactive_expiration_ms - (now - this.inactive_start_time);
  var absolute_time_left = this.absolute_expiration_ms - (now - this.absolute_start_time);

  if (this.disabled || (this.dialog && this.dialog.dialog('isOpen'))) {
    // Do not show warning if disabled or already a dialog shown
  } else if ( inactive_time_left <= (this.options.warning_inactive_time * 1000)) {
    this.inactiveWarning(inactive_time_left);
  } else if ( absolute_time_left <= (this.options.warning_absolute_time * 1000)) {
    this.absoluteWarning(absolute_time_left);
  }

  if ( absolute_time_left <= 0) { // check inactive timeout
    this.timeout('absolute');
  } else if ( inactive_time_left <= 0) { // check inactive timeout
    this.timeout('inactive');
  }

  // update timer if necessary
  if (this.options.update_timer) {
    $("#timeout-inactive-timer").html("Inactive time left : " + Math.round(inactive_time_left/1000));
    $("#timeout-absolute-timer").html("Absolute time left : " + Math.round(absolute_time_left/1000));
  }
};

DigiD.SessionTimer.prototype.inactiveWarning = function (inactive_time_left) {
  var that = this; // set scope of this
  this.dialog = openDialog(this.options.dialog_title, this.options.dialog_inactive_content, "timeout_warning");
  this.dialog.one( "dialogclose", function( event, ui ) {
    that.resetInactiveTimer();
    that.pingServer();
  });
};

DigiD.SessionTimer.prototype.absoluteWarning = function (absolute_time_left) {
  var that = this; // set scope of this
  this.dialog = openDialog(this.options.dialog_title, this.options.dialog_absolute_content, "timeout_warning", true);
  this.dialog.one( "dialogclose", function( event, ui ) {
    that.resetInactiveTimer();
    that.pingServer();
    that.disabled = true
  });
};

DigiD.SessionTimer.prototype.pingServer = function () {
  $.ajax({
    method: "post",
    url: "/session_ping",
    cache: false
  });
};

DigiD.SessionTimer.prototype.timeout = function (type) {
  closeAllTrackedWindows();
  window.location = this.options.sign_out_path + '?session_end=' + type;
};
