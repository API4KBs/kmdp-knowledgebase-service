package edu.mayo.kmdp.knowledgebase.introspectors.owl2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Draft;

import edu.mayo.kmdp.language.detectors.owl2.OWLDetector;
import edu.mayo.kmdp.util.DateTimeUtil;
import edu.mayo.kmdp.util.Util;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedIntrospectDirect;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DetectApiInternal._applyDetect;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.Publication;

class OWL2IntrospectorTest {

  private final _applyDetect detector = new OWLDetector();
  private final _applyNamedIntrospectDirect introspector = new OWLMetadataIntrospector();

  @Test
  void testOntologyMetadata() {
    var ans = loadOntology();
    assertTrue(ans.isSuccess());

    var surrogate = ans.flatMap(kc ->
            introspector.applyNamedIntrospectDirect(OWLMetadataIntrospector.OP_ID, kc, null))
        .flatOpt(kc -> kc.as(KnowledgeAsset.class))
        .orElseGet(Assertions::fail);

    assertNotNull(surrogate);

    checkAssetId(surrogate.getAssetId());
    checkOntologyId(surrogate.getSecondaryId());
    checkAssetPublicationStatus(surrogate.getLifecycle());

    checkArtifact(surrogate.getCarriers().get(0));
  }

  private void checkArtifact(KnowledgeArtifact carrier) {
    assertTrue(OWL_2.sameTermAs(carrier.getRepresentation().getLanguage()));
    assertNotNull(carrier.getRepresentation().getProfile());
    assertNotNull(carrier.getRepresentation().getSerialization());

    checkArtifactId(carrier.getArtifactId());
    checkArtifactPublicationStatus(carrier.getLifecycle());
  }

  private void checkArtifactId(ResourceIdentifier artifactId) {
    assertEquals("1.0.4-SNAPSHOT", artifactId.getVersionTag());
  }

  private void checkOntologyId(List<ResourceIdentifier> secondaryId) {
    assertEquals(1, secondaryId.size());
    var id = secondaryId.get(0);
    assertEquals("http://ontology.mayo.edu/ontologies/SNAPSHOT/testExample/",
        id.getVersionId().toString());
    assertEquals("testExample", id.getTag());
    assertNull(id.getEstablishedOn());
  }

  private void checkAssetPublicationStatus(Publication lifecycle) {
    assertTrue(Draft.sameTermAs(lifecycle.getPublicationStatus()));
    assertNotNull(lifecycle.getCreatedOn());
    assertNull(lifecycle.getLastReviewedOn());
  }

  private void checkArtifactPublicationStatus(Publication lifecycle) {
    assertTrue(Draft.sameTermAs(lifecycle.getPublicationStatus()));
    assertEquals(DateTimeUtil.parseDate("2023-01-15"), lifecycle.getCreatedOn());
    assertEquals(DateTimeUtil.parseDate("2023-01-19"), lifecycle.getLastReviewedOn());
  }

  private void checkAssetId(ResourceIdentifier assetId) {
    assertNull(assetId.getEstablishedOn());
    assertEquals("0.0.0-SNAPSHOT", assetId.getVersionTag());
    assertEquals(Util.uuid("http://ontology.mayo.edu/ontologies/testExample/"), assetId.getUuid());
  }

  private Answer<KnowledgeCarrier> loadOntology() {
    var is = OWL2IntrospectorTest.class.getResourceAsStream("/introspectors/owl2/testOntology.owl");
    var kc = AbstractCarrier.of(is);
    return detector.applyDetect(kc, null);
  }

}
