package edu.mayo.kmdp.knowledgebase.selectors.fhir.stu3;

import static edu.mayo.kmdp.language.common.fhir.stu3.FHIRPlanDefinitionUtils.getNestedPlanDefs;
import static edu.mayo.kmdp.language.common.fhir.stu3.FHIRPlanDefinitionUtils.getSubActions;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Selection_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;

import edu.mayo.kmdp.knowledgebase.AbstractKnowledgeBaseOperator;
import java.net.URI;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.dstu3.model.ValueSet.ConceptSetComponent;
import org.hl7.fhir.dstu3.model.ValueSet.ConceptSetFilterComponent;
import org.hl7.fhir.dstu3.model.ValueSet.FilterOperator;
import org.hl7.fhir.dstu3.model.ValueSet.ValueSetExpansionComponent;
import org.hl7.fhir.dstu3.model.ValueSet.ValueSetExpansionContainsComponent;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedSelect;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedSelectDirect;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPComponent;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link _applyNamedSelect} (Direct) that performs a Selection_Task, which
 * extracts semantic concepts, collected and manifested as a fhir:{@link ValueSet}, from a
 * homogeneous KnowledgeBase that consists in fhir:{@link PlanDefinition} that may possibly contain
 * other fhir:{@link PlanDefinition}
 * <p>
 * More specifically, focuses on the concepts used to annotate the input and output {@link
 * org.hl7.fhir.dstu3.model.DataRequirement} of the PlanDefinition's Actions, recursively nested
 * actions.
 * <p>
 * The client can further specify filtering criteria, passing an intensionally defined fhir:{@link
 * ValueSet}. If the input Valueset has {@link ConceptSetComponent} with scheme URIs, this Selector
 * will filter the returned concepts, and only return those Concepts where the Concept's system
 * matches any one of the provided URIs.
 */
