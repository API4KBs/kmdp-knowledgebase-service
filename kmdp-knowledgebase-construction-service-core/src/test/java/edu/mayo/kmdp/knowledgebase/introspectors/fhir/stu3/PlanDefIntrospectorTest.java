package edu.mayo.kmdp.knowledgebase.introspectors.fhir.stu3;

import static edu.mayo.kmdp.util.DateTimeUtil.parseDate;
import static java.nio.charset.Charset.defaultCharset;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultSurrogateUUID;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Cognitive_Care_Process_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Plans_Processes_Pathways_And_Protocol_Definitions;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Draft;

import edu.mayo.kmdp.util.DateTimeUtil;
import java.io.InputStream;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;

class PlanDefIntrospectorTest {

  @Test
  void testPlanDefIntrospect() {
    var surr = introspect();
    testAssetMetadata(surr);

    testArtifactMetadata(surr.getCarriers().get(0));

    testSurrogateMetadata(surr.getAssetId(), surr.getSurrogate().get(0));
  }

  private void testSurrogateMetadata(ResourceIdentifier assetId, KnowledgeArtifact surr) {
    assertEquals(
        defaultSurrogateUUID(assetId, Knowledge_Asset_Surrogate_2_0),
        surr.getArtifactId().getUuid()
    );
    assertTrue(Knowledge_Asset_Surrogate_2_0
        .sameAs(surr.getRepresentation().getLanguage()));

  }

  private void testArtifactMetadata(KnowledgeArtifact carrier) {
    assertEquals(
        UUID.fromString("be033ebc-4aed-3ffd-9c2b-82ff0bcd84f9"),
        carrier.getArtifactId().getUuid()
    );

    SyntacticRepresentation rep = carrier.getRepresentation();
    assertTrue(FHIR_STU3.sameAs(rep.getLanguage()));
    assertTrue(JSON.sameAs(rep.getFormat()));
    assertNotNull(rep.getCharset());
    assertNotNull(rep.getEncoding());

    assertEquals("0.0.0-SNAPSHOT", carrier.getArtifactId().getVersionTag());
    assertTrue(FHIR_STU3
        .sameAs(carrier.getRepresentation().getLanguage()));
    assertTrue(DateTimeUtil.isSameDay(
        parseDate("2023-05-09"),
        carrier.getArtifactId().getEstablishedOn()));

    assertEquals(3, carrier.getLinks().size());

    assertEquals(Draft, carrier.getLifecycle().getPublicationStatus());
    assertTrue(DateTimeUtil.isSameDay(
        parseDate("2023-05-09"),
        carrier.getLifecycle().getLastReviewedOn()));
  }

  private void testAssetMetadata(KnowledgeAsset surr) {
    assertNotNull(surr.getAssetId());
    assertEquals("191e1358-2c2c-7d11-0c35-8a197a14cac5", surr.getAssetId().getTag());
    assertEquals("0.0.0", surr.getAssetId().getVersionTag());

    assertTrue(Cognitive_Care_Process_Model.isAnyOf(surr.getFormalType()));
    assertTrue(Plans_Processes_Pathways_And_Protocol_Definitions.isAnyOf(surr.getFormalCategory()));

    assertEquals(3, surr.getLinks().size());

    assertEquals(1, surr.getCarriers().size());
    assertEquals(1, surr.getSurrogate().size());
    assertEquals(Draft, surr.getLifecycle().getPublicationStatus());
  }

  private KnowledgeAsset introspect() {
    InputStream is = PlanDefIntrospectorTest.class.getResourceAsStream(
        "/introspectors/fhir/stu3/example.plandef.json");
    KnowledgeCarrier kc = AbstractCarrier.of(is)
        .withRepresentation(rep(FHIR_STU3, JSON, defaultCharset(), Encodings.DEFAULT));

    Answer<KnowledgeCarrier> ans = new PlanDefinitionMetadataIntrospector()
        .applyNamedIntrospectDirect(PlanDefinitionMetadataIntrospector.id, kc, null);
    assertTrue(ans.isSuccess());

    return ans.flatOpt(x -> x.as(KnowledgeAsset.class)).orElseGet(Assertions::fail);
  }


}
