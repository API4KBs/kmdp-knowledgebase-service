package edu.mayo.kmdp.knowledgebase.weavers.fhir.stu3;

import static edu.mayo.kmdp.language.common.fhir.stu3.FHIRPlanDefinitionUtils.getNestedPlanDefs;
import static edu.mayo.kmdp.language.common.fhir.stu3.FHIRPlanDefinitionUtils.getSubActions;
import static edu.mayo.kmdp.util.URIUtil.normalizeURIString;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.AbstractCompositeCarrier.ofUniformAggregate;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Lexicon;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Weaving_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;

import edu.mayo.kmdp.knowledgebase.AbstractKnowledgeBaseOperator;
import edu.mayo.kmdp.knowledgebase.KnowledgeBaseProvider;
import edu.mayo.kmdp.knowledgebase.flatteners.fhir.stu3.CodeSystemFlattener;
import edu.mayo.kmdp.language.parsers.fhir.stu3.FHIR3Deserializer;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Named;
import org.apache.jena.vocabulary.SKOS;
import org.hl7.fhir.dstu3.model.CodeSystem;
import org.hl7.fhir.dstu3.model.CodeSystem.ConceptDefinitionComponent;
import org.hl7.fhir.dstu3.model.CodeSystem.ConceptDefinitionDesignationComponent;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DataRequirement;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.PlanDefinition.PlanDefinitionActionComponent;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedWeave;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetCatalogApiInternal;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetRepositoryApiInternal;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPComponent;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@KPSupport({FHIR_STU3})
@KPComponent
@KPOperation(Weaving_Task)
public class PlanDefTerminologyWeaver
    extends AbstractKnowledgeBaseOperator
    implements _applyNamedWeave {

  private static final Logger logger = LoggerFactory.getLogger(PlanDefTerminologyWeaver.class);

  public static final UUID id = UUID.fromString("3440cb49-8e17-4174-b933-9d56a8e92cfb");
  public static final String version = "1.0.0";

  private static final FHIR3Deserializer parser = new FHIR3Deserializer();

  public PlanDefTerminologyWeaver(KnowledgeBaseApiInternal kbManager) {
    this();
    this.kbManager = kbManager;
  }

  public PlanDefTerminologyWeaver() {
    super(SemanticIdentifier.newId(id, version));
  }

  public static Answer<KnowledgeCarrier> getLexica(URI[] annotationVocabularies,
      KnowledgeAssetCatalogApiInternal cat, KnowledgeAssetRepositoryApiInternal repo) {
    CodeSystemFlattener flattener = new CodeSystemFlattener();

    Set<String> vocabSet = Arrays.stream(annotationVocabularies)
        .map(Objects::toString)
        .collect(Collectors.toSet());

    return cat.listKnowledgeAssets(Lexicon.getTag(), null, null, null, null)
        .map(list -> list.stream().filter(p ->
            cat.getKnowledgeAssetVersion(p.getUuid(), p.getVersionTag())
                .map(KnowledgeAsset::getSecondaryId)
                .map(ids -> ids.stream()
                    .anyMatch(id -> vocabSet.contains(normalizeURIString(id.getResourceId()))))
                .orElse(false)).collect(Collectors.toList()))
        .flatList(Pointer.class,
            p -> repo.getKnowledgeAssetCanonicalCarrier(p.getUuid(), codedRep(FHIR_STU3, JSON)))
        .flatMap(css -> flattener.flattenArtifact(ofUniformAggregate(css), null, null));
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return FHIR_STU3;
  }

  @Override
  public Answer<KnowledgeCarrier> applyNamedWeave(UUID operatorId, KnowledgeCarrier aspects,
      UUID kbaseId, String versionTag, String xParams) {
    Optional<CodeSystem> opt = aspects.as(CodeSystem.class);
    Answer<KnowledgeCarrier> kc = kbManager.getKnowledgeBaseManifestation(kbaseId, versionTag);
    return opt.isPresent()
        ? kc.flatMap(x -> this.innerWeave(x, opt.get(), xParams))
        : kc;
  }

  private Answer<KnowledgeCarrier> innerWeave(
      KnowledgeCarrier kc, CodeSystem cs, String xParams) {
    return parser.applyLift(kc, Abstract_Knowledge_Expression, codedRep(FHIR_STU3), null)
        .flatOpt(x -> x.as(PlanDefinition.class))
        .map(pd -> visitComplex(pd, cs, xParams))
        .map(pd -> AbstractCarrier.ofAst(pd)
            .withAssetId(kc.getAssetId())
            .withArtifactId(kc.getArtifactId())
            .withRepresentation(rep(FHIR_STU3)));
  }

  private PlanDefinition visitComplex(
      PlanDefinition rootPD, CodeSystem cs, String xParams) {
    String nsURL = cs.getUrl();
    Map<String, String> index = indexConcepts(cs, xParams);
    getNestedPlanDefs(rootPD)
        .forEach(atomicPD -> visit(atomicPD, nsURL, index));
    return rootPD;
  }


  private void visit(
      PlanDefinition pd, String nsUrl, Map<String, String> index) {
    getSubActions(pd)
        .forEach(act -> visit(act, nsUrl, index));
  }

  private void visit(PlanDefinitionActionComponent act, String nsUrl, Map<String, String> index) {
    rewriteTermLabels(act.getInput(), nsUrl, index);
    rewriteTermLabels(act.getOutput(), nsUrl, index);
  }

  private void rewriteTermLabels(
      List<DataRequirement> list,
      String nsUrl, Map<String, String> index) {
    for (DataRequirement dr : list) {
      dr.getCodeFilter().stream()
          .flatMap(cf -> cf.getValueCodeableConcept().stream())
          .flatMap(cc -> cc.getCoding().stream())
          .forEach(x -> rewriteCode(x, nsUrl, index));
    }
  }

  private void rewriteCode(Coding cd, String nsUrl, Map<String, String> index) {
    if (cd.getSystem().equals(nsUrl)) {
      String newTerm = index.getOrDefault(cd.getCode(), cd.getDisplay());
      logger.debug("Rewriting code {} display {} as {}", cd.getCode(), cd.getDisplay(), newTerm);
      cd.setDisplay(newTerm);
    }
  }


  private Map<String, String> indexConcepts(CodeSystem cs, String xParams) {
    Map<String, String> index = new HashMap<>();
    cs.getConcept()
        .forEach(cd ->
            findDesignation(cd, xParams).ifPresent(label -> index.put(cd.getCode(), label)));
    return index;
  }

  private Optional<String> findDesignation(ConceptDefinitionComponent cd, String labelType) {
    return getDesignation(cd, labelType)
        .or(() -> getDesignation(cd, SKOS.prefLabel.getLocalName()))
        .or(() -> Optional.ofNullable(cd.getDisplay()));
  }

  private Optional<String> getDesignation(ConceptDefinitionComponent cd, String labelType) {
    if (labelType == null) {
      return Optional.empty();
    }
    return cd.getDesignation().stream()
        .filter(dx -> dx.getUse() != null && labelType.equals(dx.getUse().getCode()))
        .map(ConceptDefinitionDesignationComponent::getValue)
        .findFirst();
  }

}
