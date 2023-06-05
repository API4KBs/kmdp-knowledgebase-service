package edu.mayo.kmdp.language.translators.fhir.stu3.structdef;

import ca.uhn.fhir.context.RuntimeChildExtension;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.ElementDefinition;
import org.hl7.fhir.dstu3.model.Enumerations.FHIRAllTypes;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.omg.spec.dmn._20180521.model.TDefinitions;
import org.omg.spec.dmn._20180521.model.TItemDefinition;

public class StructDefToDMNHelper {

  public static final Character PATH_SEPARATOR = '.';

  protected StructDefToDMNHelper() {
    // helper class with only static methods
  }

  public static TItemDefinition add(TItemDefinition definition, TDefinitions dmnRoot) {
    boolean alreadyDefined = dmnRoot.getItemDefinition().stream()
        .anyMatch(def -> def.getId().equals(definition.getId()));
    if (!alreadyDefined) {
      dmnRoot.getItemDefinition().add(definition);
    }
    return definition;
  }

  public static ElementDefinition findRootElement(StructureDefinition profile) {
    Set<ElementDefinition> roots = profile.getSnapshot().getElement().stream()
        .filter(StructDefToDMNHelper::isRoot)
        .collect(Collectors.toSet());
    if (roots.isEmpty()) {
      throw new IllegalArgumentException("Unable to find root element in profile " + profile.getName());
    }
    if (roots.size() > 1) {
      Set<String> rootNames = roots.stream()
          .map(ElementDefinition::getPath)
          .collect(Collectors.toSet());
      throw new IllegalArgumentException("Only one root element is supported - found " + rootNames);
    }
    // there can only be one
    return roots.iterator().next();
  }

  public static boolean isRoot(ElementDefinition el) {
    return el.getPath().indexOf(PATH_SEPARATOR) < 0;
  }

  public static boolean isExtension(Object x) {
    if (x instanceof ElementDefinition) {
      return FHIRAllTypes.EXTENSION.toCode().equals(
          ((ElementDefinition) x).getTypeFirstRep().getCode());
    } else {
      return x instanceof RuntimeChildExtension;
    }
  }

  public static boolean isFeelPrimitive(String type) {
    switch (type) {
      case "Any":
      case "string":
      case "boolean":
      case "date":
      case "dateTime":
      case "time":
      case "number":
        return true;
      default:
        return false;
    }
  }
}
