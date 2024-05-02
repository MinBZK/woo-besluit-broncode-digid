
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

import com.google.common.io.ByteStreams;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

public class AbstractBaseTest {
    protected static MockHttpServletRequest httpServletRequestMock;
    protected static MockHttpServletResponse httpServletResponseMock;

    @BeforeAll
    public static void setup() throws IOException {
        httpServletResponseMock = new MockHttpServletResponse();
        httpServletRequestMock = new MockHttpServletRequest();
        httpServletRequestMock.setMethod("POST");
        httpServletRequestMock.setServerName("localhost:8080");
    }

    private static String readXMLFile(Resource xmlFile) {
        try (Reader reader = new InputStreamReader(xmlFile.getInputStream(), UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public MockHttpServletRequest preparePostRequest(Resource resource) {
        String samlRequest = readXMLFile(resource);
        String decodeSAMLRequest = encodeAuthnRequest(samlRequest);
        httpServletRequestMock.setParameter("SAMLRequest", decodeSAMLRequest);
        httpServletRequestMock.setParameter("RelayState", "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");

        return httpServletRequestMock;
    }

    public MockHttpServletRequest prepareSoapRequest(Resource resource) throws IOException {
        httpServletRequestMock.setContentType("text/xml");
        httpServletRequestMock.setContent(ByteStreams.toByteArray(resource.getInputStream()));

        return httpServletRequestMock;
    }

    private String encodeAuthnRequest(String xmlAuthnRequest) {
        return new String(Base64.getEncoder().encode(xmlAuthnRequest.getBytes()));
    }
}
