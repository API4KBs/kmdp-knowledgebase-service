package edu.mayo.kmdp.knowledgebase.simplifiers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.AbstractCompositeCarrier.ofMixedAggregate;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;

import ca.uhn.fhir.context.FhirContext;
import edu.mayo.kmdp.knowledgebase.KnowledgeBaseProvider;
import edu.mayo.kmdp.knowledgebase.simplifiers.fhir.stu3.FHIRSimplifier;
import edu.mayo.kmdp.knowledgebase.simplifiers.fhir.stu3.components.MetadataTrimmer;
import edu.mayo.kmdp.language.parsers.fhir.stu3.FHIR3Deserializer;
import edu.mayo.kmdp.util.JSonUtil;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import org.hl7.fhir.dstu3.model.Parameters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.AbstractCompositeCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;

class FHIRSimplifierTest {

  @Test
  void testSimplifierDirect() {
    Parameters caseData = loadCase()
        .flatOpt(kc -> kc.as(Parameters.class))
        .orElseGet(Assertions::fail);

    long l = estimateSize(caseData);
    new MetadataTrimmer().accept(caseData);
    long m = estimateSize(caseData);

    assertTrue(m < l);
  }

  @Test
  void testSimplifierWithAPI() {
    KnowledgeCarrier caseData = loadCase()
        .orElseGet(Assertions::fail);
    KnowledgeCarrier drugData = loadDrugDB()
        .orElseGet(Assertions::fail);
    CompositeKnowledgeCarrier ckc = ofMixedAggregate(Arrays.asList(caseData,drugData));

    KnowledgeBaseProvider kbp = new KnowledgeBaseProvider(null);
    FHIRSimplifier simplifier = new FHIRSimplifier(kbp);


    long l = caseData.as(Parameters.class).map(this::estimateSize).orElse(Long.MAX_VALUE);

    Parameters trimmed = kbp.initKnowledgeBase(ckc, null)
        .flatMap(ptr -> simplifier.applyRedact(ptr.getUuid(), ptr.getVersionTag(), null))
        .flatOpt(kc -> kc.as(Parameters.class))
        .orElseGet(Assertions::fail);

    long m = estimateSize(trimmed);
    assertTrue(m < l);
  }

  private Answer<KnowledgeCarrier> loadDrugDB() {
    InputStream is = FHIRSimplifierTest.class
        .getResourceAsStream("/simplifiers/rxnorm_categories_mock.json");
    return Answer.of(JSonUtil.readJson(is)
        .map(AbstractCarrier::ofTree));
  }

  private Answer<KnowledgeCarrier> loadCase() {
    InputStream is = FHIRSimplifierTest.class
        .getResourceAsStream("/simplifiers/afib_test_case.json");
    return new FHIR3Deserializer().applyLift(
        AbstractCarrier.of(is, rep(FHIR_STU3, JSON, Charset.defaultCharset(), Encodings.DEFAULT)),
        Abstract_Knowledge_Expression,
        codedRep(FHIR_STU3),
        null);
  }


  private long estimateSize(Parameters parameters) {
    return FhirContext.forDstu3().newJsonParser().encodeResourceToString(parameters).length();
  }
}
