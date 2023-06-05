package edu.mayo.kmdp.language.common.fhir.stu3;

import static edu.mayo.kmdp.util.StreamUtil.filterAs;

import java.util.stream.Stream;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.dstu3.model.Resource;

public class FHIRVisitor {

  private FHIRVisitor() {
    // nothing
  }

  public static Stream<Resource> traverse(Resource resource, boolean deep) {
    return getContained(resource)
        .flatMap(res -> visit(res, deep));
  }

  protected static Stream<Resource> visit(Resource resource, boolean deep) {
    if (resource instanceof Bundle) {
      Bundle b = (Bundle) resource;
      if (deep) {
        return Stream.concat(
            Stream.of(b),
            b.getEntry().stream()
                .map(BundleEntryComponent::getResource)
                .flatMap(r -> traverse(r, deep)));
      } else {
        return Stream.of(b);
      }
    } else if (resource instanceof Parameters) {
      Parameters p = (Parameters) resource;
      return flattenParameters(p, deep);
    } else {
      return deep && resource != null
          ? Stream.of(resource)
          : Stream.empty();
    }
  }

  protected static Stream<Resource> flattenParameters(
      Parameters root, boolean deep) {
    return Stream.concat(Stream.of(root),
        root.getParameter().stream()
            .flatMap(param -> flattenParameters(param)
                .map(ParametersParameterComponent::getResource)
                .flatMap(r -> traverse(r, deep))));
  }

  protected static Stream<ParametersParameterComponent> flattenParameters(
      ParametersParameterComponent root) {
    return Stream.concat(Stream.of(root),
        root.getPart().stream().flatMap(FHIRVisitor::flattenParameters));
  }

  /**
   * Traverses a Resource with contained Resources
   * @param root the root resource
   * @return a Stream of the contained resources
   */
  public static Stream<Resource> getContained(Resource root) {
    return Stream.concat(Stream.of(root),
        root instanceof DomainResource
            ? ((DomainResource) root).getContained().stream().flatMap(FHIRVisitor::getContained)
            : Stream.empty());
  }

  /**
   * Traverses a DomainResource with nested (contained) Resources
   * @param root the root resource
   * @return a Stream of the nested resources
   */
  public static <T extends DomainResource> Stream<T> getContained(T root, Class<T> type) {
    return Stream.concat(Stream.of(root),
        root.getContained().stream()
            .flatMap(filterAs(type))
            .flatMap(x -> getContained(x,type)));
  }

}

