
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

class SearchEngineController < ApplicationController
  def search # rubocop:disable Metrics/AbcSize
    @results = {}
    return unless params[:query].present?

    @results[:sectorcode]        = !params[:query].match(/\s/) ? Sectorcode.ransack(sectoraalnummer_eq: params[:query]).result.order(:id).page(params[:sectorcode_page]) : Sectorcode.none.page(params[:sectorcode_page])
    @results[:gba_block]         = !params[:query].match(/\s/) ? GbaBlock.ransack(blocked_data_eq: params[:query]).result.order(:id).page(params[:gba_block_page]) : GbaBlock.none.page(params[:gba_block_page])
    @results[:webservice]        = Webservice.ransack(name_or_description_start: params[:query]).result.order(:id).page(params[:webservice_page])
    @results[:front_desk]        = FrontDesk.ransack(params[:query])
    @results[:afmeldlijst]       = !params[:query].match(/\s/) ? current_afmeldlijst.subscribers.ransack(bsn_eq: params[:query]).result.order(:id).page(params[:afmeldlijst_page]) : current_afmeldlijst.subscribers.none.page(params[:afmeldlijst_page])
    @results[:email]             = Email.ransack(adres_eq: params[:query]).result.page(params[:email_page]).includes(:account)
    @results[:email_historic]    = AccountHistory.ransack(email_adres_eq: params[:query]).result.page(params[:email_historic_page]).includes(:account)
    @results[:username]          = PasswordTool.ransack(username_eq: params[:query]).result.page(params[:username_page]).includes(:account)
    @results[:username_historic] = AccountHistory.ransack(gebruikersnaam_eq: params[:query]).result.page(params[:gebruikersnaam_page]).includes(:account)

    mobiel_nummer = params[:query]
    mobiel_nummer = '+316' + params[:query].slice(2..-1) if params[:query].start_with?('06')
    mobiel_nummer = '+316' + params[:query].slice(5..-1) if params[:query].start_with?('00316')
    mobiel_nummer = mobiel_nummer.delete('-').delete(' ')

    instrument_logger("1463", query: params[:query], manager_id: session[:current_user_id])

    @results[:mobile] = Account.joins(:sms_tools).where("sms_tools.phone_number": mobiel_nummer, "sms_tools.status": SmsTool::Status::ACTIVE).page(params[:mobile_numbers_page]).per(10)
    @results[:mobile_historic] = AccountHistory.ransack(mobiel_nummer_eq: mobiel_nummer).result.page(params[:mobile_historic_page]).includes(:account)
  end
end
