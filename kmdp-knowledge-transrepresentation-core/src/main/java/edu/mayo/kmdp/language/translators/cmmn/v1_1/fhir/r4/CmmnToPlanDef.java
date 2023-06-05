/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.mayo.kmdp.language.translators.cmmn.v1_1.fhir.r4;

import static edu.mayo.kmdp.util.NameUtils.nameToIdentifier;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.Captures;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Care_Process_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIRPath_STU1;

import edu.mayo.kmdp.util.NameUtils.IdentifierType;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DataRequirement;
import org.hl7.fhir.r4.model.DataRequirement.DataRequirementCodeFilterComponent;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.PlanDefinition.ActionCardinalityBehavior;
import org.hl7.fhir.r4.model.PlanDefinition.ActionConditionKind;
import org.hl7.fhir.r4.model.PlanDefinition.ActionGroupingBehavior;
import org.hl7.fhir.r4.model.PlanDefinition.ActionPrecheckBehavior;
import org.hl7.fhir.r4.model.PlanDefinition.ActionRelationshipType;
import org.hl7.fhir.r4.model.PlanDefinition.ActionRequiredBehavior;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionRelatedActionComponent;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.TriggerDefinition.TriggerType;
import org.omg.spec.api4kp._20200801.id.ConceptIdentifier;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.Term;
import org.omg.spec.api4kp._20200801.surrogate.Annotation;
import org.omg.spec.cmmn._20151109.model.TAssociation;
import org.omg.spec.cmmn._20151109.model.TCase;
import org.omg.spec.cmmn._20151109.model.TCaseFileItem;
import org.omg.spec.cmmn._20151109.model.TCaseFileItemDefinition;
import org.omg.spec.cmmn._20151109.model.TCaseFileItemOnPart;
import org.omg.spec.cmmn._20151109.model.TCmmnElement;
import org.omg.spec.cmmn._20151109.model.TDecision;
import org.omg.spec.cmmn._20151109.model.TDecisionTask;
import org.omg.spec.cmmn._20151109.model.TDefinitions;
import org.omg.spec.cmmn._20151109.model.TDiscretionaryItem;
import org.omg.spec.cmmn._20151109.model.TEntryCriterion;
import org.omg.spec.cmmn._20151109.model.TEventListener;
import org.omg.spec.cmmn._20151109.model.TExitCriterion;
import org.omg.spec.cmmn._20151109.model.TExtensionElements;
import org.omg.spec.cmmn._20151109.model.THumanTask;
import org.omg.spec.cmmn._20151109.model.TMilestone;
import org.omg.spec.cmmn._20151109.model.TOnPart;
import org.omg.spec.cmmn._20151109.model.TPlanItem;
import org.omg.spec.cmmn._20151109.model.TPlanItemControl;
import org.omg.spec.cmmn._20151109.model.TPlanItemDefinition;
import org.omg.spec.cmmn._20151109.model.TPlanItemOnPart;
import org.omg.spec.cmmn._20151109.model.TPlanningTable;
import org.omg.spec.cmmn._20151109.model.TProcessTask;
import org.omg.spec.cmmn._20151109.model.TSentry;
import org.omg.spec.cmmn._20151109.model.TStage;
import org.omg.spec.cmmn._20151109.model.TTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO this class is out of date with respect to its STU3 counterpart
 */
public class CmmnToPlanDef {

  private static final Logger log = LoggerFactory.getLogger(CmmnToPlanDef.class);

  static final String CMIS_DOCUMENT_TYPE = "http://www.omg.org/spec/CMMN/DefinitionType/CMISDocument";
  static final String XSD_ELEMENT_TYPE = "http://www.omg.org/spec/CMMN/DefinitionType/XSDElement";

  public CmmnToPlanDef() {
    // nothing to do
  }

  public PlanDefinition transform(ResourceIdentifier assetId, TDefinitions caseModel) {
    // Use of discretionary items causes a PlanningTable to be added to a separate Case? Check...
    List<TCase> nonDefaultCase = caseModel.getCase().stream()
        .filter(c -> c.getName() == null || !c.getName().startsWith("Page"))
        .collect(Collectors.toList());

    if (nonDefaultCase.size() != 1) {
      throw new IllegalArgumentException("TODO Support case models with 2+ cases");
    }
    return Optional.ofNullable(nonDefaultCase.get(0))
        .map(topCase -> process(assetId.getResourceId(), caseModel, topCase))
        .orElseThrow(IllegalArgumentException::new);
  }

