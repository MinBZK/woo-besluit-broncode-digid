
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

class FourEyesReportsController < ApplicationController
  include FilterConcern

  authorize_resource

  def index
    generate_search_results

    instrument_logger('1215', manager_id: session[:current_user_id])
  end

  def search
    unless params[:q].present?
      redirect_to four_eyes_report_url
      return
    end

    generate_search_results
    render action: :index
  end

  def export
    generate_search_results

    from_date = parse_time_filter('changed_at_gt') ? I18n.l(parse_time_filter('changed_at_gt')) : ""

    instrument_logger('1216', filter_query: params[:query], from: from_date, to: I18n.l(parse_time_filter('changed_at_lt') || Time.zone.now, format: :default) , manager_id: session[:current_user_id])
    send_data(@results.to_csv, filename: "export_four_eyes_report.csv", disposition: 'attachment', type: 'text/csv; charset=utf8; header=present')
  end

  private

  def filter_on_time_range
    return if params[:q].blank? || params[:q]['changed_at_gt(1i)'].blank? || params[:q]['changed_at_lt(1i)'].blank?

    from = parse_time_filter('changed_at_gt')
    to = parse_time_filter('changed_at_lt')

    if from && to.nil? # alleen Van ingevuld, OK
      @search.changed_at_gt = from
    elsif from && to && from < to # beide geldig, OK
      @search.changed_at_gt = from
      @search.changed_at_lt = to
    end
  end

  def generate_search_results
    found_ids = FourEyesReport.ransack(merged_search_params).result.ids
    found_ids += FourEyesReport.ransack(creator_manager_account_name_cont: params[:query]).result.ids
    found_ids += FourEyesReport.ransack(acceptor_manager_account_name_cont: params[:query]).result.ids
    @search = FourEyesReport.where(id: found_ids).ransack()

    filter_on_time_range
    @results = @search.result.page(params[:page]).per(per_page).includes(:manager, :creator_manager, :acceptor_manager)
  end

  def merged_search_params
    search_params = params.has_key?(:q) ? params[:q].to_hash.symbolize_keys : {}
    # Delete time filter, we set this manually later on
    search_params = search_params.except(
      'changed_at_gt(1i)',
      'changed_at_gt(2i)',
      'changed_at_gt(3i)',
      'changed_at_gt(4i)',
      'changed_at_gt(5i)',
      'changed_at_lt(1i)',
      'changed_at_lt(2i)',
      'changed_at_lt(3i)',
      'changed_at_lt(4i)',
      'changed_at_lt(5i)'
    )

    # Een leeg filtercriterium of `_` geeft alle resultaten (voor de ingestelde of default van/tot periode)
    query = params[:query].blank? ? nil : params[:query].to_s
    search_params.merge(g: [ { description_or_manager_account_name_cont: query }])
  end
end
