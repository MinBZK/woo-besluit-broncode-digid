
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

class AccountPresenter < BasePresenter
  presents :account

  def status
    status = I18n.t("accounts.presenter.states.#{@object.status}")
    return status unless @object.blocked?
    [status, '/', blocked_tag(I18n.t('accounts.presenter.blocked'))].join(' ')
  end

  def colored_status
    if @object.status == Account::Status::ACTIVE
      strong green @object.human_status
    elsif @object.status == Account::Status::SUSPENDED
      strong red @object.human_status
    else
      strong @object.human_status
    end
  end

  def colored_reason_suspension color = :red
    if ["O", "F"].include? @object.reason_suspension
      if color == :no_color
        @object.reason_suspension
      else
        strong(send(color, @object.reason_suspension))
      end
    else
      "Niet opgeschort"
    end
  end

  def active?
    @object.status.eql?(Account::Status::ACTIVE) && !@object.blocked?
  end

  def attempts
    @object.login_pogingen
  end

  def blocked_till
    blocked_tag(format_date(@object.blocked_till)) if @object.blocked?
  end

  def last_login
    return if (@object.current_sign_in_at.nil? && @object.last_sign_in_at.nil?) || @object.initial?
    format_date(@object.current_sign_in_at || @object.last_sign_in_at)
  end

  def issuer_type
    I18n.t("issuer_type.#{@object.password_tool&.issuer_type}") if @object.password_tool&.issuer_type.present? && active?
  end

  private

  def blocked_tag(content)
    "<span class='content_blocked'>#{content}</span>"
  end
end
