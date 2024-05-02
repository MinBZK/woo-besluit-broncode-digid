
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

class Webservice < AccountBase
  include FourEyes

  class Authentications
    ASELECT = 'aselect'.freeze
    SAML = 'saml'.freeze
    NONE = ''.freeze
    ALL = [ASELECT, SAML, NONE].freeze
  end

  NAME_ALLOWED_CHARACTERS = /\A[a-zA-Z0-9ÀÁÂÃÄÅàáâãäåÈÉÊËèéêëÌÍÎÏìíîïÒÓÔÕÖòóôõöÙÚÛÜùúûüç@_'!\s\&()\+,-\.\/:;\?]+\z/

  default_scope { where(cluster: false) }

  paginates_per 10

  has_many :sector_authentications, -> { order(:position) }
  accepts_nested_attributes_for :sector_authentications, allow_destroy: true
  has_many :sectors, through: :sector_authentications

  serialize :apps

  has_many :certificates, -> { where(cert_type: "TLS") }, dependent: :destroy
  accepts_nested_attributes_for :certificates, allow_destroy: true
  validate :validate_certificates_ca, if: -> { active? || new_record? }
  validate :validate_saml_metadata_certificate, if: -> { active? || new_record? }

  has_one :saml_provider
  accepts_nested_attributes_for :saml_provider
  has_one :aselect_webservice
  accepts_nested_attributes_for :aselect_webservice

  belongs_to :organization

  validates :description, presence: true
  validates :name, presence: true
  validates :organization, presence: true
  validates :website_url, format: { with: %r{\A(http://|https://)}, message: :valid_url_prefix }, presence: true
  validate :max_number_of_certificates
  validates :name, format: { with: NAME_ALLOWED_CHARACTERS, message: :contains_illegal_characters }

  validate :validate_assurance_presence, if: -> { assurance_from.present? || assurance_to.present? || assurance_date.present? }
  validate :validate_assurance_levels, if: -> { assurance_from.present? && assurance_to.present? && assurance_date.present? }
  validate :validate_assurance_date, if: -> { assurance_date.present? }
  validate :validate_apps, if: -> { app_to_app }

  before_validation :clear_saml_provider_entity_id

  scope :active, -> { where('webservices.active = 1 AND (active_from IS NULL or active_from < NOW()) AND (active_until IS NULL or active_until > NOW())') }

  def apps
    super || []
  end

  def validate_apps
    invalid_apps = apps.select{|i| i["name"].present? && i["url"].blank? || i["name"].blank? && i["url"].present?}
    errors.add(:apps, I18n.t("app_to_app_validation_failed")) if invalid_apps.size > 0
  end

  def validate_certificates_ca
    message = I18n.t('certificates.ca_verification_failed')
    begin
      invalids = certificates.select {|c| !c.certificate_ca_valid? && !c.marked_for_destruction?}
      errors.add(:certificate, message) if invalids.size > 0
    rescue
      errors.add(:certificate, message)
    end
  end

  def validate_saml_metadata_certificate
    if self.try(:saml_provider).try(:cached_metadata).try(:present?)
      errors.add(:"saml_provider.certificate", I18n.t('certificates.ca_verification_failed')) if !saml_provider.metadata_certificate_valid?
    end
  end

  def max_number_of_certificates
    errors.add(:certificates, I18n.t('certificates.to_many_certificates')) if certificates.size > 2
  end

  def validate_assurance_presence
    unless assurance_from.present? && assurance_to.present? && assurance_date.present?
      errors.add("Infomelding aankomende verhoging zekerheidsniveau:", "Van zekerheidsniveau, Naar zekerheidsniveau en Ingangsdatum zijn verplicht of dienen alle drie leeg gelaten te worden.")
    end
  end

  def validate_assurance_levels
    levels = %(Basis Midden Substantieel Hoog)
    if levels.index(assurance_from) >= levels.index(assurance_to)
      errors.add("Infomelding aankomende verhoging zekerheidsniveau:", "Het Naar zekerheidsniveau dient hoger te zijn dan het Van zekerheidsniveau.")
    end
  end

  def validate_assurance_date
    errors.add("Infomelding aankomende verhoging zekerheidsniveau:", "De ingangsdatum dient in de toekomst te liggen")  if assurance_date <= Date.current
  end

  def ssl_certificates_expire
    expire_dates = []
    certificates.each { |cert| expire_dates << cert.certificate.not_after }
    expire_dates
  end


  def encryption_certificate_expire
    if saml_provider && saml_provider.cached_metadata && encryption_certificate
      [OpenSSL::X509::Certificate.new(encryption_certificate).not_after]
    else
      []
    end
  end

  def encryption_certificate
    saml_provider&.client_encryption_certificate
  end

  def signing_certificate_expire
    if saml_provider && saml_provider.cached_metadata && signing_certificate
      [OpenSSL::X509::Certificate.new(signing_certificate).not_after]
    else
      []
    end
  end

  def signing_certificate
    saml_provider&.client_signing_certificate
  end

  def with_expired_certificates?
    [ssl_certificates_expire + encryption_certificate_expire + signing_certificate_expire].flatten.each do |expire_date|
      return true if expire_date < Time.now + 3.months
    end
    false
  end

  def saml?
    authentication_method == Authentications::SAML
  end

  def aselect?
    authentication_method == Authentications::ASELECT
  end

  def sanitized_params=(params)
    # TODO consider sanitizing for added certificates without a file set
    if params.try(:[], :certificates_attributes)
      certificate_params = params[:certificates_attributes]
      certificate_params.each do |k, v|
        if certificate_params[k][:certificate_file]
          certificate_params[k][:certificate_file].tempfile.seek(0) # ensure the file read pointer is at the beginning of the file
          tmp_certificate = Certificate.new(certificate_file: certificate_params[k][:certificate_file])
          certificate_params[k][:cached_certificate] = tmp_certificate.cached_certificate
          certificate_params[k][:distinguished_name] = tmp_certificate.distinguished_name
          certificate_params[k][:fingerprint] = tmp_certificate.fingerprint
          certificate_params[k].delete(:certificate_file)
        end
      end
    end

    if params.try(:[], :saml_provider_attributes)
      saml_provider_params = params[:saml_provider_attributes]

      saml_provider_params.delete(:clear_metadata) if saml_provider_params[:metadata_file].present?

      if saml_provider_params[:clear_metadata] == "1"
        saml_provider_params[:cached_metadata] = nil
        saml_provider_params[:entity_id] = nil
      else
        if (metadata_file = saml_provider_params.delete(:metadata_file))
          begin
            tmp_saml_provider = SamlProvider.new(metadata_file: metadata_file)
            saml_provider_params[:cached_metadata] = tmp_saml_provider.cached_metadata
            saml_provider_params.delete(:metadata_file)
          ensure
            metadata_file.try(:tempfile).try(:seek, 0) # ensure the file read pointer is at the beginning of the file
          end
        end
      end
    end

    @sanitized_params = params
  end

  private

  def clear_saml_provider_entity_id
    return unless authentication_method == Authentications::ASELECT
    saml_provider && saml_provider.entity_id = nil
  end

  def saml_metadata
    @saml_metadata ||= Saml::Elements::EntityDescriptor.parse(saml_provider.cached_metadata)
  end
end
