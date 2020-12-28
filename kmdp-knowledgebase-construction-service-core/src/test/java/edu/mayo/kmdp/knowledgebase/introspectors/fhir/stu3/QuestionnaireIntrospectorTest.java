package edu.mayo.kmdp.knowledgebase.introspectors.fhir.stu3;

import static java.nio.charset.Charset.defaultCharset;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultCarrierUUID;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultSurrogateUUID;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Structured_Information_And_Data_Capture_Models;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Questionnaire;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;

import java.io.InputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;

class QuestionnaireIntrospectorTest {

  @Test
  void testQuestionnaireIntrospect() {
    InputStream is = QuestionnaireIntrospectorTest.class.getResourceAsStream(
        "/introspectors/fhir/stu3/mock.questionnaire.json");
    KnowledgeCarrier kc = AbstractCarrier.of(is)
        .withRepresentation(rep(FHIR_STU3, JSON, defaultCharset(), Encodings.DEFAULT));

    Answer<KnowledgeCarrier> ans = new QuestionnaireMetadataIntrospector()
        .applyNamedIntrospectDirect(QuestionnaireMetadataIntrospector.id, kc, null);
    assertTrue(ans.isSuccess());

    KnowledgeAsset surr = ans.flatOpt(x -> x.as(KnowledgeAsset.class)).orElseGet(Assertions::fail);
    assertNotNull(surr.getAssetId());
    assertEquals("b6b047aa-0a4b-4cb7-8e4d-45a58280fbea", surr.getAssetId().getTag());
    assertEquals("0.0.0", surr.getAssetId().getVersionTag());
    assertTrue(Questionnaire.isAnyOf(surr.getFormalType()));
    assertTrue(Structured_Information_And_Data_Capture_Models.isAnyOf(surr.getFormalCategory()));

    assertEquals(1, surr.getCarriers().size());
    assertEquals(
        defaultCarrierUUID(surr.getAssetId(), FHIR_STU3),
        surr.getCarriers().get(0).getArtifactId().getUuid()
    );
    assertEquals("1.0.0", surr.getCarriers().get(0).getArtifactId().getVersionTag());
    assertTrue(FHIR_STU3
        .sameAs(surr.getCarriers().get(0).getRepresentation().getLanguage()));

    assertEquals(1, surr.getSurrogate().size());
    assertEquals(
        defaultSurrogateUUID(surr.getAssetId(), Knowledge_Asset_Surrogate_2_0),
        surr.getSurrogate().get(0).getArtifactId().getUuid()
    );
    assertTrue(Knowledge_Asset_Surrogate_2_0
        .sameAs(surr.getSurrogate().get(0).getRepresentation().getLanguage()));
  }

}