  private PlanDefinition process(
      URI assetId,
      TDefinitions caseModel,
      TCase tCase) {

    PlanDefinition cpm = new PlanDefinition();

    mapIdentity(cpm, assetId);
    mapName(cpm, caseModel);
    mapSubject(cpm, caseModel);

    try {
      processStage(tCase.getCasePlanModel(), assetId, caseModel)
          .forEach(cpm::addAction);
    } catch (Throwable t) {
      t.printStackTrace();
    }

    return cpm;
  }

  private void mapIdentity(PlanDefinition cpm, URI assetId
      //    , TDefinitions caseModel
  ) {
    // TODO Need formal "Asset ID" and "Artifact ID" roles
    Identifier fhirAssetId = new Identifier()
        .setType(toCode(SemanticAnnotationRelTypeSeries.Is_Identified_By))
        .setValue(assetId.toString());

    cpm.setIdentifier(Collections.singletonList(fhirAssetId))
        .setVersion("TODO");

    cpm.setType(toCode(Care_Process_Model));
    cpm.setId("#" + UUID.randomUUID().toString());
  }


  private List<PlanDefinitionActionComponent> processStage(
      TStage stage,
      URI ccpmId,
      TDefinitions caseModel) {
    List<PlanDefinitionActionComponent> mappedPlanElements
        = processStageInternals(stage, ccpmId, caseModel);

    PlanDefinitionActionComponent group = new PlanDefinitionActionComponent();

    group.setId(stage.getId());
    group.setTitle(stage.getName());

    mapControls(stage.getDefaultControl(), group);

    getTypeCode(stage.getExtensionElements()).stream()
        .map(this::toCode)
        .forEach(cd -> this.addCodeIfMissing(cd, group));

    group.setGroupingBehavior(ActionGroupingBehavior.LOGICALGROUP);
    group.setType(new CodeableConcept().addCoding(new Coding()
        .setSystem("TODO")
        .setCode("Stage"))
    );
    mappedPlanElements.forEach(
        group::addAction
    );

    return Collections.singletonList(group);
  }


  private List<PlanDefinitionActionComponent> processStageInternals(TStage stage,
      URI ccpmId, TDefinitions caseModel) {
    List<PlanDefinitionActionComponent> mappedPlanElements = new ArrayList<>();

    for (TPlanItem planItem : stage.getPlanItem()) {
      processPlanItem(planItem, planItem.getDefinitionRef(), ccpmId, mappedPlanElements, caseModel);
    }

    for (TDiscretionaryItem discretionaryItem : getDiscretionaryItems(stage.getPlanningTable())) {
      processPlannableItem(discretionaryItem, discretionaryItem.getDefinitionRef(), ccpmId,
          mappedPlanElements, caseModel);
    }

    for (TPlanItem planItem : stage.getPlanItem()) {
      List<TSentry> entrySentries = collectEntrySentries(planItem);

      for (TSentry sentry : entrySentries) {
        if (planItem.getDefinitionRef() instanceof TPlanItemDefinition) {
          processSequentiallyRelated((TPlanItemDefinition) planItem.getDefinitionRef(),
              mappedPlanElements, stage, sentry);
        } else {
          throw new UnsupportedOperationException("Defensive!");
        }
      }
    }

    return mappedPlanElements;
  }

  private List<TDiscretionaryItem> getDiscretionaryItems(TPlanningTable tableItem) {
    if (tableItem == null) {
      return Collections.emptyList();
    }
    return tableItem.getTableItem().stream()
        .map(JAXBElement::getValue)
        .flatMap(StreamUtil.filterAs(TDiscretionaryItem.class))
        .collect(Collectors.toList());
  }

  private void processPlanItem(TPlanItem planItem,
      Object definition, URI ccpmId,
      List<PlanDefinitionActionComponent> mappedPlanElements,
      TDefinitions caseModel) {
    if (definition != null) {
      if (definition instanceof TDecisionTask) {
        mappedPlanElements
            .add(this.processDecisionTask(planItem, (TDecisionTask) definition, caseModel));
      } else if (definition instanceof TStage) {
        mappedPlanElements.addAll(this.processStage((TStage) definition, ccpmId, caseModel));
      } else if (definition instanceof TProcessTask) {
        // Implement mapping of TProcessTask
      } else if (definition instanceof TEventListener) {
        // Implement mapping of TEventListener
      } else if (definition instanceof THumanTask) {
        mappedPlanElements.add(this.processHumanTask(planItem, (THumanTask) definition, caseModel));
      } else if (definition instanceof TTask) {
        mappedPlanElements.add(this.processGenericTask(planItem, (TTask) definition, caseModel));
      } else if (definition instanceof TMilestone) {
        this.processMilestone(planItem, (TMilestone) definition, caseModel);
      } else {
        throw new UnsupportedOperationException(
            "Cannot process " + definition.getClass().getName());
      }
    }
  }


