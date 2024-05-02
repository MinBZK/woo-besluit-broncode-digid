
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

class Webservice < ActiveRecord::Base
  has_many :sector_authentications
  has_many :sectors, through: :sector_authentications
  has_many :certificates
  has_one :aselect_webservice, class_name: "Aselect::Webservice"
  has_one :saml_provider, class_name: "SamlProvider"
  serialize :apps

  module Authentications
    ASELECT = "aselect"
    SAML = "saml"
    ALL = [ASELECT, SAML].freeze
  end

  def self.from_authentication_session(authentication)
    return nil if authentication.blank? || authentication[:webservice_id].blank? || authentication[:webservice_type].blank?
    webservice_id = authentication[:webservice_id]

    Rails.cache.fetch("from_authentication_session_#{authentication[:webservice_type]}-#{webservice_id}", expires_in: 10.minutes) do
      if authentication[:webservice_type] == "AdProvider"
        Webservice.find(webservice_id)
      else
        authentication_webservice = authentication[:webservice_type].constantize.find(webservice_id)
        authentication_webservice.webservice
      end
    end
  end

  def check_sector_authorization(account, authentication={})

    account_sectors = Hash[account.sectorcodes.map { |s| [s.sector.id, s] }]

    # only bsn sector allowed for AdProvider
    if authentication[:webservice_type] == "AdProvider"
      bsn_sector = Sector.where(name: "bsn", active: true).first
      return account_sectors[bsn_sector.id] if bsn_sector
    else

      cached_sector_authentications = Rails.cache.fetch("check_sector_authorization-#{id}", expires_in: 10.minutes) do
        sector_authentications = self.sector_authentications.includes(:sector)
        sector_authentications.to_a.sort! { |a, b| a.position <=> b.position }
      end

      cached_sector_authentications.each do |sector_authentication|
        sector = sector_authentication.sector
        return account_sectors[sector.id] if account_sectors[sector.id] && sector.active?
      end
    end
    nil
  end

  def active?
    now = Time.zone.now
    active &&
      (active_from.nil?  || active_from <= now) &&
      (active_until.nil? || now <= active_until) &&
      sectors.where(active: true).any?
  end

  def redirect_url_valid?(url)
    Log.instrument("114", webservice_id: id, url: url) if redirect_url_domain.present? && !url.match(redirect_url_regex)
    check_redirect_url? ? url.match(redirect_url_regex) : true
  end

  def authorized_for?(options = {})
    return false if [:sector, :operation] - options.keys != []
    sector = Sector.find_by(number_name: options[:sector])
    if sector
      authentication = sector_authentications.find_by(sector_id: sector.id)
      return authentication.send(options[:operation]) if authentication
    end
    false
  end

  # Checks if the associated saml_provider allows SSO.
  def allows_sso?
    saml_provider.try(:allow_sso)
  end

  def is_mijn_digid? # rubocop:disable PredicateName
    name.eql?("Mijn DigiD")
  end

  # is being used from the SamlEngine
  def app_details
    return {} unless app_configuration.present? && app_configuration.app_to_app
    app_configuration
  end

  # is being used from the SamlEngine and in AppAuthenticationsController#send_artifact_without_redirect
  def app_return_uri
    return unless app_configuration.present?
    {
      app_return_url_scheme: app_configuration.app_return_url_scheme,
      app_return_ios_link: app_configuration.app_return_ios_link,
      app_return_android_link: app_configuration.app_return_android_link
    }
  end

  def short_name(length)
    name.truncate(length, separator: /\b,?[ -]/)
  end

  def basis_to_midden?
    assurance_date.present? && (assurance_from == "Basis" && assurance_to == "Midden")
  end

  def basis_or_midden_to_substantieel?
    assurance_date.present? && ((assurance_from == "Basis" || assurance_from == "Midden" ) && assurance_to == "Substantieel")
  end

  def assurance_to_int
    case assurance_to
    when "Midden"
      return 20
    when "Substantieel"
      return 25
    else
      return nil
    end
  end


  private

  def redirect_url_regex
    http = Rails.env.test? ? "https?://" : Regexp.escape("https://")
    domain = Regexp.escape(redirect_url_domain || "")
    # FIXME: The .* is scary, are we really checking the domain correctly?
    %r{#{http}.*#{domain}\/}
  end
end
