package edu.mayo.kmdp.knowledgebase.binders.fhir.stu3;

import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;

import edu.mayo.kmdp.knowledgebase.AbstractKnowledgeBaseOperator;
import edu.mayo.kmdp.util.NameUtils;
import edu.mayo.kmdp.util.StreamUtil;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
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
    rewriteRequirements(act.getInput(), bindings, act::addInput, false);
    rewriteRequirements(act.getOutput(), bindings, act::addOutput, true);
    //System.out.println("Original " + original.size() + " vs woven : " + woven.size());
  }

  private void rewriteRequirements(
      List<DataRequirement> list,
      Bindings bindings,
      Consumer<DataRequirement> linker,
      boolean output) {
    List<DataRequirement> original = new ArrayList<>(list);
    list.clear();

    for (DataRequirement dr : original) {
      dr.getCodeFilter().stream()
          .flatMap(cf -> cf.getValueCodeableConcept().stream())
          .flatMap(cc -> cc.getCoding().stream())
          .flatMap(x -> bindDataRequirement(dr, x, bindings, output))
          .forEach(linker);
    }
  }

  private Stream<DataRequirement> bindDataRequirement(DataRequirement dr, Coding cd, Bindings bindings, boolean output) {
    UUID x = UUID.fromString(cd.getCode());

    if (! bindings.containsKey(x)) {
      DataRequirement dr2 = dr.copy();
      dr2.setType(null);
      dr2.getProfile().clear();

      if (output) {
       // FUTURE Default to Observation for derived data
        String typeUri = "https://www.hl7.org/fhir/STU3/procedure.profile.xml";
        dr2.setType(getFHIRType(URI.create(typeUri)));
        dr2.addProfile(typeUri);
      }
      return Stream.of(dr2);
    }
    Set<URI> typeUris = (Set<URI>) bindings.get(x);

    return typeUris.stream()
        .map(typeUri -> {
          DataRequirement dr2 = dr.copy();
          dr2.setType(getFHIRType(typeUri));
          dr2.getProfile().clear();
          dr2.addProfile(typeUri.toString());
          return dr2;
        });
  }

  private String getFHIRType(URI typeUri) {
    String str = NameUtils.getTrailingPart(typeUri.toString());
    int index = str.indexOf('.');
    if (index > 0) {
      str = str.substring(0, index);
    }
    final String code = str;
    return Arrays.stream(FHIRAllTypes.values())
        .map(e -> e.toCode())
        .map(String::toLowerCase)
        .filter(e -> e.equals(code))
        .findFirst()
        .orElseThrow();
  }


  private Stream<PlanDefinitionActionComponent> getNestedActions(PlanDefinition x) {
    return x.getAction().stream()
        .flatMap(this::getNestedActions);
  }

  private Stream<? extends PlanDefinitionActionComponent> getNestedActions(
      PlanDefinitionActionComponent act) {
    return Stream.concat(
        Stream.of(act),
        act.getAction().stream().flatMap(this::getNestedActions));
  }

  private Stream<PlanDefinition> getNestedPlanDefs(PlanDefinition pd) {
    return Stream.concat(Stream.of(pd),
        pd.getContained().stream()
            .flatMap(StreamUtil.filterAs(PlanDefinition.class))
            .flatMap(this::getNestedPlanDefs));
  }

}