@Component
@KPComponent
@KPSupport(FHIR_STU3)
@KPOperation(Selection_Task)
public class PlanDefSelector
    extends AbstractKnowledgeBaseOperator
    implements _applyNamedSelect, _applyNamedSelectDirect {

  public static final UUID id = UUID.fromString("0d82b8dd-50b1-43b9-b9f9-e8946d87db6e");
  public static final String version = "1.0.0";

  public PlanDefSelector() {
    super(SemanticIdentifier.newId(id, version));
  }

  public PlanDefSelector(KnowledgeBaseApiInternal kbManager) {
    this();
    this.kbManager = kbManager;
  }


  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return FHIR_STU3;
  }


  /**
   * Core operation
   *
   * @param operatorId the ID of this operator (class)
   * @param definition A {@link KnowledgeCarrier} that wraps a {@link ValueSet} with {@link
   *                   ConceptSetComponent} that indicate the scheme (URIs) of the concepts to be
   *                   selected
   * @param kbaseId    the ID of the root {@link PlanDefinition} in the {@link
   *                   org.omg.spec.api4kp._20200801.services.KnowledgeBase}
   * @param versionTag the version of the root PlanDefinition
   * @param xParams    extra configuration parameters (not supported)
   * @return an expanded {@link ValueSet} with the selected concepts, wrapped
   */
  @Override
  public Answer<KnowledgeCarrier> applyNamedSelect(UUID operatorId, KnowledgeCarrier definition,
      UUID kbaseId, String versionTag, String xParams) {

    return kbManager.getKnowledgeBaseManifestation(kbaseId, versionTag)
        .flatMap(artifact -> applyNamedSelectDirect(operatorId, artifact, definition, xParams));
  }

  /**
   * Core operation
   *
   * @param operatorId the ID of this operator (class)
   * @param definition A {@link KnowledgeCarrier} that wraps a {@link ValueSet} with {@link
   *                   ConceptSetComponent} that indicate the scheme (URIs) of the concepts to be
   *                   selected
   * @param artifact   the {@link PlanDefinition} to select concepts from
   * @param xParams    extra configuration parameters (not supported)
   * @return an expanded {@link ValueSet} with the selected concepts, wrapped
   */
  @Override
  public Answer<KnowledgeCarrier> applyNamedSelectDirect(UUID operatorId, KnowledgeCarrier artifact,
      KnowledgeCarrier definition, String xParams) {
    return Answer.of(artifact)
        .flatOpt(kc -> kc.as(PlanDefinition.class))
        .map(this::visit)
        .map(vs -> filter(vs, definition));
  }

  /**
   * Applies the filter to the selected concepts, such that only the concepts whose system is one of
   * the URIs in the definitional Valueset are returned.
   *
   * @param vs         the (extensional) Valueset with the selected concepts
   * @param definition the (intensional) Valueset with the filter URIs
   * @return the filtered {@link ValueSet}, wrapped
   */
  private KnowledgeCarrier filter(ValueSet vs, KnowledgeCarrier definition) {
    ValueSet def = definition.as(ValueSet.class)
        .orElseThrow(UnsupportedOperationException::new);
    Set<String> systems = def.getCompose().getInclude().stream()
        .filter(in -> in.getFilterFirstRep().getOp().equals(FilterOperator.IN))
        .map(ConceptSetComponent::getSystem)
        .collect(Collectors.toSet());

    ValueSet filtered = new ValueSet();
    vs.getExpansion().getContains().stream()
        .filter(comp -> systems.contains(comp.getSystem()))
        .forEach(filtered.getExpansion()::addContains);

    return AbstractCarrier.ofAst(filtered, rep(FHIR_STU3));
  }

  /**
   * Processes the given {@link PlanDefinition}, as well as any nested (contained) PlanDefinitions
   *
   * @param pd the (root) {@link PlanDefinition}
   * @return the concepts used to annotate the {@link PlanDefinition}'s Input {@link
   * org.hl7.fhir.dstu3.model.DataRequirement}
   * @see edu.mayo.kmdp.language.common.fhir.stu3.FHIRPlanDefinitionUtils#getNestedPlanDefs(PlanDefinition)
   */
  private ValueSet visit(PlanDefinition pd) {
    ValueSet vs = new ValueSet();

    Stream<CodeableConcept> cdStream = getNestedPlanDefs(pd)
        .flatMap(this::getConcepts);

    cdStream
        .flatMap(cc -> cc.getCoding().stream()
            .map(cd -> (ValueSetExpansionContainsComponent) new ValueSetExpansionContainsComponent()
                .setCode(cd.getCode())
                .setSystem(cd.getSystem())
                .setDisplay(cd.getDisplay())
                .setVersion(cd.getVersion())
                .setExtension(cc.getExtension())))
        .filter(comp -> !contains(vs.getExpansion(), comp))
        .forEach(comp -> vs.getExpansion().addContains(comp));
    return vs;
  }

  /**
   * Filter that prevents a given Concept from being added more than once to a ValueSet. Concept
   * (descriptors) are considered equivalent if code and system match.
   * <p>
   * Despite the name, the implementation relies on {@link java.util.List} rather than {@link Set}
   *
   * @param expansion the ValueSet (expansion section) to add the concept (descriptor) to
   * @param comp      the prospective concept (descriptor) to be added
   * @return true if an equivalent concept is already present in the Valueset
   */
  private boolean contains(ValueSetExpansionComponent expansion,
      ValueSetExpansionContainsComponent comp) {
    return expansion.getContains().stream()
        .anyMatch(c -> c.getSystem().equals(comp.getSystem())
            && c.getCode().equals(comp.getCode()));
  }

  /**
   * Traverses the {@link PlanDefinition} to select the Input {@link org.hl7.fhir.dstu3.model.DataRequirement},
   * pulling the {@link CodeableConcept} and {@link org.hl7.fhir.dstu3.model.Extension} to be
   * returned.
   *
   * @param pd the {@link PlanDefinition} to be processed
   * @return the annotated {@link CodeableConcept} from the {@link PlanDefinition}
   */
  private Stream<? extends CodeableConcept> getConcepts(PlanDefinition pd) {
    return getSubActions(pd)
        .flatMap(act -> act.getInput().stream())
        .flatMap(dr -> dr.getCodeFilter().stream()
            .flatMap(cf ->
                cf.getValueCodeableConcept().stream()
                    .map(CodeableConcept::copy)
                    .map(cd -> (CodeableConcept) cd.setExtension(dr.getExtension()))
            ));
  }


  /**
   * Utility function to build the intensional ValueSet filter, that allows to restrict the scope of
   * the concepts to be selected
   *
   * @param schemes URIs, such that only Concepts whose system matches one of the URIs will be
   *                returned
   * @return a ValueSet filter to be fed back to this Selector, wrapped
   */
  public static KnowledgeCarrier pivotQuery(URI... schemes) {
    ValueSet vsDef = new ValueSet();
    for (URI uri : schemes) {
      ConceptSetComponent set = new ConceptSetComponent()
          .setSystem(uri.toString())
          .addFilter(new ConceptSetFilterComponent().setOp(FilterOperator.IN));
      vsDef.getCompose().addInclude(set);
    }
    return AbstractCarrier.ofAst(vsDef, rep(FHIR_STU3));
  }


}
