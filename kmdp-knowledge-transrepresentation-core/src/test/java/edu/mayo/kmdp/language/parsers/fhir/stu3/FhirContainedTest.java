package edu.mayo.kmdp.language.parsers.fhir.stu3;

import static org.junit.jupiter.api.Assertions.fail;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.mayo.kmdp.util.JSonUtil;
import edu.mayo.kmdp.util.StreamUtil;
import java.util.function.Function;
import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.Reference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FhirContainedTest {

  FhirContext ctx = FhirContext.forDstu3();

  PlanDefinition root;
  PlanDefinition child;
  ActivityDefinition spec;

  boolean log = false;

  @BeforeEach
  void reset() {
    root = new PlanDefinition();
    root.setId("#001");
    root.setTitle("Master Plan");

    child = new PlanDefinition();
    child.setId("#007");
    child.setTitle("My Recipe");

    spec = new ActivityDefinition();
    spec.setId("#123");
    spec.setTitle("Deviled Details");

    System.err.println("\n");
  }


  @Test
  void testNativeDeserializationWithReferences() {
    root.addContained(child);
    root.getActionFirstRep().setDefinition(new Reference(child));

    child.addContained(spec);
    child.getActionFirstRep().setDefinition(new Reference(spec));

    System.err.println("********** Linked References");

    try {
      check(p -> p);
    } catch (CorruptedResourceException e) {
      fail(e);
    } catch (LossOfInformationException e) {
      // success
    }
  }


  @Test
  void testNativeDeserializationWithReferenceIDs() {
    root.addContained(child);
    root.getActionFirstRep().setDefinition(new Reference().setReference(child.getId()));

    child.addContained(spec);
    child.getActionFirstRep().setDefinition(new Reference().setReference(spec.getId()));

    System.err.println("********** Foreign Key");
    try {
      check(p -> p);
      fail();
    } catch (CorruptedResourceException e) {
      fail(e);
    } catch (LossOfInformationException e) {
      // success
    }
  }

  @Test
  void testNativeDeserializationWithReferencesCopy() {
    root.addContained(child);
    root.getActionFirstRep().setDefinition(new Reference(child));

    child.addContained(spec);
    child.getActionFirstRep().setDefinition(new Reference(spec));

    System.err.println("********** Linked References, Copy");
    try {
      check(PlanDefinition::copy);
      fail();
    } catch (CorruptedResourceException e) {
      // success
    } catch (Exception e) {
      fail(e);
    }
  }

  @Test
  void testNativeDeserializationWithReferenceIDsCopy() {
    root.addContained(child);
    root.getActionFirstRep().setDefinition(new Reference().setReference(child.getId()));

    child.addContained(spec);
    child.getActionFirstRep().setDefinition(new Reference().setReference(spec.getId()));

    System.err.println("********** Foreign Keys, Copy");
    try {
      check(PlanDefinition::copy);
    } catch (CorruptedResourceException e) {
      fail(e);
    } catch (LossOfInformationException e) {
      // success
    }
  }


  private void check(Function<PlanDefinition, PlanDefinition> processor)
      throws CorruptedResourceException, LossOfInformationException {
    IParser jp = ctx.newJsonParser().setPrettyPrint(true);

    root = processor.apply(root);

    checkStruct(root, "Pre-Built Object");

    String s1 = jp.encodeResourceToString(root);
    checkString(s1);
    log(s1);

    var temp = (PlanDefinition) jp.parseResource(s1);
    temp = processor.apply(temp);
    checkStruct(temp, "First Round-trip");

    String s2 = jp.encodeResourceToString(temp);
    checkString(s2);
    log(s2);

    var rec = (PlanDefinition) jp.parseResource(s2);
    rec = processor.apply(rec);
    checkStruct(rec, "Next Reiteration");

    String s3 = jp.encodeResourceToString(rec);
    checkString(s3);
    log(s3);
  }

  private void checkString(String str) {
    var jn = JSonUtil.readJson(str).orElseGet(Assertions::fail);
    var rootContained = (ArrayNode) jn.get("contained");

    int numContained = -1;
    int numChildContained = -1;

    if (rootContained != null) {
      numContained = rootContained.size();
      for (int j = 0; j < rootContained.size(); j++) {
        var cn = (ObjectNode) rootContained.get(j);
        boolean isPlanDef = "PlanDefinition".equals(cn.get("resourceType").toString());
        if (isPlanDef) {
          var childContained = (ArrayNode) cn.get("contained");
          numChildContained = childContained.size();
        }
      }
    }

    System.err.printf("\t\t\t Root #numContained = %d,"
            + "\t\t Child #numContained = %d\n",
        numContained, numChildContained);
  }

  private void checkStruct(PlanDefinition root, String phase)
      throws CorruptedResourceException, LossOfInformationException {
    int rootContained = root.getContained().size();

    boolean isRefSet = root.getActionFirstRep().getDefinition().getResource() != null;

    var child = root.getContained().stream()
        .filter(r -> r.getId().equals("#007"))
        .flatMap(StreamUtil.filterAs(PlanDefinition.class))
        .findFirst();

    int childContained = child.map(c -> c.getContained().size()).orElse(-1);

    boolean isChildSet = child.map(c -> c.getActionFirstRep().getDefinition().getResource() != null)
        .orElse(false);

    System.err.printf(phase + "\t Root #numContained = %d, linkedRef = %s "
            + "\t\t Child #numContained = %d, linkedRef = %s\n",
        rootContained, isRefSet, childContained, isChildSet);

    if (rootContained + childContained <= 0) {
      throw new CorruptedResourceException();
    } else if (childContained <= 0) {
      throw new LossOfInformationException();
    }
  }

  private void log(String str) {
    if (log) {
      System.err.println(str);
    }
  }

  private static class CorruptedResourceException extends Exception {

  }

  private static class LossOfInformationException extends Exception {

  }


}
