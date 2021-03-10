package edu.mayo.kmdp.knowledgebase.simplifiers.fhir.stu3.components;

import edu.mayo.kmdp.language.common.fhir.stu3.FHIRVisitor;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import org.hl7.fhir.dstu3.model.Resource;

/**
 * Abstract implementation of a Consumer that api4kp:redacts a (complex) FHIR Resource,
 * i.e. removes unnecessary/undesired syntactic constructs
 */
public abstract class AbstractFHIRRedactor implements Consumer<Resource> {

  public void trimAllComponents(Resource resource, boolean deep, Consumer<Resource>... trimmers) {
    FHIRVisitor.traverse(resource, deep)
        .filter(Objects::nonNull)
        .forEach(res -> Arrays.stream(trimmers)
            .forEach(trimmer -> trimmer.accept(res)));
  }

  public String getName() {
    return this.getClass().getSimpleName();
  }

}
