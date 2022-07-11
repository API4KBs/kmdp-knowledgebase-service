package edu.mayo.kmdp.knowledgebase.introspectors.fhir.stu3;

import static edu.mayo.kmdp.util.Util.uuid;
import static java.nio.charset.Charset.defaultCharset;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultArtifactId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultCarrierUUID;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultSurrogateUUID;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Terminology_Ontology_And_Assertional_KBs;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Value_Set;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;

import edu.mayo.kmdp.util.DateTimeUtil;
import java.io.InputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;

class ValuesetIntrospectorTest {


  @Test
  void testValueSetIntrospect() {
    InputStream is = ValuesetIntrospectorTest.class.getResourceAsStream(
        "/introspectors/fhir/stu3/example.valueset.json");
    KnowledgeCarrier kc = AbstractCarrier.of(is)
        .withRepresentation(rep(FHIR_STU3, JSON, defaultCharset(), Encodings.DEFAULT));

    Answer<KnowledgeCarrier> ans = new ValueSetMetadataIntrospector()
        .applyNamedIntrospectDirect(ValueSetMetadataIntrospector.OPERATOR_ID, kc, null);
    assertTrue(ans.isSuccess());

    KnowledgeAsset surr = ans.flatOpt(x -> x.as(KnowledgeAsset.class)).orElseGet(Assertions::fail);
    assertNotNull(surr.getAssetId());
    assertEquals(uuid("http://hl7.org/fhir/ValueSet/example-extensional").toString(),
        surr.getAssetId().getTag());
    assertEquals("20150622.0.0", surr.getAssetId().getVersionTag());
    assertEquals(DateTimeUtil.parseDate("2015-06-22"), surr.getAssetId().getEstablishedOn());

    assertTrue(Value_Set.isAnyOf(surr.getFormalType()));
    assertTrue(Terminology_Ontology_And_Assertional_KBs.isAnyOf(surr.getFormalCategory()));

    assertEquals(1, surr.getCarriers().size());
    assertEquals(
        defaultCarrierUUID(surr.getAssetId(), FHIR_STU3),
        surr.getCarriers().get(0).getArtifactId().getUuid()
    );

    SyntacticRepresentation rep = surr.getCarriers().get(0).getRepresentation();
    assertTrue(FHIR_STU3.sameAs(rep.getLanguage()));
    assertTrue(JSON.sameAs(rep.getFormat()));
    assertNotNull(rep.getCharset());
    assertNotNull(rep.getEncoding());

    assertEquals("0.0.0", surr.getCarriers().get(0).getArtifactId().getVersionTag());
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


  @Test
  void testValueSetIntrospectIdentifiers() {
    InputStream is = ValuesetIntrospectorTest.class.getResourceAsStream(
        "/introspectors/fhir/stu3/example2.valueset.json");
    KnowledgeCarrier kc = AbstractCarrier.of(is)
        .withRepresentation(rep(FHIR_STU3, JSON, defaultCharset(), Encodings.DEFAULT));

    Answer<KnowledgeCarrier> ans = new ValueSetMetadataIntrospector()
        .applyNamedIntrospectDirect(ValueSetMetadataIntrospector.OPERATOR_ID, kc, null);
    assertTrue(ans.isSuccess());

    KnowledgeAsset surr = ans.flatOpt(x -> x.as(KnowledgeAsset.class)).orElseGet(Assertions::fail);
    assertNotNull(surr.getAssetId());
    assertEquals(uuid("2.16.840.1.100000.3.2.11.1.100").toString(),
        surr.getAssetId().getTag());
    assertEquals("0.0.0-revision1", surr.getAssetId().getVersionTag());

    assertTrue(surr.getSecondaryId().stream()
        .anyMatch(rid -> {
          var tagMatch = "2.16.840.1.100000.3.2.11.1.100".equals(rid.getTag());
          var sysMatch = "http://terms.my.edu/valueset".equals(rid.getNamespaceUri().toString());
          return tagMatch && sysMatch;
        }));

    assertEquals(defaultArtifactId(surr.getAssetId(), FHIR_STU3).getTag(),
        surr.getCarriers().get(0).getArtifactId().getTag());
  }

}
