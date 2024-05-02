
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

describe DigidUtils::Property do
  let!(:property_data_1) {["gba_ssl_cert_key_file", "key_data"]}
  let!(:property_data_2) {["gba_ssl_cert_file", "./tmp/existing_file_path"]}
  let!(:property_data_3) {["gba_ssl_cert_file", "certificate_data"]}
  let!(:file) { File.open("./tmp/existing_file_path", "w+") {|f| f.write("content_from_file")}}

  let(:property_1) { described_class.new(*property_data_1) }
  let(:property_2) { described_class.new(*property_data_2) }
  let(:property_3) { described_class.new(*property_data_3) }

  describe "initialize" do
    it "should set the attributes correctly for keys" do
      expect(property_1.key).to eq("gba_ssl_cert_key_file")
      expect(property_1.value).to eq("key_data")
      expect(property_1.path).to eq("./tmp/tmp_gba_ssl_cert_key_file.key")
    end

    it "should set the attributes correctly for certs" do
      expect(property_3.key).to eq("gba_ssl_cert_file")
      expect(property_3.value).to eq("certificate_data")
      expect(property_3.path).to eq("./tmp/tmp_gba_ssl_cert_file.crt")
    end

    it "should create a file if it doesn't exists" do
      expect(File.exist?(property_1.path)).to eq(true)
    end

    it "should use name as path if there is already a file" do
      expect(property_2.path).to eq("./tmp/existing_file_path")
    end
  end

  describe "read" do
    it "should read the content from file" do
      expect(property_2.read).to eq("content_from_file")
    end

    it "should read the content from value" do
      expect(property_1.read).to eq("key_data")
    end
  end

  after do
    File.delete(*Dir.glob("./tmp/*"))
  end
end
