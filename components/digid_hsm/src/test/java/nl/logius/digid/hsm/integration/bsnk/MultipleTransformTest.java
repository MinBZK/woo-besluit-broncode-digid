
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import nl.logius.bsnkpp.kmp.KMPRecipientTransformInfo;
import nl.logius.bsnkpp.usve.USvEExtraElements;
import nl.logius.bsnkpp.usve.USvEExtraElementsKeyValuePair;
import nl.logius.bsnkpp.usve.USvESignedEncryptedIdentity;
import nl.logius.bsnkpp.usve.USvESignedEncryptedPseudonym;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
public class MultipleTransformTest extends TransformBaseTest {
    @Test
    @MethodSource("getEncryptionVersions")
    public void multipleTransform() throws Exception {
        final ResponseEntity<String> response = multiple(ImmutableMap.of(
            "polymorph", getPolymorph("pip", false),
            "requests", ImmutableMap.of(
                "abc", ImmutableMap.of("ksv", 1, "identity", false, "pseudonym", true),
                "def", ImmutableMap.of("ksv", 2, "identity", true, "pseudonym", false)),
            "targetMsgVersion", 2));

        assertEquals(HttpStatus.OK, response.getStatusCode());

        final ObjectNode result = (ObjectNode) mapper.readTree(response.getBody());
        assertEquals(ImmutableSet.of("abc", "def"), ImmutableSet.copyOf(result.fieldNames()));

        final ObjectNode abc = (ObjectNode) result.get("abc");
        assertEquals(NullNode.instance, abc.get("identity"));
        checkPseudonym(abc.get("pseudonym").asText(), "abc", 1);

        final ObjectNode def = (ObjectNode) result.get("def");
        checkIdentity(def.get("identity").asText(), "def", 2);
        assertEquals(NullNode.instance, def.get("pseudonym"));
    }

    // HA1: PIP naar VI
    @Test
    @MethodSource("getEncryptionVersions")
    public void testHa1PipToVi() throws Exception {
        final ResponseEntity<String> response = multiple(ImmutableMap.of(
            "polymorph", getPolymorph("pip", false),
            "requests", ImmutableMap.of(
                "ghi", ImmutableMap.of("ksv", 3, "identity", true, "pseudonym", false)),
            "targetMsgVersion", 2));

        assertEquals(HttpStatus.OK, response.getStatusCode());

        final ObjectNode result = (ObjectNode) mapper.readTree(response.getBody());
        assertEquals(ImmutableSet.of("ghi"), ImmutableSet.copyOf(result.fieldNames()));

        final ObjectNode ghi = (ObjectNode) result.get("ghi");
        checkIdentity(ghi.get("identity").asText(), "ghi", 3);
        assertEquals(NullNode.instance, ghi.get("pseudonym"));
    }

    // HA1: PIP naar VP
    @Test
    @MethodSource("getEncryptionVersions")
    public void testHa1PipToVp() throws Exception {
        final ResponseEntity<String> response = multiple(ImmutableMap.of(
            "polymorph", getPolymorph("pip", false),
            "requests", ImmutableMap.of(
                "ghi", ImmutableMap.of("ksv", 3, "identity", false, "pseudonym", true)),
            "targetMsgVersion", 2));

        assertEquals(HttpStatus.OK, response.getStatusCode());

        final ObjectNode result = (ObjectNode) mapper.readTree(response.getBody());
        assertEquals(ImmutableSet.of("ghi"), ImmutableSet.copyOf(result.fieldNames()));

        final ObjectNode ghi = (ObjectNode) result.get("ghi");
        checkPseudonym(ghi.get("pseudonym").asText(), "ghi", 3);
        assertEquals(NullNode.instance, ghi.get("identity"));
    }

