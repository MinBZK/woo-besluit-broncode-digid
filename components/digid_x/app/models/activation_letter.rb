
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

# model for letters
class ActivationLetter < ActiveRecord::Base
  module Status
    CREATED   = "created"
    FINISHED  = "finished" # ready to send
    SENT      = "sent" # sent to printer
  end

  include Stateful

  # possible values of attribute letter_type
  module LetterType
    CREATE                                      = "init"                                         ## Briefcodes
    UITBREIDING_APP                             = "uitbreiding_app"                              # "006"
    UITBREIDING                                 = "uitbreiding_sms"                              # "004",
    RECOVER_PWD                                 = "recovery_password"                            # "008",
    RECOVER_SMS                                 = "recovery_sms"                                 # "009"
    AANVRAAG_DEBLOKKERINGSCODE_RIJBEWIJS        = "aanvraag_deblokkeringscode_rijbewijs"         # "011"
    ACTIVATION_APP_ONE_DEVICE                   = "activation_app_one_device"                    # "012"
    BALIE                                       = "balie_aanvraag"                               # "014"
    AANVRAAG_DEBLOKKERINGSCODE_IDENTITEITSKAART = "aanvraag_deblokkeringscode_identiteitskaart"  # "015"
    AANVRAAG_ACCOUNT_ACTIVATIE_VIA_APP          = "activeringscode_aanvraag_via_digid_app"       # "018"
    AANVRAAG_WITH_SMS                           = "activation_aanvraag_met_sms"                  # "019"
    APP_NOTIFICATION_LETTER                     = "app_notification_letter"                      # "020"
  end

  belongs_to :registration
  before_save :set_postcode_from_gba

  alias_attribute :letter_sent_at, :created_at

  def self.mark_as_sent(ids)
    ids.each do |id|
      letter = ActivationLetter.find(id)
      letter.update_attribute(:status, Status::SENT)
      Log.instrument("356", registration_id: letter.registration_id)
    end
  end

  def gba
    @gba ||= GbaWebservice::Data.new(JSON.parse(self[:gba])) if self[:gba].present?
  end

  def locale
    @locale ||= Account.with_bsn(registration.try(:burgerservicenummer)).last.try(:locale) || "nl"
  end

  private

  def set_postcode_from_gba
    self.postcode = gba.postcode if gba.present?
  end
end
