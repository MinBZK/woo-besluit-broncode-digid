
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

# require 'letter'

class ActivationLetterFilesController < ApplicationController
  load_and_authorize_resource

  def index
    @activation_letter_files = ActivationLetterFile.page(params[:page]).per(params[:per_page])
    @configurations = {}
    begin
      cron_chars = find_letters_job['cron'].split(' ')
    rescue DigidUtils::Iapi::Error => e
      Rails.logger.error("Failed to get handle_letters_job info from scheduler: '#{e.message}'")
    end
    @configurations['tijdstip_genereren_briefbestand'] = "#{cron_chars[2].rjust(2, "0")}:#{cron_chars[1].rjust(2, "0")}" if cron_chars
    instrument_logger('uc22.inzien_post_gelukt', manager_id: session[:current_user_id])
  end

  def download_xml
    letter = ActivationLetterFile.find(params[:id])
    xml = Saml::Encoding.decode_gzip Saml::Encoding.decode_64(letter.xml_content)
    instrument_logger('uc22.downloaden_briefbestand_gelukt', manager_id: session[:current_user_id])
    send_data xml, filename: letter.filename, disposition: 'attachment', type: :xml
  end

  def download_csv
    letter = ActivationLetterFile.find(params[:id])
    csv = letter.csv_content
    instrument_logger('376')
    send_data csv, filename: letter.filename_csv, disposition: 'attachment', type: 'text/csv; charset=iso-8859-1; header=present'
  end

  def download_processed
    letter = ActivationLetterFile.find(params[:id])
    xml = letter.processed_xml
    send_data xml, filename: letter.processed_file, disposition: 'attachment', type: :xml
  end


  def reupload_letter
    iapi_x_client.post("/iapi/reupload_letter", id: params[:id])
    flash[:notice] = "Het briefbestand is opnieuw geupload."
  rescue DigidUtils::Iapi::Error => e
    Rails.logger.error("Failed to send reupload_letter request: '#{e.message}'")
    flash[:notice] = "Het is niet gelukt om het briefstand opnieuw te uploaden."
  ensure
    redirect_to(activation_letter_files_path)
  end

  def preferences
    return unless params[:tijdstip_genereren_briefbestand].present?

    update_letters_cronjob(params[:tijdstip_genereren_briefbestand])

    flash[:notice] = I18n.t('letters.forms.messages.success')
    instrument_logger('uc22.post_wijzigen_gelukt', manager_id: session[:current_user_id])
    redirect_to activation_letter_files_path
  end

  private

  def update_letters_cronjob(time_to_run)
    letters_job = find_letters_job
    times = time_to_run.split(":")
    letters_job['cron'] = "0 #{times[1].to_i} #{times[0].to_i} * * *"
    DigidUtils::Iapi::Client.new(url: APP_CONFIG["urls"]["internal"]["scheduler"], timeout: 30).patch("/iapi/tasks/#{letters_job['id']}", body: letters_job.to_json, header: {"Content-Type" => "application/json"})
  end

  def find_letters_job
    letter_job_response = DigidUtils::Iapi::Client.new(url: APP_CONFIG["urls"]["internal"]["scheduler"], timeout: 30).get('/iapi/tasks/find?name=handle_letters_job')
    letters_job = JSON[letter_job_response.body]
  end

  def iapi_x_client
    @iapi_x_client ||= DigidUtils::Iapi::Client.new(url: APP_CONFIG["urls"]["internal"]["x"], timeout: 15)
  end
end
