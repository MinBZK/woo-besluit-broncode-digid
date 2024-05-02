
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

class Button
  module Position
    LEFT = %i[cancel cancel_back_to_my_digid cancel_back_to_home skip skip_verification
              switch_to_buitenland_aanvraag_proces nope not_now previous resend_text_sms
              continue_without_phone_number no_continue back skip_eha].freeze
    RIGHT = %i[activate annie_are_you_okay change change continue continue_activating
               continue_request control_sms_request deactivate delete login_with_app log_in next
               ok_fill_in request request_again resend_spoken_sms resend_test_sms
               revoke revoke_wid save save_password save_password_next to_my_details yeah
               yes_continue_revocation ok_without_phone_number download_digid_app
               download_the_app open_the_app download_the_app_id_check not_now_continue_log_in
               more_info email_add_reminder_accept email_add_reminder_refuse email_add_skip_warning_no email_add_skip_warning_yes apply].freeze
  end

  VALIDATE = :right
  PRIMARY = :right

  attr_accessor :action, :arrow, :inverse

  def self.pos(action, inverse = false)
    if Position::LEFT.include?(action.to_sym) ^ inverse
      :left
    elsif Position::RIGHT.include?(action.to_sym) ^ inverse
      :right
    else
      :middle
    end
  end

  def initialize(options)
    @action = options[:action]
    @arrow = options[:arrow]
    @inverted_arrow = options[:inverted_arrow]
    @inverse = options[:inverse]
  end

  def pos(inverse = true)
    self.class.pos(@action, inverse && @inverse)
  end

  def validate?
    pos(false) == VALIDATE
  end

  def primary?
    pos(false) == PRIMARY
  end

  def inverted_arrow?
    @inverted_arrow
  end

  def arrow?
    @arrow || [:back, :previous, :cancel, :cancel_back_to_home, :cancel_back_to_my_digid, :next].include?(@action)
  end

  def css_class(type = "button")
    klass = "actions__#{pos}--#{type}"
    klass +=  " primary--button" if primary?
    klass += " arrow" if arrow? # arrow is not supported for input elements
    klass += " inverted_arrow" if inverted_arrow? # arrow is not supported for input elements
    klass
  end

  def attributes
    {
      pos: pos,
      css_class: css_class,
      primary: primary?
    }
  end
end
