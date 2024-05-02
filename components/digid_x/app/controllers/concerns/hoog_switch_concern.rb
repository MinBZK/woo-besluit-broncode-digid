
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

# rubocop:disable Metrics/ModuleLength
module HoogSwitchConcern
  extend ActiveSupport::Concern

  def tonen_rijbewijs_switch?
    show_driving_licence?(current_account&.bsn) || hoog_cookie_driving_licence?
  end

  def tonen_identiteitskaart_switch?
    show_identity_card?(current_account&.bsn) || hoog_cookie_id_card?
  end

  private

  def show_hoog_options?
    session && session[:session] == "sign_in" && webservice_present? && tonen_rijbewijs_switch?
  end

  def show_driving_licence_option?
    session && session[:session] == "sign_in" && webservice_present? && tonen_rijbewijs_switch?
  end

  def show_id_card_option?
    session && session[:session] == "sign_in" && webservice_present? && tonen_identiteitskaart_switch?
  end

  def hoog_cookie_driving_licence?
    cookies[:in_hoog_pilot_driving_licence] && Switch.find_by(name: "Tonen DigiD Hoog - Rijbewijs")&.status == Switch::Status::PILOT_GROUP
  end

  def hoog_cookie_id_card?
    cookies[:in_hoog_pilot_id_card] && Switch.find_by(name: "Tonen DigiD Hoog - Identiteitskaart")&.status == Switch::Status::PILOT_GROUP
  end

  def check_tonen_rijbewijs_switch
    return if tonen_rijbewijs_switch?
    render_not_found
  end

  def check_tonen_identiteitskaart_switch
    return if tonen_identiteitskaart_switch?
    render_not_found
  end

  def check_tonen_hoog_switch
    if session[:chosen_wid_type] == "NL-Rijbewijs"
      return if tonen_rijbewijs_switch?
      render_not_found
    elsif  session[:chosen_wid_type] == "NI"
      return if tonen_identiteitskaart_switch?
      render_not_found
    end
  end
end
