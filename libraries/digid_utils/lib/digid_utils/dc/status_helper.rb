
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

module DigidUtils
  module Dc
    module StatusHelper
      def render_status(object)
        if object.respond_to?(:status)
          capture do
            concat field t("services.active"), t(object.status&.active)
            concat field t("services.date_from"), render_status_from(object)
            concat field t("services.date_until"), render_status_until(object)
          end
        else
          raw = t(active?(object) ? "services.active" : "services.inactive")
        end
      end

      def highlight(text, setting)
        setting ? "<span class='marker_yellow'>#{text}</span>" : text
      end

      def grayed_out(text, setting)
        setting ? "<span class='marker_gray'>#{text}</span>" : text
      end

      def render_status_active(object)
        raw active?(object) ? t("services._yes") : t("services._no")
      end

      def render_status_from(object, text=nil)
        if object.active_from
          setting = (DateTime.now < object.active_from.to_date) && object.active
          raw highlight("#{text} #{I18n.l(object.active_from.to_date, format: :default)}", setting)
        else
          raw text || "-"
        end
      end

      def render_status_until(object, text=nil)
        if object.active_until
          setting = (DateTime.now >= object.active_until.to_date) && object.active
          raw highlight("#{text} #{I18n.l(object.active_until.to_date, format: :default)}", setting)
        else
          raw text || "-"
        end
      end

      def render_from(date, active=true, text=nil)
        if date.present?
          setting = (DateTime.now < date) && active
          highlight("#{text} #{I18n.l(date.to_date, format: :default)}", setting)
        else
          text || "-"
        end
      end

      def render_until(date, active=true, text=nil)
        if date.present?
          setting = (DateTime.now >= date) && active
          highlight("#{text} #{I18n.l(date.to_date, format: :default)}", setting)
        else
          text || "-"
        end
      end

      def active?(object)
        object.try(:active?) || object.try(:active)
      end

      def render_organization_role(object)
        html = ""
        if active?(object)
          if object.active_from
            html << " " unless html.blank?
            html << "#{render_status_from(object, t("services.date_from").downcase)}"
          end
          if object.active_until
            html << " " unless html.blank?
            html << "#{render_status_until(object, t("services.date_until").downcase)}"
          end
        else
          html << "#{grayed_out(t("services.inactive"), true)}"
        end
        html = " (#{html})" unless html.blank?
        html = "#{t("organization_role.#{object.type}")}#{html}"
        raw html
      end


      def render_organization_role_type(object)
        too_soon = object.active_from.present? && DateTime.now < object.active_from.to_date
        too_late = object.active_until.present? && DateTime.now >= object.active_until.to_date
        setting = too_soon || too_late || !object.active
        raw grayed_out("#{t("organization_role.#{object.type}")}", setting)
      end
    end
  end
end
