
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


# encoding: UTF-8
# frozen_string_literal: true

# Protecting from doing a seed on a production environment (preprod1, preprod2, productie)
# Initial seed is sometimes needed to start from scratch
#
# Production seeds must be provided by seperate seed file and a seperate Rake task
#
unless %w(preprod1 preprod2 productie).include?(Rails.env)
  Afmeldlijst.create_with(description: 'Groep voor burgers die geen DigiD willen')
             .find_or_create_by(name: 'AfmeldLijst')

  Switch.create_with(description: 'Deze switch is standaard actief voor alle accounts en zorgt ervoor dat burgers met de DigiD app kunnen inloggen bij webdiensten en vanuit Mijn DigiD de DigiD app kunnen activeren, het inlogniveau kunnen verhogen naar Substantieel, hun wachtwoord opnieuw kunnen instellen of hun telefoonnummer kunnen wijzigen. Bij calamiteiten met de DigiD app kan de koppeling afgesloten worden door deze switch op inactief te zetten. De eindgebruiker krijgt dan op het inlogscherm of in Mijn DigiD een melding te zien dat het gebruik van de DigiD app tijdelijk niet mogelijk is. (Deactiveren is nog wel mogelijk.)', status: Switch::Status::ALL)
        .find_or_create_by(name: 'Koppeling met DigiD app')

  Switch.create_with(description: 'Met deze switch kan het verhogen van het inlogniveau (van de DigiD app) naar Substantieel aan- of uitgezet worden. Bij calamiteiten met de RDA server kan de koppeling afgesloten worden door deze switch op inactief te zetten. De eindgebruiker krijgt dan in Mijn DigiD een melding te zien dat het verhogen van het inlogniveau tijdelijk niet mogelijk is.', status: Switch::Status::ALL)
        .find_or_create_by(name: 'Koppeling met RDA server')

  Switch.create_with(description: 'Met deze switch kan het verhogen van het inlogniveau (van de DigiD app) naar Substantieel aan- of uitgezet worden. Bij calamiteiten met de DigiD RDA server kan de koppeling afgesloten worden door deze switch op inactief te zetten. De eindgebruiker krijgt dan in de DigiD app een melding te zien dat het verhogen van het inlogniveau tijdelijk niet mogelijk is.', status: Switch::Status::ALL)
        .find_or_create_by(name: 'Koppeling met DigiD RDA server')

  Switch.create_with(description: 'Met deze switch wordt de radiobutton op het inlogscherm voor het testen van betrouwbaarheidsniveaus getoond of verborgen. Deze switch is alleen op ontwikkel, test, acceptatie en preprod omgevingen beschikbaar (NIET op productie!).', status: Switch::Status::INACTIVE)
        .find_or_create_by(name: 'Tonen testen betrouwbaarheidsniveau')

  Switch.create_with(description: 'Met deze switch is het mogelijk om voor DigiD Hoog de koppeling met de RDW in- en /of uit te schakelen en het wel / niet mogelijk te maken om met het rijbewijs bij webdiensten in te loggen. Bij calamiteiten met het rijbewijs kan de koppeling met de RDW afgesloten worden, waardoor ook inloggen met het rijbewijs niet meer mogelijk is, of alleen het inloggen met het rijbewijs uitgezet worden, waarbij de koppeling met de RDW wel actief blijft. De eindgebruiker krijgt, afhankelijk van de stand van deze switch, dan in DigiD een melding te zien dat het inloggen met of activeren / blokkeren / etc. van het rijbewijs tijdelijk niet mogelijk is.', status: Switch::Status::INACTIVE)
        .find_or_create_by(name: 'Koppeling met DigiD Hoog - Rijbewijs')

  Switch.create_with(description: 'Met deze switch is het mogelijk om voor DigiD Hoog de koppeling met de RvIG in- en /of uit te schakelen en het wel / niet mogelijk te maken om met de identiteitskaart bij webdiensten in te loggen. Bij calamiteiten met de identiteitskaart kan de koppeling met de RvIG afgesloten worden, waardoor ook inloggen met de identiteitskaart niet meer mogelijk is, of alleen het inloggen met de identiteitskaart uitgezet worden, waarbij de koppeling met de RvIG wel actief blijft. De eindgebruiker krijgt, afhankelijk van de stand van deze switch, dan in DigiD een melding te zien dat het inloggen met of activeren / blokkeren / etc. van de identiteitskaart tijdelijk niet mogelijk is.', status: Switch::Status::INACTIVE)
        .find_or_create_by(name: 'Koppeling met DigiD Hoog - Identiteitskaart')

  Switch.create_with(description: 'Met deze switch kan de functionaliteit van de (re)set van de PIN-code van het rijbewijs aan- of uitgezet worden. Standaard staat de switch op "Nee" en is het niet mogelijk om de PIN-code van het rijbewijs te (re)setten. Wanneer de switch op "Ja, alleen PIN-set" staat, is het wel mogelijk een initiële PIN-code in te stellen (PIN-set), maar niet om de resetten (PIN-reset).', status: Switch::Status::INACTIVE)
        .find_or_create_by(name: 'PIN-(re)set rijbewijs')
  Switch.where(name: 'PIN (re)set rijbewijs').destroy_all

  Switch.create_with(description: 'Met deze switch kan de functionaliteit van resetten van de PIN-code van de identiteitskaart aan- of uitgezet worden. Standaard staat de switch op "Nee" en is het niet mogelijk om de PIN-code van de identiteitskaart te resetten. Wanneer de switch op "Ja, PIN-reset" staat is het wel mogelijk om de PIN-code van de identiteitskaart te resetten.', status: Switch::Status::INACTIVE)
        .find_or_create_by(name: 'PIN-reset identiteitskaart')
  Switch.where(name: 'PIN (re)set identiteitskaart').destroy_all

  Switch.create_with(description: 'Met deze switch kan het verhogen van het betrouwbaarheidsniveau van de DigiD app vanuit Mijn DigiD uitgeschakeld worden. Indien ingeschakeld (\'Ja, voor alle accounts\') verdwijnt de link in Mijn DigiD waarmee het proces voor het verhogen gestart kan worden. Tevens dienen eventuele teksten die betrekking hebben op het verhogen van het betrouwbaarheidsniveau van de DigiD app vanuit Mijn DigiD, aangepast te worden.', status: Switch::Status::INACTIVE)
      .find_or_create_by(name: 'Uitschakelen verhogen betrouwbaarheidsniveau DigiD app vanuit Mijn DigiD')

  Switch.create_with(description: 'Met deze switch kan de wijze van het versturen van een sms aangepast worden. Standaard staat de switch op "Automatisch" en zal DigiD een sms op eigen initiatief versturen tijdens een DigiD app activatie. Wanneer de switch op "Wacht op verzoek van app" staat, zal DigiD pas een sms-code versturen als de DigiD app daar om vraagt.', status: Switch::Status::INACTIVE)
      .find_or_create_by(name: 'Versturen sms app-activatie')

  Switch.create_with(description: "Met deze switch wordt de koppeling met de ID-checker voor het verhogen naar niveau Substantieel beheerd. Indien uitgeschakeld, is het niet mogelijk het betrouwbaarheidsniveau van een DigiD app met behulp van een ID-checker te verhogen naar niveau Substantieel.", status: Switch::Status::INACTIVE)
    .find_or_create_by(name: "Koppeling met ID-checker")

  Switch.create_with(description: "Met deze switch wordt de koppeling tot de DigiD kiosk voor het verhogen naar niveau Substantieel beheerd. Indien uitgeschakeld, is het niet mogelijk het betrouwbaarheidsniveau van een DigiD app met behulp van een DigiD kiosk te verhogen naar niveau Substantieel.", status: Switch::Status::INACTIVE)
    .find_or_create_by(name: "Koppeling met DigiD kiosk")

  Switch.create_with(description: "Deze switch is standaard actief voor alle accounts en zorgt ervoor dat burgers bij een RvIG-Aanvraagstation (gemeente-balie) de DigiD app met ID-check kunnen activeren.", status: Switch::Status::INACTIVE)
    .find_or_create_by(name: "Koppeling met RvIG-Aanvraagstation")

  Switch.create_with(description: 'Met deze switch is het mogelijk Eenvoudige Herauthenticatie aan/uit te zetten.', status: Switch::Status::INACTIVE)
    .find_or_create_by(name: 'Eenvoudige Herauthenticatie')


  PilotGroup.create_with(description: "Friends & family, gerelateerd aan pilot switch ‘DigiD Hoog - Rijbewijs' EN 'DigiD Hoog - Identiteitskaart'")
          .find_or_create_by(name: "DigiD Hoog").tap do |group|
    Switch.create_with(description: 'Met deze switch is het mogelijk alle functionaliteit met betrekking tot inloggen met rijbewijs op niveau Hoog in of uit te schakelen: tonen/verbergen van velden, het activeren / intrekken / blokkeren / deblokkeren van het rijbewijs in Mijn DigiD en het tonen/verbergen van de inlogoptie met rijbewijs (radiobutton).', status: Switch::Status::INACTIVE)
          .find_or_create_by(name: 'Tonen DigiD Hoog - Rijbewijs', pilot_group: group)
    Switch.create_with(description: 'Met deze switch is het mogelijk alle functionaliteit met betrekking tot inloggen met identiteitskaart op niveau Hoog in of uit te schakelen: tonen/verbergen van velden, het activeren / intrekken / blokkeren / deblokkeren van de identiteitskaart in Mijn DigiD en het tonen/verbergen van de inlogoptie met identiteitskaart (radiobutton).', status: Switch::Status::INACTIVE)
          .find_or_create_by(name: 'Tonen DigiD Hoog - Identiteitskaart', pilot_group: group)
  end

  PilotGroup.create_with(description: "Friends & family, gerelateerd aan pilot switch ‘Mijn DigiD - Frontend'")
          .find_or_create_by(name: "Mijn DigiD - Frontend").tap do |group|
    Switch.create_with(description: 'Met deze switch is het mogelijk alle functionaliteit met betrekking tot de nieuwe Mijn DigiD frontend weer te geven/te verbergen.', status: Switch::Status::INACTIVE)
          .find_or_create_by(name: 'Tonen Mijn DigiD - Frontend', pilot_group: group)
  end


  # make admin obsolete
  ActiveRecord::Base.connection.execute('DELETE FROM `sectors` WHERE id=1;')
  ActiveRecord::Base.connection.execute('DELETE FROM `sectors` WHERE id=2;')
  ActiveRecord::Base.connection.execute('DELETE FROM `sectors` WHERE id=3;')
  ActiveRecord::Base.connection.execute('DELETE FROM `sectors` WHERE id=4;')
  ActiveRecord::Base.connection.execute('DELETE FROM `organizations` WHERE id IN (1,2,3);')

  ActiveRecord::Base.connection.execute("INSERT INTO `sectors`
                                         (`id`,`name`,`number_name`,`active`,`test`,`created_at`,`updated_at`,`expiration_time`,`valid_for`,`warn_before`,`pretty_name`)
                                         VALUES (1, 'bsn', '00000000', 1, 0, '2011-06-08 09:24:18', '2011-06-08 09:24:18', 45, 36, 30, 'Burgerservicenummer');")

  ActiveRecord::Base.connection.execute("INSERT INTO `sectors`
                                         (`id`,`name`,`number_name`,`active`,`test`,`created_at`,`updated_at`,`expiration_time`,`valid_for`,`warn_before`,`pretty_name`)
                                         VALUES (2, 'SOFI', '00000001', 1, 0, '2011-06-08 09:24:18', '2011-06-08 09:24:18', 45, 36, 30, 'Sofinummer');")

  ActiveRecord::Base.connection.execute("INSERT INTO `sectors`
                                         (`id`,`name`,`number_name`,`active`,`test`,`created_at`,`updated_at`,`expiration_time`,`valid_for`,`warn_before`,`pretty_name`)
                                         VALUES (3, 'a-nummer', '00000002', 1, 0, '2011-06-08 09:24:18', '2011-06-08 09:24:18', NULL, NULL, NULL, 'A-Nummer');")

  ActiveRecord::Base.connection.execute("INSERT INTO `sectors`
                                         (`id`,`name`,`number_name`,`active`,`test`,`created_at`,`updated_at`,`expiration_time`,`valid_for`,`warn_before`,`pretty_name`)
                                         VALUES (4, 'OEB', '00000100', 1, 0, '2011-06-08 09:24:18', '2011-06-08 09:24:18', 45, 36, 30, 'OEP-Nummer');")

  ActiveRecord::Base.connection.execute("INSERT INTO `organizations`
                                         (`id`,`name`,`description`,`created_at`,`updated_at`)
                                         VALUES (1, 'DigiD', 'De DigiD organisatie', '2011-06-08 09:24:18', '2011-06-08 09:24:18');")

  ActiveRecord::Base.connection.execute("INSERT INTO `organizations`
                                         (`id`,`name`,`description`,`created_at`,`updated_at`)
                                         VALUES (2, 'Stubs', 'De DigiD stubs', '2011-06-08 09:24:18', '2011-06-08 09:24:18');")

  ActiveRecord::Base.connection.execute("INSERT INTO `organizations`
                                         (`id`,`name`,`description`,`created_at`,`updated_at`)
                                         VALUES (3, 'JMeter', 'De JMeter afnemers', '2011-06-08 09:24:18', '2011-06-08 09:24:18');")

  # Mijn DigiD
  website_url = URI(APP_CONFIG["urls"]["external"]["digid_home"]).tap { |uri| uri.host = "mijn.#{uri.host}" }.to_s
  webservice = Webservice.create_with(active: true, description: 'Mijn DigiD is een Saml webservice', website_url: website_url, authentication_method: 'saml', organization_id: 1, check_redirect_url: false, app_to_app: true, substantieel_active: true)
                         .find_or_create_by(name: 'Mijn DigiD')

  webservice.sector_authentications.destroy_all
  webservice.sector_authentications << SectorAuthentication.create(webservice_id: webservice.id, sector_id: 1, position: 0)
  webservice.sector_authentications << SectorAuthentication.create(webservice_id: webservice.id, sector_id: 2, position: 1)
  webservice.sector_authentications << SectorAuthentication.create(webservice_id: webservice.id, sector_id: 3, position: 2)
  webservice.sector_authentications << SectorAuthentication.create(webservice_id: webservice.id, sector_id: 4, position: 3)

  # Signing
  signing_certificate_file = APP_CONFIG['my_digid_signing_cert_file']&.read || APP_CONFIG["saml_signing_cert_file"]&.read
  protocol = APP_CONFIG['protocol']
  mijn_cert_sign = OpenSSL::X509::Certificate.new(signing_certificate_file)
  mijn_host = APP_CONFIG['mijn_host'] || APP_CONFIG["hosts"]["mijn"]

  cached_metadata = <<XML
<?xml version="1.0" encoding="UTF-8"?>
<md:EntityDescriptor xmlns:md="urn:oasis:names:tc:SAML:2.0:metadata" entityID="MijnDigiD">
  <md:SPSSODescriptor protocolSupportEnumeration="urn:oasis:names:tc:SAML:2.0:protocol">
    <md:KeyDescriptor use="signing">
      <ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
        <ds:X509Data>
          <ds:X509Certificate>#{Base64.encode64(mijn_cert_sign.to_der).strip}</ds:X509Certificate>
        </ds:X509Data>
      </ds:KeyInfo>
    </md:KeyDescriptor>
    <md:KeyDescriptor use="encryption">
      <ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
        <ds:X509Data>
          <ds:X509Certificate>#{Base64.encode64(mijn_cert_sign.to_der).strip}</ds:X509Certificate>
        </ds:X509Data>
      </ds:KeyInfo>
    </md:KeyDescriptor>
    <md:AssertionConsumerService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Artifact" Location="#{protocol}://#{APP_CONFIG["hosts"]["was"]}/saml/sp/artifact_resolution" index="0"/>
    <md:SingleLogoutService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" Location="#{protocol}://#{mijn_host}/uitgelogd"/>
  </md:SPSSODescriptor>
  <md:ContactPerson contactType="technical">
    <md:GivenName>Administrator</md:GivenName>
    <md:EmailAddress>na@example.org</md:EmailAddress>
  </md:ContactPerson>
</md:EntityDescriptor>
XML

  SamlProvider.create_with(webservice_id: webservice.id, allow_sso: 1, cached_metadata: cached_metadata)
              .find_or_create_by(entity_id: 'MijnDigiD')


  # Stubs SSO
  sso_domain = Saml::SsoDomain.create_with(description: 'Stubs ABC', session_time: 15, grace_period_time: 15, show_current_session_screen: true)
                              .find_or_create_by(name: 'Stubs ABC')

  # Stubs SAML
  %w[saml sso-a sso-b sso-c l16j1-saml l16j1-sso-a l16j1-sso-b l16j1-sso-c l16j2-saml l16j2-sso-a l16j2-sso-b l16j2-sso-c l16j3-saml l16j3-sso-a l16j3-sso-b l16j3-sso-c].each do |key|
    l16j = key.include?('l16j')
    sso = key.include?('sso')
    if sso || l16j
      name = "Stubs #{key.sub('-', ' ').upcase}"
    else
      name = 'Stubs SAML'
    end
    host = l16j ? APP_CONFIG["#{key.split(/-/).first}_host"] : APP_CONFIG["hosts"]["stubs"]
    webservice = Webservice.create_with(active: true, description: name, website_url: "#{protocol}://#{host}/saml/#{key}/", authentication_method: 'saml', organization_id: 2, check_redirect_url: false, substantieel_active: true)
                           .find_or_create_by(name: name)

    webservice.sector_authentications.destroy_all
    webservice.sector_authentications << SectorAuthentication.create(webservice_id: webservice.id, sector_id: 1, position: 0)
    webservice.sector_authentications << SectorAuthentication.create(webservice_id: webservice.id, sector_id: 2, position: 1)
    webservice.sector_authentications << SectorAuthentication.create(webservice_id: webservice.id, sector_id: 3, position: 2)
    webservice.sector_authentications << SectorAuthentication.create(webservice_id: webservice.id, sector_id: 4, position: 3)

    saml_cert = OpenSSL::X509::Certificate.new(Rails.root.join('config','ssl',"stubs-#{key}.crt").read)
    cached_metadata = <<XML
<?xml version="1.0" encoding="UTF-8"?>
<md:EntityDescriptor xmlns:ds="http://www.w3.org/2000/09/xmldsig#" xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#" xmlns:md='urn:oasis:names:tc:SAML:2.0:metadata' ID='#{Saml.generate_id}' entityID='#{protocol}://#{host}/saml/#{key}/metadata'>
 <md:SPSSODescriptor protocolSupportEnumeration='urn:oasis:names:tc:SAML:2.0:protocol' AuthnRequestsSigned='true' WantAssertionsSigned='true'>
  <md:NameIDFormat>
   urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress
  </md:NameIDFormat>
  <md:KeyDescriptor use="signing">
    <ds:KeyInfo>
      <ds:KeyName>#{OpenSSL::Digest::SHA1.new(saml_cert.to_der).to_s}</ds:KeyName>
      <ds:X509Data>
        <ds:X509Certificate>#{Base64.encode64(saml_cert.to_der).strip}</ds:X509Certificate>
    </ds:X509Data>
    </ds:KeyInfo>
  </md:KeyDescriptor>
  <md:AssertionConsumerService Binding='urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Artifact' Location='#{protocol}://#{host}/saml/#{key}/assertion_consumer_service' isDefault='true' index='0'/>
  <md:SingleLogoutService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" Location="#{protocol}://#{host}/saml/#{key}/single_logout_service"/>
 </md:SPSSODescriptor>
</md:EntityDescriptor>
XML

    SamlProvider.create_with(webservice_id: webservice.id, cached_metadata: cached_metadata, sso_domain: sso ? sso_domain : nil, allow_sso: sso)
                .find_or_create_by(entity_id: "#{protocol}://#{host}/saml/#{key}/metadata")
  end

  # Stubs Aselect
  [10, 20, 25, 30].each do |assurance_level|
    webservice = Webservice.create_with(active: true, description: "Stubs Aselect #{assurance_level}", website_url: "#{protocol}://#{APP_CONFIG["hosts"]["stubs"]}/aselect", authentication_method: 'aselect', organization_id: 2, check_redirect_url: false, substantieel_active: true)
                           .find_or_create_by(name: "Stubs Aselect #{assurance_level}")

    webservice.sector_authentications.destroy_all
    webservice.sector_authentications << SectorAuthentication.create(webservice_id: webservice.id, sector_id: 1, position: 0)
    webservice.sector_authentications << SectorAuthentication.create(webservice_id: webservice.id, sector_id: 2, position: 1)
    webservice.sector_authentications << SectorAuthentication.create(webservice_id: webservice.id, sector_id: 3, position: 2)
    webservice.sector_authentications << SectorAuthentication.create(webservice_id: webservice.id, sector_id: 4, position: 3)

    webservice.aselect_webservice ||= Aselect::Webservice.new(app_id: "stubs-#{assurance_level}", assurance_level: assurance_level, active: true)
    webservice.aselect_webservice.shared_secrets << Aselect::SharedSecret.new(shared_secret: Digest::SHA1.new.hexdigest("0123-4567-89AB-CDEF-0123-00#{assurance_level}").upcase) if webservice.aselect_webservice.shared_secrets.empty?
    webservice.certificates << Certificate.first_or_create(cached_certificate: Rails.root.join('config', 'ssl', "stubs-aselect-#{assurance_level}.crt").read, distinguished_name: "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS") if webservice.certificates.empty?
  end

  webservice = Webservice.create_with(active: true, description: "Stubs SVB", website_url: "#{protocol}://#{APP_CONFIG["hosts"]["stubs"]}/svb", authentication_method: 'aselect', organization_id: 2, check_redirect_url: false, substantieel_active: true)
                          .find_or_create_by(name: "Stubs SVB")

  webservice.sector_authentications.destroy_all
  webservice.sector_authentications << SectorAuthentication.create(webservice_id: webservice.id, sector_id: 1, position: 0)
  webservice.sector_authentications << SectorAuthentication.create(webservice_id: webservice.id, sector_id: 2, position: 1, revocation: true, registration: true)
  webservice.sector_authentications << SectorAuthentication.create(webservice_id: webservice.id, sector_id: 3, position: 2)
  webservice.sector_authentications << SectorAuthentication.create(webservice_id: webservice.id, sector_id: 4, position: 3, revocation: true, registration: true)

  webservice.aselect_webservice ||= Aselect::Webservice.new(app_id: "stubs-svb", assurance_level: 10, active: true)
  webservice.certificates << Certificate.first_or_create(cached_certificate: Rails.root.join('config', 'ssl', 'stubs-svb.crt').read, distinguished_name: 'SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS') if webservice.certificates.empty?

  # JMeter Aselect
  webservice = Webservice.create_with(active: true, description: "JMeter Aselect", website_url: "SSSSSSSSSSSSSSSSSSSSSSSSSSSSS", authentication_method: 'aselect', organization_id: 3, check_redirect_url: false, substantieel_active: true)
                         .find_or_create_by(name: "JMeter Aselect")

  webservice.sector_authentications.destroy_all
  webservice.sector_authentications << SectorAuthentication.create(webservice_id: webservice.id, sector_id: 1, position: 0)
  webservice.sector_authentications << SectorAuthentication.create(webservice_id: webservice.id, sector_id: 2, position: 1)

  webservice.aselect_webservice ||= Aselect::Webservice.new(app_id: "SSSSSSSSSSS", assurance_level: 10, active: true)
  webservice.aselect_webservice.shared_secrets << Aselect::SharedSecret.new(shared_secret: Digest::SHA1.new.hexdigest("E607-C4E8-69D1-FCF5-IYO5-90CN")) if webservice.aselect_webservice.shared_secrets.empty?

  # JMeter SAML
  webservice = Webservice.create_with(active: true, description: 'JMeter SAML', website_url: "SSSSSSSSSSSSSSSSSSSSSSSSSSSSS", authentication_method: 'saml', organization_id: 3, check_redirect_url: false, substantieel_active: true)
                         .find_or_create_by(name: 'JMeter SAML')

  webservice.sector_authentications.destroy_all
  webservice.sector_authentications << SectorAuthentication.create(webservice_id: webservice.id, sector_id: 1, position: 0)
  webservice.sector_authentications << SectorAuthentication.create(webservice_id: webservice.id, sector_id: 2, position: 1)

  saml_cert = OpenSSL::X509::Certificate.new(Rails.root.join('config','ssl','jmeter-saml.crt').read)
  cached_metadata = <<XML
<md:EntityDescriptor xmlns:md="urn:oasis:names:tc:SAML:2.0:metadata" entityID="JMeter">
  <md:SPSSODescriptor protocolSupportEnumeration="urn:oasis:names:tc:SAML:2.0:protocol">
    <md:KeyDescriptor use="signing">
      <ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
        <ds:X509Data>
          <ds:X509Certificate>#{Base64.encode64(saml_cert.to_der).strip}</ds:X509Certificate>
        </ds:X509Data>
      </ds:KeyInfo>
    </md:KeyDescriptor>
        <md:KeyDescriptor use="encryption">
      <ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
        <ds:X509Data>
          <ds:X509Certificate>#{Base64.encode64(saml_cert.to_der).strip}</ds:X509Certificate>
        </ds:X509Data>
      </ds:KeyInfo>
    </md:KeyDescriptor>
    <md:SingleLogoutService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" Location="#{protocol}://#{APP_CONFIG["hosts"]["stubs"]}/jmeter/logout"/>
    <md:SingleLogoutService Binding="urn:oasis:names:tc:SAML:2.0:bindings:SOAP" Location="#{protocol}://#{APP_CONFIG["hosts"]["stubs"]}/jmeter/logout"/>
    <md:AssertionConsumerService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST" Location="#{protocol}://#{APP_CONFIG["hosts"]["stubs"]}/jmeter/acs" index="0"/>
    <md:AssertionConsumerService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Artifact" Location="#{protocol}://#{APP_CONFIG["hosts"]["stubs"]}/jmeter/artifact" index="1"/>
  </md:SPSSODescriptor>
  <md:ContactPerson contactType="technical">
    <md:GivenName>Administrator</md:GivenName>
    <md:EmailAddress>na@example.org</md:EmailAddress>
  </md:ContactPerson>
</md:EntityDescriptor>
XML

  SamlProvider.create_with(webservice_id: webservice.id, cached_metadata: cached_metadata)
              .find_or_create_by(entity_id: "JMeter")

  # Default config app version
  AppVersion.create_with(operating_system: 'Android', version: '1.0.0', release_type: "Productie", not_valid_before: Time.zone.now)
            .find_or_create_by(operating_system: 'Android', version: '1.0.0', release_type: "Productie") unless Rails.env.test?

  # app used in performace tests
  AppVersion.create_with(operating_system: 'Android', version: '1.1.0', release_type: "Productie", not_valid_before: Time.zone.now)
            .find_or_create_by(operating_system: 'Android', version: '1.1.0', release_type: "Productie") if Rails.application.config.performance_mode

  load Rails.root.join('lib/key_loader.rb')
end