  private void processPlannableItem(TDiscretionaryItem discretionaryItem,
      Object definition, URI ccpmId,
      List<PlanDefinitionActionComponent> mappedPlanElements,
      TDefinitions caseModel) {
    if (definition != null) {
      if (definition instanceof THumanTask) {
        mappedPlanElements.add(
            this.processDiscretionaryHumanTask(discretionaryItem, (THumanTask) definition,
                caseModel));
      }
    }
  }


  private void processCaseFileItem(
      TCaseFileItem caseFileItem,
      PlanDefinitionActionComponent planAction,
      TDefinitions caseModel) {
    String definitionId = caseFileItem.getDefinitionRef().getLocalPart();
    caseModel.getCaseFileItemDefinition().stream()
        .filter(itemDef -> itemDef.getId().equals(definitionId))
        .findFirst()
        .ifPresent(cfiDef -> processCaseFileItem(cfiDef, caseFileItem, planAction));
  }

  private void processCaseFileItem(TCaseFileItemDefinition cfiDef,
      TCaseFileItem cfi,
      PlanDefinitionActionComponent planAction) {
    if (CMIS_DOCUMENT_TYPE.equals(cfiDef.getDefinitionType())) {
      String url = resolveKnowledgeAsset(cfiDef);
      planAction.addDocumentation(new RelatedArtifact()
          .setUrl(url)
          .setDisplay(cfiDef.getName())
          .setDocument(new Attachment()
              .setTitle(cfiDef.getName())
              .setUrl(url)
              .setContentType("text/html")));
    } else if (XSD_ELEMENT_TYPE.equals(cfiDef.getDefinitionType())) {
      Collection<Term> annos = getSemanticAnnotation(cfi.getExtensionElements());
      if (annos.isEmpty()) {
        throw new IllegalStateException("Defensive!");
      }
      planAction.addInput(toSemanticInput(annos.iterator().next()));
    } else {
      throw new UnsupportedOperationException(
          "Unable to map CaseFileItems of type " + cfiDef.getDefinitionType());
    }
  }


  private String resolveKnowledgeAsset(TCaseFileItemDefinition cfiDef) {
    if (cfiDef.getStructureRef() == null) {
      return null;
    }
    return cfiDef.getStructureRef().getNamespaceURI() + "/" + cfiDef.getStructureRef()
        .getLocalPart().substring(1);
  }

  private List<TSentry> collectEntrySentries(TPlanItem planItem) {
    List<TSentry> sentries = new ArrayList<>();
    if (!planItem.getEntryCriterion().isEmpty()) {
      for (TEntryCriterion entryCriterion : planItem.getEntryCriterion()) {
        sentries.add((TSentry) entryCriterion.getSentryRef());
      }
    }
    return sentries;
  }

  private List<TSentry> collectExitSentries(TPlanItem planItem) {
    List<TSentry> sentries = new ArrayList<>();
    if (!planItem.getExitCriterion().isEmpty()) {
      for (TExitCriterion exitCriterion : planItem.getExitCriterion()) {
        sentries.add((TSentry) exitCriterion.getSentryRef());
      }
    }
    return sentries;
  }


  private void mapSubject(PlanDefinition cpm, TDefinitions tCase) {
    Optional.ofNullable(tCase.getExtensionElements())
        .flatMap(this::findSubject)
        .map(this::toCode)
        .map(Collections::singletonList)
        .ifPresent(cpm::setTopic);
  }

  private CodeableConcept toCode(Term cid) {
    return new CodeableConcept()
        .setCoding(Collections.singletonList(
            new Coding()
                .setCode(cid.getUuid().toString())
                .setDisplay(cid.getLabel())
                .setSystem(cid.getNamespaceUri().toString())
                .setVersion(cid.getVersionTag())));
  }

