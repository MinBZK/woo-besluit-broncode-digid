
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

//= require jquery3
//= require jquery-ui
//= require jquery_ujs
//= require jquery.uniform
//= require jquery.locale
//= require jquery.tokeninput
//= require dialogs
//= require session_timer
//= require download_attribute_ie
//= require at_request
//= require bulk_order
//= require_self
//

function loadEventHandlers() {
  $('.edit_app_version').on("submit",function() {
    postData = $(this).serializeArray();
    var id = $(this).find("[name='id']")[0].value;
    var url = $(this).find("[name='update_warning_path']")[0].value;

    postData.push({name: "id", value: id});
    postData.push({name: "commit", value: "Opslaan"});

    $.ajax({
        'async': false,
        'global': false,
        'url': url,
        'type': "POST",
        'dataType': "json",
        'data': postData,
        'success': handleRemoteMessage
    });
  });

  $("[id^='delete_']").click(function(e){
    e.preventDefault();

    var dialog = $(this).closest(".hidden.dialog-wrapper")[0] || $(".hidden.dialog-wrapper")[0];
    var title = $(dialog).find(".title").html();
    var notice = $(dialog).find(".notice").html();
    var body = $(dialog).find(".body").html();

    var confirmed = confirm(notice);

    if (confirmed == true) {
      openDialog(title, body, "dialog");
      loadButtons();
    }
  })

  $(".tracked-window").on("click", function(e){
    openTrackedWindow(e.currentTarget.getAttribute("path"), e.currentTarget.getAttribute("name"))
  });

  $(".close_dialog").on("click", function(){
   formDialog = null
   $(".dialog").dialog('close')
  })

  $("#new_shared_secret").on("submit", function(e) {
    e.preventDefault();

    var aselect_provider_id = $(e.currentTarget).find("#aselect_webservice_id")[0].value;
    var encrypted_shared_secret = $("#shared_secret_shared_secret").val();
    var new_label = "Gegenereerd Shared Secret";
    addSharedSecret(encrypted_shared_secret, aselect_provider_id, new_label);
    closeDialog();

    return false
  });
}

function handleRemoteMessage(event, data, status, xhr) {
  $(".dialog").dialog('close');

  if (data.warning && !confirm(data.text)) { return false; }

  if (data.redirect_url) {
    window.location.href = data.redirect_url;
  };

  if (data.dialog_body) {
    openDialog(data.dialog_title, data.dialog_body, "dialog");
  };

  if (data.dialog_content) {
    openDialog(data.title, data.dialog_content, "dialog");
  };

  if (data.target && data.body) {
    $(data.target).html(data.body);
  }

  if (data.target && data.append) {
    $(data.target).append(data.append);
  }

  loadButtons();
  loadEventHandlers();
};

$(document).ready(loadEventHandlers);
$(document).on("ajax:success", "form[data-remote=true]", handleRemoteMessage);
$(document).on("ajax:success", "a[data-remote=true]", handleRemoteMessage);

function addCertificate(label, prefix){
   var new_certificate_id = (new Date().getTime() / 1000)

    var certificate_input = $('<input>', {
        type: 'file',
        name: prefix || 'webservice[certificates_attributes][' + new_certificate_id + '][certificate_file]'
    })

    var remove_button = removeParentElementButton({label: " Verwijder", onclick: function(e) { e.currentTarget.closest('li').remove(); return false; }});

    $("#certificates table").show();
    $("#certificates tbody").append(
      $("<tr>").append(
          $('<td>').append(certificate_input),
          $('<td>').append(''),
          $('<td>').append(remove_button)
        )
    );

    // old certficate input
    $("#certificates ul").append(
      $("<li>").append(certificate_input).append("&nbsp;").append(remove_button)
    )
}

 function addSharedSecret(encrypted_shared_secret, aselect_webservice_id, shared_secret_label){
    var new_shared_secret_id = (new Date().getTime() / 1000)

    if (aselect_webservice_id) {
      var aselect_webservice_id_input = $('<input>', {
          type: 'hidden',
          value: aselect_webservice_id,
          name: 'webservice[aselect_webservice_attributes][id]'
      })
      $("#shared_secrets").append(aselect_webservice_id_input) // Required if you add shared secrets to an excisting webservice with an excisting aselect_webservice associated
    }

   var shared_secret_input = $('<input>', {
        type: 'hidden',
        value: encrypted_shared_secret,
        name: 'webservice[aselect_webservice_attributes][shared_secrets_attributes][' + new_shared_secret_id + '][shared_secret]'
    })

    var remove_button = removeParentElementButton({label: "Verwijderen", onclick: function(e) { $(e.currentTarget).parent().remove(); return false; }});

    $("#shared_secrets ul").append(
      $("<li>").append(shared_secret_input).append(shared_secret_label).append("&nbsp;").append(remove_button)
    );

  }

