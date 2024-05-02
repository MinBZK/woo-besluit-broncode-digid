
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

class ApplicationMailer < ActionMailer::Base
  before_action :add_inline_attachments
  layout 'mailer'

  def i18n_mail(options)
    locale = Account.find(options[:account_id]).try(:locale)

    I18n.with_locale(locale) do
      options[:subject] = i18n_subject(options)
      mail(options)
    end
  end

  def i18n_subject(options)
    subject_options = options[:subject_options].to_h.map {|k,v| [k, I18n.t(v)] }.to_h
    I18n.t(options[:subject], subject_options)
  end

  private
    def add_inline_attachments
      attachments.inline["rijkslogo_op_wit-compressed.png"] = File.read('public/rijkslogo_op_wit-compressed.png')
      attachments.inline["digid.png"] = File.read('public/digid.png')
      attachments.inline["rijksblauw_op_wit-compressed.png"] = File.read('public/rijksblauw_op_wit-compressed.png')
    end
end
