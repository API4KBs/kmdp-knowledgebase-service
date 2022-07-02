package edu.mayo.kmdp.knowledgebase.flatteners.fhir.stu3;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage._20210401.KnowledgeRepresentationLanguage.FHIR_STU3;

import ca.uhn.fhir.context.FhirContext;
import java.util.Comparator;
import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;

class PlanDefFlattenTest {

  PlanDefinition root;

  @BeforeEach
  void reset() {
    root = new PlanDefinition();
    root.setId("#001");
    root.setTitle("Master Plan");

    var child = new PlanDefinition();
    child.setId("#007");
    child.setTitle("My Recipe");

    var spec = new ActivityDefinition();
    spec.setId("#123");
    spec.setTitle("Deviled Details");

    root.addContained(child);
    root.getActionFirstRep().setDefinition(new Reference(child));

    child.addContained(spec);
    child.getActionFirstRep().setDefinition(new Reference(spec));
  }

  @Test
  void testReshapeContained() {
    assertEquals(1, root.getContained().size());

    var assetId = SemanticIdentifier.randomId();

    var kc = AbstractCarrier.ofAst(root)
        .withRepresentation(rep(FHIR_STU3))
        .withAssetId(assetId);

    new PlanDefinitionFlattener().innerFlatten(root, emptyList(), emptyList());

    assertEquals(2, root.getContained().size());

    var jp = FhirContext.forDstu3().newJsonParser();

    var ser1 = jp.encodeResourceToString(root);
    PlanDefinition rec1 = (PlanDefinition) jp.parseResource(ser1);
    var ser2 = jp.encodeResourceToString(rec1);
    PlanDefinition rec2 = (PlanDefinition) jp.parseResource(ser2);

    rec1.getContained().sort(Comparator.comparing(Resource::getId));
    rec2.getContained().sort(Comparator.comparing(Resource::getId));
    assertTrue(rec1.equalsDeep(rec2));
  }


}
