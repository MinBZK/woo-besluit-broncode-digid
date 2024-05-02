
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

# encoding: UTF-8

class CertificatesController < ApplicationController
  skip_before_action :authenticate!, only: [:white_list, :fingerprints]
  respond_to :html, :js

  def index
    raise(CanCan::AccessDenied) unless can?(:read, Webservice)

    @certificates = Certificate.joins(:webservice).merge(Webservice.active).includes(webservice: :organization)

    @certificates = @certificates.where(webservices: webservice_search_params) if webservice_search_params.present?
    @certificates = @certificates.where(cert_type: cert_type) if cert_type.present?
    @certificates = @certificates.where("not_after < ?", certificate_not_after) if certificate_not_after.present?
    @certificates = @certificates.where("not_before < ?", certificate_not_before) if certificate_not_before.present?
    @certificates = @certificates.order(:not_after).page(params[:page])

    instrument_logger("1422", entity: "Oude webdienstcertificaten") if searching?
  end

  # GET /certificates/1
  def show
    @certificate = Certificate.find(params[:id])
    @dn = '/' + @certificate.distinguished_name_from_cert.to_a.map { |e| "#{e[0]}=#{e[1].force_encoding('UTF-8')}" }.join('/')
    send_data @certificate.cached_certificate, filename: "#{@certificate.common_name.encode('UTF-8').parameterize}.cer", disposition: 'inline'
  end

  def fingerprints
    webservices = Webservice.active.includes(:certificates)

    certificates = []
    webservices.find_each do |webservice|
      webservice.certificates.each do |certificate|
        next unless !::Configuration.get_boolean('check_certificate_expirations') || (certificate.certificate.not_after > Time.now)

        certificate_type = webservice.authentication_method == 'saml' ? 'SAML' : 'WSDL'
        certificates << <<-CERT.strip_heredoc
          "#{certificate_type}:#{certificate.fingerprint}" { "#{webservice.name}-#{certificate.id}" }
        CERT
      end
    end
    render plain: certificates.join
  end

  # Deprecated remove if not used anymore
  def white_list
    webservices = Webservice.active.includes(:certificates)

    certificates = []
    webservices.find_each do |webservice|
      webservice.certificates.each do |certificate|
        next unless !::Configuration.get_boolean('check_certificate_expirations') || (certificate.certificate.not_after > Time.now)

        certificate_type = webservice.authentication_method == 'saml' ? 'SAML' : 'WSDL'
        certificates << <<-CERT.strip_heredoc
          "#{certificate_type}:#{Digest::MD5.hexdigest(certificate.load_balancer_subject)}" { "#{webservice.name}-#{certificate.id}" }
        CERT
      end
    end
    render plain: certificates.join
  end

  private

  def searching?
    certificate_not_after.present? || certificate_not_before.present? || webservice_search_params.present? || cert_type.present?
  end

  def expiration_date
    ::Configuration.get_int("Waarschuwingstermijn_te_verlopen_certificaten").days.from_now.to_datetime
  end

  def certificate_not_after
    return if params.dig(:certificate, "not_after(1i)").blank?

    params[:certificate][:not_after] = DateTime.new(
      params[:certificate]["not_after(1i)"].to_i,
      params[:certificate]["not_after(2i)"].to_i,
      params[:certificate]["not_after(3i)"].to_i
    )
  end

  def certificate_not_before
    return if params.dig(:certificate, "not_before(1i)").blank?

    params[:certificate][:not_before] = DateTime.new(
      params[:certificate]["not_before(1i)"].to_i,
      params[:certificate]["not_before(2i)"].to_i,
      params[:certificate]["not_before(3i)"].to_i
    )
  end

  def cert_type
    params.dig(:certificate, :cert_type)
  end

  def webservice_search_params
    @webservice_search ||= params.slice(:webservices)[:webservices]&.to_h&.select{|_,v| v.present? }&.tap do |params|
      params[:id] = Webservice.where("name LIKE (?)", "%#{params[:id]}%").ids if params[:id].present?
      params[:organization_id] = Organization.where("name LIKE (?)", "%#{params[:organization_id]}%").ids if params[:organization_id].present?
    end
  end
end
