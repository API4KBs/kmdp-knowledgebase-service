package edu.mayo.kmdp.knowledgebase.simplifiers.fhir.stu3.components;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Resource;

/**
 * Redactor that removes empty bundles with no entries and total = 0
 *
 */
public class EmptyBundleTrimmer extends AbstractFHIRRedactor {


  @Override
  public void accept(Resource resource) {
    trimBundle(resource);
  }

  public void trimBundle(Resource resource) {
    trimAllComponents(resource, true, this::trimBundleInternal);
  }

  public void trimBundleInternal(Resource resource) {
    if (resource instanceof Parameters){
      Parameters p = (Parameters) resource;
      p.getParameter().forEach(part -> {
        if (part.getResource() instanceof Bundle) {
          Bundle b = (Bundle) part.getResource();
          if (b.getEntry().isEmpty()) {
            part.setResource(null);
          }
        }
      });
    }
    if (resource instanceof Bundle) {
      Bundle b = (Bundle) resource;
      b.getEntry().forEach(entry -> entry.setFullUrl(null));
    }
  }

}
