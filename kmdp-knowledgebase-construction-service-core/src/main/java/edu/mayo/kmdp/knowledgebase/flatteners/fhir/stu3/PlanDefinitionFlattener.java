package edu.mayo.kmdp.knowledgebase.flatteners.fhir.stu3;

import static org.omg.spec.api4kp.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Knowledge_Resource_Flattening_Task;
import static org.omg.spec.api4kp.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;

import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.URIUtil;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Named;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.PlanDefinition.PlanDefinitionActionComponent;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.CompositionalApiInternal;
import org.omg.spec.api4kp._20200801.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@KPOperation(Knowledge_Resource_Flattening_Task)
@KPSupport(FHIR_STU3)
@Named
public class PlanDefinitionFlattener implements CompositionalApiInternal._flattenArtifact {
  
  private static Logger logger = LoggerFactory.getLogger(PlanDefinitionFlattener.class);

  // TODO root Asset ID should be marked inside the struct, not passed as an argument.
  //  Need a 'rootComponent' or something..
  public Answer<KnowledgeCarrier> flattenArtifact(KnowledgeCarrier carrier, UUID rootAssetId) {
    if (! (carrier instanceof CompositeKnowledgeCarrier)) {
      return Answer.of(carrier);
    }

    CompositeKnowledgeCarrier kc = (CompositeKnowledgeCarrier) carrier;
    // TODO The struct has all the dependencies between the components
    // What is missing is the 'composite root = kc.getAssetId  HAS PART  X
    // With - this being a root-based composite - the picking of the root asset,
    // so that the effect is that all PlanDefs are injected into the root PlanDef
//      KnowledgeCarrier root = kc.getAssetId().getVersionId();

    // TODO get the root asset properly..
    Model x = kc.getStruct().as(Model.class)
        .map(m -> m.query(new SimpleSelector() {
          public boolean test(Statement st) {
            return st.getSubject().getURI().contains(rootAssetId.toString());
          }
        })).get();

    KnowledgeCarrier masterPlan = kc.getComponent().stream()
        .filter(comp -> comp.getAssetId().getResourceId().toString().contains(rootAssetId.toString()))
        .findFirst()
        .orElseThrow(IllegalStateException::new);

    PlanDefinition masterPlanDefinition = masterPlan.as(PlanDefinition.class)
        .orElseThrow(IllegalStateException::new);

    List<PlanDefinition> subPlans =
        kc.getComponent().stream()
            .filter(comp -> !comp.getAssetId().getResourceId().toString().contains(rootAssetId.toString()))
            .map(comp -> comp.as(PlanDefinition.class))
            .flatMap(StreamUtil::trimStream)
            .collect(Collectors.toList());

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
            kc.getComponent())
    );

    return Answer.of(masterPlan);

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
          actionReference.getDefinition().setReference(target.getId());

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
            }
          }
        });
    action.forEach(act ->
        rewriteActionDefinition(act.getAction(), mergedPlan)
    );
  }



}
