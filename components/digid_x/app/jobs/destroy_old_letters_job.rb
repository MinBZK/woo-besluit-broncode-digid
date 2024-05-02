
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

class DestroyOldLettersJob
  def perform
    # files are deleted immediately (xml ftp'd to Rotaform & csv's from Rotaform)
    # database content is retained 42 days (ActivationLetter & ActivationLetterFile)

    ActivationLetter.where("created_at < ?", 42.days.ago).destroy_all
    ActivationLetterFile.where("created_at < ?", 42.days.ago).destroy_all

    FileUtils.rm(Dir.glob(APP_CONFIG["letter_local_path"]+"/letters/*"))
    FileUtils.rm(Dir.glob(APP_CONFIG["letter_local_path"]+"/csv/C-*"))

    # verwijder registraties waarvan
    # 0. status is NULL en sessie is verlopen of,
    Cronjob.new.clean_up_null_registrations
    # 1. status is initieel en sessie is verlopen of,
    Cronjob.new.clean_up_initial_registrations
    # 2. status is aangevraagd en langer dan 6 weken,
    Cronjob.new.clean_up_aangevraagd_registrations
    # 3. status is afgebroken.
    Cronjob.new.clean_up_afgebroken_registrations
    # 5. status is completed.
    Cronjob.new.clean_up_completed_registrations

    Cronjob.new.clean_up_web_registrations
  end

end
