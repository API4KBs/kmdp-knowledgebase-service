package edu.mayo.kmdp.knowledgebase.simplifiers.fhir.stu3.components;

import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.Resource;

/**
 * Redactor that removes the 'Meta' header, as well as the auto-generated HTML Narrative
 */
public class MetadataTrimmer extends AbstractFHIRRedactor {

  @Override
  public void accept(Resource resource) {
    trimMetadata(resource);
  }

  public void trimMetadata(Resource resource) {
    trimAllComponents(resource, true, this::trimMetadataInternal);
  }

  public void trimMetadataInternal(Resource resource) {
    if (resource.getMeta() != null) {
      resource.setMeta(null);
    }
    if (resource.getImplicitRules() != null) {
      resource.setImplicitRules(null);
    }
    if (resource instanceof DomainResource) {
      DomainResource d = (DomainResource) resource;
      d.setText(null);
    }
  }

}
