package edu.mayo.kmdp.knowledgebase.flatteners.fhir.stu3;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.AbstractCompositeCarrier.ofUniformAggregate;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.randomId;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import java.nio.charset.Charset;
import org.hl7.fhir.dstu3.model.CodeSystem;
import org.hl7.fhir.dstu3.model.CodeSystem.ConceptDefinitionComponent;
import org.hl7.fhir.dstu3.model.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;

class CodeSystemFlattenerTest {

  final IParser parser = FhirContext.forDstu3().newJsonParser();

  @Test
  void testFlattenCodeSystems() {

    String nsURI = "http://codes.sys/tmp";

    CodeSystem cs1 = new CodeSystem()
        .setUrl(nsURI)
        .setName("Mock Code System 1")
        .addConcept(new ConceptDefinitionComponent()
            .setCode("c1"))
        .addConcept(new ConceptDefinitionComponent()
            .setCode("c2"));

    CodeSystem cs2 = new CodeSystem()
        .setUrl(nsURI)
        .setName("Mock Code System 2")
        .addConcept(new ConceptDefinitionComponent()
            .setCode("c3"))
        .addConcept(new ConceptDefinitionComponent()
            .setCode("c4"));

    CodeSystem cs3 = new CodeSystem()
        .setUrl(nsURI)
        .setName("Mock Code System 3")
        .addConcept(new ConceptDefinitionComponent()
            .setCode("c5"));

    KnowledgeCarrier kc1 = carry(cs1);
    KnowledgeCarrier kc2 = carry(cs2);
    KnowledgeCarrier kc3 = carry(cs3);
    CompositeKnowledgeCarrier ckc = ofUniformAggregate(asList(kc1, kc2, kc3));

    CodeSystem cs = new CodeSystemFlattener().flattenArtifact(ckc, null, null)
        .flatOpt(kc -> kc.as(CodeSystem.class))
        .orElseGet(Assertions::fail);

    assertEquals(nsURI, cs.getUrl());
    assertEquals(5, cs.getConcept().size());
  }

  private KnowledgeCarrier carry(Resource cs) {
    return AbstractCarrier.of(parser.encodeResourceToString(cs).getBytes())
        .withAssetId(randomId())
        .withArtifactId(randomId())
        .withRepresentation(rep(FHIR_STU3, JSON, Charset.defaultCharset(), Encodings.DEFAULT));
  }
}
