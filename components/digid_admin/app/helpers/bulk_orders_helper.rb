
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

module BulkOrdersHelper
  def localize(*args)
    # Avoid I18n::ArgumentError for nil values
    I18n.localize(*args) unless args.first.nil?
  end

  def render_bulk_order_errors
    # remove internal validation errors
    @bulk_order.errors.delete(:bsn_list)
    @bulk_order.errors.delete(:status)
    render 'shared/errors', model: @bulk_order
  end

  def download_invalid_bsn_list_button
    return unless @bulk_order.invalid_status?
    url = download_invalid_bsn_bulk_order_url(@bulk_order)
    link_to(I18n.t('bulk_order.download_invalid_bsn_list'), url, class: 'file_button', download: '')
  end

  def download_account_status_csv_button
    return if @bulk_order.invalid_status?
    label = I18n.t('bulk_order.download_account_status')
    url = download_account_status_bulk_order_url(@bulk_order)
    link_to(label, url, class: 'file_button', download: '')
  end

  def download_address_list_button
    return unless @bulk_order.allow_address_list_download?
    label = I18n.t('bulk_order.download_address_list')
    url = download_address_list_bulk_order_url(@bulk_order)
    link_to(label, url, class: 'file_button', download: '')
  end
end
