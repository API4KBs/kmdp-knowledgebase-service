package edu.mayo.kmdp.knowledgebase.flatteners.fhir.stu3;

import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Knowledge_Resource_Flattening_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.inject.Named;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.dstu3.model.ValueSet.ValueSetExpansionContainsComponent;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.CompositionalApiInternal;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;

/**
 * Implementation of the
 * {@link
 * org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.CompositionalApiInternal._flattenArtifact}
 * Knowledge operation that flattens multiple {@link ValueSet} - either defined by
 * {@link ValueSet#getCompose()} or enumerated by {@link ValueSet#getExpansion()} - and returns a
 * single, anonymous, enumerated ValueSet that references the source ValueSets (if named)
 */
@KPOperation(Knowledge_Resource_Flattening_Task)
@KPSupport(FHIR_STU3)
@Named
public class ValueSetFlattener
    extends AbstractFhirFlattener<ValueSet>
    implements CompositionalApiInternal._flattenArtifact {

  public static final UUID OP_ID = UUID.fromString("d3d4a5ee-c3bd-4f3d-a217-b07c7ed2c061");
  public static final String OP_VERSION = "1.0.0";

  public ValueSetFlattener() {
    super(SemanticIdentifier.newId(OP_ID, OP_VERSION));
  }


  @Override
  protected ValueSet merge(ValueSet target, ValueSet source) {

    var expansion = target.getExpansion();
    if (expansion.getTimestamp() != null) {
      // this attribute is mandatory in the FHIR schema
      expansion.setTimestamp(new Date());
    }
    var expandedList = expansion.getContains();

    source.getExpansion().getContains()
        .forEach(concept -> addUnique(concept, expandedList));

    source.getCompose().getInclude().forEach(included ->
        included.getConcept().forEach(cd -> {
              ValueSetExpansionContainsComponent concept = new ValueSetExpansionContainsComponent()
                  .setCode(cd.getCode())
                  .setSystem(included.getSystem())
                  .setDisplay(cd.getDisplay());
              addUnique(concept, expandedList);
            }
        ));

    if (source.getUrl() != null) {
      target.getCompose().getIncludeFirstRep()
          .addValueSet(source.getUrl());
    }

    return target;
  }

  private void addUnique(
      ValueSetExpansionContainsComponent concept,
      List<ValueSetExpansionContainsComponent> expandedList) {
    // not the most efficient way, since this is called for each 'fold'
    // consider refactoring if this becomes a bottleneck
    boolean present = expandedList.stream()
        .anyMatch(
            cd -> Objects.equals(cd.getCode(), concept.getCode())
                && Objects.equals(cd.getSystem(), concept.getSystem()));
    if (!present) {
      expandedList.add(concept);
    }
  }

  @Override
  protected Class<ValueSet> getComponentType() {
    return ValueSet.class;
  }

  @Override
  protected ValueSet newInstance() {
    return (ValueSet) new ValueSet()
        .setId("zip");
  }
}
