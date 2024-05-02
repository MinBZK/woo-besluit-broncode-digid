
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

package nl.logius.digid.hsm.integration.bsnk;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class TransformTest extends TransformBaseTest {
        @ParameterizedTest
        @MethodSource("getEncryptionVersions")
        public void singleTransform(int encryptionVersion) throws Exception {
                final ResponseEntity<String> response = single(ImmutableMap.of(
                                "polymorph", getPolymorph("pp", false), "oin", "abc", "ksv", 1, "targetMsgVersion",
                                encryptionVersion));

                assertEquals(HttpStatus.OK, response.getStatusCode());

                final ObjectNode result = (ObjectNode) mapper.readTree(response.getBody());
                assertEquals(ImmutableSet.of("identity", "pseudonym"), ImmutableSet.copyOf(result.fieldNames()));
                assertEquals(NullNode.instance, result.get("identity"));
                checkPseudonym(result.get("pseudonym").asText(), "abc", 1);
        }

        @ParameterizedTest
        @MethodSource("getEncryptionVersions")
        public void singleTransformWithDifferentEncryptionVersion(int encryptionVersion) throws Exception {
                final ResponseEntity<String> response = single(ImmutableMap.of(
                                "polymorph", getPolymorph("pp", false), "oin", "abc", "ksv", 1, "targetMsgVersion",
                                encryptionVersion));

                assertEquals(HttpStatus.OK, response.getStatusCode());

                final ObjectNode result = (ObjectNode) mapper.readTree(response.getBody());
                assertEquals(ImmutableSet.of("identity", "pseudonym"), ImmutableSet.copyOf(result.fieldNames()));
                assertEquals(NullNode.instance, result.get("identity"));
                checkPseudonym(result.get("pseudonym").asText(), "abc", 1);
        }

        @ParameterizedTest
        @MethodSource("getEncryptionVersions")
        public void singleTransformSigned(int encryptionVersion) throws Exception {
                final ResponseEntity<String> response = single(ImmutableMap.of(
                                "polymorph", getPolymorph("pp", true), "oin", "abc", "ksv", 1, "targetMsgVersion",
                                encryptionVersion));

                assertEquals(HttpStatus.OK, response.getStatusCode());

                final ObjectNode result = (ObjectNode) mapper.readTree(response.getBody());
                assertEquals(ImmutableSet.of("identity", "pseudonym"), ImmutableSet.copyOf(result.fieldNames()));
                assertEquals(NullNode.instance, result.get("identity"));
                checkPseudonym(result.get("pseudonym").asText(), "abc", 1);
        }

        @ParameterizedTest
        @MethodSource("getEncryptionVersions")
        public void singleTransformWithIdentity(int encryptionVersion) throws Exception {
                final ResponseEntity<String> response = single(ImmutableMap.of(
                                "polymorph", getPolymorph("pip", false), "oin", "def", "ksv", 2, "identity", true,
                                "targetMsgVersion",
                                encryptionVersion));

                assertEquals(HttpStatus.OK, response.getStatusCode());

                final ObjectNode result = (ObjectNode) mapper.readTree(response.getBody());
                assertEquals(ImmutableSet.of("identity", "pseudonym"), ImmutableSet.copyOf(result.fieldNames()));
                checkIdentity(result.get("identity").asText(), "def", 2);
                checkPseudonym(result.get("pseudonym").asText(), "def", 2);
        }

        @ParameterizedTest
        @MethodSource("getEncryptionVersions")
        public void singleTransformWithIdentitySigned(int encryptionVersion) throws Exception {
                final ResponseEntity<String> response = single(ImmutableMap.of(
                                "polymorph", getPolymorph("pip", true), "oin", "def", "ksv", 2, "identity", true,
                                "targetMsgVersion",
                                encryptionVersion));

                assertEquals(HttpStatus.OK, response.getStatusCode());

                final ObjectNode result = (ObjectNode) mapper.readTree(response.getBody());
                assertEquals(ImmutableSet.of("identity", "pseudonym"), ImmutableSet.copyOf(result.fieldNames()));
                checkIdentity(result.get("identity").asText(), "def", 2);
                checkPseudonym(result.get("pseudonym").asText(), "def", 2);
        }

        @ParameterizedTest
        @MethodSource("getEncryptionVersions")
        public void singleTransformWithoutPseudonym(int encryptionVersion) throws Exception {
                ImmutableMap map = ImmutableMap.builder()
                                .put("polymorph", getPolymorph("pi", false))
                                .put("oin", "ghi")
                                .put("ksv", 3)
                                .put("identity", true)
                                .put("pseudonym", false)
                                .put("targetMsgVersion", encryptionVersion)
                                .build();

                final ResponseEntity<String> response = single(map);

                assertEquals(HttpStatus.OK, response.getStatusCode());

                final ObjectNode result = (ObjectNode) mapper.readTree(response.getBody());
                assertEquals(ImmutableSet.of("identity", "pseudonym"), ImmutableSet.copyOf(result.fieldNames()));
                checkIdentity(result.get("identity").asText(), "ghi", 3);
                assertEquals(NullNode.instance, result.get("pseudonym"));
        }

        @ParameterizedTest
        @MethodSource("getEncryptionVersions")
        public void singleTransformWithoutPseudonymSigned(int encryptionVersion) throws Exception {
                ImmutableMap map = ImmutableMap.builder()
                                .put("polymorph", getPolymorph("pi", true))
                                .put("oin", "ghi")
                                .put("ksv", 3)
                                .put("identity", true)
                                .put("pseudonym", false)
                                .put("targetMsgVersion", encryptionVersion)
                                .build();

                final ResponseEntity<String> response = single(map);

                assertEquals(HttpStatus.OK, response.getStatusCode());

                final ObjectNode result = (ObjectNode) mapper.readTree(response.getBody());
                assertEquals(ImmutableSet.of("identity", "pseudonym"), ImmutableSet.copyOf(result.fieldNames()));
                checkIdentity(result.get("identity").asText(), "ghi", 3);
                assertEquals(NullNode.instance, result.get("pseudonym"));
        }

        @ParameterizedTest
        @MethodSource("getEncryptionVersions")
        public void singleTransformBadRequest(int encryptionVersion) throws Exception {
                final ResponseEntity<String> response = single(
                                ImmutableMap.of("polymorph", getPolymorph("pip", true), "targetMsgVersion",
                                                encryptionVersion));

                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
}
