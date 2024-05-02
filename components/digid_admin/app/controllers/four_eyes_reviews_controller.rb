
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

class FourEyesReviewsController < ApplicationController
  respond_to :html

  def index
    @webservices_in_review = FourEyesReview.where(record_table: "Webservice")
    @app_versions_in_review = FourEyesReview.where(record_table: "AppVersion")
    @managers_in_review = FourEyesReview.where(record_table: "Manager")
    @roles_in_review = FourEyesReview.where(record_table: "Role")
    @kiosks_in_review = FourEyesReview.where(record_table: "Kiosk")
    @whitelisted_phone_numbers_in_review = FourEyesReview.where(record_table: "WhitelistedPhoneNumber")

    @dc_organizations_review = FourEyesReview.where(record_table: "Dc::Organization")
    @dc_connections_review = FourEyesReview.where(record_table: "Dc::Connection")
    @dc_services_review = FourEyesReview.where(record_table: "Dc::Service")

    instrument_logger("four_eyes_review.inzien")
  end
end
