
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

module MyDigidHelper

  def mobile_number(mobile_number:, trunk: 0, mask: false)
    return unless mobile_number.present?

    if (nl_nummer = mobile_number.match(/^(\+31|0031)(\d*)$/))
      displayable_mobile_number = "#{trunk}#{nl_nummer[2]}"
    else
      displayable_mobile_number = mobile_number
    end

    if mask
      displayable_mobile_number = "X" * (displayable_mobile_number.length - 3) + displayable_mobile_number[-3,3]
    else
      displayable_mobile_number
    end
  end

  def mask_email(email)
    return unless email.present?

    username, domain = email.split(/\@/)
    domain, tld = domain.split(/\./, 2)

    masked_email = username[0,2] + "*****@" + domain[0,2] + "*****." + tld
  end

  def last_active_app?
    current_account.app_authenticators.active.count == 1
  end

  def account_login_level_two_factor?
    current_account.login_level_two_factor?
  end

  def check_existing_code_emails
    return unless current_account.max_emails_per_day?(::Configuration.get_int("aantal_controle_emails_per_dag"))
    Log.instrument("1095", account_id: current_account.id)
    max_emails_per_day_error
  end

  def digid_app_android_store_link
    external_link_to(t("my_digid.links.digid_app_android_store_url"), Configuration.get_string("digid_app_android_store_url"))
  end

  def digid_app_ios_store_link
    external_link_to(t("my_digid.links.digid_app_ios_store_url"), Configuration.get_string("digid_app_ios_store_url"))
  end

  def digid_app_info_link
    external_link_to(t("my_digid.links.digid_app_info"), "https://www.digid.nl/over-digid/app")
  end

  def digid_app_id_check_link
    external_link_to(t("my_digid.links.digid_app_substantial_info"), "https://www.digid.nl/over-digid/app/controle-identiteitsbewijs/")
  end

  def spinner
    "<div class=\"lds-spinner\" aria-label=\"#{t('one_moment_please')}\"><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div></div>".html_safe
  end

  def sms_keuze_url(params = nil)
    if controller_name == "sms_verifications"
      my_digid_controleer_sms_start_url(params)
    elsif controller_name == "sms"
      authenticators_check_mobiel_url(params)
    end
  end

  def sms_keuze_nummer
    number = if @sms_uitdaging && @sms_uitdaging.mobile_number
               @sms_uitdaging.mobile_number
             elsif current_account && current_account.phone_number
               current_account.phone_number
             end
    mobile_number(mobile_number: number)
  end

  def pin_reset_allowed?(document)
    document[:allow_pin_reset] == true
  end

  def toeslag_betaald?(document)
    document.eidtoeslag != "Niet betaald"
  end
end
