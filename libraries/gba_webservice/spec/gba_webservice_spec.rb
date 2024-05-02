
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

require File.expand_path(File.dirname(__FILE__) + '/spec_helper')
require 'gba_webservice'

describe GbaWebservice, vcr: { cassette_name: 'GbaWebservice' } do
  describe '.get_gba_data' do
    before { WebMock.stub_request(:post, 'gba.nl').to_return body: File.read('spec/fixtures/alles.xml') }
    let(:gba_data) { described_class.get_gba_data 'SSSSSSSSSSSSSS', 10_120 => 392_706_052 }
    let(:request) { GbaWebservice.new(wsdl_endpoint: 'SSSSSSSSSSSSSS', search: {10_120 => 392_706_052}).send(:build_request) }

    it 'builds the right request' do
      expect(request.headers["SOAPAction"]).to be_nil
      expect(request.body).to eq("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ver=\"http://www.bprbzk.nl/GBA/LRDPlus/version1.1\"><soapenv:Header/><soapenv:Body><ver:vraag><ver:in0><ver:indicatieAdresvraag>0</ver:indicatieAdresvraag><ver:indicatieZoekenInHistorie>0</ver:indicatieZoekenInHistorie><ver:masker><ver:item>010110</ver:item><ver:item>010120</ver:item><ver:item>010210</ver:item><ver:item>010220</ver:item><ver:item>010230</ver:item><ver:item>010240</ver:item><ver:item>010310</ver:item><ver:item>010410</ver:item><ver:item>016110</ver:item><ver:item>040510</ver:item><ver:item>050230</ver:item><ver:item>050240</ver:item><ver:item>050610</ver:item><ver:item>050710</ver:item><ver:item>077010</ver:item><ver:item>080910</ver:item><ver:item>081110</ver:item><ver:item>081120</ver:item><ver:item>081130</ver:item><ver:item>081140</ver:item><ver:item>081150</ver:item><ver:item>081160</ver:item><ver:item>081170</ver:item><ver:item>081210</ver:item><ver:item>123510</ver:item><ver:item>123520</ver:item><ver:item>123550</ver:item><ver:item>123560</ver:item><ver:item>123570</ver:item></ver:masker><ver:parameters><ver:item><ver:rubrieknummer>10120</ver:rubrieknummer><ver:zoekwaarde>392706052</ver:zoekwaarde></ver:item></ver:parameters></ver:in0></ver:vraag></soapenv:Body></soapenv:Envelope>")
    end

    it 'returns a GbaData instance with methods for all fields' do
      expect(gba_data.a_nummer).not_to be_empty
      expect(gba_data.bsn).not_to be_empty
      expect(gba_data.voornamen).not_to be_empty
      expect(gba_data.adellijke_titel).to be_nil
      expect(gba_data.voorvoegsel_geslachtsnaam).to be_nil
      expect(gba_data.geslachtsnaam).not_to be_empty
      expect(gba_data.geboortedatum).not_to be_empty
      expect(gba_data.geslachtsaanduiding).not_to be_empty
      expect(gba_data.aanduiding_naamgebruik).not_to be_empty
      expect(gba_data.nationaliteiten).not_to be_empty
      expect(gba_data.voorvoegsel_geslachtsnaam_partner).to be_nil
      expect(gba_data.geslachtsnaam_partner).to be_nil
      expect(gba_data.datum_huwelijk).to be_nil
      expect(gba_data.datum_ontbinding_huwelijk).to be_nil
      expect(gba_data.indicatie_geheim).not_to be_empty
      expect(gba_data.gemeente_van_inschrijving).not_to be_empty
      expect(gba_data.straatnaam).not_to be_empty
      expect(gba_data.huisnummer).not_to be_empty
      expect(gba_data.huisletter).to be_nil
      expect(gba_data.huisnummertoevoeging).to be_nil
      expect(gba_data.aanduiding_bij_huisnummer).to be_nil
      expect(gba_data.postcode).not_to be_empty
      expect(gba_data.woonplaats).to be_nil
      expect(gba_data.locatieomschrijving).to be_nil
      expect(gba_data.reisdocumenten).not_to be_empty
    end

    it 'returns a GbaData instance with a status' do
      expect(gba_data.status).not_to eq(nil)
    end

    it 'returns a GbaData instance with deprecated old hash style interface' do
      expect(gba_data['010120']).to eq('PPPPPPPPP')
    end

    it 'returns nil if an error occurs' do
      WebMock.stub_request(:post, 'gba.nl').to_return status: 401
      data = described_class.get_gba_data 'SSSSSSSSSSSSSS', 10_120 => 392_706_052
      expect(data).to eq(nil)
    end
  end

  describe 'partner name' do
    it 'should be empty if there are no mariages' do
      WebMock.stub_request(:post, 'gba.nl').to_return body: File.read('spec/fixtures/zonder_huwelijk.xml')
      data = described_class.get_gba_data 'SSSSSSSSSSSSSS', 10_120 => 123_460_037
      expect(data.voorvoegsel_geslachtsnaam_partner).to be_nil
      expect(data.geslachtsnaam_partner).to be_nil
    end

    it 'should return the current partner when active mariage available' do
      WebMock.stub_request(:post, 'gba.nl').to_return body: File.read('spec/fixtures/actief_huwelijk_geen_ontbonden.xml')
      data = described_class.get_gba_data 'SSSSSSSSSSSSSS', 10_120 => 123_460_037
      expect(data.voorvoegsel_geslachtsnaam_partner).to eq('de')
      expect(data.geslachtsnaam_partner).to eq('PPPP')
    end

    it 'should return the current partner when active mariage available with 1 divorce' do
      WebMock.stub_request(:post, 'gba.nl').to_return body: File.read('spec/fixtures/actief_huwelijk_1_ontbonden.xml')
      data = described_class.get_gba_data 'SSSSSSSSSSSSSS', 10_120 => 123_460_037
      expect(data.voorvoegsel_geslachtsnaam_partner).to eq('de')
      expect(data.geslachtsnaam_partner).to eq('PPPP')
    end

    it 'should return the current partner when active mariage available with multiple divorces' do
      WebMock.stub_request(:post, 'gba.nl').to_return body: File.read('spec/fixtures/actief_huwelijk_veel_ontbonden.xml')
      data = described_class.get_gba_data 'SSSSSSSSSSSSSS', 10_120 => 123_460_037
      expect(data.voorvoegsel_geslachtsnaam_partner).to eq('de')
      expect(data.geslachtsnaam_partner).to eq('PPPP')
    end

    it 'should return the last divorcee with 1 divorce present' do
      WebMock.stub_request(:post, 'gba.nl').to_return body: File.read("spec/fixtures/1_ontbonden.xml")
      data = described_class.get_gba_data 'SSSSSSSSSSSSSS', 10_120 => 123_460_037
      expect(data.voorvoegsel_geslachtsnaam_partner).to eq('van der')
      expect(data.geslachtsnaam_partner).to eq('PPP')
    end

    it 'should return the most recent divorcee with multiple divorces' do
      WebMock.stub_request(:post, 'gba.nl').to_return body: File.read('spec/fixtures/veel_ontbonden.xml')
      data = described_class.get_gba_data 'SSSSSSSSSSSSSS', 10_120 => 123_460_037
      expect(data.voorvoegsel_geslachtsnaam_partner).to eq('van der')
      expect(data.geslachtsnaam_partner).to eq('PPP')
    end

    it 'should return the most recent divorcee with multiple divorces different order' do
      WebMock.stub_request(:post, 'gba.nl').to_return body: File.read('spec/fixtures/veel_ontbonden_andere_volgorde.xml')
      data = described_class.get_gba_data 'SSSSSSSSSSSSSS', 10_120 => 123_460_037
      expect(data.voorvoegsel_geslachtsnaam_partner).to eq('van der')
      expect(data.geslachtsnaam_partner).to eq('PPP')
    end
  end

  describe GbaWebservice::Data do
    let(:data) { GbaWebservice::Data.new('010110' => 'PPPPPP') }
    let(:nested_data) { GbaWebservice::Data.new('010110' => 'PPPPPP', 'data' => [GbaWebservice::Data.new('040510' => 'PPPP')]) }

    it 'can be transformed to JSON' do
      expect(JSON.parse(data.to_json)).to eq('010110' => 'PPPPPP')
    end

    it 'can be transformed to JSON if nested' do
      expect(JSON.parse(nested_data.to_json)).to eq('010110' => 'PPPPPP', 'data' => [{'040510' => 'PPPP'}])
    end

    it 'can be transformed as JSON' do
      expect(JSON.parse(ActiveSupport::JSON.encode(data))).to eq('a_nummer' => 'PPPPPP')
    end

    it 'can be transformed as JSON if nested' do
      expect(JSON.parse(ActiveSupport::JSON.encode(nested_data))).to eq('a_nummer' => 'PPPPPP', 'data' => [{'nationaliteit' => 'PPPP'}])
    end

    it 'can be serialized' do
      serialized_data = YAML.dump(data)
      expect(serialized_data).to be_a(String)
      expect(YAML.load(serialized_data)).to eq(data)
    end

    it 'can be serialized if nested' do
      serialized_data = YAML.dump(nested_data)
      expect(serialized_data).to be_a(String)
      expect(YAML.load(serialized_data)).to eq(nested_data)
    end

    describe 'behaves like a hash for backwards compatibility' do
      it 'supports adding values' do
        data['foo'] = 'bar'
        expect(data['foo']).to eq('bar')
      end

      it 'supports has_key?' do
        expect(data.has_key?('foo')).to eq(false)
        data['foo'] = 'bar'
        expect(data.has_key?('foo')).to eq(true)
      end
    end
  end

  describe 'nationaliteit' do
    it 'should produce a nil when no nationalities are present' do
      WebMock.stub_request(:post, 'gba.nl').to_return body: File.read('spec/fixtures/nationaliteit_geen.xml')
      data = described_class.get_gba_data 'SSSSSSSSSSSSSS', 10_120 => 123_460_037
      expect(data.nationaliteiten).to be_empty
    end

    it 'should produce an array when one nationality is present' do
      WebMock.stub_request(:post, 'gba.nl').to_return body: File.read('spec/fixtures/nationaliteit_1.xml')
      data = described_class.get_gba_data 'SSSSSSSSSSSSSS', 10_120 => 123_460_037
      expect(data.nationaliteiten.size).to eq(1)

      expect(data.nationaliteiten[0].nationaliteit).to eq('PPPP')
    end

    it 'should produce an array when many nationalities are present' do
      WebMock.stub_request(:post, 'gba.nl').to_return body: File.read('spec/fixtures/nationaliteit_veel.xml')
      data = described_class.get_gba_data 'SSSSSSSSSSSSSS', 10_120 => 123_460_037
      expect(data.nationaliteiten.size).to eq(2)

      expect(data.nationaliteiten[0].nationaliteit).to eq('PPPP')
      expect(data.nationaliteiten[1].nationaliteit).to eq('PPPP')
    end
  end


  describe 'reisdocument' do
    it 'should produce a nil when no identity documents are present' do
      WebMock.stub_request(:post, 'gba.nl').to_return body: File.read('spec/fixtures/reisdocument_geen.xml')
      data = described_class.get_gba_data 'SSSSSSSSSSSSSS', 10_120 => 301_575_927
      expect(data.reisdocumenten).to be_empty
    end

    it 'should produce an array when one identity document is present' do
      WebMock.stub_request(:post, 'gba.nl').to_return body: File.read('spec/fixtures/reisdocument_1.xml')
      data = described_class.get_gba_data 'SSSSSSSSSSSSSS', 10_120 => 780_921_902
      expect(data.reisdocumenten.size).to eq(1)

      expect(data.reisdocumenten[0].soort_reisdocument).to eq('PP')
      expect(data.reisdocumenten[0].nummer_reisdocument).to eq('PPPPPPPPP')
      expect(data.reisdocumenten[0].datum_einde_geldigheid_reisdocument).to eq('PPPPPPPP')
      expect(data.reisdocumenten[0].datum_inhouding_vermissing_reisdocument).to be_nil
      expect(data.reisdocumenten[0].aanduiding_inhouding_vermissing_reisdocument).to be_nil
    end

    it 'should produce an array when many identity documents are present' do
      WebMock.stub_request(:post, 'gba.nl').to_return body: File.read('spec/fixtures/reisdocument_veel.xml')
      data = described_class.get_gba_data 'SSSSSSSSSSSSSS', 10_120 => 900_095_271
      expect(data.reisdocumenten.size).to eq(8)

      expect(data.reisdocumenten[0].soort_reisdocument).to eq('PP')
      expect(data.reisdocumenten[0].nummer_reisdocument).to eq('PPPPPPPPP')
      expect(data.reisdocumenten[0].datum_einde_geldigheid_reisdocument).to eq('PPPPPPPP')
      expect(data.reisdocumenten[0].datum_inhouding_vermissing_reisdocument).to eq('PPPPPPPP')
      expect(data.reisdocumenten[0].aanduiding_inhouding_vermissing_reisdocument).to eq('V')

      expect(data.reisdocumenten[1].soort_reisdocument).to eq('PP')
      expect(data.reisdocumenten[1].nummer_reisdocument).to eq('PPPPPPPPP')
      expect(data.reisdocumenten[1].datum_einde_geldigheid_reisdocument).to eq('PPPPPPPP')
      expect(data.reisdocumenten[1].datum_inhouding_vermissing_reisdocument).to be_nil
      expect(data.reisdocumenten[1].aanduiding_inhouding_vermissing_reisdocument).to be_nil

      expect(data.reisdocumenten[2].soort_reisdocument).to eq('PP')
      expect(data.reisdocumenten[2].nummer_reisdocument).to eq('PPPPPPPPP')
      expect(data.reisdocumenten[2].datum_einde_geldigheid_reisdocument).to eq('PPPPPPPP')
      expect(data.reisdocumenten[2].datum_inhouding_vermissing_reisdocument).to eq('PPPPPPPP')
      expect(data.reisdocumenten[2].aanduiding_inhouding_vermissing_reisdocument).to eq('V')

      expect(data.reisdocumenten[3].soort_reisdocument).to eq('PP')
      expect(data.reisdocumenten[3].nummer_reisdocument).to eq('PPPPPPPPP')
      expect(data.reisdocumenten[3].datum_einde_geldigheid_reisdocument).to eq('PPPPPPPP')
      expect(data.reisdocumenten[3].datum_inhouding_vermissing_reisdocument).to be_nil
      expect(data.reisdocumenten[3].aanduiding_inhouding_vermissing_reisdocument).to be_nil

      expect(data.reisdocumenten[4].soort_reisdocument).to eq('PP')
      expect(data.reisdocumenten[4].nummer_reisdocument).to eq('PPPPPPPPP')
      expect(data.reisdocumenten[4].datum_einde_geldigheid_reisdocument).to eq('PPPPPPPP')
      expect(data.reisdocumenten[4].datum_inhouding_vermissing_reisdocument).to be_nil
      expect(data.reisdocumenten[4].aanduiding_inhouding_vermissing_reisdocument).to be_nil

      expect(data.reisdocumenten[5].soort_reisdocument).to eq('PP')
      expect(data.reisdocumenten[5].nummer_reisdocument).to eq('PPPPPPPPP')
      expect(data.reisdocumenten[5].datum_einde_geldigheid_reisdocument).to eq('PPPPPPPP')
      expect(data.reisdocumenten[5].datum_inhouding_vermissing_reisdocument).to be_nil
      expect(data.reisdocumenten[5].aanduiding_inhouding_vermissing_reisdocument).to be_nil

      expect(data.reisdocumenten[6].soort_reisdocument).to eq('PP')
      expect(data.reisdocumenten[6].nummer_reisdocument).to eq('PPPPPPPPP')
      expect(data.reisdocumenten[6].datum_einde_geldigheid_reisdocument).to eq('PPPPPPPP')
      expect(data.reisdocumenten[6].datum_inhouding_vermissing_reisdocument).to be_nil
      expect(data.reisdocumenten[6].aanduiding_inhouding_vermissing_reisdocument).to be_nil

      expect(data.reisdocumenten[7].soort_reisdocument).to eq('PP')
      expect(data.reisdocumenten[7].nummer_reisdocument).to eq('PPPPPPPPP')
      expect(data.reisdocumenten[7].datum_einde_geldigheid_reisdocument).to eq('PPPPPPPP')
      expect(data.reisdocumenten[7].datum_inhouding_vermissing_reisdocument).to be_nil
      expect(data.reisdocumenten[7].aanduiding_inhouding_vermissing_reisdocument).to be_nil
    end
  end

  describe 'enrich' do
    describe 'onderzoek_algemeen?' do
      it 'should return true if 018310 is set and 018330 not' do
        data = GbaWebservice::Data.new('018310' => '010310', 'nationaliteiten' => [], 'reisdocumenten' => [])
        expect(GbaWebservice.enrich(data).onderzoek_algemeen?).to be(true)
      end

      it 'should return false if 018310 and 018330 are set' do
        data = GbaWebservice::Data.new('018310' => '010310', '018330' => '20170330', 'nationaliteiten' => [], 'reisdocumenten' => [])
        expect(GbaWebservice.enrich(data).onderzoek_algemeen?).to be(false)
      end

      it 'should return false if 018310 and 018330 are not set' do
        data = GbaWebservice::Data.new('nationaliteiten' => [], 'reisdocumenten' => [])
        expect(GbaWebservice.enrich(data).onderzoek_algemeen?).to be(false)
      end
    end

    describe 'onderzoek_geboortedatum?' do
      it 'should return true if 018310 is set to 010310 and 018330 not' do
        data = GbaWebservice::Data.new('018310' => '010310', 'nationaliteiten' => [], 'reisdocumenten' => [])
        expect(GbaWebservice.enrich(data).onderzoek_geboortedatum?).to be(true)
      end

      it 'should return true if 018310 is set to 010300 and 018330 not' do
        data = GbaWebservice::Data.new('018310' => '010300', 'nationaliteiten' => [], 'reisdocumenten' => [])
        expect(GbaWebservice.enrich(data).onderzoek_geboortedatum?).to be(true)
      end

      it 'should return false if 018310 is not 010310 nor 010300 and 018330 not set' do
        data = GbaWebservice::Data.new('018310' => '010410', 'nationaliteiten' => [], 'reisdocumenten' => [])
        expect(GbaWebservice.enrich(data).onderzoek_geboortedatum?).to be(false)
      end

      it 'should return false if 018310 and 018330 are set' do
        data = GbaWebservice::Data.new('018310' => '081110', '018330' => '20170330', 'nationaliteiten' => [], 'reisdocumenten' => [])
        expect(GbaWebservice.enrich(data).onderzoek_geboortedatum?).to be(false)
      end

      it 'should return false if 018310 and 083030 are not set' do
        data = GbaWebservice::Data.new('nationaliteiten' => [], 'reisdocumenten' => [])
        expect(GbaWebservice.enrich(data).onderzoek_geboortedatum?).to be(false)
      end
    end

    describe 'onderzoek_adres?' do
      it 'should return true if 088310 is set and 088330 not' do
        data = GbaWebservice::Data.new('088310' => '081110', 'nationaliteiten' => [], 'reisdocumenten' => [])
        expect(GbaWebservice.enrich(data).onderzoek_adres?).to be(true)
      end

      it 'should return false if 088310 and 088330 are set' do
        data = GbaWebservice::Data.new('088310' => '081110', '088330' => '20170330', 'nationaliteiten' => [], 'reisdocumenten' => [])
        expect(GbaWebservice.enrich(data).onderzoek_adres?).to be(false)
      end

      it 'should return false if 088310 and 083030 are not set' do
        data = GbaWebservice::Data.new('nationaliteiten' => [], 'reisdocumenten' => [])
        expect(GbaWebservice.enrich(data).onderzoek_adres?).to be(false)
      end
    end

    describe 'nationaliteit onderzoek' do
      it 'should return true if 048310 is set and 048330 not' do
        data = GbaWebservice::Data.new('nationaliteiten' => [GbaWebservice::Data.new('048310' => '040510')], 'reisdocumenten' => [])
        expect(GbaWebservice.enrich(data).nationaliteiten.first.onderzoek?).to be(true)
      end

      it 'should return false if 048310 and 048330 are set' do
        data = GbaWebservice::Data.new('nationaliteiten' => [GbaWebservice::Data.new('048310' => '040510', '048330' => '20170330')], 'reisdocumenten' => [])
        expect(GbaWebservice.enrich(data).nationaliteiten.first.onderzoek?).to be(false)
      end

      it 'should return false if 048310 and 048330 are not set' do
        data = GbaWebservice::Data.new('nationaliteiten' => [GbaWebservice::Data.new()], 'reisdocumenten' => [])
        expect(GbaWebservice.enrich(data).nationaliteiten.first.onderzoek?).to be(false)
      end
    end

    describe 'reisdocument onderzoek' do
      it 'should return true if 128310 is set and 128330 not' do
        data = GbaWebservice::Data.new('reisdocumenten' => [GbaWebservice::Data.new('128310' => '20310229')], 'nationaliteiten' => [])
        expect(GbaWebservice.enrich(data).reisdocumenten.first.onderzoek?).to be(true)
      end

      it 'should return false if 128310 and 128330 are set' do
        data = GbaWebservice::Data.new('reisdocumenten' => [GbaWebservice::Data.new('128310' => '040510', '128330' => '20170330')], 'nationaliteiten' => [])
        expect(GbaWebservice.enrich(data).reisdocumenten.first.onderzoek?).to be(false)
      end

      it 'should return false if 128310 and 128330 are not set' do
        data = GbaWebservice::Data.new('reisdocumenten' => [GbaWebservice::Data.new()], 'nationaliteiten' => [])
        expect(GbaWebservice.enrich(data).reisdocumenten.first.onderzoek?).to be(false)
      end
    end

    describe 'reisdocument vermist' do
      it 'should return true if 123570 is set' do
        data = GbaWebservice::Data.new('reisdocumenten' => [GbaWebservice::Data.new('123570' => '040510')], 'nationaliteiten' => [])
        expect(GbaWebservice.enrich(data).reisdocumenten.first.vermist?).to be(true)
      end

      it 'should return false if 123570 is not set' do
        data = GbaWebservice::Data.new('reisdocumenten' => [GbaWebservice::Data.new()], 'nationaliteiten' => [])
        expect(GbaWebservice.enrich(data).reisdocumenten.first.vermist?).to be(false)
      end
    end

    describe 'reisdocument vervaldatum' do
      it 'should return nil for invalid date' do
        data = GbaWebservice::Data.new('reisdocumenten' => [GbaWebservice::Data.new('123550' => '20310229')], 'nationaliteiten' => [])
        expect(GbaWebservice.enrich(data).reisdocumenten.first.vervaldatum).to be(nil)
      end

      it 'should return nil for illegal format' do
        data = GbaWebservice::Data.new('reisdocumenten' => [GbaWebservice::Data.new('123550' => '2031-02-04')], 'nationaliteiten' => [])
        expect(GbaWebservice.enrich(data).reisdocumenten.first.vervaldatum).to be(nil)
      end

      it 'should replace zero day with 01' do
        data = GbaWebservice::Data.new('reisdocumenten' => [GbaWebservice::Data.new('123550' => '21000200')], 'nationaliteiten' => [])
        expect(GbaWebservice.enrich(data).reisdocumenten.first.vervaldatum).to eq(Date.new(2100,02,01))
      end

      it 'should replace zero day and month with 01' do
        data = GbaWebservice::Data.new('reisdocumenten' => [GbaWebservice::Data.new('123550' => '21000000')], 'nationaliteiten' => [])
        expect(GbaWebservice.enrich(data).reisdocumenten.first.vervaldatum).to eq(Date.new(2100,01,01))
      end

      it 'should return nil if only month is zero' do
        data = GbaWebservice::Data.new('reisdocumenten' => [GbaWebservice::Data.new('123550' => '21000001')], 'nationaliteiten' => [])
        expect(GbaWebservice.enrich(data).reisdocumenten.first.vervaldatum).to be(nil)
      end
    end
  end

  def stub_soap_endpoint
    WebMock.stub_request(:post, /localhost:8080/) # endpoint according to wsdl, don't ask
  end
end
