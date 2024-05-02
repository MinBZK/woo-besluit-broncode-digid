
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

class EditVerificationForm
  ATTRIBUTES = %i[birthday citizen_service_number first_names id_established id_expires_at nationality id_number
    id_signaled surname suspected_fraud].freeze

  include ActiveModel::Model
  include Concerns::DelegationOnBlank

  attr_accessor(*ATTRIBUTES)
  attr_accessor :verification

  delegate_on_blank(*ATTRIBUTES, to: :verification)

  validates :id_established, presence: true
  validates :id_signaled, presence: true, unless: -> { Configuration.get_boolean("balie_aanvraag_voor_rni") }

  def id_expires_at_day
    id_expires_at.try(:day)
  end

  def id_expires_at_month
    id_expires_at.try(:month)
  end

  def id_expires_at_year
    id_expires_at.try(:year)
  end

  def submit
    return false unless valid?

    if Configuration.get_boolean("balie_aanvraag_voor_rni")
      verification.update(verification_attributes_rni)
    else
      verification.update(verification_attributes)
    end
  end

  def submit!
    verification.update!(verification_attributes)
  end

  private

  def verification_attributes
    {
      id_established: id_established,
      id_signaled: id_signaled,
      suspected_fraud: suspected_fraud
    }
  end

  def verification_attributes_rni
    {
      id_established: id_established
    }
  end
end
