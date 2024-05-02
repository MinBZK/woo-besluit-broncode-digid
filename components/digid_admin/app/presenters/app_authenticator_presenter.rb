
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

class AppAuthenticatorPresenter < BasePresenter
  presents :app_authenticator

  delegate :activated_at, :substantieel_activated_at, :last_sign_in_at, :requested_at, to: :app_authenticator

  def issuer_type
    I18n.t("issuer_type.#{app_authenticator.issuer_type.downcase}") if app_authenticator.issuer_type.present?
  end

  def nfc_support
    # Done in a little strange way, because translations like 'no' and 'yes' did not work
    support = case
    when app_authenticator.nfc_support.nil? then 'onbekend'
      when app_authenticator.nfc_support then 'ja'
      else 'nee'
    end
    I18n.t("app_authenticator.nfc_support.#{support}")
  end

  def substantieel_document_type
    I18n.t("app_authenticator.substantieel_document_type.#{app_authenticator.substantieel_document_type}") if app_authenticator.substantieel_document_type.present?
  end

  def instance_id
    app_authenticator.instance_id[0..5].upcase
  end

  def device_name
    app_authenticator.device_name.truncate(35) if app_authenticator.device_name
  end

  def last_sign_in_at
    time_helper(@object.last_sign_in_at, :slash, false)
  end

  def requested_at
    time_helper(@object.requested_at, :slash)
  end

  def activated_at
    time_helper(@object.activated_at, :slash)
  end

  def colored_status
    if @object.status == Authenticators::AppAuthenticator::Status::ACTIVE
      strong green @object.human_status
    elsif @object.status == Authenticators::AppAuthenticator::Status::INACTIVE
      strong red @object.human_status
    else
      strong @object.human_status
    end
  end

  def verhoogd
    time_helper @object.substantieel_activated_at, :slash
  end

  def type_id
    t @object.substantieel_document_type, scope: [:app_authenticator, :substantieel_document_type] if @object.substantieel_document_type.present?
  end
end
