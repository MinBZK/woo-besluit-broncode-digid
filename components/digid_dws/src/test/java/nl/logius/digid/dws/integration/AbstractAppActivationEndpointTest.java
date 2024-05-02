
/*
  Deze broncode is openbaar gemaakt vanwege een Woo-verzoek zodat deze
  gericht is op transparantie en niet op hergebruik. Hergebruik van 
  de broncode is toegestaan onder de EUPL licentie, met uitzondering 
  van broncode waarvoor een andere licentie is aangegeven.
  
  Het archief waar dit bestand deel van uitmaakt is te vinden op:
    https://github.com/MinBZK/woo-besluit-broncode-digid
  
  Eventuele kwetsbaarheden kunnen worden gemeld bij het NCSC via:
    https://www.ncsc.nl/contact/kwetsbaarheid-melden
  onder vermelding van "Logius, openbaar gemaakte broncode DigiD" 
  
  Voor overige vragen over dit Woo-besluit kunt u mailen met open@logius.nl
  
  This code has been disclosed in response to a request under the Dutch
  Open Government Act ("Wet open Overheid"). This implies that publication 
  is primarily driven by the need for transparence, not re-use.
  Re-use is permitted under the EUPL-license, with the exception 
  of source files that contain a different license.
  
  The archive that this file originates from can be found at:
    https://github.com/MinBZK/woo-besluit-broncode-digid
  
  Security vulnerabilities may be responsibly disclosed via the Dutch NCSC:
    https://www.ncsc.nl/contact/kwetsbaarheid-melden
  using the reference "Logius, publicly disclosed source code DigiD" 
  
  Other questions regarding this Open Goverment Act decision may be
  directed via email to open@logius.nl
*/

package nl.logius.digid.dws.integration;

import https.digid_nl.schema.aanvraagstation.AanvraagstationPort;
import nl.logius.digid.dws.TestAanvraagstationApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static nl.logius.digid.dws.utils.TestHelper.sendResourceSoapRequest;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes={
    TestAanvraagstationApplication.class})
@ActiveProfiles({ "default", "integration-test" })
public abstract class AbstractAppActivationEndpointTest {

    @Autowired
    protected AanvraagstationPort aanvraagstationPortService;

    @Value(value="classpath:requests/aanvraagstation/signed_validate_activation_from_aanvraagstation.xml")
    protected Resource validSignedValidateAppActivationRequest;

    @Value(value="classpath:requests/aanvraagstation/signed_invalid_oin_validate_activation.xml")
    protected Resource SignedOinMismatchValidateAppActivationRequest;

    @Value(value="classpath:requests/aanvraagstation/signed_invalid_oin_app_activation.xml")
    protected Resource SignedOinMismatchAppActivationRequest;

    @Value(value="classpath:requests/aanvraagstation/invalid_signed_validate_activation_from_aanvraagstation.xml")
    protected Resource invalidSignedValidateAppActivationRequest;

    @Value(value="classpath:requests/aanvraagstation/signed_activation_from_aanvraagstation.xml")
    protected Resource validSignedAppActivationRequest;

    @Value(value="classpath:requests/aanvraagstation/invalid_signed_activation_from_aanvraagstation.xml")
    protected Resource invalidSignedAppActivationRequest;

    @Value(value="classpath:requests/aanvraagstation/signed_invalid_doc_nr_validate_activation.xml")
    protected Resource SignedInvalidDocNrValidateAppActivationRequest;

    @Value(value="classpath:requests/aanvraagstation/signed_invalid_requester_activation.xml")
    protected Resource SignedInvalidRequesterAppActivationRequest;

    @Value(value="classpath:requests/aanvraagstation/VersionMismatchRequest.xml")
    protected Resource versionMismatchRequest;

    @Value("${ws.server.rvig-aanvraagstation.version}")
    protected String wsVersion;

    @Value("${ws.server.rvig-aanvraagstation.xsd-file-name}")
    protected String xsdFileName;

    @Value(value="classpath:ws/${ws.server.rvig-aanvraagstation.xsd-file-name}")
    protected Resource xsdSchema;

    @LocalServerPort
    protected int serverPort;

    @BeforeEach
    public void init() throws Exception {
    }

    protected <T> T sendRequest(Resource request, String service, Class<T> responseClass) {
        T detail = null;
        try{
            detail = sendResourceSoapRequest(request, responseClass, serverPort, service);
        } catch(Exception e ) {
            fail("Exception raised:" + e.getMessage());
        }

        return detail;
    }
}
