package edu.mayo.kmdp.language.translators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.MVF_1_0;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;

import ca.uhn.fhir.context.FhirContext;
import edu.mayo.kmdp.language.TransrepresentationExecutor;
import edu.mayo.kmdp.language.parsers.mvf.v1_0.MVFParser;
import edu.mayo.kmdp.language.translators.mvf.fhir.stu3.MVFToFHIRTermsTranslator;
import java.nio.charset.Charset;
import java.util.List;
import org.hl7.fhir.dstu3.model.CodeSystem;
import org.hl7.fhir.dstu3.model.ConceptMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.TransxionApiInternal;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;

class MVFtoFHIRTranslatorTest {

  TransxionApiInternal tx =
      new TransrepresentationExecutor(List.of(new MVFToFHIRTermsTranslator()));

  static KnowledgeCarrier rawData = load("/mvf/mock.mvf.xml");

  @Test
  void testTranslator() {
    var ans =
        tx.applyTransrepresent(rawData, codedRep(FHIR_STU3, JSON, Charset.defaultCharset()), null)
            .orElseGet(Assertions::fail);
    assertTrue(ans.getExpression() instanceof String);

    var cs = FhirContext.forDstu3().newJsonParser().parseResource(CodeSystem.class, ans.asString()
        .orElseGet(Assertions::fail));
    assertFalse(cs.getContained().isEmpty());
    assertTrue(cs.getContained().get(0) instanceof ConceptMap);
  }

  @Test
  void testTranslationlogic() {
    var ans =
        tx.applyTransrepresent(rawData, codedRep(FHIR_STU3), null)
            .orElseGet(Assertions::fail);
    assertTrue(ans.getExpression() instanceof CodeSystem);
    var cs = (CodeSystem) ans.getExpression();

    testTranslatedModel(cs);
  }

  private void testTranslatedModel(CodeSystem cs) {
    assertEquals(22, cs.getConcept().size());

    var map = (ConceptMap) cs.getContained().get(0);
    var sctMap = map.getGroup().stream()
        .filter(g -> g.getTarget().contains("snomed"))
        .findFirst().orElseGet(Assertions::fail);
    assertEquals(8, sctMap.getElement().size());
    var csvMap = map.getGroup().stream()
        .filter(g -> g.getTarget().contains("clinicalsituation"))
        .findFirst().orElseGet(Assertions::fail);
    assertEquals(14, csvMap.getElement().size());

    for (var x : cs.getConcept()) {
      sctMap.getElement().stream()
          .filter(el -> el.getCode().equals(x.getCode()))
          .filter(el -> el.getTarget().get(0).getCode() != null)
          .findFirst()
          .or(() -> csvMap.getElement().stream()
              .filter(el -> el.getCode().equals(x.getCode()))
              .filter(el -> el.getTarget().get(0).getCode() != null)
              .findFirst())
          .orElseGet(Assertions::fail);
    }
  }

  @Test
  void testParser() {
    new MVFParser()
        .applyLift(rawData, Abstract_Knowledge_Expression, codedRep(MVF_1_0), null)
        .orElseGet(Assertions::fail);
  }

  private static KnowledgeCarrier load(String file) {
    var is = MVFtoFHIRTranslatorTest.class.getResourceAsStream(file);
    return AbstractCarrier.of(is)
        .withAssetId(SemanticIdentifier.randomId())
        .withArtifactId(SemanticIdentifier.randomId())
        .withRepresentation(rep(MVF_1_0, XML_1_1, Charset.defaultCharset(), Encodings.DEFAULT));
  }

}
