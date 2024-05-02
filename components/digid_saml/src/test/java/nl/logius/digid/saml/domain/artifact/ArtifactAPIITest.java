
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

package nl.logius.digid.saml.domain.artifact;

import nl.logius.digid.saml.exception.InvalidInputException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * This is first setup of the integration test of the artifact domain.
 * Because of the external services and dependencies it is hard to setup an proper integration test
 * Work in progress
 */
@ExtendWith(SpringExtension.class)
@ActiveProfiles({"default", "integration-test"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ArtifactAPIITest {
    @Autowired
    private MockMvc mockMvc;

    @Value("classpath:requests/artifact-request-valid.xml")
    private Resource artifactResolveValid;

    private static String readXMLFile(Resource xmlFile) {
        try (Reader reader = new InputStreamReader(xmlFile.getInputStream(), UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    @DisplayName("When redirect with saml artifact is successful in CombiConnect flow, resolve artifact")
    void resolveArtifactErrorCombiConnectFlow() {
        try {
            mockMvc.perform(
                    post("/backchannel/saml/v4/entrance/resolve_artifact")
                            .contentType("text/xml")
                            .content(readXMLFile(artifactResolveValid)))
                    .andExpect(status().isBadRequest());
        } catch (Exception e) {
            throw new InvalidInputException("Invalid request");
        }
    }

    @Test
    @DisplayName("When redirect with saml artifact is successful in Idp flow, resolve artifact")
    void resolveArtifactErrorIdpFlow() {

        try {
            mockMvc.perform(
                    post("/backchannel/saml/v4/idp/resolve_artifact"))
                    .andExpect(status().is4xxClientError());
        } catch (Exception e) {
            throw new InvalidInputException("Invalid request");
        }
    }

    @Test
    @DisplayName("When authentication is successful redirect transactionId from BVD")
    void redirectWithTransactionIdError() {

        try {
            mockMvc.perform(
                    get("/frontchannel/saml/v4/idp/return_from_bvd")
                            .param("transactionId", "transactionId")
                            .param("status", "success")
            )
                    .andExpect(status().is4xxClientError());
        } catch (Exception e) {
            throw new InvalidInputException("Invalid request");
        }
    }
}