function removeParentElementButton(options){
   var remove_link = $("<a>").attr({
      href: "#",
      class: "delete_button ui-button ui-corner-all ui-widget"
    })

   remove_link.on("click", options.onclick);

    var text = $("<span>").attr({class: "text"}).text(options.label);
    var icon = $("<span>").attr({class: "ui-button-icon ui-icon ui-icon-trash"});

    return remove_link.append(icon).append(text)
}


function openTrackedWindow(url, target) {
  if (!$.isArray(window.opened_windows))
    window.opened_windows = new Array();

  var new_window = window.open(url, target);
  window.opened_windows.push(new_window);
}

function closeAllTrackedWindows() {
  if (window.opened_windows) {
    $(window.opened_windows).each(function() {
      this.close();
    });
  }
}
$(window).on('unload', closeAllTrackedWindows);

function reorderSectors() {
  var order = 1;
  $('ol.sectors li').each(function() {
    $('input[id$=position]', this).val(order);
    order++;
  });
}

$(document).on('click', '.authorize_sector', function() {
  authorizeSector();
  return false;
});

function authorizeSector() {
  var sectorId = $('select#sectors').val();

  var webserviceId = null
  var webserviceIdMatch = $('form.webservice').first().attr('action').match(/\/webservices\/(\d*)/);

  if (webserviceIdMatch){
    webserviceId = webserviceIdMatch[1];
  }

  var $sectorAuthentication = $('.sectors input[id$=sector_id][value=' + sectorId + ']');
  if ($sectorAuthentication.length > 0) {
    $('select#sectors option[value=' + sectorId + ']').remove();
    $sectorAuthentication.parents('li').show();
  } else {
    $.get("/webservices/authorize_sector/" + sectorId + "?webservice_id=" + webserviceId,
      function(data) {
        $('ol.sectors').append(data.append)
        $('#sectors').find("option[value="+data.id+"]").remove();
        reorderSectors();
      }
    );
  }
  reorderSectors();
}

$(document).on("click", ".delete_sector", function(e) {
  removeSector("sector_authentication_" + e.currentTarget.id);
  return false;
});

function removeSector(id) {
  var $item = $('#' + id);
  $item.slideUp();
  $item.find('input[id$=_destroy]').val(true);
  var name = $item.find('.name').text();
  var options = $('select#sectors').html();
  var sectorId = $item.find('input[id$=sector_id]').val();
  $('select#sectors').html(options + '<option value="' + sectorId + '">' + name + '</option>');
  reorderSectors();
}

function loadButtons() {
  $('.new_button').button({icon: 'ui-icon-circle-plus' });
  $('.edit_button').button({icon: 'ui-icon-pencil' });
  $('.disabled_edit_button').button({disabled: true, icon: 'ui-icon-pencil' });
  $('.delete_button').button({icon: 'ui-icon-trash' });
  $('.print_button').button({icon: 'ui-icon-print' });
  $('.file_button').button({icon: 'ui-icon-arrowthickstop-1-s' });
  $('.list_button').button({icon: 'ui-icon-note' });
  $('.note-button').button();
  $('.unblock_button').button({icon: 'ui-icon-circle-plus' });
  $('.refresh_button').button({icon: 'ui-icon-arrowrefresh-1-e' });
  $('.check_button').button({icon: 'ui-icon-circle-check' });
  $('.close_button').button({icon: 'ui-icon-circle-close' });

  $('#main input:submit, .normal_button').button();
}

