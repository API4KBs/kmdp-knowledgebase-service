package edu.mayo.kmdp.terms;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;

import ca.uhn.fhir.context.FhirContext;
import java.net.URI;
import java.util.UUID;
import org.hl7.fhir.dstu3.model.CodeSystem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;

class ContentNegotiationTest {

  @Test
  void testCStoHTML() {
    var testCS = ContentNegotiationTest.class.getResourceAsStream("/test.cs.json");
    CodeSystem cs = FhirContext.forDstu3().newJsonParser().parseResource(CodeSystem.class, testCS);
    UUID csId = UUID.fromString(URI.create(cs.getIdentifier().getValue()).getFragment());
    String csVersion = cs.getVersion();

    var server = new TermsFHIRFacade() {
      @Override
      void init() {
        schemeIndex.put(csId, cs);
        online.set(true);
      }
    };
    server.init();

    var html = server.getVocabulary(csId, csVersion, codedRep(HTML))
        .flatOpt(AbstractCarrier::asString)
        .orElseGet(Assertions::fail);

    assertTrue(html.contains(
        "<a href='http://ontology.mayo.edu/ontologies/clinicalsituationontology/"
            + "fd0e8faa-7dd2-499d-b0c6-eb176822485f'>"));
  }


}
