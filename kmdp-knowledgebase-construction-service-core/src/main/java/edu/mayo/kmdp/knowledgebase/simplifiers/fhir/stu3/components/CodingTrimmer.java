package edu.mayo.kmdp.knowledgebase.simplifiers.fhir.stu3.components;

import org.hl7.fhir.dstu3.model.Condition;
import org.hl7.fhir.dstu3.model.Medication;
import org.hl7.fhir.dstu3.model.MedicationStatement;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Procedure;
import org.hl7.fhir.dstu3.model.Resource;

/**
 * Drastic Redactor that removes all the "code : Code[x]" from FHIR resources
 */
public class CodingTrimmer extends AbstractFHIRRedactor {


  @Override
  public void accept(Resource resource) {
    trimCodes(resource);
  }

  public void trimCodes(Resource resource) {
    trimAllComponents(resource, true, this::trimCodesInternal);
  }

  public void trimCodesInternal(Resource resource) {
    if (resource instanceof MedicationStatement) {
      MedicationStatement x = (MedicationStatement) resource;
      Medication med = (Medication) x.getContained().get(0);
      med.setCode(null);
      med.getIngredient().forEach(ing -> ing.setItem(null));
    }
    if (resource instanceof Procedure) {
      Procedure x = (Procedure) resource;
      x.setCode(null);
    }
    if (resource instanceof Observation) {
      Observation x = (Observation) resource;
      x.setCode(null);
    }
    if (resource instanceof Condition) {
      Condition x = (Condition) resource;
      x.setCode(null);
    }
  }

}
