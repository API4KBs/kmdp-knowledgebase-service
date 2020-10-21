package edu.mayo.kmdp.knowledgebase.binders.fhir.stu3;

import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;

import edu.mayo.kmdp.knowledgebase.AbstractKnowledgeBaseOperator;
import edu.mayo.kmdp.util.StreamUtil;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DataRequirement;
import org.hl7.fhir.dstu3.model.Enumerations.FHIRAllTypes;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.PlanDefinition.PlanDefinitionActionComponent;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedBind;
import org.omg.spec.api4kp._20200801.datatypes.Bindings;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPComponent;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.springframework.stereotype.Component;

@Component
@KPComponent
@KPSupport(FHIR_STU3)
public class PlanDefDataShapeBinder
    extends AbstractKnowledgeBaseOperator
    implements _applyNamedBind {

  public static final UUID id = UUID.fromString("467d6562-746c-4bcb-9614-b56d34bc847c");
  public static final String version = "1.0.0";

  public PlanDefDataShapeBinder() {
    super(SemanticIdentifier.newId(id,version));
  }

  public PlanDefDataShapeBinder(KnowledgeBaseApiInternal kbManager) {
    this();
    this.kbManager = kbManager;
  }


  @Override
  public Answer<KnowledgeCarrier> applyNamedBind(UUID operatorId, Bindings bindings, UUID kbaseId,
      String versionTag, String xParams) {
    return kbManager.getKnowledgeBaseManifestation(kbaseId,versionTag)
        .flatMap(kc -> this.visit(kc,bindings));
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return FHIR_STU3;
  }

  private Answer<KnowledgeCarrier> visit(KnowledgeCarrier kc, Bindings bindings) {
    PlanDefinition pd = kc.as(PlanDefinition.class).map(x -> x.copy())
        .orElseThrow();
    visitComplex(pd,bindings);
    KnowledgeCarrier boundKC = AbstractCarrier.ofAst(pd)
        .withRepresentation(kc.getRepresentation())
        .withAssetId(kc.getAssetId())
        .withArtifactId(kc.getArtifactId())
        .withLabel(kc.getLabel())
        .withLevel(kc.getLevel());
    return Answer.of(boundKC);
  }

  private void visitComplex(PlanDefinition rootPD, Bindings bindings) {
    getNestedPlanDefs(rootPD)
        .forEach(atomicPD -> visit(atomicPD,bindings));
  }

  private void visit(PlanDefinition pd, Bindings bindings) {
    getNestedActions(pd)
        .forEach(act -> visit(act,bindings));
  }

  private void visit(PlanDefinitionActionComponent act, Bindings bindings) {
    List<DataRequirement> original = act.getInput();
    act.getInput().clear();

    for (DataRequirement dr : original) {
      dr.getCodeFilter().stream()
          .flatMap(cf -> cf.getValueCodeableConcept().stream())
          .flatMap(cc -> cc.getCoding().stream())
          .map(Coding::getCode)
          .map(x -> bindDataRequirement(dr, x, bindings))
          .flatMap(StreamUtil::trimStream)
          .forEach(act::addInput);
    }
  }

  private Optional<DataRequirement> bindDataRequirement(DataRequirement dr, String x, Bindings bindings) {
    if (! bindings.containsKey(x)) {
      return Optional.empty();
    }
    DataRequirement dr2 = dr.copy();
    URI typeUri = URI.create((String) bindings.get(dr2));
    dr2.setType(FHIRAllTypes.valueOf(typeUri.getFragment()).toCode());
    dr2.addProfile(typeUri.toString());

    return Optional.of(dr2);
  }


  private Stream<PlanDefinitionActionComponent> getNestedActions(PlanDefinition x) {
    return x.getAction().stream()
        .flatMap(act -> Stream.concat(Stream.of(act),act.getAction().stream()));
  }

  private Stream<PlanDefinition> getNestedPlanDefs(PlanDefinition pd) {
    return Stream.concat(Stream.of(pd),
        pd.getContained().stream()
            .flatMap(StreamUtil.filterAs(PlanDefinition.class))
            .flatMap(this::getNestedPlanDefs));
  }

}