$(document).on('click', '.close_dialog', function() {
  closeDialog();
  return false;
});

function closeDialog() {
  $(".dialog").dialog("close");
}

function replaceHTML(element, html) {
  $(element).html(html);
}

$(document).ajaxComplete(function() {
  loadButtons();
});

// Show tooltip for truncated pseudonyms
$(function() {
  $('.pseudonym').tooltip();
});

$(function() {
  loadButtons();

  $.datepicker.setDefaults({
    regional: 'nl',
    showWeek: true,
    firstDay: 1
  });

  // enabling per_page selector for table footers
  $('select#per_page').bind('change', function() {
    var new_window_location = window.location.toString();
    var per_page_regex = /([&|?]per_page=)\d+/;

    if (window.location.toString().match(per_page_regex) === null) { // we don't have a per_page parameter in the location yet; add it.
      new_window_location = new_window_location + (new_window_location.indexOf('?') === -1 ? '?' : '&') + 'per_page=' + $(this).val();
    } else { // we already have a per_page parameter in the location string, replace it with correct one.
      new_window_location = new_window_location.replace(per_page_regex, "$1" + $(this).val());
      new_window_location = new_window_location.replace(/([&|?]page=)\d+/, "$1" + '1');
    }
    window.location = new_window_location;
  });

  // Make Sectors sortable
  $('form ol.sectors').sortable({
    stop : function() {
      reorderSectors();
    }
  });

  //Switch for authentications
  function showAuthentication(type) {
    $('#authentication_aselect, #authentication_saml').hide();
    $('#authentication_' + type).show();
  }

  $('.authentication').each(function() {
    if (this.checked) {
      showAuthentication($(this).val());
    }
    $(this).bind('change', function() {
      if (this.checked) {
        showAuthentication($(this).val());
      }
    });
  });

});

function alarmNotificationsHandler(event) {
  if ($("#alarm_notifications input[type='checkbox']:checked").length == 0) {
    $('#manager_notify_sms, #manager_notify_email').attr('disabled', true);
  }
  else {
    $('#manager_notify_sms, #manager_notify_email').removeAttr('disabled');
  }
}

function managerFormSubmitHandler(event) {
  if ($("#alarm_notifications input[type='checkbox']:checked").length == 0) {
    $('#manager_notify_sms, #manager_notify_email').prop('checked', false);
  }
  else {
    if ($('#manager_notify_sms:checked, #manager_notify_email:checked').length == 0) {
      msg = '<p>Kies hier eerst hoe je gealarmeerd wilt worden!</p>';
      $('#manager_notify_sms').closest('div.field').append(msg).attr('id', 'alert');
      return(false);
    }
  }
  return(true);
}

function setupDatepicker(target) {
  var element = $('<input type="text" class="datepicker" />');
  $(target).prepend(element);
  element.datepicker({
    defaultDate: "",
    changeMonth: true,
    numberOfMonths: 1
  });

  return element;
}
function setDatepickers(force, list){
  $(list || '#main .datepicker').each(function () {
    var $this = $(this);
    // add datepicker
    var hidden_date_fields = []
    var old_values = []

    $datepicker = setupDatepicker(this)

    // transform 'old' date select fields to hidden input fields
    // 1i = year, 2i = month, 3i = day

    jQuery.each(["select[id$=1i]", "select[id$=2i]", "select[id$=3i]"], function(i, name) {
      var el = $this.find(name)
      if (!el) return

      var val = el.val() || ""
      hidden_date_fields.push($('<input type="hidden" id="' + el.prop('id') + '" name="' + el.prop('name') + '" value="' + val + '" />').prependTo($this))
      el.remove()
      if (val === '' || val == '0') return

      // debugger;
      old_values.push(val.length == 1 ? "0" + val : val)
    })



    // when a user changes the textfield by hand (without datepicker interaction)
    // we update the hidden fields to the given date.
    $datepicker.bind('change', function() {
      var day_month_year_triple = $(this).val().split('-');
      if (day_month_year_triple.length == 3) {
        if (day_month_year_triple[2].length == 4) day_month_year_triple.reverse()
        hidden_date_fields[0].val(day_month_year_triple[0]); // year
        hidden_date_fields[1].val(day_month_year_triple[1]); // month
        hidden_date_fields[2].val(day_month_year_triple[2]); // day
      } else {
        hidden_date_fields.forEach(function(e) { e.val(""); });
      }
    });

    // initialize datepicker
    if (old_values.length == 3) $datepicker.val(old_values.reverse().join('-'));

    $('select.initially_empty').val('');

    $this.addClass("initialized");
  });
}

