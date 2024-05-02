
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

class DetectExpiredCertificatesJob
  include Sidekiq::Worker
  sidekiq_options retry: false, queue: 'bulk'

  def perform
    result = {}
    result[:rda_cert] = rda_certificates.count
    result[:rda_crl]  = rda_crls.count
    result[:eid_cert] = eid_certificates.count
    result[:eid_crl]  = eid_crls.count
    result[:dc]       = dc_certificates.count
    result[:admin]    = admin_certificates.count

    warning.update(active: result.values.sum > 1, data: result)
  end

  private

  def warning
    ManagerWarning.find_by(name: "Verlopen certificaten melding")
  end

  def filter_by_date(field = :not_after)
    ->(item) { item.send(field) < expiration_date }
  end

  def rda_certificates
    @rda_certificates ||= Rda::Certificate.all.select(&filter_by_date)
  rescue ActiveResource::ServerError
    []
  end

  def rda_crls
    @rda_crls ||= Rda::Crl.all.select(&method(:filter_by_date).call(:next_update))
  rescue ActiveResource::ServerError
    []
  end

  def eid_certificates
    @eid_certificates ||= Eid::Certificate.all.select(&filter_by_date)
  rescue ActiveResource::ServerError
    []
  end

  def eid_crls
    @eid_crls ||= Eid::Crl.all.select(&method(:filter_by_date).call(:next_update))
  rescue ActiveResource::ServerError
    []
  end

  def dc_certificates
    date_until_criteria = {:organization => {"name"=>""},
                           :certificate => {"fingerprint"=>"", "cert_type"=>"", "active_until" => "#{expiration_date.to_s}"},
                           :page => nil}
    @dc_certificates ||= Dc::Certificate.search(date_until_criteria)
  end

  def admin_certificates
    @admin_certificates ||= Certificate.joins(:webservice).merge(Webservice.active).includes(webservice: :organization).where("not_after < ?", expiration_date)
  end

  def expiration_date
    Configuration.get_int("Waarschuwingstermijn_te_verlopen_certificaten").days.from_now.to_datetime
  end
end