    // HA4: Verkeerd versienummer voor de schema sleutelset,
    @MethodSource("getEncryptionVersions")
    @Test
    public void testHa4WrongTargetMsgVersion() throws Exception {
        int encryptionVersion = 1;
        final ResponseEntity<String> response = multiple(ImmutableMap.of(
            "polymorph", getPolymorph("pip", false),
            "requests", ImmutableMap.of(
                "ghi", ImmutableMap.of("ksv", 3, "identity", true, "pseudonym", false)),
            "targetMsgVersion", encryptionVersion));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // HA5: verkeerd Relaying Party OIN of KSV
    @Test
    @MethodSource("getEncryptionVersions")
    public void testHa5WrongRelayingParty() throws Exception {
        final ResponseEntity<String> response = multiple(ImmutableMap.of(
            "polymorph", getPolymorph("pip", false),
            "requests", ImmutableMap.of(
                "non_existing_oin",
                ImmutableMap.of("ksv", 3, "identity", true, "pseudonym", false),
                "ghi",
                ImmutableMap.of("ksv", 99, "identity", true, "pseudonym", false)),
            "targetMsgVersion", 2));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        final ObjectNode result = (ObjectNode) mapper.readTree(response.getBody());
        assertEquals(ImmutableSet.of("non_existing_oin", "ghi"), ImmutableSet.copyOf(result.fieldNames()));
    }

    @Test
    @MethodSource("getEncryptionVersions")
    public void testLinkedTransformations() throws Exception {
        final ResponseEntity<String> response = multiple(ImmutableMap.of(
            "polymorph", getPolymorph("pip", false),
            "requests", ImmutableMap.of(
                "abc", ImmutableMap.of("ksv", 1, "identity", false, "pseudonym", true, "includeLinks", false),
                "def", ImmutableMap.of("ksv", 2, "identity", true, "pseudonym", false, "includeLinks", true)),
            "targetMsgVersion", 2));

        assertEquals(HttpStatus.OK, response.getStatusCode());

        final ObjectNode result = (ObjectNode) mapper.readTree(response.getBody());
        assertEquals(ImmutableSet.of("abc", "def"), ImmutableSet.copyOf(result.fieldNames()));

        final ObjectNode abc = (ObjectNode) result.get("abc");
        assertEquals(NullNode.instance, abc.get("identity"));
        checkPseudonym(abc.get("pseudonym").asText(), "abc", 1);

        final ObjectNode def = (ObjectNode) result.get("def");
        checkIdentity(def.get("identity").asText(), "def", 2);
        assertEquals(NullNode.instance, def.get("pseudonym"));

        List<KMPRecipientTransformInfo> encryptedItems = new ArrayList<>();

        KMPRecipientTransformInfo recipientAbc = new KMPRecipientTransformInfo();
        recipientAbc.setEncryptedData(Base64.getDecoder().decode(abc.get("pseudonym").asText()));
        recipientAbc.setIdentifierFormat(KMPRecipientTransformInfo.FRMT_PSEUDO);
        recipientAbc.includeLinks(false);
        encryptedItems.add(recipientAbc);

        KMPRecipientTransformInfo recipientDef = new KMPRecipientTransformInfo();
        recipientDef.setEncryptedData(Base64.getDecoder().decode(def.get("identity").asText()));
        recipientDef.setIdentifierFormat(KMPRecipientTransformInfo.FRMT_IDENTIFIER);
        recipientDef.includeLinks(true);
        encryptedItems.add(recipientDef);

        USvEExtraElements extraElementsAbc = getExtraElementsFromEncryptedItem(encryptedItems.get(0));
        assertNull(extraElementsAbc);

        USvEExtraElements extraElementsDef = getExtraElementsFromEncryptedItem(encryptedItems.get(1));
        assertNotNull(extraElementsDef);

        assertEncryptedItemsInLinkingElement(encryptedItems, encryptedItems.get(1));
    }

    private USvEExtraElements getExtraElementsFromEncryptedItem(KMPRecipientTransformInfo linkingElment) throws Exception {
        USvEExtraElements extraElements;

        if (linkingElment.getIdentifierFormat() == KMPRecipientTransformInfo.FRMT_PSEUDO) {
            USvESignedEncryptedPseudonym sep = USvESignedEncryptedPseudonym
                .decode(linkingElment.getEncryptedData());
            extraElements = sep.getSignedEP().getExtraElements();
        } else {
            USvESignedEncryptedIdentity sei = USvESignedEncryptedIdentity
                .decode(linkingElment.getEncryptedData());
            extraElements = sei.getSignedEI().getExtraElements();
        }

        return extraElements;
    }

    private void assertEncryptedItemsInLinkingElement(List<KMPRecipientTransformInfo> encrypted_items, KMPRecipientTransformInfo linkingElment) throws Exception {
        USvEExtraElements extraElements = getExtraElementsFromEncryptedItem(linkingElment);

        for (int i = 0; i < encrypted_items.size(); i++) {
            if (encrypted_items.get(i) == linkingElment) {
                continue;
            }

            if (encrypted_items.get(i).getIdentifierFormat() == KMPRecipientTransformInfo.FRMT_PSEUDO) {
                USvESignedEncryptedPseudonym sep = USvESignedEncryptedPseudonym
                    .decode(encrypted_items.get(i).getEncryptedData());
                byte[] hash = sep.getUnsignedEncryptedPseudonym().calculateHashSha384();

                assertTrue(findHashInExtraElements(extraElements, hash));
            } else if (encrypted_items.get(i).getIdentifierFormat() == KMPRecipientTransformInfo.FRMT_IDENTIFIER) {
                USvESignedEncryptedIdentity sei = USvESignedEncryptedIdentity
                    .decode(encrypted_items.get(i).getEncryptedData());
                byte[] hash = sei.getUnsignedEncryptedIdentity().calculateHashSha384();

                assertTrue(findHashInExtraElements(extraElements, hash));
            }
        }
    }

    private boolean findHashInExtraElements(USvEExtraElements extraElements, byte[] hash) {
        for (int j = 0; j < extraElements.findAll("SameID").size(); j++) {
            USvEExtraElementsKeyValuePair same_id = extraElements.findAll("SameID").get(j);
            byte[] same_id_hash = (byte[]) same_id.getValue();

            if (Arrays.equals(hash, same_id_hash)) {
                return true;
            }
        }
        return false;
    }
}
