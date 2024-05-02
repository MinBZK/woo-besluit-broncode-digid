
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

class MnnQryRunner
  def self.get_mnn_qry_list
    qry_class_list = MnnBaseQry.subclasses.find_all { |klass| klass.name =~ /^Mnn.*Qry$/ }
    return create_mnn_qry_instances_per_type(qry_class_list)
  end

  def self.update_metrics_timestamp(mnn_qry_list)
    mnn_qry_list.each do |mnn_qry|
      mnn_qry.update_metrics_timestamp
    end
  end

  def self.run
    mnn_qry_list = get_mnn_qry_list();
    # mnn_qry_list = [MnnAuthsQry.new(:success)]

    # First we save the end_timeframe in the updated_at column
    # In case a mnn_qry takes more than 5 minutes, we don't get overlapping problems
    update_metrics_timestamp(mnn_qry_list)
    mnn_qry_list.each do |mnn_qry|
      mnn_qry.run
    end
  end

  def self.reset_counters
    mnn_qry_list = get_mnn_qry_list();
    mnn_qry_list.each do |mnn_qry|
      mnn_qry.reset_metrics()
    end
  end

  def self.get_organizations_to_monitor
    organization_names = APP_CONFIG['organizations_to_monitor'].split(",")
    organizations = []

    organization_names.each do |organization_name|
      organization = Organization.select([:id, :name]).where(:name => organization_name).first
      unless organization
        Rails.logger.error("Organization '#{organization_name} couldn't be found")
      else
        organizations << organization
      end
    end

    return organizations
  end

  def self.get_webservices_to_monitor
    webservice_names = APP_CONFIG['webservices_to_monitor'].split(",")
    webservices = []

    webservice_names.each do |webservice_name|
      webservice = Webservice.unscoped.select([:id, :name]).where(:name => webservice_name).first
      unless webservice
        Rails.logger.error("Webservice '#{webservice_name} couldn't be found")
      else
        webservices << webservice
      end
    end

    return webservices
  end

  private
  def self.create_mnn_qry_instances_per_type(qry_class_list)
    mnn_qry_list = []
    organizations = get_organizations_to_monitor()
    webservices = get_webservices_to_monitor()

    qry_class_list.each do |qry_class|
      qry_class.get_types.each do |type|
        if qry_class.name =~ /ByOrgQry$/
          organizations.each do |organization|
            mnn_qry_list << qry_class.new(type: type, organization: organization)
          end
        elsif qry_class.name =~ /ByWebsQry$/
          webservices.each do |webservice|
            mnn_qry_list << qry_class.new(type: type, webservice: webservice)
          end
        else
          mnn_qry_list << qry_class.new(type: type)
        end
      end
    end
    return mnn_qry_list
  end

end
