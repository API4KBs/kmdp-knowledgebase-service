package edu.mayo.kmdp.language.translators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import edu.mayo.kmdp.language.translators.fhir.stu3.ecase.CaseConverter;
import java.util.Optional;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Composition;
import org.junit.jupiter.api.Test;

class CaseConverterTest {

  static FhirContext stu3ctx = FhirContext.forDstu3();
  static FhirContext r4ctx = FhirContext.forR4();

  @Test
  void testCaseConversion() {
    var stu3Case = (Parameters) stu3ctx.newJsonParser().parseResource(CaseConverterTest.class.getResourceAsStream("/fhir.stu3/test-hld-params.json"));
    var r4Bundle = new CaseConverter().convertCase(stu3Case);
    var r4 = r4ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(r4Bundle);
    assertNotNull(r4Bundle);
    assertEquals(4, r4Bundle.getEntry().size());

  }

  @Test
  void testCaseConversionAndDeduplication() {
    var stu3Case = (Parameters) stu3ctx.newJsonParser().parseResource(CaseConverterTest.class.getResourceAsStream("/fhir.stu3/test-htn-params.json"));
    var r4Bundle = new CaseConverter().convertCase(stu3Case);
    var r4 = r4ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(r4Bundle);
    assertNotNull(r4Bundle);
    assertEquals(55, r4Bundle.getEntry().size());
    assertEquals(28, ( (Composition)r4Bundle.getEntry().get(54).getResource()).getSection().size());
    assertNotNull(r4Bundle.getEntry().get(0).getResource().getMeta());

    Optional<BundleEntryComponent> patientResource = r4Bundle.getEntry().stream().filter(x -> x.getResource().getResourceType().toString().equals("Patient")).findFirst();
    assertTrue(patientResource.isPresent());
    assertEquals(2, r4Bundle.getEntry().get(34).getResource().getMeta().getTag().size());
  }

}