  private void mapName(PlanDefinition cpm, TDefinitions tCase) {
    cpm.setName(nameToIdentifier(tCase.getName(), IdentifierType.CLASS));
    cpm.setTitle(tCase.getName());
  }

  private void processSequentiallyRelated(
      TPlanItemDefinition itemWithSentry,
      List<PlanDefinitionActionComponent> scopedActions,
      TStage stage, TSentry sentry) {

    if (sentry.getOnPart() != null && !sentry.getOnPart().isEmpty()) {
      TOnPart onPart = sentry.getOnPart().get(0).getValue();

      if (onPart instanceof TPlanItemOnPart) {
        TPlanItemOnPart planItemOnPartPart = (TPlanItemOnPart) onPart;
        Object sourceRef = planItemOnPartPart.getSourceRef();
        if (sourceRef instanceof TSentry) {
          TSentry src = (TSentry) sourceRef;
          Optional<Object> itemRef = stage.getPlanItem().stream()
              .filter(pi -> collectExitSentries(pi).contains(src))
              .findFirst()
              .map(TPlanItem::getDefinitionRef);
          if (itemRef.isPresent() && itemRef.get() instanceof TPlanItemDefinition) {
            scopedActions.stream()
                .filter(act -> act.getId().equals(itemWithSentry.getId()))
                .findFirst()
                .ifPresent(act -> act.addRelatedAction(
                    new PlanDefinitionActionRelatedActionComponent()
                        .setRelationship(ActionRelationshipType.AFTER)
                        .setActionId(((TPlanItemDefinition) itemRef.get()).getId())));
          }
        } else if (sourceRef instanceof TPlanItem) {
          TPlanItem sourceItem = (TPlanItem) sourceRef;
          Object sourceDef = sourceItem.getDefinitionRef();

          Optional<PlanDefinitionActionComponent> whiteActOpt = scopedActions.stream()
              .filter(act -> act.getId().equals(itemWithSentry.getId()))
              .findFirst();
          PlanDefinitionActionComponent whiteAct = whiteActOpt.orElseThrow();

          if (sourceDef instanceof TPlanItem) {
            String refId = ((TCmmnElement) sourceDef).getId();
            // act resulting from the mapping of the item with the while diamond sentry
            whiteAct.addRelatedAction(
                new PlanDefinitionActionRelatedActionComponent()
                    .setRelationship(ActionRelationshipType.AFTER)
                    .setActionId(refId));
          } else if (sourceDef instanceof TEventListener) {
            TEventListener eventListener = (TEventListener) sourceDef;
            whiteAct.addTrigger()
                .setName(eventListener.getName())
                .setType(TriggerType.NAMEDEVENT);
          } else if (sourceDef instanceof TStage) {
            TStage srcStage = (TStage) sourceDef;
            PlanDefinitionActionComponent blackAct = scopedActions.stream()
                .filter(act -> act.getId().equals(srcStage.getId()))
                .findFirst().orElseThrow();
            whiteAct.addRelatedAction(
                new PlanDefinitionActionRelatedActionComponent()
                    .setRelationship(ActionRelationshipType.AFTER)
                    .setActionId(blackAct.getId()));
          } else if (sourceDef instanceof TTask) {
            TTask srcTask = (TTask) sourceDef;
            PlanDefinitionActionComponent blackAct = scopedActions.stream()
                .filter(act -> act.getId().equals(srcTask.getId()))
                .findFirst().orElseThrow();
            whiteAct.addRelatedAction(
                new PlanDefinitionActionRelatedActionComponent()
                    .setRelationship(ActionRelationshipType.AFTER)
                    .setActionId(blackAct.getId()));
          } else if (sourceDef instanceof TMilestone) {
            TMilestone milestone = (TMilestone) sourceDef;
            // expect one annotation - this will break defensively if the milestone is not annotated
            Collection<Term> annos = getSemanticAnnotation(milestone.getExtensionElements());
            if (annos.isEmpty()) {
              throw new IllegalStateException("Defensive!");
            }
            // model milestone as a state + enabler
            Term anno = annos.iterator().next();
            whiteAct.addCondition()
                .setKind(ActionConditionKind.START)
                .setExpression(
                    new Expression()
                        .setLanguage(FHIRPath_STU1.getReferentId().toString())
                        .setExpression("Resource.where(tag = '" + anno.getTag() + "').exists()"));

            // model milestone as a trigger
            DataRequirement dataRequirement = toSemanticInput(anno);
            whiteAct.addTrigger()
                .setType(TriggerType.DATAACCESSED)
                .addData(dataRequirement);
          } else {
            throw new UnsupportedOperationException("Defensive!");
          }
        } else {
          throw new UnsupportedOperationException("Defensive!");
        }
      }
      if (onPart instanceof TCaseFileItemOnPart) {
        TCaseFileItemOnPart caseFileItemOnPart = (TCaseFileItemOnPart) onPart;
        Object sourceRef = caseFileItemOnPart.getSourceRef();
        if (sourceRef instanceof TCaseFileItem) {
          TCaseFileItem srcCFI = (TCaseFileItem) sourceRef;
          PlanDefinitionActionComponent whiteAct = scopedActions.stream()
              .filter(act -> act.getId().equals(itemWithSentry.getId()))
              .findFirst().orElseThrow();
          whiteAct.addTrigger()
              .setType(TriggerType.DATACHANGED)
              .setName(srcCFI.getName());
        } else {
          throw new UnsupportedOperationException("Defensive!");
        }
      } else if (onPart instanceof TPlanItemOnPart) {
        TPlanItemOnPart planItemOnPart = (TPlanItemOnPart) onPart;
        if (planItemOnPart.getSourceRef() instanceof TPlanItem) {
          TPlanItem sourceRef = (TPlanItem) planItemOnPart.getSourceRef();
          Object sourceDef = sourceRef.getDefinitionRef();
          if (sourceDef instanceof TEventListener) {
            TEventListener eventListener = (TEventListener) sourceDef;
            PlanDefinitionActionComponent whiteAct = scopedActions.stream()
                .filter(act -> act.getId().equals(itemWithSentry.getId()))
                .findFirst().orElseThrow();
            whiteAct.addTrigger()
                .setType(TriggerType.NAMEDEVENT)
                .setName(eventListener.getName());
          } else {
            //TODO FIXME this gets called for a combination already visited - the IF/THEN may be overlapping
            //throw new UnsupportedOperationException("Defensive!");
          }
        } else if (planItemOnPart.getSourceRef() instanceof TSentry) {
          // TODO Black diamond - White diamond
        } else {
          throw new UnsupportedOperationException("Defensive!");
        }
      } else {
        throw new UnsupportedOperationException("Defensive!");
      }
    }
  }


