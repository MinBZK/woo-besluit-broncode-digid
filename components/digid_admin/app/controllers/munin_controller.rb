
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

class MuninController < ApplicationController
  skip_before_action :authenticate!, except: :index
  skip_before_action :verify_authenticity_token, only: :alarm
  before_action :whitelisted?, only: [:show, :configuration]

  def index
    munin_params[:file] = 'index.html' unless munin_params[:file]
    filename = File.expand_path(File.join(MUNIN_FOLDER, munin_params[:file]))

    raise ActionController::RoutingError, 'Not Found' unless filename.starts_with?(MUNIN_FOLDER) && File.exist?(filename)

    extname = File.extname(filename)[1..-1]
    mime_type = Mime::Type.lookup_by_extension(extname)
    content_type = mime_type.to_s unless mime_type.nil?

    send_file filename, type: content_type, disposition: 'inline'
    instrument_logger('uc18.grafieken_raadplegen_gelukt', manager_id: session[:current_user_id])
  end

  def show
    name = munin_params[:metric_name]
    type = munin_params[:metric_type]

    Metric.transaction do
      metric = Metric.select(type.downcase).where(name: "#{name}.duration").first
      metric_text = <<-MUNIN.strip_heredoc
        number.value #{metric[type]}
      MUNIN
      Metric.where(name: "#{name}.duration").update_all(average: 0, total: 0) if type == 'average'
      render plain: metric_text
    end
  end

  def configuration
    name = munin_params[:metric_name]
    type = munin_params[:metric_type]
    config_text = <<-MUNIN.strip_heredoc
      graph_title #{name.humanize} #{type == 'average' ? 'gemiddelde tijd' : 'Aantal'}
      graph_vlabel #{type == 'average' ? 'Tijd' : 'Totaal'}
      number.label #{type == 'average' ? 'Tijd' : 'Totaal'}
    MUNIN

    render plain: config_text
  end

  def alarm
    AlarmNotifier::Munin.new(params).notify
    head(200)
  end

  private

  def munin_params
    params.permit(:metric_name, :metric_type, :file)
  end

  def whitelisted?
    name = munin_params[:metric_name]
    type = munin_params[:metric_type]
    return if Metric::AllowedNameValues::ALL.include?(name) && Metric::AllowedTypes::ALL.include?(type)
    raise ActionController::RoutingError, 'Not Found'
  end
end
