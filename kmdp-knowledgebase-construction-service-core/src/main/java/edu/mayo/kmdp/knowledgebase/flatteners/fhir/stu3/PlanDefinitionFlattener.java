package edu.mayo.kmdp.knowledgebase.flatteners.fhir.stu3;

import static edu.mayo.kmdp.language.common.fhir.stu3.FHIRPlanDefinitionUtils.setKnowledgeIdentifiers;
import static org.omg.spec.api4kp._20200801.id.IdentifierConstants.SNAPSHOT;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.hashIdentifiers;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Knowledge_Resource_Flattening_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;

import edu.mayo.kmdp.knowledgebase.AbstractKnowledgeBaseOperator;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.URIUtil;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Named;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.PlanDefinition.PlanDefinitionActionComponent;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.CompositionalApiInternal;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@KPOperation(Knowledge_Resource_Flattening_Task)
@KPSupport(FHIR_STU3)
@Named
public class PlanDefinitionFlattener
    extends AbstractKnowledgeBaseOperator
    implements CompositionalApiInternal._flattenArtifact {

  private static final Logger logger = LoggerFactory.getLogger(PlanDefinitionFlattener.class);

  public static final UUID id = UUID.fromString("bdd19e7b-fa7c-4d84-b5be-f0e8c487a4ce");
  public static final String version = "1.0.0";

  public PlanDefinitionFlattener() {
    super(SemanticIdentifier.newId(id, version));
  }


  @Override
  public Answer<KnowledgeCarrier> flattenArtifact(KnowledgeCarrier carrier, UUID rootAssetId,
      String params) {
    if (!(carrier instanceof CompositeKnowledgeCarrier)) {
      return Answer.of(carrier);
    }

    CompositeKnowledgeCarrier kc = (CompositeKnowledgeCarrier) carrier;
    List<PlanDefinition> subPlans =
        kc.getComponent().stream()
            .filter(comp -> !comp.getAssetId().getResourceId().toString()
                .contains(rootAssetId.toString()))
            .map(comp -> comp.as(PlanDefinition.class))
            .flatMap(StreamUtil::trimStream)
            .collect(Collectors.toList());

    PlanDefinition masterPlanDefinition = kc.mainComponentAs(PlanDefinition.class);

    innerFlatten(masterPlanDefinition, subPlans, kc.getComponent());

    return repackage(kc, masterPlanDefinition, subPlans);
  }

  public void innerFlatten(
      PlanDefinition masterPlanDefinition,
      List<PlanDefinition> subPlans,
      List<KnowledgeCarrier> components) {

    subPlans.forEach(subPlan -> {
      // TODO FIXME FlattenInfo resolves the references of the Master Plan..
      flattenInto(subPlan, masterPlanDefinition);
    });

    subPlans.forEach(subPlan ->
        // .. while resolveReferences takes care of the sub-decision refs across models.//
        // There should be one method, not two partially overlapping ones
        resolveReferences(
            masterPlanDefinition,
            subPlan,
            components)
    );

    reshapeContained(masterPlanDefinition);
  }

  private void reshapeContained(PlanDefinition masterPlanDefinition) {
    pullContained(masterPlanDefinition)
        .filter(x -> x != masterPlanDefinition)
        .forEach(x -> masterPlanDefinition.getContained().add(x));
  }

  private Stream<DomainResource> pullContained(DomainResource root) {
    return Stream.concat(
        Stream.of(root),
        new ArrayList<>(root.getContained()).stream()
            .flatMap(StreamUtil.filterAs(DomainResource.class))
            .flatMap(x -> {
              root.getContained().remove(x);
              return pullContained(x);
            }));
  }


  private Answer<KnowledgeCarrier> repackage(
      CompositeKnowledgeCarrier kc,
      PlanDefinition masterPlanDefinition,
      List<PlanDefinition> subPlans) {
    ResourceIdentifier assetId = mapAssetId(kc);
    ResourceIdentifier flatArtifactId = mapArtifactId(kc);

    stampArtifactId(masterPlanDefinition, flatArtifactId);

    subPlans.forEach(pd -> {
          var tmp = new ArrayList<>(pd.getIdentifier());
          tmp.removeIf(x -> !isAssetId(x));
          pd.setIdentifier(tmp);
        }
    );

    List<RelatedArtifact> rels = new LinkedList<>(masterPlanDefinition.getRelatedArtifact());
    subPlans.forEach(pd -> {
      rels.addAll(pd.getRelatedArtifact());
      pd.setRelatedArtifact(Collections.emptyList());
    });
    masterPlanDefinition.setRelatedArtifact(rels);

    setKnowledgeIdentifiers(masterPlanDefinition, assetId, flatArtifactId);

    return Answer.of(AbstractCarrier.ofAst(masterPlanDefinition)
        .withRepresentation(kc.mainComponent().getRepresentation())
        .withArtifactId(flatArtifactId)
        .withAssetId(assetId)
        .withLabel(masterPlanDefinition.getName()));
  }

  private boolean isAssetId(Identifier id) {
    return id != null
        && id.getType() != null
        && "KnowledgeAsset".equals(id.getType().getCodingFirstRep().getCode());
  }

  private void stampArtifactId(PlanDefinition masterPlanDefinition,
      ResourceIdentifier flatArtifactId) {
    masterPlanDefinition.setId(flatArtifactId.getTag());
    masterPlanDefinition.setVersion(
        flatArtifactId.getVersionTag()
            .replace(SNAPSHOT, Long.toString(new Date().toInstant().toEpochMilli())));
  }

  private ResourceIdentifier mapArtifactId(CompositeKnowledgeCarrier kc) {
    return kc.components()
        .map(KnowledgeCarrier::getArtifactId)
        .reduce((i1, i2) -> hashIdentifiers(i1, i2, true))
        .orElseThrow(() -> new IllegalStateException("Unable to combine Artifact IDs"));
  }

  private ResourceIdentifier mapAssetId(CompositeKnowledgeCarrier kc) {
    return kc.getAssetId();
  }

  protected void resolveReferences(PlanDefinition masterPlan,
      PlanDefinition subPlan, List<KnowledgeCarrier> component) {
    // copy before iterating, since the collection may change during the processing
    new ArrayList<>(subPlan.getAction())
        .forEach(act -> resolveReferences(masterPlan, subPlan, act, component));
  }

  protected void resolveReferences(
      PlanDefinition masterPlan,
      PlanDefinition subPlan,
      PlanDefinitionActionComponent action,
      List<KnowledgeCarrier> component) {

    if (action.getDefinition().getReference() != null
        && !action.getDefinition().getReference().startsWith("#")) {

      PlanDefinitionActionComponent actionReference = action;

      URI ref = URI.create(actionReference.getDefinition().getReference());
      String artifactRef = URIUtil.normalizeURIString(ref);
      String fragmentId = actionReference.getDefinition().getIdentifier().getValue();
      logger.info("Found action pure reference {}", ref);
      Optional<PlanDefinition> referredPlan = component.stream()
          .map(kc -> kc.as(PlanDefinition.class))
          .flatMap(StreamUtil::trimStream)
          .filter(pd -> pd.getId().substring(1)  // ignore the leading '#'
              .equals(artifactRef.substring(artifactRef.lastIndexOf('/') + 1)))
          .findAny();

      if (referredPlan.isPresent()) {
        PlanDefinition target = referredPlan.get();
        logger.info("Resolved into {}", target.getName());
        Optional<PlanDefinitionActionComponent> referredAction = lookupDefinedAction(target,
            fragmentId);

        logger.info("Resolved {}", referredAction.isPresent());
        if (referredAction.isPresent()) {

          subPlan.getAction().remove(actionReference);
          subPlan.addAction(referredAction.get());
          actionReference.getDefinition()
              .setReference(target.getId())
              .setResource(target);

          if (!masterPlan.getContained().contains(target)) {
            masterPlan.addContained(target);
          }
        }

      } else {
        logger.warn(
            "WARNING Unable to resolve referred action {}", actionReference.getDefinition()
                .getDisplay());
      }
    } else {
      if (action.getDefinition().getReference() != null) {
        logger.warn("WARNING : UNRESOLVED REFERENCE {}", action.getDefinition().getReference());
      }
    }
    new ArrayList<>(action.getAction())
        .forEach(act -> resolveReferences(masterPlan, subPlan, act, component));
  }

  private Optional<PlanDefinitionActionComponent> lookupDefinedAction(PlanDefinition target,
      String fragmentId) {
    return target.getAction().stream()
        .map(subAct -> lookupDefinedAction(subAct, fragmentId))
        .flatMap(StreamUtil::trimStream)
        .findAny();
  }

  private Optional<PlanDefinitionActionComponent> lookupDefinedAction(
      PlanDefinitionActionComponent target,
      String fragmentId) {
    if (("#" + fragmentId).equals(target.getId())) {
      return Optional.of(target);
    }
    return target.getAction().stream()
        .filter(act -> fragmentId.equals(act.getId()))
        .findAny();
  }


  private void flattenInto(PlanDefinition subPlan, PlanDefinition masterPlan) {
    logger.info(">>>>>>>>>> Flattening {} into {}", subPlan.getName(), masterPlan.getName());

    if (masterPlan.getType().getCodingFirstRep()
        .equalsDeep(subPlan.getType().getCodingFirstRep())) {
      // won't happen here, but TODO
    } else {
      logger.info(
          "--  Merging " + subPlan.getName() + ", a " + subPlan.getType().getCodingFirstRep()
              .getDisplay()
              + " into a " + masterPlan.getType().getCodingFirstRep().getDisplay());

      String absoluteSubPlanId = subPlan.getId();
      String localSubPlanId = "#" +
          absoluteSubPlanId.substring(absoluteSubPlanId.lastIndexOf('/') + 1);
      subPlan.setId(localSubPlanId);
      if (masterPlan.getContained() == null || !masterPlan.getContained().contains(subPlan)) {
        masterPlan.addContained(subPlan);
      }

      rewriteActionDefinition(masterPlan.getAction(), subPlan);
    }

  }

  protected void rewriteActionDefinition(List<PlanDefinitionActionComponent> action,
      PlanDefinition mergedPlan) {
    action.stream()
        .filter(act -> act.getDefinition().getReference() != null)
        .forEach(act -> {
          if (act.getDefinition().getReference() != null
              && !act.getDefinition().getReference().startsWith("#")) {
            if (act.getDefinition().getReference().contains(mergedPlan.getId().replace("#", ""))) {
              act.getDefinition().setReference(mergedPlan.getId());
              act.getDefinition().setResource(mergedPlan);
            }
          }
        });
    action.forEach(act ->
        rewriteActionDefinition(act.getAction(), mergedPlan)
    );
  }


  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return FHIR_STU3;
  }

}