  private PlanDefinitionActionComponent processTask(
      TTask task,
      TDefinitions caseModel,
      TPlanItem planItem) {

    PlanDefinitionActionComponent planAction = new PlanDefinitionActionComponent();
    planAction.setId(task.getId());
    planAction.setTitle(task.getName());

    getTypeCode(task.getExtensionElements()).stream()
        .map(this::toCode)
        .forEach(cd -> this.addCodeIfMissing(cd, planAction));

    getControls(planItem, task)
        .ifPresent(ctrl -> mapControls(ctrl, planAction));

    processAssociatedItems(planItem, caseModel, planAction);

    return planAction;
  }

  private void addCodeIfMissing(CodeableConcept cd, PlanDefinitionActionComponent planAction) {
    boolean hasCode = planAction
        .getCode().stream().anyMatch(c ->
            cd.getCoding().stream().anyMatch(d ->
                c.getCoding().stream().anyMatch(e ->
                    e.getCode().equals(d.getCode()))));
    if (! hasCode) {
      planAction.addCode(cd);
    }
  }

  private PlanDefinitionActionComponent processDiscretionaryTask(
      TTask task,
      TDefinitions caseModel,
      TDiscretionaryItem discretionaryItem) {

    PlanDefinitionActionComponent planAction = new PlanDefinitionActionComponent();
    planAction.setId(task.getId());
    planAction.setTitle(task.getName());

    getTypeCode(task.getExtensionElements()).stream()
        .map(this::toCode)
        .forEach(cd -> this.addCodeIfMissing(cd, planAction));

    getControls(discretionaryItem, task)
        .ifPresent(ctrl -> mapControls(ctrl, planAction));

    processAssociatedItems(discretionaryItem, caseModel, planAction);

    return planAction;
  }


  private PlanDefinitionActionComponent processHumanTask(
      TPlanItem planItem,
      THumanTask humanTask,
      TDefinitions caseModel) {
    PlanDefinitionActionComponent planAction
        = processTask(humanTask, caseModel, planItem);

    addAnnotations(humanTask.getExtensionElements(), planAction);

    planAction.setType(new CodeableConcept()
        .addCoding(new Coding()
            .setSystem("TODO")
            .setCode("HumanTask"))
    );
    return planAction;
  }

