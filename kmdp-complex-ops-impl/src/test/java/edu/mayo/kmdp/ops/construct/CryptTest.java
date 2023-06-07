package edu.mayo.kmdp.ops.construct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import edu.mayo.kmdp.ops.EphemeralAssetFabricator.UUIDEncrypter;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries;

class CryptTest {

  @Test
  void testEphemeralUUIDs() {
    UUID src = UUID.randomUUID();
    UUID key = KnowledgeRepresentationLanguageSeries.FHIR_STU3.getUuid();

    var enc = UUIDEncrypter.encrypt(src, key);
    var dec = UUIDEncrypter.decrypt(enc, key);
    assertEquals(src, dec);
    assertNotEquals(src, enc);
  }
}
