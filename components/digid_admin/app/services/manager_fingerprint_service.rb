
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

class ManagerFingerprintService

  attr_accessor :updated_managers, :report_output

  def initialize()
    @updated_managers = 0
    @report_output = true
  end


  def update_fingerprints(dir)
    file_names = Dir.glob("#{dir}/*")
    file_names.each do |file_name|
      file = File.read file_name
      certificate = OpenSSL::X509::Certificate.new(file) rescue next
      print "Reading from file #{file_name}" if report_output

      dn = certificate.subject.to_s(OpenSSL::X509::Name::RFC2253)
      #fingerprint = OpenSSL::Digest::SHA1.new(certificate.to_der).to_s.scan(/../).map{ |s| s.upcase }.join(":")
      # puts "DN = #{dn}"
      # puts "Fingerprint = #{fingerprint}"

      managers = Manager.where(distinguished_name: dn)

      if managers.empty?
        puts " - \e[33mNo manager found on DN #{dn}\e[0m" if report_output
        next
      end

      if managers.size > 1
        puts " - \e[91mMultiple managers found in DN #{dn}, managers not updated\e[0m" if report_output
        next
      end

      manager = managers.first
      manager.certificate = certificate
      if manager.save
        puts " - \e[32mFingerprint updated for #{manager.name}\e[0m" if report_output
        @updated_managers += 1
      else
        puts "\e[31m#{manager.errors.full_messages.join(", ")}\e[0m"
      end
    end

    puts "\e[36mResult #{updated_managers} managers updated from #{file_names.size} certificates\e[0m" if report_output
  end
end
