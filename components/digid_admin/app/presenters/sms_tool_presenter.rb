
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

class SmsToolPresenter < BasePresenter
  presents :sms_tool

  def active?
    @object.active?
  end

  def pending?
    @object.pending? && !account.blocked?
  end

  def status
    status = I18n.t("accounts.sms_tool.status.#{@object.status}")
    return status unless account.blocked?
    [status, '/', blocked_tag(I18n.t('accounts.presenter.blocked'))].join(' ')
  end

  def attempts
    return unless account.status.eql?(Account::Status::ACTIVE)
    account.login_pogingen
  end

  def blocked_till
    blocked_tag(format_date(account.blocked_till)) if account.blocked?
  end

  def account
    @object.account
  end

  def issuer_type
    I18n.t("issuer_type.#{@object.issuer_type}") if @object.issuer_type.present? && active?
  end

  def last_usage
    time_helper(@object.account.sms_last_date_attempt(sms_tool), :slash, false) if @object.account.active_sms_tool.present?
  end

  def created_at
    time_helper(@object.created_at, :slash)
  end

  def activated_at
    time_helper(@object.account.active_sms_tool&.activated_at, :slash)
  end

  def spoken_sms
    @object.gesproken_sms ? t('accounts.fields.labels.set') : t('accounts.fields.labels.not_set')
  end

  def colored_status
    html = capture do
      if @object.status == SmsTool::Status::ACTIVE
        strong green @object.human_status
      elsif @object.status == SmsTool::Status::INACTIVE
        strong red @object.human_status
      else
        strong @object.human_status
      end
    end

    if @object.account.blocked?
      html << " / "
      html << capture do
          content_tag :strong do
            content_tag :span, class: :red do
            content_tag :span, class: :content_blocked do 
              "Geblokkeerd"
            end
          end
        end
      end
    end
    html
  end

  private

  def blocked_tag(content)
    "<span class='content_blocked'>#{content}</span>"
  end
end
