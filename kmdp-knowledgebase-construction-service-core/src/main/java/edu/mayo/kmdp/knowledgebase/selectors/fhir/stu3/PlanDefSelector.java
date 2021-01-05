package edu.mayo.kmdp.knowledgebase.selectors.fhir.stu3;

import static edu.mayo.kmdp.language.common.fhir.stu3.FHIRUtils.getNestedActions;
import static edu.mayo.kmdp.language.common.fhir.stu3.FHIRUtils.getNestedPlanDefs;
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
import org.hl7.fhir.dstu3.model.ValueSet.ValueSetExpansionContainsComponent;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedSelect;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPComponent;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.springframework.stereotype.Component;

@Component
@KPComponent
@KPSupport(FHIR_STU3)
@KPOperation(Selection_Task)
public class PlanDefSelector
    extends AbstractKnowledgeBaseOperator
    implements _applyNamedSelect {

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


  @Override
  public Answer<KnowledgeCarrier> applyNamedSelect(UUID operatorId, KnowledgeCarrier definition,
      UUID kbaseId, String versionTag, String xParams) {

    return kbManager.getKnowledgeBaseManifestation(kbaseId,versionTag)
        .flatOpt(kc -> kc.as(PlanDefinition.class))
        .map(pd -> visit(pd))
        .map(vs -> filter(vs,definition));
  }

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

    return AbstractCarrier.ofAst(filtered,rep(FHIR_STU3));
  }


  private ValueSet visit(PlanDefinition pd) {
    ValueSet vs = new ValueSet();

    Stream<CodeableConcept> cdStream = getNestedPlanDefs(pd)
        .flatMap(this::getConcepts);

    cdStream
        .flatMap(cc -> cc.getCoding().stream())
        .map(cd -> new ValueSetExpansionContainsComponent()
            .setCode(cd.getCode())
            .setSystem(cd.getSystem())
            .setDisplay(cd.getDisplay())
            .setVersion(cd.getVersion()))
        .forEach(comp -> vs.getExpansion().addContains(comp));
    return vs;
  }

  private Stream<? extends CodeableConcept> getConcepts(PlanDefinition x) {
    return getNestedActions(x)
        .flatMap(act -> act.getInput().stream())
            .flatMap(dr -> dr.getCodeFilter().stream())
            .flatMap(cf -> cf.getValueCodeableConcept().stream());
  }


  public static KnowledgeCarrier pivotQuery(URI... schemes) {
    ValueSet vsDef = new ValueSet();
    for (URI uri : schemes) {
      ConceptSetComponent set = new ConceptSetComponent()
          .setSystem(uri.toString())
          .addFilter(new ConceptSetFilterComponent().setOp(FilterOperator.IN));
      vsDef.getCompose().addInclude(set);
    }
    return AbstractCarrier.ofAst(vsDef,rep(FHIR_STU3));
  }


}
