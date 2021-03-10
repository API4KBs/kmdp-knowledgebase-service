package edu.mayo.kmdp.knowledgebase.simplifiers.fhir.stu3.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Condition;
import org.hl7.fhir.dstu3.model.Medication;
import org.hl7.fhir.dstu3.model.MedicationStatement;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Procedure;
import org.hl7.fhir.dstu3.model.Resource;

/**
 * Redactor that rewrites codes from the same code system using a post-coordinated form
 *
 * e.g.
 *
 * {code : "c1", system : "S"}, {code : "c2", system : "S"},
 * ->
 * {code : "c1 + c2"}
 *
 * While the current implementation assumes that the codes are in 'or' (+),
 * the operator should be configurable to support 'and' (*) at a minimum
 */
public class CodingCompressor extends AbstractFHIRRedactor {

  @Override
  public void accept(Resource resource) {
    compactCodes(resource);
  }

  public void compactCodes(Resource resource) {
    trimAllComponents(resource, true, this::compactCodesInternal);
  }

  public void compactCodesInternal(Resource resource) {
    if (resource instanceof MedicationStatement) {
      MedicationStatement x = (MedicationStatement) resource;
      Medication med = (Medication) x.getContained().get(0);
      med.setCode(compactCodes(med.getCode()));
      med.getIngredient().forEach(ing -> {
        if (ing.getItem() instanceof CodeableConcept) {
          ing.setItem(compactCodes(ing.getItemCodeableConcept()));
        }
      });
    }
    if (resource instanceof Procedure) {
      Procedure x = (Procedure) resource;
      x.setCode(compactCodes(x.getCode()));
    }
    if (resource instanceof Observation) {
      Observation x = (Observation) resource;
      x.setCode(compactCodes(x.getCode()));
    }
    if (resource instanceof Condition) {
      Condition x = (Condition) resource;
      x.setCode(compactCodes(x.getCode()));
    }
  }

  private CodeableConcept compactCodes(CodeableConcept code) {
    Map<String, List<String>> x = new HashMap<>();
    code.getCoding().forEach(
        cd -> x.computeIfAbsent(cd.getSystem(),s -> new ArrayList<>()).add(cd.getCode())
    );
    CodeableConcept compact = new CodeableConcept();
    x.forEach((s,cs) -> compact.addCoding(new Coding().setCode(String.join(" + ", cs)).setSystem(s)));
    return compact;
  }


}
