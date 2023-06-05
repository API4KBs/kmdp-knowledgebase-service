package edu.mayo.kmdp.language;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.mayo.kmdp.language.common.fhir.stu3.FHIRPlanDefinitionUtils;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.PlanDefinition.PlanDefinitionActionComponent;
import org.junit.jupiter.api.Test;

class FHIRUtilTest {

  @Test
  void testNestedPlanDefinitions() {
    PlanDefinition p1 = new PlanDefinition().setName("A");
    PlanDefinition p2 = new PlanDefinition().setName("B");
    PlanDefinition p3 = new PlanDefinition().setName("C");
    PlanDefinition p4 = new PlanDefinition().setName("D");

    p1.addContained(p2);
    p1.addContained(p3);
    p3.addContained(p4);

    Set<String> names = FHIRPlanDefinitionUtils.getNestedPlanDefs(p1)
        .map(PlanDefinition::getName)
        .collect(Collectors.toSet());

    assertEquals(
        new HashSet<>(Arrays.asList("A","B","C","D")),
        names);
  }

  @Test
  void testNestedActions() {
    PlanDefinition p1 = new PlanDefinition().setName("A");
    p1
        .addAction(new PlanDefinitionActionComponent()
            .setTitle("A")
            .addAction(new PlanDefinitionActionComponent()
                .setTitle("C"))
        )
        .addAction(new PlanDefinitionActionComponent()
            .setTitle("B")
        .addAction(new PlanDefinitionActionComponent()
            .setTitle("D"))
        );

    Set<String> names = FHIRPlanDefinitionUtils.getSubActions(p1)
        .map(PlanDefinitionActionComponent::getTitle)
        .collect(Collectors.toSet());

    assertEquals(
        new HashSet<>(Arrays.asList("A","B","C","D")),
        names);
  }

  @Test
  void testDeepNestedActions() {
    PlanDefinition p1 = new PlanDefinition().setName("A");
    p1.addAction(new PlanDefinitionActionComponent()
        .setTitle("A")
        .addAction(new PlanDefinitionActionComponent()
            .setTitle("C"))
    );

    PlanDefinition p2 = new PlanDefinition().setName("B");
    p2.addAction(new PlanDefinitionActionComponent()
        .setTitle("B")
        .addAction(new PlanDefinitionActionComponent()
            .setTitle("D"))
    );

    p1.addContained(p2);

    Set<String> names = FHIRPlanDefinitionUtils.getDeepNestedSubActions(p1)
        .map(PlanDefinitionActionComponent::getTitle)
        .collect(Collectors.toSet());

    assertEquals(
        new HashSet<>(Arrays.asList("A","B","C","D")),
        names);
  }


}