var createDateSelecter = function(fields) {
  return '<span class="datepicker"> \
    <select id="'+fields[0]+'" name="'+fields[1]+'"> \
      <option value=""></option> \
      <option value="1">1</option> \
      <option value="2">2</option> \
      <option value="3">3</option> \
      <option value="4">4</option> \
      <option value="5">5</option> \
      <option value="6">6</option> \
      <option value="7">7</option> \
      <option value="8">8</option> \
      <option value="9">9</option> \
      <option value="10">10</option> \
      <option value="11">11</option> \
      <option value="12">12</option> \
      <option value="13">13</option> \
      <option value="14">14</option> \
      <option value="15">15</option> \
      <option value="16">16</option> \
      <option value="17">17</option> \
      <option value="18">18</option> \
      <option value="19">19</option> \
      <option value="20">20</option> \
      <option value="21">21</option> \
      <option value="22">22</option> \
      <option value="23">23</option> \
      <option value="24">24</option> \
      <option value="25">25</option> \
      <option value="26">26</option> \
      <option value="27">27</option> \
      <option value="28">28</option> \
      <option value="29">29</option> \
      <option value="30">30</option> \
      <option value="31">31</option> \
    </select> \
    <select id="'+fields[2]+'" name="'+fields[3]+'"> \
      <option value=""></option> \
      <option value="1">januari</option> \
      <option value="2">februari</option> \
      <option value="3">maart</option> \
      <option value="4">april</option> \
      <option value="5">mei</option> \
      <option value="6">juni</option> \
      <option value="7">juli</option> \
      <option value="8">augustus</option> \
      <option value="9">september</option> \
      <option value="10">oktober</option> \
      <option value="11">november</option> \
      <option value="12">december</option> \
    </select> \
    <select id="'+fields[4]+'" name="'+fields[5]+'"> \
      <option value=""></option> \
      <option value="2014">2014</option> \
      <option value="2015">2015</option> \
      <option value="2016">2016</option> \
      <option value="2017">2017</option> \
      <option value="2018">2018</option> \
      <option value="2019">2019</option> \
      <option value="2020">2020</option> \
      <option value="2021">2021</option> \
      <option value="2022">2022</option> \
      <option value="2023">2023</option> \
      <option value="2024">2024</option> \
    </select> \
  </span>'
}

$(function() {
  setDatepickers();

  $('form.edit_manager').submit(function(event) {
    return managerFormSubmitHandler(event);
  });
  $("#alarm_notifications input[type='checkbox']").change(function(event) {
    alarmNotificationsHandler(event);
  });
  $(document).ready(function(event) {
    alarmNotificationsHandler(event);
  });

  $("#add_row,.add_row").click(function(e) {
    target = e.currentTarget.getAttribute("data-target");
    table = target ? $("table#" + target) : $("table");
    parent = $(table).find("tbody.rows")[0];
    child = parent.firstElementChild.cloneNode(true);
    $(child).find("input").val("");
    $(child).find("input.checkbox").each(function(i, e) { e.checked = false;})
    $(child).find(".delete_button").click(remove_row)
    $(child).find("span.datepicker").each(function(i, e) {
      $(e).removeClass("initialized");
      e.outerHTML = createDateSelecter(
        $(e).find("input[type='hidden']").map(function(i, e) {
          return [e.getAttribute("id"), e.getAttribute("name")]
        })
      )
    })
    $(child).find('a.hidden').removeClass('hidden')
    parent.appendChild(child);
    setDatepickers(true, $("span.datepicker:not(.initialized)"))
  })

  var remove_row = function(e) {
    tr = e.currentTarget.closest("tr");

    if (tr.closest("tbody").children.length > 1) {
      tr.remove();
    }
  }
  $(".rows .delete_button").click(remove_row)
})
