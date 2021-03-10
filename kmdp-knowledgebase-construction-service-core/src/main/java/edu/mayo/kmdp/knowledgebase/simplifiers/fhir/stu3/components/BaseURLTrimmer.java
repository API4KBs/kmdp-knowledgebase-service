package edu.mayo.kmdp.knowledgebase.simplifiers.fhir.stu3.components;

import org.hl7.fhir.dstu3.model.MedicationStatement;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Procedure;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;

/**
 * Trimmer that removes a fixed baseURL from References that should only contain relative paths
 *
 * e.g. http://foo.bar/server/Observation?id=123 -> /Observation?id=123
 */
public class BaseURLTrimmer extends AbstractFHIRRedactor {

  private static final String PLACEHOLDER = "";

  private String baseURL = "https://peptest.api.mayo.edu/epicfhir/vexternal/api/FHIR/STU3/";

  public BaseURLTrimmer() {
    // use default base URL
  }

  public BaseURLTrimmer(String baseURL) {
    this.baseURL = baseURL;
  }

  @Override
  public void accept(Resource resource) {
    trimFHIRServerURL(resource);
  }

  public void trimFHIRServerURL(Resource resource) {
    trimAllComponents(resource, true, this::trimFHIRServerURLInternal);
  }

  public void trimFHIRServerURLInternal(Resource resource) {
    if (resource instanceof Observation) {
      Observation o = (Observation) resource;
      o.getBasedOn().forEach(this::pruneReference);
      pruneReference(o.getContext());
      pruneReference(o.getSubject());
      pruneReference(o.getSpecimen());
      o.getPerformer().forEach(this::pruneReference);
    }
    if (resource instanceof Procedure) {
      Procedure p = (Procedure) resource;
      pruneReference(p.getContext());
      pruneReference(p.getSubject());
    }
    if (resource instanceof MedicationStatement) {
      MedicationStatement m = (MedicationStatement) resource;
      pruneReference(m.getInformationSource());
      m.getBasedOn().forEach(this::pruneReference);
    }

  }

  private void pruneReference(Reference reference) {
    if (reference.hasReference()) {
      reference.setReference(reference.getReference().replace(baseURL, PLACEHOLDER));
    }
  }

}
