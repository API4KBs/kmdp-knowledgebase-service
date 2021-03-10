package edu.mayo.kmdp.knowledgebase.simplifiers.fhir.stu3.components;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Medication;
import org.hl7.fhir.dstu3.model.MedicationStatement;
import org.hl7.fhir.dstu3.model.Resource;

/**
 * Redactor that processes MedicationStatement and Medication Resources with expanded RxNorm
 * mappings, categorizing the codes and possibly filtering out codes by category
 *
 * e.g.
 * {code : "c1", system : "RxNorm" }
 * c1 -> "IN" -> filter or retain depending on whether "IN" is acceptable
 *
 */
public class RxNormCodingOptimizer extends AbstractFHIRRedactor {

  private static final String RX_NORM = "http://www.nlm.nih.gov/research/umls/rxnorm";
  static Map<String, String> codeCategories = new HashMap<>();

  static Map<String, String> parseRxNormData(JsonNode root) {
    clear();
    ArrayNode concepts = (ArrayNode) root.get("minConceptGroup")
        .get("minConcept");
    concepts
        .forEach(
            entry -> {
              String cd = entry.get("rxcui").asText();
              String cat = entry.get("tty").asText();
              codeCategories.put(cd, cat);
            });
    return codeCategories;
  }

  static void clear() {
    codeCategories.clear();
  }

  public RxNormCodingOptimizer(JsonNode rxNormData) {
    if (codeCategories.isEmpty()) {
      parseRxNormData(rxNormData);
    }
  }

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
      prune(med.getCode(), Collections.singletonList("BN"));

      med.getIngredient().stream()
          .filter(ing -> ing.getItem() instanceof CodeableConcept)
          .forEach(ing -> prune(ing.getItemCodeableConcept(),
              Arrays.asList("IN", "PIN")));
    }
  }

  private void prune(CodeableConcept cd, Collection<String> admissibleCategories) {
    List<Coding> iterable = cd.getCoding().stream()
        .filter(c -> RX_NORM.equals(c.getSystem()))
        .collect(Collectors.toList());
    for (Coding c : iterable) {
      Optional<String> category = Optional.ofNullable(codeCategories.get(c.getCode()));
      String cat = category.orElse("");
      if (admissibleCategories.contains(cat)) {
        // keep
      } else {
        cd.getCoding().remove(c);
      }
    }
  }

}
