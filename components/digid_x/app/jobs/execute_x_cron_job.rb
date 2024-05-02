
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

class ExecuteXCronJob
    include Sidekiq::Worker
    sidekiq_options retry: false, queue: "x-cron"

    def perform(task_name)

        case task_name
        when "handle_letters_job"
          CreateLettersFileJob.new.perform
          UploadLettersFileJob.new.perform
          DownloadLettersFileJob.new.perform
          DestroyOldLettersJob.new.perform
        when "clean_up_expired_deceased_accounts"
          AccountDeceasedJob.new.perform
        when "clean_up_expired_activations"
          Cronjob.new.clean_up_expired_activations
        when "clean_up_inactive_app_authenticators"
          Cronjob.new.clean_up_inactive_app_authenticators
        when "clean_up_app_authenticators_with_expired_activationcode"
          Cronjob.new.clean_up_app_authenticators_with_expired_activationcode
        when "clean_up_sms_tools_with_expired_activationcode"
          Cronjob.new.clean_up_sms_tools_with_expired_activationcode
        when "clean_up_aselect"
          Cronjob.new.clean_up_aselect
        when "clean_up_saml"
          Cronjob.new.clean_up_saml
        when "clean_up_initial_accounts"
          Cronjob.new.clean_up_initial_accounts
        when "clean_up_email_deliveries"
          Cronjob.new.clean_up_email_deliveries
        when "send_activation_reminders"
          Account.send_activation_reminders
        when "send_expiry_notifications"
          Account.send_expiry_notifications
        when "expire_accounts"
          Account.expire_accounts(::Configuration.get_string('account_expire_removal_batch_size'))
        when "clean_up_expired_accounts"
          Account.clean_up_expired_accounts(::Configuration.get_string('account_expire_removal_batch_size'))
        end

    end

end

