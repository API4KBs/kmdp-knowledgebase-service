package edu.mayo.kmdp.knowledgebase.simplifiers.fhir.stu3.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.ImagingStudy;
import org.hl7.fhir.dstu3.model.ImagingStudy.ImagingStudySeriesComponent;
import org.hl7.fhir.dstu3.model.Resource;

/**
 * Redactor that compacts distinct ImagingSeries resources with the same patient and accession
 * into a single ImagingSeries Resource with one 'series' entry per original resource
 */
public class ImagingSeriesCompressor extends AbstractFHIRRedactor {


  @Override
  public void accept(Resource resource) {
    compactImagingSeries(resource);
  }

  public void compactImagingSeries(Resource resource) {
    trimAllComponents(resource, false, this::compactImagingSeriesInternal);
  }

  private void compactImagingSeriesInternal(Resource resource) {
    if (resource instanceof Bundle) {
      Bundle b = (Bundle) resource;
      if (b.getTotal() > 0
          && b.getEntry().size() > 1
          && b.getEntry().get(0).getResource() instanceof ImagingStudy) {

        Map<String, List<ImagingStudy>> byAccession = new HashMap<>();
        b.getEntry().stream()
            .map(BundleEntryComponent::getResource)
            .map(ImagingStudy.class::cast)
            .forEach(img -> byAccession
                .computeIfAbsent(img.getAccession().getValue(), i -> new ArrayList<>()).add(img));

        b.getEntry().clear();
        byAccession.values().forEach(imgs -> {
          // get the first by accession number
          ImagingStudy initial = imgs.get(0);
          // use the first
          initial.setEndpoint(null);
          if (imgs.size() > 1) {
            for (int j = 1; j < imgs.size(); j++) {
              ImagingStudy followup = imgs.get(j);
              for (ImagingStudySeriesComponent comp : followup.getSeries()) {
                Optional<ImagingStudySeriesComponent> comp0 = getInitialComponentOfSameType(initial,
                    comp);
                if (comp0.isPresent()) {
                  comp.getEndpoint().forEach(comp0.get().getEndpoint()::add);
                } else {
                  initial.getSeries().add(comp);
                }
              }
            }
          }
          b.getEntry().add(new BundleEntryComponent().setResource(initial));
        });
        b.setTotal(byAccession.size());
      }

    }
  }

  private Optional<ImagingStudySeriesComponent> getInitialComponentOfSameType(
      ImagingStudy initial, ImagingStudySeriesComponent comp) {
    return initial.getSeries().stream()
        .filter(x -> x.getModality().getCode().equals(comp.getModality().getCode()))
        .findFirst();
  }

}
