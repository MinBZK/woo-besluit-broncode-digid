
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

package nl.logius.digid.eid.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import nl.logius.digid.card.asn1.Asn1ObjectMapper;
import nl.logius.digid.card.crypto.X509Factory;
import nl.logius.digid.eid.BaseTest;
import nl.logius.digid.eid.models.EidSession;
import nl.logius.digid.eid.models.asn1.CvCertificate;
import nl.logius.digid.eid.models.db.Certificate;
import nl.logius.digid.eid.models.rest.RequestHeader;
import nl.logius.digid.eid.models.rest.Version;
import nl.logius.digid.eid.repository.CertificateRepository;
import nl.logius.digid.eid.repository.EidSessionRepository;
import nl.logius.digid.eid.security.SecurityFactory;
import nl.logius.digid.eid.service.ConfirmService;
import nl.logius.digid.eid.service.SignatureService;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class AuthServiceSetup extends BaseTest {
    @MockBean
    protected ConfirmService confirmService;
    @MockBean
    protected SignatureService signatureService;
    @MockBean
    protected SecurityFactory securityFactory;

    @Autowired
    protected Asn1ObjectMapper asn1Mapper;
    @Autowired
    protected CertificateRepository certificateRepo;
    @Autowired
    protected EidSessionRepository sessionRepo;
    @Autowired
    protected Flyway flyway;
    @Autowired
    protected ObjectMapper mapper;
    @Autowired
    protected TestRestTemplate restTemplate;

    protected final String sessionId = UUID.randomUUID().toString();

    protected void loadCscaCertificate(String path) throws CertificateException, IOException {
        final Certificate cert = Certificate.from(X509Factory.toCertificate(readFixture(path)));
        cert.setTrusted(true);
        certificateRepo.saveAndFlush(cert);
    }

    protected void loadCvCertificate(String path, boolean trusted) throws IOException {
        final Certificate cert = Certificate.from(asn1Mapper.read(readFixture(path), CvCertificate.class));
        cert.setTrusted(trusted);
        certificateRepo.saveAndFlush(cert);
    }

    protected void createSession(Consumer<EidSession> consumer) {
        final EidSession session = new EidSession();
        session.setId(sessionId);
        session.setExpiration(62);
        consumer.accept(session);
        sessionRepo.save(session);
    }

    protected Map<String, Object> header() {
        final Map<String, Integer> version = ImmutableMap.of("minor", 1, "major", 1);
        return ImmutableMap.of("sessionId", sessionId, "supportedAPIVersion", version);
    }

    protected RequestHeader createRequestHeader() {
        Version version = new Version();
        version.setMinor("1");
        version.setMajor("1");
        RequestHeader requestHeader = new RequestHeader();
        requestHeader.setSessionId("");
        requestHeader.setSupportedAPIVersion(version);
        return requestHeader;
    }
}
