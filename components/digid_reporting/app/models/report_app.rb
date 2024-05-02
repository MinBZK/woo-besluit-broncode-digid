
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

# Report app counters
#
# The reports counts the users and usage of the DigiD app.
# To determine the numbers of app users
# the app_authenticators table is used.
# The numbers of events are counted by logging codes from the logs
# table.
#
# v0.1 2015 PPPPPPPPPPPPPPPPPPPPP
#
class ReportApp < AdminReport
  # Construct a report object. The period parameter describe the report
  # period. Default is a week.
  # The period is a constant from the ReportParam class.
  def initialize(period = ReportParam::Week)
    @report_class_name = 'ReportApp'
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Instantiate report class #{@report_class_name} Report range is a #{period}"
    @rep_param = prepare_report_param(period, Time.now, @report_class_name)
    @header = ["periode", "beschrijving van de teller", "aantal"]
    @period = period
    @report_name = 'Overzicht gebruik DigiD app'
    @nr_active_appauthenticators = 0
    @nr_active_app_without_sms = 0
    # List of counters in this report.
    @counters = [
        "report_accounts_with_active_appauthenticators_midden",
        "report_accounts_with_active_appauthenticators_substantieel",
        "report_total_active_appauthenticators_midden",
        "report_total_active_appauthenticators_substantieel",
        "report_app_start_activation_total",
        "report_app_successful_activation_total",
        "report_app_start_activation_with_sms",
        "report_app_successful_activation_with_sms",
        "report_start_request_letter",
        "report_successful_request_letter",
        "report_start_activation_letter",
        "report_successful_activation_letter",
        "report_app_start_activation_password",
        "report_app_successful_activation_password",
        "report_app_start_activation_substantieel",
        "report_app_successful_activation_substantieel",
        "report_app_start_activation_with_other_app",
        "report_app_successful_activation_with_other_app",
        "report_start_verhogen_substantieel",
        "report_successful_verhogen_substantieel",
        "report_start_verhogen_substantieel_via_kiosk",
        "report_successful_verhogen_substantieel_via_kiosk",
        "report_start_verhogen_substantieel_via_id_checker",
        "report_successful_verhogen_substantieel_via_id_checker",
        "report_start_activatie_met_id_checker_via_gemeentebalie",
        "report_successful_activatie_met_id_checker_via_gemeentebalie",
        "report_attempts_auth",
        "report_success_auth_app_midden",
        "report_success_auth_app_substantial",
        "report_attempts_auth_hoog",
        "report_success_auth_app_hoog",
        "report_attempts_activate_hoog",
        "report_success_activate_app_hoog",
        "report_attempts_revoke_hoog",
        "report_success_revoke_app_hoog",
        "report_attempts_unblock_hoog",
        "report_success_unblock_app_hoog"
        ]
  end


  # Generic worker method. Will be called by it's
  # siblings ReportApp<period>
  #
  # The start_date parameter must lay in the requested period. The default,
  # when no parameter is supplied, is the last, completed, period.
  # I.E. last week or last month.
  #
  # This report consists of a number counters, each require their own
  # database query. Queries are execute in their own method and added
  # to the resulting CSV.
  #
  # Returns the generated data as CSV with a header row.
  def report(start_date = nil)
    @rep_param = prepare_report_param(@period, start_date, @report_class_name)
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Generate report with startdate #{start_date}"

    result = [@header]  #result = []

    # Build the report
    @counters.each do |counter|
      result << [@rep_param.period_value.to_s, send(counter)].flatten
    end

    logger.info "INFO -> #{@report_class_name} ===> Result: #{result}"
    return result
  end

  def report_accounts_with_active_appauthenticators_midden
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Generate data"
    name = "Aantal accounts met tenminste één geactiveerde DigiD app (alleen zonder ID-check)"

    @nr_accounts_active_appauthenticators = Authenticators::AppAuthenticator
                                                   .where(app_authenticators: {
                                                          status: Authenticators::AppAuthenticator::Status::ACTIVE,
                                                          substantieel_activated_at: nil })
                                                   .select("distinct(account_id)")
                                                   .count

    logger.debug "DEBUG -> #{me} ===> name: #{name};
        query result: #{@nr_accounts_active_appauthenticators}"

    result = [name, @nr_accounts_active_appauthenticators]
    logger.debug "DEBUG -> #{me} ===> result: #{result}"
    return result
  end

  def report_accounts_with_active_appauthenticators_substantieel
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Generate data"
    name = "Aantal accounts met tenminste één geactiveerde DigiD app met ID-check"

    @nr_accounts_active_appauthenticators = Authenticators::AppAuthenticator
                                                   .where(app_authenticators: { status: Authenticators::AppAuthenticator::Status::ACTIVE })
                                                   .where.not(app_authenticators: { substantieel_activated_at: nil })
                                                   .select("distinct(account_id)")
                                                   .count

    logger.debug "DEBUG -> #{me} ===> name: #{name};
        query result: #{@nr_accounts_active_appauthenticators}"
    result = [name, @nr_accounts_active_appauthenticators]
    logger.debug "DEBUG -> #{me} ===> result: #{result}"
    return result
  end

  def report_total_active_appauthenticators_midden
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Generate data"
    name = "Aantal geactiveerde DigiD apps zonder ID-check"

    @nr_active_appauthenticators = Authenticators::AppAuthenticator
                                   .where(status: Authenticators::AppAuthenticator::Status::ACTIVE)
                                   .where(substantieel_activated_at: nil)
                                   .count

    logger.debug "DEBUG -> #{me} ===> name: #{name};
        query result: #{@nr_active_appauthenticators}"
    result = [name, @nr_active_appauthenticators]
    logger.debug "DEBUG -> #{me} ===> result: #{result}"
    return result
  end

  def report_total_active_appauthenticators_substantieel
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Generate data"
    name = "Aantal geactiveerde DigiD apps met ID-check"

    @nr_active_appauthenticators = Authenticators::AppAuthenticator
                                   .where(status: Authenticators::AppAuthenticator::Status::ACTIVE)
                                   .where.not(substantieel_activated_at: nil)
                                   .count

    logger.debug "DEBUG -> #{me} ===> name: #{name};
        query result: #{@nr_active_appauthenticators}"
    result = [name, @nr_active_appauthenticators]
    logger.debug "DEBUG -> #{me} ===> result: #{result}"
    return result
  end

  def report_app_start_activation_total
    return generate_result("Aantal keren start DigiD app activeren - totaal", [1365, 1476, 1436, 105_267, 105_226, 1394])
  end

  def report_app_successful_activation_total
    return generate_result("Aantal keren succesvolle activering - totaal", [741, 917, 1219, 1367, 105_111, 105_193, 105_268, 1410, 1484])
  end

  def report_app_start_activation_with_sms
    return generate_result("Aantal keren start DigiD app activeren met sms", [1436])
  end

  def report_app_successful_activation_with_sms
    return generate_result("Aantal keren succesvolle DigiD app activering met sms", ['uc5.app_activation_done'])
  end

  def report_start_request_letter
    return generate_result("Aantal keren start aanvraag per brief", [735, 914, 105_226, 105_191])
  end

  def report_successful_request_letter
    return generate_result("Aantal keren succesvolle aanvraag per brief", ['uc5.app_activation_by_letter_activation_code_requested'])
  end

  def report_start_activation_letter
    return generate_result("Aantal keren start invoeren activeringscode uit brief", [1089, 105_227])
  end

  def report_successful_activation_letter
    return generate_result("Aantal keren succesvolle activering per brief", ['uc5.app_activation_by_letter_activationcode_success'])
  end

  def report_app_start_activation_password
    return generate_result("Aantal keren start DigiD app activeren met wachtwoord", [1476])
  end

  def report_app_successful_activation_password
    return generate_result("Aantal keren succesvolle DigiD app activering met wachtwoord", [1484])
  end

  def report_app_start_activation_substantieel
    return generate_result("Aantal keren start DigiD app activeren met ID-check", [1218])
  end

  def report_app_successful_activation_substantieel
    return generate_result("Aantal keren succesvolle activering met ID-check", [105_268, 1219])
  end

  def report_app_start_activation_with_other_app
    return generate_result("Aantal keren start DigiD app activeren met andere app", [1365])
  end

  def report_app_successful_activation_with_other_app
    return generate_result("Aantal keren succesvolle activering met andere app", [1367])
  end

  def report_start_verhogen_substantieel
    return generate_result("Aantal keren start toevoegen ID-check op eigen initiatief of tijdens inloggen", ["uc5.app_to_substantial"])
  end

  def report_successful_verhogen_substantieel
    return generate_result("Aantal keren ID-check toegevoegd op eigen initiatief of tijdens inloggen", ['uc5.app_to_substantial_successful'])
  end

  def report_start_verhogen_substantieel_via_kiosk
    return generate_result("Aantal keren start toevoegen ID-check via kiosk", ["uc5.kiosk_app_to_substantial_start"])
  end

  def report_successful_verhogen_substantieel_via_kiosk
    return generate_result("Aantal keren succesvolle ID-check toegevoegd via kiosk", ['uc5.kiosk_app_to_substantial_successful'])
  end

  def report_start_verhogen_substantieel_via_id_checker
    return generate_result("Aantal keren start toevoegen ID-check via ID-checker app", [105_276, 1307])
  end

  def report_successful_verhogen_substantieel_via_id_checker
    return generate_result("Aantal keren succesvolle ID-check toegevoegd via ID-checker app", [105_281, 1321])
  end

  def report_start_activatie_met_id_checker_via_gemeentebalie
    return generate_result("Aantal keren start activatie DigiD app met ID-check via gemeentebalie", [1409])
  end

  def report_successful_activatie_met_id_checker_via_gemeentebalie
    return generate_result("Aantal keren succesvolle activatie DigiD app met ID-check via gemeentebalie", [1410])
  end

  def report_attempts_auth
    return generate_result("Aantal authenticatiepogingen met de DigiD app", ['uc2.authenticeren_digid_app_to_app_start', 102_084, 102_106, 837, 1121, 1186])
  end

  def report_success_auth_app_midden
    return generate_result("Aantal gelukte authenticaties met de DigiD app zonder toegevoegde ID-check", ['uc2.authenticeren_digid_app_gelukt', 'uc2.authenticeren_digid_app_to_app_gelukt'])
  end

  def report_success_auth_app_substantial
    return generate_result("Aantal gelukte authenticaties met de DigiD app met toegevoegde ID-check", ['uc2.authenticeren_substantieel_gelukt', 'uc2.authenticeren_digid_app_to_app_substantieel_gelukt'])
  end

  # Digid Hoog
  def report_attempts_auth_hoog
    return generate_result("Aantal authenticatiepogingen Hoog met DigiD app", ['digid_hoog.authenticate.chose_app', 'digid_hoog.authenticate.chose_desktop_app', 'uc2.authenticeren_digid_app_to_app_wid_upgrade'])
  end

  def report_success_auth_app_hoog
    return generate_result("Aantal gelukte authenticatiepogingen Hoog met DigiD app", ['uc2.authenticeren_hoog_gelukt', 'uc2.authenticeren_digid_app_to_app_hoog_gelukt'])
  end

  def report_attempts_activate_hoog
    return generate_result("Aantal activatiepogingen inloggen met identiteitsbewijs via DigiD app", [920, 921])
  end

  def report_success_activate_app_hoog
    return generate_result("Aantal gelukte activatiepogingen inloggen met identiteitsbewijs via DigiD app", ['digid_hoog.activate.success'])
  end

  def report_attempts_revoke_hoog
    return generate_result("Aantal intrekkingspogingen inlogfunctie identiteitsbewijs via DigiD app", ['digid_hoog.revoke.app_midden_start', 'digid_hoog.revoke.app_substantieel_start'])
  end

  def report_success_revoke_app_hoog
    return generate_result("Aantal gelukte intrekkingen inlogfunctie identiteitsbewijs via DigiD app", ['digid_hoog.revoke.success'])
  end

  def report_attempts_unblock_hoog
    return generate_result("Aantal deblokkeringspogingen inlogfunctie identiteitsbewijs via DigiD app", ['digid_hoog.unblock.app_chosen', 'digid_hoog.unblock.usb_chosen'])
  end

  def report_success_unblock_app_hoog
    return generate_result("Aantal gelukte deblokkeringen inlogfunctie identiteitsbewijs met DigiD app", ['digid_hoog.unblock.unblocked'])
  end

  private

  def generate_result(name, logcodes = [])
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Generate data"
    result = [name, count_logcodes(logcodes)]
    logger.debug "DEBUG -> #{me} ===> result: #{result}"
    return result
  end

  def count_logcodes(uc_labels)
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> count all occurences of the given logcodes: #{uc_labels}"
    result = 0
    idx_hint = "#{Log.quoted_table_name} FORCE INDEX(index_logs_on_created_at)"
    codes = lookup_codes(uc_labels, nil)&.flatten
    result = Log.from(idx_hint).where(
      :created_at => (@rep_param.start_ts..@rep_param.end_ts),
      :code       => codes).count
    return result
  end

  def subtract_logcodes(subtract_labels)
    uc_label = subtract_labels.shift
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> subtract all occurences of the given logcodes: #{uc_label} from the following logcodes: #{subtract_labels}"
    result = 0
    idx_hint = "#{Log.quoted_table_name} FORCE INDEX(index_logs_on_created_at)"
    codes = lookup_codes(uc_label, nil)
    subtract_codes = lookup_codes(subtract_labels, nil)
    result = Log.from(idx_hint).where(
      :created_at => (@rep_param.start_ts..@rep_param.end_ts),
      :code       => codes).count
    subtract_result = Log.from(idx_hint).where(
      :created_at => (@rep_param.start_ts..@rep_param.end_ts),
      :code       => subtract_codes).count
    return result - subtract_result
  end

end
