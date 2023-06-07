package edu.mayo.kmdp.examples._0.basic;

import static edu.mayo.kmdp.registry.Registry.BASE_UUID_URN_URI;
import static edu.mayo.kmdp.util.Util.uuid;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.artifactId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.assetId;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.XHTML;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;

public class CarrierExample {

  @Test
  /**
   * KnowledgeCarriers ({@link KnowledgeCarrier}) wrap a Knowledge Artifact,
   * whether in byte[], String, Stream, Document or Object format,
   * providing minimal metadata necessary at computation time:
   * - the Knowledge Asset Id
   * - the Knowledge Artifact Id
   * - the Representation Information (language, profile, serialization, format, etc)
   * Carriers can also include a URL, in case the actual Artifact is not serializable
   *
   * The interface {@link AbstractCarrier} provides utility constructor methods
   */
  void testCarrier() {

    KnowledgeCarrier kc = AbstractCarrier.of(
        "<html xmlns=\"http://www.w3.org/1999/xhtml\"> "
            + "<head> "
            + "  <title>Title of document</title> "
            + "</head> "
            + "<body> "
            + "  some content "
            + "</body> "
            + "</html> ")
        .withRepresentation(rep(XHTML, XML_1_1))
        .withAssetId(assetId(BASE_UUID_URN_URI,uuid("asset000")))
        .withArtifactId(artifactId(BASE_UUID_URN_URI,uuid("artifact123"), "0.0.1"))
        .withHref(URI.create("http://www.foo.bar/home"));

    assertNotNull(kc.getAssetId());
    assertNotNull(kc.getArtifactId());
    assertSame(XHTML, kc.getRepresentation().getLanguage());
  }


}