  private PlanDefinitionActionComponent processDiscretionaryHumanTask(
      TDiscretionaryItem planItem,
      THumanTask humanTask,
      TDefinitions caseModel) {
    PlanDefinitionActionComponent planAction
        = processDiscretionaryTask(humanTask, caseModel, planItem);

    addAnnotations(humanTask.getExtensionElements(), planAction);

    planAction.setType(new CodeableConcept()
        .addCoding(new Coding()
            .setSystem("TODO")
            .setCode("HumanTask"))
    );
    return planAction;
  }

  private PlanDefinitionActionComponent processGenericTask(
      TPlanItem planItem,
      TTask task,
      TDefinitions caseModel) {
    PlanDefinitionActionComponent planAction
        = processTask(task, caseModel, planItem);

    addAnnotations(task.getExtensionElements(), planAction);

    planAction.setType(new CodeableConcept()
        .addCoding(new Coding()
            .setSystem("TODO")
            .setCode("Generic Task"))
    );
    return planAction;
  }

  private PlanDefinitionActionComponent processMilestone(
      TPlanItem planItem,
      TMilestone milestone,
      TDefinitions caseModel) {
    // nothing to to with Milestones per se
    // Milestones get absorbed into the Task/Action that the milestone is linked to
    return null;
  }

  private PlanDefinition.PlanDefinitionActionComponent processDecisionTask(
      TPlanItem planItem,
      TDecisionTask tDecisionTask,
      TDefinitions caseModel) {

    PlanDefinitionActionComponent planAction
        = processTask(tDecisionTask, caseModel, planItem);

    addDefinition(planAction, tDecisionTask, caseModel);

    addAnnotations(tDecisionTask.getExtensionElements(), planAction);

    planAction.setType(new CodeableConcept()
        .addCoding(new Coding()
            .setSystem("TODO")
            .setCode("DecisionTask")
        ));
    return planAction;
  }

  private void addAnnotations(TExtensionElements extensionElements,
      PlanDefinitionActionComponent planAction) {
    if (extensionElements == null || extensionElements.getAny().isEmpty()) {
      return;
    }

    extensionElements.getAny().stream()
        .flatMap(StreamUtil.filterAs(Annotation.class))
        .map(Annotation::getRef)
        .map(this::toCode)
        .forEach(planAction::addCode);
  }

  private void processAssociatedItems(TPlanItem planItem, TDefinitions caseModel,
      PlanDefinitionActionComponent planAction) {
    caseModel.getArtifact().stream()
        .map(JAXBElement::getValue)
        .flatMap(StreamUtil.filterAs(TAssociation.class))
        .filter(assoc -> assoc.getSourceRef() == planItem
            || assoc.getTargetRef() == planItem)
        .forEach(assoc -> processAssociatedItem(caseModel, planAction, assoc));
  }


  private void processAssociatedItems(TDiscretionaryItem planItem, TDefinitions caseModel,
      PlanDefinitionActionComponent planAction) {
    caseModel.getArtifact().stream()
        .map(JAXBElement::getValue)
        .flatMap(StreamUtil.filterAs(TAssociation.class))
        .filter(assoc -> assoc.getSourceRef() == planItem
            || assoc.getTargetRef() == planItem)
        .forEach(assoc -> processAssociatedItem(caseModel, planAction, assoc));
  }

  private void processAssociatedItem(
      TDefinitions caseModel,
      PlanDefinitionActionComponent planAction,
      TAssociation assoc) {
    if (assoc.getTargetRef() instanceof TCaseFileItem) {
      processCaseFileItem((TCaseFileItem) assoc.getTargetRef(), planAction, caseModel);
    } else if (assoc.getSourceRef() instanceof TCaseFileItem) {
      processCaseFileItem((TCaseFileItem) assoc.getSourceRef(), planAction, caseModel);
    }
  }

  private void addDefinition(
      PlanDefinitionActionComponent planAction,
      TDecisionTask tDecisionTask,
      TDefinitions caseModel) {

    Optional<TDecision> decisionPointer = getDecisionForTask(tDecisionTask, caseModel);
    decisionPointer.ifPresent(dec -> {
      if (dec.getExternalRef() == null) {
        log.warn("Broken Decision Pointer : {} - {}",
            dec.getName(),
            dec.getId());
      } else {
        planAction.setDefinition(
            new CanonicalType()
                .setValue(dec.getExternalRef().getNamespaceURI())
        );
      }
    });
  }

