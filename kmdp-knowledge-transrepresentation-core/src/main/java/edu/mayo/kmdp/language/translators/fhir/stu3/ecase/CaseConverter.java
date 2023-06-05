package edu.mayo.kmdp.language.translators.fhir.stu3.ecase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hl7.fhir.convertors.VersionConvertor_30_40;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Composition.CompositionStatus;
import org.hl7.fhir.r4.model.Composition.SectionComponent;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Reference;

public class CaseConverter {

  private static final String ONTOLOGYURI = "https://ontology.mayo.edu/taxonomies/clinicalsituations";
  private static final String PARAMETERS = "Parameters";
  private static final String PATIENT = "Patient";
  private static final String BUNDLE = "Bundle";

  public Bundle convertCase(Parameters stu3Case) {
    if (stu3Case == null) {
      return null;
    }

    Bundle bun = new Bundle();
    bun.setId(UUID.randomUUID().toString());
    bun.setType(BundleType.COLLECTION);

    // Get Resources out of Params and into Bundle - Run through version converter
    List<ParametersParameterComponent> params = stu3Case.getParameter().stream()
        .collect(Collectors.toList());

    for (ParametersParameterComponent param : params) {
      String concept = param.getName();
      if (!param.hasPart() && param.getResource() != null) {
        Collection<Resource> extractedResources = extractResources(param);
        addToBundle(bun, extractedResources, concept);
      } else if (param.hasPart()) {
        for (ParametersParameterComponent p : param.getPart()) {
          if (p.getResource() != null) {
            Collection<Resource> extractedResources = extractResources(p);
            addToBundle(bun, extractedResources, concept);
          }
        }
      }
    }
    //Run through again and build composition
    buildComposition(bun, params);

    return bun;
  }

  private void buildComposition(Bundle bun, List<ParametersParameterComponent> params) {
    Composition comp = new Composition().setStatus(CompositionStatus.FINAL);

    for (ParametersParameterComponent p : params) {
      String paramCode = p.getName();

      if (!p.hasPart() && p.getResource() != null && !p.fhirType().equals(PARAMETERS)) {
        extractAndAddResources(p, comp, paramCode);
      } else if (p.hasPart()) {
        // TODO: Do we want to name these with the concept id or the service profile id or both?
        for (ParametersParameterComponent part : p.getPart()) {
          if (part.getResource() != null) {
            extractAndAddResources(part, comp, paramCode);
          }
        }
      }
    }
    bun.addEntry(new BundleEntryComponent().setResource(comp));
  }

  private void addResourceToComposition(Collection<Resource> extractedResources, Composition comp,
      String concept) {
    if (!extractedResources.isEmpty()) {
      SectionComponent sc = new SectionComponent().setCode(
          new CodeableConcept().addCoding(
              new Coding().setSystem(ONTOLOGYURI).setCode(concept)));
      for (Resource r : extractedResources) {
        if (r != null && r.hasId()) {
          Reference ref = new Reference().setReference(r.getId());
          sc.addEntry(ref);
        }
      }
      comp.addSection(sc);
    }
  }

  private void extractAndAddResources(ParametersParameterComponent paramComponent, Composition comp,
      String paramCode) {
    Collection<Resource> extractedResources = extractResources(paramComponent);
    if (extractedResources != null && !extractedResources.stream()
        .allMatch(r -> r.fhirType().equals(PARAMETERS))) {
      addResourceToComposition(extractedResources, comp, paramCode);
    }
  }

  private Collection<Resource> extractResources(ParametersParameterComponent pc) {
    String type = pc.getResource().fhirType();
    if (pc == null) {
      return null;
    }
    if (type.equals(BUNDLE)) {
      return extractResourceFromBundle(
          (org.hl7.fhir.dstu3.model.Bundle) pc.getResource());
    }
    if (type.equals(PATIENT) || type.equals(PARAMETERS)) {
      return extractResourceFromParameter(pc);
    }
    return null;
  }

  private Collection<Resource> extractResourceFromParameter(ParametersParameterComponent resource) {
    List<Resource> resources = new ArrayList<>();
    resources.add(resource.getResource());
    return resources;
  }

  private Collection<Resource> extractResourceFromBundle(org.hl7.fhir.dstu3.model.Bundle resource) {
    List<Resource> resources = new ArrayList<>();
    if (resource.hasEntry()) {
      for (org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent r : resource.getEntry()) {
        resources.add(r.getResource());
      }
    }
    return resources;
  }

  private org.hl7.fhir.r4.model.Resource convertResource(Resource stu3Resource) {
    return VersionConvertor_30_40.convertResource(stu3Resource, true);
  }

  private void addToBundle(Bundle bun, Collection<Resource> resources, String concept) {
    // Tag each resource with the Concept
    //TODO - Probably don't want to just skip null resources... How do we want to represent the inability to resolve something?
    if (resources != null) {
      for (Resource r : resources) {
        // if Resource doesn't have an ID add one, if it does then check to see if it's already in bundle.  If so add meta tag to existing resource, otherwise add new resource
        if (!r.hasId()) {
          r.setId(UUID.randomUUID().toString());
        }
        Optional<BundleEntryComponent> x = bun.getEntry().stream()
            .filter(g -> g.getResource().getId().equals(r.getId()))
            .findFirst();
        if (x.isPresent()) {
          x.get().getResource().getMeta().addTag((
              new Coding().setSystem(ONTOLOGYURI).setCode(concept)));
        } else {
          org.hl7.fhir.r4.model.Resource res = convertResource(r);
          res.setMeta(
              new Meta().addTag(
                  new Coding().setSystem(ONTOLOGYURI).setCode(concept)));
          bun.addEntry(new BundleEntryComponent().setResource(res));
        }

      }
    }

  }
}
