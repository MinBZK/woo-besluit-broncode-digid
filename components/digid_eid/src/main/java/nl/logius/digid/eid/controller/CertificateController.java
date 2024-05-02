
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

package nl.logius.digid.eid.controller;

import com.google.common.collect.ImmutableMap;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import nl.logius.digid.card.asn1.Asn1ObjectMapper;
import nl.logius.digid.card.crypto.X509Factory;
import nl.logius.digid.eid.exceptions.ClientException;
import nl.logius.digid.eid.exceptions.NotFoundException;
import nl.logius.digid.eid.models.asn1.CvCertificate;
import nl.logius.digid.eid.models.db.Certificate;
import nl.logius.digid.eid.models.rest.digid.GenerateAtRequest;
import nl.logius.digid.eid.repository.CertificateRepository;
import nl.logius.digid.eid.service.CSCACertificateService;
import nl.logius.digid.eid.service.CVCertificateService;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/iapi/certificates")
@Tag(name = "CertificateController", description = "Certificate API for Admin")
public class CertificateController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Sort sort = Sort.by(Sort.DEFAULT_DIRECTION, "documentType", "issuer", "subject");

    @Autowired
    private Asn1ObjectMapper objectMapper;

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private CSCACertificateService cscaCertificateService;

    @Autowired
    private CVCertificateService cvCertificateService;

    @Operation(summary = "Generate new AT certificate")
    @PostMapping(value = "at-request", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public Map<String, byte[]> generateAtRequest(@Valid @RequestBody GenerateAtRequest request) {
        return ImmutableMap.of("request", cvCertificateService.generateAtRequest(
            request.getDocumentType(), request.getAuthorization(), request.getSequenceNo(), request.getReference()
        ));
    }

    @Operation(summary ="Generate new AT certificate")
    @GetMapping(value = "at-keys", produces = "application/json")
    @ResponseBody
    public List<String> listAtKeys() {
        return cvCertificateService.getAtKeyList();
    }

    @Operation(summary ="List all certificates")
    @GetMapping(value = "", produces = "application/json")
    @ResponseBody
    public List<Certificate> all() {
        return emptyRaw(certificateRepository.findAll(sort));
    }

    @Operation(summary ="Get single certificates")
    @GetMapping(value = "{id}", produces = "application/json")
    @ResponseBody
    public Certificate single(@PathVariable("id") long id) {
        Optional<Certificate> cert = certificateRepository.findById(id);
        if (!cert.isPresent())
            throw new NotFoundException("Could not find certificate with id " + id);
        return cert.get();
    }

    @Operation(summary ="Add certificate")
    @PostMapping(value = "", produces = "application/json")
    public Map<String, Object> create(@RequestBody byte[] request) {
        final PemObject obj = readPem(request);
        if (obj != null) {
            if (!"CERTIFICATE".equals(obj.getType())) {
                throw new ClientException("Unexpected PEM type " + obj.getType());
            }
            request = obj.getContent();
        }

        final Certificate cert;
        if (request.length == 0) {
            throw new ClientException("Empty body");
        } else if (request[0] == 0x30) {
            cert = cscaCertificateService.add(X509Factory.toCertificate(request));
        } else if (request[0] == (byte) 0x7f) {
            cert = cvCertificateService.add(toCV(request));
        } else {
            throw new ClientException("Unknown object");
        }
        return ImmutableMap.of("id", cert.getId());
    }

    private CvCertificate toCV(byte[] request) {
        try {
            return objectMapper.read(request, CvCertificate.class);
        } catch (Exception e) {
            logger.error("Could not decode CV certificate", e);
            throw new ClientException("Could not decode CV certificate", e);
        }
    }

    private PemObject readPem(byte[] input) {
        try (final ByteArrayInputStream is = new ByteArrayInputStream(input)) {
            final PemReader reader = new PemReader(new InputStreamReader(is, StandardCharsets.US_ASCII));
            return reader.readPemObject();
        } catch (IOException e) {
            throw new ClientException("Unexpected IOException", e);
        }
    }
}
