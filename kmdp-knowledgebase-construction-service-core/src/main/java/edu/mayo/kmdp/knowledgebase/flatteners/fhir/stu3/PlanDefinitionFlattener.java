package edu.mayo.kmdp.knowledgebase.flatteners.fhir.stu3;

import static edu.mayo.ontology.taxonomies.api4kp.knowledgeoperations.KnowledgeProcessingOperationSeries.Knowledge_Resource_Flattening_Task;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;

import edu.mayo.kmdp.knowledgebase.v3.server.CompositionalApiInternal;
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
import org.omg.spec.api4kp._1_0.Answer;
import org.omg.spec.api4kp._1_0.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._1_0.services.KPOperation;
import org.omg.spec.api4kp._1_0.services.KPSupport;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;

@KPOperation(Knowledge_Resource_Flattening_Task)
@KPSupport(FHIR_STU3)
@Named
public class PlanDefinitionFlattener implements CompositionalApiInternal._flattenArtifact {

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
        .filter(comp -> comp.getAssetId().getUri().toString().contains(rootAssetId.toString()))
        .findFirst()
        .orElseThrow(IllegalStateException::new);

    PlanDefinition masterPlanDefinition = masterPlan.as(PlanDefinition.class)
        .orElseThrow(IllegalStateException::new);

    List<PlanDefinition> subPlans =
        kc.getComponent().stream()
            .filter(comp -> !comp.getAssetId().getUri().toString().contains(rootAssetId.toString()))
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
      System.out.println("Found action pure reference " + ref);
      Optional<PlanDefinition> referredPlan = component.stream()
          .map(kc -> kc.as(PlanDefinition.class))
          .flatMap(StreamUtil::trimStream)
          .filter(pd -> pd.getId().substring(1)  // ignore the leading '#'
              .equals(artifactRef.substring(artifactRef.lastIndexOf('/') + 1)))
          .findAny();

      if (referredPlan.isPresent()) {
        PlanDefinition target = referredPlan.get();
        System.out.println("Resolved into " + target.getName());
        Optional<PlanDefinitionActionComponent> referredAction = lookupDefinedAction(target,
            fragmentId);

        System.out.println("Resolved  " + referredAction.isPresent());
        if (referredAction.isPresent()) {

          subPlan.getAction().remove(actionReference);
          subPlan.addAction(referredAction.get());
          //referredAction.get().addAction(actionReference);
          actionReference.getDefinition().setReference(target.getId());

          if (!masterPlan.getContained().contains(target)) {
            masterPlan.addContained(target);
          }
        }

      } else {
        System.err.println(
            "WARNING Unable to resolve referred action " + actionReference.getDefinition()
                .getDisplay());
      }
    } else {
      if (action.getDefinition().getReference() != null) {
        System.err
            .println("WARNING : UNRESOLVED REFERENCE " + action.getDefinition().getReference());
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
    System.out
        .println(">>>>>>>>>> Flattening " + subPlan.getName() + " into " + masterPlan.getName());

    if (masterPlan.getType().getCodingFirstRep()
        .equalsDeep(subPlan.getType().getCodingFirstRep())) {
      // won't happen here, but TODO
    } else {
      System.out.println(
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
//        System.out.println("Found external reference in " + act.getLabel() + " to " + act.getDefinition().getDisplay());
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