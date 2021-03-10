package edu.mayo.kmdp.knowledgebase.simplifiers.fhir.stu3.components;

import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.dstu3.model.Provenance;
import org.hl7.fhir.dstu3.model.Resource;

/**
 * Redactor that removes Provenance resources
 */
public class ProvenanceTrimmer extends AbstractFHIRRedactor {

  @Override
  public void accept(Resource resource) {
    trimProvenance(resource);
  }

  public void trimProvenance(Resource resource) {
    trimAllComponents(resource, true, this::trimProvenanceInternal);
  }

  public void trimProvenanceInternal(Resource resource) {
    List<ParametersParameterComponent> provs = new ArrayList<>();
    if (resource instanceof Parameters) {
      Parameters p = (Parameters) resource;
      for (ParametersParameterComponent key : p.getParameter()) {
        Resource root = key.getResource();
        boolean isProv = root instanceof Provenance;
        if (!isProv && root instanceof Parameters) {
          isProv = ((Parameters) root).getParameterFirstRep().getResource() instanceof Provenance;
        }
        if (isProv) {
          provs.add(key);
        }
      }
      provs.forEach(x -> x.setResource(null));
    }
  }

}
