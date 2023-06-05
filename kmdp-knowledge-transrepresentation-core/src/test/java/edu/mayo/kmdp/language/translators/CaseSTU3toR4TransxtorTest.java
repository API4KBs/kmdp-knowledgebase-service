package edu.mayo.kmdp.language.translators;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_R4;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;

import edu.mayo.kmdp.language.translators.fhir.stu3.ecase.CaseSTU3toR4Transrepresentator;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.IntegerType;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.StringType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;

class CaseSTU3toR4TransxtorTest {

  CaseSTU3toR4Transrepresentator translator = new CaseSTU3toR4Transrepresentator();

  @Test
  void testIntegration() {
    Parameters sourceCase = buildMockCase();

    var source = AbstractCarrier.ofAst(sourceCase, rep(FHIR_STU3));
    var result = translator.applyTransrepresent(source, codedRep(FHIR_R4), null);

    var resultCarrier = result.orElseGet(Assertions::fail);

    assertTrue(FHIR_R4.sameAs(resultCarrier.getRepresentation().getLanguage()));
    assertNull(resultCarrier.getRepresentation().getSerialization());

    org.hl7.fhir.r4.model.Bundle r4Case = resultCarrier.as(org.hl7.fhir.r4.model.Bundle.class)
        .orElseGet(Assertions::fail);

    assertFalse(r4Case.getEntry().isEmpty());
  }

  private Parameters buildMockCase() {
    Parameters p = new Parameters();
    p.addParameter()
        .setName("6bbf0b1e-063c-4163-9adf-8a7316d43cef")
        .setResource(new Bundle().addEntry(new BundleEntryComponent()
            .setResource(new Observation().setValue(new StringType("TEST")))));
    return p;
  }

}
