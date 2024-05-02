
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

class FourEyesReviewMailer < ActionMailer::Base
  default from: APP_CONFIG['mailer_default_email_address'] || "noreply@#{APP_CONFIG['hosts']['digid']}"

  #EB001_notificatiemail_wijziging_geaccordeerd
  #EB002_notificatiemail_wijziging_afgekeurd
  def notify_creator(review, reviewer_id, result: nil)
    @reviewer = Manager.find(reviewer_id)
    @subject_action = review[:subject_action]
    @action = review[:action]
    @model = review[:model]
    @result = I18n.t("four_eyes_review_mailer.#{result}")

    @name = review[:name]

    subject = default_i18n_subject(environment: Rails.env.capitalize, action: @subject_action, model: @model, name: @name, result: @result)

    @reviewer_account_name = @reviewer.account_name
    @reviewed_at = Time.zone.now

    @creator_account_name = review[:creator_account_name]
    @created_at = review[:created_at]
    mail(to: review[:manager_email], subject: subject, &:text)
  end
end