  private void mapControls(TPlanItemControl ctrl, PlanDefinitionActionComponent planAction) {
    planAction.setPrecheckBehavior(ActionPrecheckBehavior.NO);

    planAction.setCardinalityBehavior(ctrl != null && ctrl.getRepetitionRule() != null
        ? ActionCardinalityBehavior.MULTIPLE
        : ActionCardinalityBehavior.SINGLE);

    planAction.setRequiredBehavior(ctrl != null && ctrl.getRequiredRule() != null
        ? ActionRequiredBehavior.MUSTUNLESSDOCUMENTED
        : ActionRequiredBehavior.COULD);

  }

  private Optional<TPlanItemControl> getControls(TPlanItem planItem, TTask tDecisionTask) {
    return Optional.ofNullable(
        planItem.getItemControl() != null
            ? planItem.getItemControl()
            : tDecisionTask.getDefaultControl());
  }

  private Optional<TPlanItemControl> getControls(TDiscretionaryItem planItem, TTask tDecisionTask) {
    return Optional.ofNullable(
        planItem.getItemControl() != null
            ? planItem.getItemControl()
            : tDecisionTask.getDefaultControl());
  }

  private Optional<TDecision> getDecisionForTask(TDecisionTask tDecisionTask,
      TDefinitions caseModel) {
    QName qname = tDecisionTask.getDecisionRef();
    if (qname == null) {
      log.error("Unlinked Decision task {}", tDecisionTask.getName());
      return Optional.empty();
    }
    return Optional.ofNullable(
        caseModel.getDecision().stream()
            .filter(dec -> dec.getId().equals(qname.getLocalPart()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Case " +
                " : Decision Task " +
                tDecisionTask.getName() +
                " without an associated Decision Model ")));
  }


  private static Collection<Term> getTypeCode(TExtensionElements extensionElements) {
    if (extensionElements == null) {
      return Collections.emptyList();
    }
    return getTypeCode(extensionElements.getAny());
  }

  private static List<Term> getTypeCode(List<Object> extensionElements) {
    if (extensionElements == null || extensionElements.isEmpty()) {
      return Collections.emptyList();
    }

    return extensionElements.stream()
        .flatMap(StreamUtil.filterAs(Annotation.class))
        .filter(ann -> Captures.sameTermAs(ann.getRel()))
        .map(Annotation::getRef)
        .map(Term.class::cast)
        .collect(Collectors.toList());
  }


  private Optional<ConceptIdentifier> findSubject(TExtensionElements extensionElements) {
    return this.findSubject(extensionElements.getAny());
  }

  private Optional<ConceptIdentifier> findSubject(List<Object> extensionElements) {
    if (extensionElements != null) {
      List<Annotation> annotations = extensionElements.stream()
          .flatMap(StreamUtil.filterAs(Annotation.class))
          .filter(annotation -> annotation.getRel().getConceptId()
              .equals(SemanticAnnotationRelTypeSeries.Has_Primary_Subject.getConceptId()))
          .collect(Collectors.toList());

      if (annotations.size() > 1) {
        throw new IllegalStateException("Cannot have more than one subject.");
      }

      if (annotations.size() == 1) {
        Annotation annotation = annotations.get(0);
        return Optional.of(annotation.getRef());
      }
    }
    return Optional.empty();
  }


  private DataRequirement toSemanticInput(Term anno) {
    DataRequirement dataRequirement = new DataRequirement();
    DataRequirementCodeFilterComponent codeFilters = new DataRequirementCodeFilterComponent();
    dataRequirement.addCodeFilter(codeFilters);
    codeFilters.addCode(toCode(anno).getCodingFirstRep());
    return dataRequirement;
  }

  private static Collection<Term> getSemanticAnnotation(TExtensionElements extensionElements) {
    return extensionElements == null
        ? Collections.emptyList()
        : getSemanticAnnotation(extensionElements.getAny());
  }

  private static List<Term> getSemanticAnnotation(List<Object> extensionElements) {
    if (extensionElements == null || extensionElements.isEmpty()) {
      return Collections.emptyList();
    }

    return extensionElements.stream()
        .flatMap(StreamUtil.filterAs(Annotation.class))
        .map(Annotation::getRef)
        .map(Term.class::cast)
        .collect(Collectors.toList());
  }


}