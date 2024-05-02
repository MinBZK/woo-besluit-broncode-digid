
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

class NewsItemsController < ApplicationController
  respond_to :html
  before_action :update_params, only: [:create, :update]
  load_and_authorize_resource

  # GET /news_items
  def index
    @active_news_items = NewsItem.active.all
    instrument_logger('uc11.nieuws_inzien_gelukt', manager_id: session[:current_user_id])
    respond_with @news_item
  end

  # GET /news_items/1
  def show
    instrument_logger('366', news_item: @news_item.name_nl, manager_id: session[:current_user_id])
  end

  # GET /news_items/new
  def new
  end

  # GET /news_items/1/edit
  def edit
  end

  # POST /news_items
  def create
    if @news_item.save
      instrument_logger('uc11.nieuws_aanmaken_gelukt', news_item_id: @news_item.id)
      redirect_to @news_item, notice: create_success(@news_item)
    else
      render :new
    end
  end

  # PUT /news_items/1
  def update
    if @news_item.update(params[:news_item])
      instrument_logger('uc11.nieuws_wijzigen_gelukt', news_item_id: @news_item.id)
      redirect_to @news_item, notice: update_success(@news_item)
    else
      render :edit
    end
  end

  # DELETE /news_items/1
  def destroy
    @news_item.destroy
    instrument_logger('uc11.nieuws_verwijderen_gelukt', news_item_id: @news_item.id)
    redirect_to(news_items_path)
  end

  private

  def update_params
    if params[:news_item]
      params[:news_item][:os] = join_and_filter(params[:news_item][:os])
      params[:news_item][:browser] = join_and_filter(params[:news_item][:browser])
    end
  end

  def join_and_filter(array)
    array.to_a.map(&:values).filter {|i| i.first.present? }.map{|i| i.join(":")}.join(";").presence
  end
end
