package edu.mayo.kmdp.knowledgebase.flatteners.fhir.stu3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.ofAst;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.AbstractCompositeCarrier.ofUniformAnonymousComposite;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;

import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.PrimitiveType;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.dstu3.model.ValueSet.ValueSetExpansionContainsComponent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.CompositionalApiInternal._flattenArtifact;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;

class ValueSetFlattenerTest {

  _flattenArtifact flattener = new ValueSetFlattener();

  @Test
  void testHybridValueSetFlattening() {
    var v1 = getExpandedVS();
    var v2 = getComposedVS();
    var v3 = getAnonVS();
    var components = List.of(v1, v2, v3);

    var flat = flattener.flattenArtifact(
            ofUniformAnonymousComposite(components), null, null)
        .orElseGet(Assertions::fail);

    assertTrue(FHIR_STU3.sameAs(flat.getRepresentation().getLanguage()));
    assertTrue(Abstract_Knowledge_Expression.sameAs(flat.getLevel()));

    assertTrue(components.stream()
        .map(KnowledgeCarrier::getAssetId)
        .noneMatch(id -> id.asKey().equals(flat.getAssetId().asKey())));

    ValueSet flatVS = flat.as(ValueSet.class).orElseGet(Assertions::fail);

    assertEquals(1, flatVS.getCompose().getInclude().size());
    var vsReferences = flatVS.getCompose().getIncludeFirstRep();
    assertEquals(2, vsReferences.getValueSet().stream()
        .map(PrimitiveType::getValue).collect(Collectors.toSet()).size());

    assertFalse(flatVS.getExpansion().getContains().isEmpty());

    var codes = flatVS.getExpansion().getContains();
    assertEquals(4, codes.size());
    assertEquals(3,
        codes.stream().filter(cd -> "urn:acme".equals(cd.getSystem())).count());

    var codeList = codes.stream()
        .map(ValueSetExpansionContainsComponent::getCode)
        .collect(Collectors.toList());
    assertEquals(4, codeList.size());
    assertTrue(codeList.containsAll(List.of("1", "2", "3", "4")));
  }

  private KnowledgeCarrier getExpandedVS() {
    ValueSet vs = new ValueSet();
    vs.setId("vs1");
    vs.setUrl("http://acme.org/vs/1");
    vs.getExpansion().addContains()
        .setCode("1")
        .setSystem("urn:acme")
        .setDisplay("Mock 1");
    return ofAst(vs)
        .withRepresentation(rep(FHIR_STU3));
  }

  private KnowledgeCarrier getAnonVS() {
    ValueSet vs = new ValueSet();
    vs.setId("vs2");
    vs.getExpansion().addContains()
        .setCode("2")
        .setSystem("urn:acme")
        .setDisplay("Mock 2");
    vs.getExpansion().addContains()
        .setCode("1")
        .setSystem("urn:acme")
        .setDisplay("Mock 1");
    return ofAst(vs)
        .withRepresentation(rep(FHIR_STU3));
  }

  private KnowledgeCarrier getComposedVS() {
    ValueSet vs = new ValueSet();
    vs.setId("vs3");
    vs.setUrl("http://acme.org/vs/2");
    var cs1 = vs.getCompose().addInclude()
        .setSystem("urn:acme");
    cs1.addConcept()
        .setCode("2")
        .setDisplay("Mock 2");
    cs1.addConcept()
        .setCode("3")
        .setDisplay("Mock 3");
    var cs2 = vs.getCompose().addInclude()
        .setSystem("urn:bogus");
    cs2.addConcept()
        .setCode("4")
        .setDisplay("Mock 4");
    return ofAst(vs)
        .withRepresentation(rep(FHIR_STU3));
  }

}
