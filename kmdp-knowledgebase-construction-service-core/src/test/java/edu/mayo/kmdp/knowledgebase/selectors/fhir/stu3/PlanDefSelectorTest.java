package edu.mayo.kmdp.knowledgebase.selectors.fhir.stu3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeprocessingtechnique.KnowledgeProcessingTechniqueSeries.Query_Technique;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;

import edu.mayo.kmdp.knowledgebase.KnowledgeBaseProvider;
import java.net.URI;
import org.hl7.fhir.dstu3.model.CodeType;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DataRequirement;
import org.hl7.fhir.dstu3.model.DataRequirement.DataRequirementCodeFilterComponent;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.PlanDefinition.PlanDefinitionActionComponent;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.dstu3.model.ValueSet.ValueSetExpansionContainsComponent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeprocessingtechnique.KnowledgeProcessingTechniqueSeries;

public class PlanDefSelectorTest {

  private static final String MOCK_CODE_SYSTEM = "http://mock.terms";

  @Test
  void testSelectorOnCarrierWithURL() {

    KnowledgeBaseProvider kb = new KnowledgeBaseProvider(null)
        .withNamedSelector(PlanDefSelector::new);
    ValueSet vs = kb.initKnowledgeBase(getPlanDefinition(), null)
        .flatMap(ptr -> kb.namedSelect(
            ptr.getUuid(),
            ptr.getVersionTag(),
            PlanDefSelector.id,
            PlanDefSelector.pivotQuery(URI.create(MOCK_CODE_SYSTEM))))
        .flatMap(ptr -> kb.getKnowledgeBaseManifestation(ptr.getUuid(), ptr.getVersionTag()))
        .flatOpt(kc -> kc.as(ValueSet.class))
        .orElseGet(Assertions::fail);
    assertEquals(1, vs.getExpansion().getContains().size());

    ValueSetExpansionContainsComponent cd = vs.getExpansion().getContainsFirstRep();
    assertFalse(cd.getExtension().isEmpty());
  }

  private KnowledgeCarrier getPlanDefinition() {
    CodeableConcept cd = new CodeableConcept()
        .addCoding(new Coding()
            .setSystem(MOCK_CODE_SYSTEM)
            .setCode("1234")
            .setDisplay("Nonsense"));

    DataRequirement dataReq = new DataRequirement()
        .addCodeFilter(new DataRequirementCodeFilterComponent()
            .addValueCodeableConcept(cd));
    dataReq.addExtension(
        KnowledgeProcessingTechniqueSeries.schemeSeriesIdentifier.getNamespaceUri().toString(),
        new CodeType(Query_Technique.getTag()));

    PlanDefinition pd = new PlanDefinition()
        .addAction(new PlanDefinitionActionComponent()
            .addInput(dataReq));
    return AbstractCarrier.of(pd, Abstract_Knowledge_Expression)
        .withHref(URI.create("http://www.mayoclinic.org/"));
  }
}
