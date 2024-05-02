
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

class SessionInspect
  def initialize(app)
    @app = app
  end

  def call(env)
    status, headers, response = @app.call(env)
    if response.is_a?(ActionDispatch::Response::RackBody) && headers["Content-Type"] =~ %r{text\/html}
      response.instance_eval do
        insert_html = "<strong>Browser\n</strong>#{SessionInspect.browser_session(@response).to_yaml}"
        if (app = SessionInspect.app_session(@response))
          insert_html += "\n\n<strong>App\n</strong>#{app.to_yaml}"
        end
        @response.body = @response.body.gsub("</body>", "<pre class='dev-info-block'>#{insert_html}</pre></body>")
      end
    end
    [status, headers, response]
  end

  def self.browser_session(response)
    # session.to_yaml gives argument errors in some cases
    JSON.parse(response.request.session.to_json)
  rescue
    {}
  end

  def self.app_session(response)
    id = response.request.session[:app_session_id]
    id && $redis.hgetall("digid_x:app:session:#{id}")
  rescue
    nil
  end
end
