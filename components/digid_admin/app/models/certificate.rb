
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

require 'openssl'
class Certificate < AccountBase
  belongs_to :webservice

  validates :cached_certificate, presence: true
  validates :distinguished_name, uniqueness: { case_sensitive: true, scope: :cert_type }, allow_nil: true

  attr_accessor :certificate_file

  def certificate_file=(file)
    if file && file.respond_to?(:read)
      file.rewind
      self.certificate = file.read
    end
  end

  def filename
    common_name + ".pem"
  end

  def as_data_uri
    return nil if self.cached_certificate.nil?
    base_64_metadata = Base64.encode64(self.cached_certificate)
    "data:application/x-pem-file;base64,#{base_64_metadata}"
  end

  def certificate
    @certificate ||= OpenSSL::X509::Certificate.new(cached_certificate) if cached_certificate
  rescue
    nil
  end

  def fingerprint
    self[:fingerprint] || OpenSSL::Digest::SHA1.new(certificate.to_der).to_s.scan(/../).map(&:upcase).join(":") if certificate
  end

  def load_balancer_subject
    distinguished_name_from_cert.to_s(OpenSSL::X509::Name::RFC2253).sub(/(^|,)jurisdictionC=([A-Z]+)/) do
      country = $2
      value = sprintf('#13%02d%s', country.size, country.unpack('H*').first).upcase
      "#{$1}1.3.6.1.4.1.311.60.2.1.3=#{value}"
    end
  end

  def certificate=(cert)
    @certificate            = OpenSSL::X509::Certificate.new(cert)
    self.distinguished_name = @certificate.subject.to_s(OpenSSL::X509::Name::RFC2253)
    self.cached_certificate = @certificate.to_pem
    self.fingerprint = fingerprint
    self.not_after = @certificate.not_after
    self.not_before = @certificate.not_before
  rescue
    errors.add(:certificate, :invalid)
    self.cached_certificate = nil
  end

  def distinguished_name_from_cert
    @distinguished_name_from_cert ||= cached_certificate.present? ? OpenSSL::X509::Certificate.new(cached_certificate).subject : nil
  end

  def common_name
    dnarr = distinguished_name_from_cert.to_a
    @cn = ''
    dnarr.each do |a|
      @cn = a[1] if a[0] == 'CN'
    end
    @cn
  end

  def certificate_ca_valid?
    Certificate.verify_ca(certificate, APP_CONFIG["trusted_ca_file"]&.path)
  end

  def self.verify_ca(certificate, ca_file)
    store  = OpenSSL::X509::Store.new
    store.add_file(ca_file)

    store.verify(certificate)
  end

  def not_after
    self[:not_after] || certificate&.not_after
  end

  def not_before
    self[:not_before] || certificate&.not_before
  end
end
