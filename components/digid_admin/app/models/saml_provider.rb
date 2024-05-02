
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

class SamlProvider < AccountBase
  INVALID_METADATA = 'INVALID'.freeze

  belongs_to :sso_domain
  belongs_to :webservice
  validates :entity_id, uniqueness: { case_sensitive: true }, allow_nil: true

  attr_accessor :clear_metadata

  def self.destroy_sso_domain(sso_domain)
    where(sso_domain_id: sso_domain.id).update_all(sso_domain_id: nil)
    sso_domain.destroy
  end

  def metadata_url=(url)
    if url.present?
      super
      uri                     = URI.parse(url)
      http                    = Net::HTTP.new(uri.host, uri.port)
      http.read_timeout       = 5 # make sure that we don't wait for a time-out longer than 5 seconds
      begin
        request               = Net::HTTP::Get.new(uri.request_uri)
        response              = http.request(request)
        self.cached_metadata  = response.body
      rescue Timeout::Error => e
        self.cached_metadata = e.message # a time-out will be handled as faulty metadata
      end
    end
  end

  def filename
    "metadata.xml"
  end

  def metadata_as_data_uri
    return nil if self.cached_metadata.nil?
    base_64_metadata = Base64.encode64(self.cached_metadata)
    "data:text/xml;base64,#{base_64_metadata}"
  end

  def metadata_file=(file)
    self.cached_metadata = file.read if file
  end

  def cached_metadata=(data)
    super
    return if cached_metadata.blank? || cached_metadata == INVALID_METADATA

    # Only use cached metadata if we can parse entity_id and can find metadata urls
    begin
      self.entity_id = metadata.try(:entity_id)
      self.metadata_urls

      if client_signing_certificate && webservice_id
        signing = Certificate.where(webservice_id: webservice_id, cert_type: "SIGNING").first_or_create
        signing.certificate = client_signing_certificate
        signing.save
      end

      if client_encryption_certificate && webservice_id
        encrypt = Certificate.where(webservice_id: webservice_id, cert_type: "ENCRYPTION").first_or_create
        encrypt.certificate = client_encryption_certificate
        encrypt.save
      end
    rescue Saml::Errors::SamlError, NoMethodError
      super(INVALID_METADATA)
    end
  end

  def metadata_urls
    return unless metadata.try(:sp_sso_descriptor)
    metadata.sp_sso_descriptor.assertion_consumer_services +
      metadata.sp_sso_descriptor.single_logout_services
  end

  def parsed_metadata?
    begin
      metadata.present?
    rescue
      nil
    end
  end

  def metadata_certificate_valid?
    return false if self.cached_metadata == INVALID_METADATA
    return true if APP_CONFIG["skip_ca_check"]

    store = OpenSSL::X509::Store.new
    store.add_file(APP_CONFIG["trusted_ca_file"]&.path)

    unless client_signing_certificate && store.verify(OpenSSL::X509::Certificate.new(client_signing_certificate))
      self.cached_metadata = nil
      return false
    else
      return true
    end
  end

  def client_signing_certificate
    key_descriptor = metadata.sp_sso_descriptor&.key_descriptors&.find { |k| k.use == Saml::Elements::KeyDescriptor::UseTypes::SIGNING }
    key_descriptor&.certificate
  end

  def client_encryption_certificate
    key_descriptor = metadata.sp_sso_descriptor&.key_descriptors&.find { |k| k.use == Saml::Elements::KeyDescriptor::UseTypes::ENCRYPTION }
    key_descriptor&.certificate
  end

  private

  def metadata
    Saml::Elements::EntityDescriptor.parse(cached_metadata)
  end

  def idp_metadata
    @idp_metadata ||= Saml::Elements::EntityDescriptor.parse(cached_metadata)
  end
end
