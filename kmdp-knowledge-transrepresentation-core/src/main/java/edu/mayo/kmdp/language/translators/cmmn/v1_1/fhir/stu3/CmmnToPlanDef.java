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
package edu.mayo.kmdp.language.translators.cmmn.v1_1.fhir.stu3;

import static edu.mayo.kmdp.language.common.fhir.stu3.FHIRPlanDefinitionUtils.setKnowledgeIdentifiers;
import static edu.mayo.kmdp.language.common.fhir.stu3.FHIRPlanDefinitionUtils.toCodeableConcept;
import static edu.mayo.kmdp.language.common.fhir.stu3.FHIRPlanDefinitionUtils.toFHIRIdentifier;
import static edu.mayo.kmdp.util.CharsetEncodingUtil.sanitizeToUTF8;
import static edu.mayo.kmdp.util.NameUtils.nameToIdentifier;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.Captures;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.Has_Focus;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.Has_Primary_Subject;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newVersionId;
import static org.omg.spec.api4kp._20200801.id.Term.newTerm;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultCarrierUUID;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Cognitive_Care_Process_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.API4KP;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIRPath_STU1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;

import edu.mayo.kmdp.language.common.fhir.stu3.FHIRPlanDefinitionUtils;
import edu.mayo.kmdp.language.translators.dmn.v1_2.fhir.stu3.DmnToPlanDefTranslator;
import edu.mayo.kmdp.util.NameUtils.IdentifierType;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.Util;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBElement;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DataRequirement;
import org.hl7.fhir.dstu3.model.Enumerations.PublicationStatus;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.PlanDefinition.ActionCardinalityBehavior;
import org.hl7.fhir.dstu3.model.PlanDefinition.ActionConditionKind;
import org.hl7.fhir.dstu3.model.PlanDefinition.ActionGroupingBehavior;
import org.hl7.fhir.dstu3.model.PlanDefinition.ActionPrecheckBehavior;
import org.hl7.fhir.dstu3.model.PlanDefinition.ActionRelationshipType;
import org.hl7.fhir.dstu3.model.PlanDefinition.ActionRequiredBehavior;
import org.hl7.fhir.dstu3.model.PlanDefinition.PlanDefinitionActionComponent;
import org.hl7.fhir.dstu3.model.PlanDefinition.PlanDefinitionActionRelatedActionComponent;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType;
import org.hl7.fhir.dstu3.model.TriggerDefinition.TriggerType;
import org.omg.spec.api4kp._20200801.id.ConceptIdentifier;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.id.Term;
import org.omg.spec.api4kp._20200801.surrogate.Annotation;
import org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries;
import org.omg.spec.cmmn._20151109.model.CaseFileItemTransition;
import org.omg.spec.cmmn._20151109.model.TApplicabilityRule;
import org.omg.spec.cmmn._20151109.model.TAssociation;
import org.omg.spec.cmmn._20151109.model.TCase;
import org.omg.spec.cmmn._20151109.model.TCaseFileItem;
import org.omg.spec.cmmn._20151109.model.TCaseFileItemDefinition;
import org.omg.spec.cmmn._20151109.model.TCaseFileItemOnPart;
import org.omg.spec.cmmn._20151109.model.TCaseParameter;
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
import org.omg.spec.cmmn._20151109.model.TPlanFragment;
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

public class CmmnToPlanDef {

  private static final Logger log = LoggerFactory.getLogger(CmmnToPlanDef.class);

  static final String CMIS_DOCUMENT_TYPE = "http://www.omg.org/spec/CMMN/DefinitionType/CMISDocument";
  static final String XSD_ELEMENT_TYPE = "http://www.omg.org/spec/CMMN/DefinitionType/XSDElement";

  public CmmnToPlanDef() {
    // nothing to do
  }

  public PlanDefinition transform(
      ResourceIdentifier assetId,
      ResourceIdentifier srcArtifactId,
      ResourceIdentifier tgtArtifactId,
      TDefinitions caseModel) {
    // Use of discretionary items causes a PlanningTable to be added to a separate Case? Check...
    List<TCase> nonDefaultCase = caseModel.getCase().stream()
        .filter(c -> c.getName() == null || !c.getName().startsWith("Page"))
        .collect(Collectors.toList());

    if (nonDefaultCase.size() != 1) {
      throw new UnsupportedOperationException(format(
          "Unable to Support Case Model %s with 2+ Cases, found %s",
          caseModel.getName(),
          nonDefaultCase.stream().map(TCase::getName).collect(Collectors.joining(","))));
    }
    return nonDefaultCase.stream().findFirst()
        .map(topCase -> process(assetId, srcArtifactId, tgtArtifactId, caseModel, topCase))
        .orElseThrow(() ->
            new IllegalStateException(format(
                "No Cases found in Case Model %s", caseModel.getName())));
  }

  private PlanDefinition process(
      ResourceIdentifier assetId,
      ResourceIdentifier srcArtifactId,
      ResourceIdentifier tgtArtifactId,
      TDefinitions caseModel,
      TCase tCase) {

    PlanDefinition cpm = new PlanDefinition();

    mapIdentity(cpm, assetId, srcArtifactId, tgtArtifactId);
    mapName(cpm, caseModel);
    mapTopic(cpm, caseModel);

    try {
      processStage(tCase.getCasePlanModel(), assetId, caseModel)
          .forEach(cpm::addAction);
    } catch (Throwable t) {
      t.printStackTrace();
    }

    return cpm;
  }

  private void mapIdentity(
      PlanDefinition cpm,
      ResourceIdentifier assetId,
      //    , TDefinitions caseModel
      ResourceIdentifier srcArtifactId,
      ResourceIdentifier tgtArtifactId) {

    setKnowledgeIdentifiers(cpm, assetId, tgtArtifactId);

    cpm.setRelatedArtifact(singletonList(
        new RelatedArtifact()
            .setType(RelatedArtifactType.DERIVEDFROM)
            .setUrl(srcArtifactId.toString())));

    cpm.setType(toCodeableConcept(Cognitive_Care_Process_Model));
    cpm.setVersion(tgtArtifactId.getVersionTag());
    cpm.setDate(tgtArtifactId.getEstablishedOn());
    cpm.setStatus(
        tgtArtifactId.getVersionTag().indexOf('-') >= 0
        ? PublicationStatus.DRAFT : PublicationStatus.ACTIVE);
    cpm.setId(tgtArtifactId.getUuid().toString());
  }


  private List<PlanDefinition.PlanDefinitionActionComponent> processStage(
      TStage stage,
      ResourceIdentifier assetId,
      TDefinitions caseModel) {
    List<PlanDefinition.PlanDefinitionActionComponent> mappedPlanElements
        = processStageInternals(stage, assetId, caseModel);

    var group = new PlanDefinitionActionComponent();

    group.setId(stage.getId());
    group.setTitle(sanitizeToUTF8(stage.getName()));
    mapSubject(group, stage);

    mapControls(stage.getDefaultControl(), group);

    getTypeCode(stage.getExtensionElements()).stream()
        .map(FHIRPlanDefinitionUtils::toCodeableConcept)
        .forEach(cd -> this.addCodeIfMissing(cd, group));

    group.setGroupingBehavior(ActionGroupingBehavior.LOGICALGROUP);
    group.setType(new Coding()
        .setSystem(
            KnowledgeRepresentationLanguageSerializationSeries.CMMN_1_1_XML_Syntax.getReferentId()
                .toString())
        .setCode("Stage")
    );
    mappedPlanElements.forEach(
        group::addAction
    );

    return singletonList(group);
  }


  private List<PlanDefinitionActionComponent> processStageInternals(TStage stage,
      ResourceIdentifier assetId, TDefinitions caseModel) {
    List<PlanDefinition.PlanDefinitionActionComponent> mappedPlanElements = new ArrayList<>();

    for (TPlanItem planItem : stage.getPlanItem()) {
      processPlanItem(planItem, planItem.getDefinitionRef(), assetId, mappedPlanElements,
          caseModel);
    }

    for (TDiscretionaryItem discretionaryItem : getDiscretionaryItems(stage.getPlanningTable())) {
      processPlannableItem(discretionaryItem, discretionaryItem.getDefinitionRef(), assetId,
          mappedPlanElements, caseModel);
    }

    for (TPlanItem planItem : stage.getPlanItem()) {
      List<TSentry> entrySentries = collectEntrySentries(planItem);

      for (TSentry sentry : entrySentries) {
        if (planItem.getDefinitionRef() instanceof TPlanItemDefinition) {
          processSequentiallyRelated((TPlanItemDefinition) planItem.getDefinitionRef(),
              mappedPlanElements, stage, sentry);
        } else {
          throw new UnsupportedOperationException(format(
              "Defensive! Sentry %s in Case %s is connected to Item of type %s",
              sentry.getId(), caseModel.getName(), planItem.getDefinitionRef().getClass()));
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
      Object definition, ResourceIdentifier assetId,
      List<PlanDefinitionActionComponent> mappedPlanElements,
      TDefinitions caseModel) {
    if (definition != null) {
      if (definition instanceof TDecisionTask) {
        mappedPlanElements
            .add(this.processDecisionTask(planItem, (TDecisionTask) definition, caseModel));
      } else if (definition instanceof TStage) {
        mappedPlanElements.addAll(this.processStage((TStage) definition, assetId, caseModel));
      } else if (definition instanceof TProcessTask) {
        mappedPlanElements.add(
            this.processProcessTask(planItem, (TProcessTask) definition, caseModel));
      } else if (definition instanceof TEventListener) {
        // Implement mapping of TEventListener
      } else if (definition instanceof THumanTask) {
        mappedPlanElements.add(this.processHumanTask(planItem, (THumanTask) definition, caseModel));
      } else if (definition instanceof TTask) {
        mappedPlanElements.add(this.processGenericTask(planItem, (TTask) definition, caseModel));
      } else if (definition instanceof TMilestone) {
        this.processMilestone(planItem, (TMilestone) definition, caseModel);
      } else {
        throw new UnsupportedOperationException(format(
            "Cannot process case model %s fragment of type %s",
            caseModel.getName(), definition.getClass().getName()));
      }
    }
  }


  private void processPlannableItem(TDiscretionaryItem discretionaryItem,
      Object definition, ResourceIdentifier assetId,
      List<PlanDefinitionActionComponent> mappedPlanElements,
      TDefinitions caseModel) {
    if (definition != null) {
      if (definition instanceof THumanTask) {
        mappedPlanElements.add(
            this.processDiscretionaryHumanTask(discretionaryItem, (THumanTask) definition,
                caseModel));
      } else if (definition instanceof TDecisionTask) {
        mappedPlanElements.add(
            this.processDiscretionaryDecisionTask(discretionaryItem, (TDecisionTask) definition,
                caseModel));
      } else {
        throw new UnsupportedOperationException(format(
            "Defensive! Unable to process Discretionary %s trait with Task type %s",
            discretionaryItem.getId(), definition.getClass().getSimpleName()));
      }
    }
  }


  private void processCaseFileItem(
      TCaseFileItem caseFileItem,
      PlanDefinitionActionComponent planAction,
      TDefinitions caseModel,
      Object associated) {
    String definitionId = caseFileItem.getDefinitionRef().getLocalPart();
    caseModel.getCaseFileItemDefinition().stream()
        .filter(itemDef -> itemDef.getId().equals(definitionId))
        .findFirst()
        .ifPresent(
            cfiDef -> processCaseFileItem(cfiDef, caseFileItem, planAction,
                toTask(caseFileItem, associated)));
  }

  private TTask toTask(TCaseFileItem cfi, Object associated) {
    Object x = associated;
    if (x instanceof TPlanItem) {
      x = ((TPlanItem) associated).getDefinitionRef();
      if (x instanceof TTask) {
        return (TTask) x;
      }
    } else if (x instanceof TDiscretionaryItem) {
      x = ((TDiscretionaryItem) associated).getDefinitionRef();
      if (x instanceof TTask) {
        return (TTask) x;
      }
    }
    throw new UnsupportedOperationException(format(
        "Defensive - Unable to process Case File Item %s associated to %s",
        cfi.getId(), x.getClass().getSimpleName()));
  }

  private void processCaseFileItem(TCaseFileItemDefinition cfiDef,
      TCaseFileItem cfi,
      PlanDefinitionActionComponent planAction,
      TTask associated) {
    if (CMIS_DOCUMENT_TYPE.equals(cfiDef.getDefinitionType())) {
      String url = resolveKnowledgeAsset(cfiDef);
      planAction.addDocumentation(new RelatedArtifact()
          .setUrl(url)
          .setDisplay(cfiDef.getName())
          .setDocument(new Attachment()
              .setTitle(sanitizeToUTF8(cfiDef.getName()))
              .setUrl(url)
              .setContentType("text/html")));
    } else if (XSD_ELEMENT_TYPE.equals(cfiDef.getDefinitionType())) {
      Collection<Term> annos = getSemanticAnnotation(cfi.getExtensionElements());
      if (annos.isEmpty()) {
        throw new IllegalStateException(format(
            "Defensive! - Data CFI %s without a semantic annotation", cfi.getName()));
      }
      if (associated.getInput().stream().map(TCaseParameter::getBindingRef)
          .anyMatch(x -> x == cfi)) {
        planAction.addInput(toSemanticDataRequirement(annos.iterator().next()));
      }
      if (associated.getOutput().stream().map(TCaseParameter::getBindingRef)
          .anyMatch(x -> x == cfi)) {
        planAction.addOutput(toSemanticDataRequirement(annos.iterator().next()));
      }
    } else {
      throw new UnsupportedOperationException(format(
          "Unable to map CaseFileItems of type %s", cfiDef.getDefinitionType()));
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


  private void mapTopic(PlanDefinition cpm, TDefinitions tCase) {
    Optional.ofNullable(tCase.getExtensionElements())
        .flatMap(this::findTopic)
        .map(FHIRPlanDefinitionUtils::toCodeableConcept)
        .map(Collections::singletonList)
        .ifPresent(cpm::setTopic);
  }

  private void mapSubject(PlanDefinitionActionComponent cpm, TPlanFragment planFragment) {
    Optional.ofNullable(planFragment.getExtensionElements())
        .flatMap(this::findSubject)
        .map(FHIRPlanDefinitionUtils::toCodeableConcept)
        .map(Collections::singletonList)
        .ifPresent(cpm::setReason);
  }

  private void mapName(PlanDefinition cpm, TDefinitions tCase) {
    cpm.setName(nameToIdentifier(tCase.getName(), IdentifierType.CLASS));
    cpm.setTitle(sanitizeToUTF8(tCase.getName()));
  }

  private void processSequentiallyRelated(
      TPlanItemDefinition itemWithSentry,
      List<PlanDefinitionActionComponent> scopedActions,
      TStage stage, TSentry sentry) {

    if (sentry.getOnPart() != null && !sentry.getOnPart().isEmpty()) {
      // TODO - handle sentries with multiple, possibly hybrid, onParts better as needed
      TOnPart onPart = sentry.getOnPart().get(0).getValue();

      if (onPart instanceof TPlanItemOnPart) {
        TPlanItemOnPart planItemOnPartPart = (TPlanItemOnPart) onPart;
        Object sourceRef = planItemOnPartPart.getSourceRef();
        if (sourceRef instanceof TSentry) {
          processPlanItemOnPartWithSentrySource(itemWithSentry, scopedActions, stage,
              (TSentry) sourceRef);
        } else if (sourceRef instanceof TPlanItem) {
          processPlanItemOnPartWithPlanItemSource(itemWithSentry, scopedActions, stage,
              (TPlanItem) sourceRef);
        } else if (sourceRef instanceof TDiscretionaryItem) {
          processPlanItemOnPartWithDiscretionarySource(itemWithSentry, scopedActions, stage,
              (TDiscretionaryItem) sourceRef);
        } else {
          throw new UnsupportedOperationException(
              format("Defensive! Unable to process Sentry %s with onPart source of type %s",
                  sentry.getId(), sourceRef.getClass().getSimpleName()));
        }
      } else if (onPart instanceof TCaseFileItemOnPart) {
        // CFI are likely AND-ed
        List<TCaseFileItemOnPart> cfiOnParts = sentry.getOnPart().stream()
            .map(JAXBElement::getValue)
            .flatMap(StreamUtil.filterAs(TCaseFileItemOnPart.class))
            .collect(Collectors.toList());
        boolean allCFISources = cfiOnParts.stream()
            .map(TCaseFileItemOnPart::getSourceRef)
            .allMatch(TCaseFileItem.class::isInstance);
        if (!allCFISources) {
          throw new IllegalStateException(format(
              "Defensive: Found Sentry %s with multiple CFI sources not connected to CFIs",
              sentry.getId()));
        }
        processCFIOnPartWithCFISource(
            itemWithSentry, scopedActions, stage, cfiOnParts);
      } else {
        throw new UnsupportedOperationException(format(
            "Defensive! Unable to process Sentry %s with onPart of type %s",
            sentry.getId(), onPart.getClass().getSimpleName()));
      }
    }
  }

  private void processCFIOnPartWithCFISource(TPlanItemDefinition itemWithSentry,
      List<PlanDefinitionActionComponent> scopedActions, TStage stage,
      List<TCaseFileItemOnPart> sourceRef) {
    PlanDefinitionActionComponent whiteAct = scopedActions.stream()
        .filter(act -> act.getId().equals(itemWithSentry.getId()))
        .findFirst().orElseThrow();

    var dataReq = new DataRequirement();
    sourceRef.stream()
        .map(TCaseFileItemOnPart::getSourceRef)
        .flatMap(StreamUtil.filterAs(TCaseFileItem.class))
        .flatMap(cfi -> getSemanticAnnotation(cfi.getExtensionElements()).stream())
        .map(FHIRPlanDefinitionUtils::toCodeableConcept)
        .forEach(cd -> dataReq.addCodeFilter().addValueCodeableConcept(cd));

    var events = sourceRef.stream()
        .flatMap(StreamUtil.filterAs(TCaseFileItemOnPart.class))
        .map(TCaseFileItemOnPart::getStandardEvent)
        .collect(Collectors.toSet());

    whiteAct.addTriggerDefinition()
        .setType(mapCaseFileItemTransitions(events))
        .setEventName(mapEventName(sourceRef, events))
        .setEventData(dataReq);
  }

  private String mapEventName(
      List<TCaseFileItemOnPart> sourceRef,
      Set<CaseFileItemTransition> events) {
    String byName = sourceRef.stream()
        .map(TOnPart::getName)
        .filter(Util::isNotEmpty)
        .collect(Collectors.joining(","));
    return Util.isNotEmpty(byName)
        ? "on " + byName
        : "on " + events.stream()
            .map(CaseFileItemTransition::value)
            .collect(Collectors.joining(", "));
  }

  private TriggerType mapCaseFileItemTransitions(Set<CaseFileItemTransition> events) {
    if (events.size() != 1) {
      return TriggerType.DATAMODIFIED;
    }
    switch (events.iterator().next()) {
      case DELETE:
        return TriggerType.DATAREMOVED;
      case CREATE:
        return TriggerType.DATAADDED;
      default:
        return TriggerType.DATAMODIFIED;
    }
  }

  private void processPlanItemOnPartWithDiscretionarySource(TPlanItemDefinition itemWithSentry,
      List<PlanDefinitionActionComponent> scopedActions, TStage stage,
      TDiscretionaryItem sourceRef) {
    Object sourceDef = sourceRef.getDefinitionRef();

    Optional<PlanDefinitionActionComponent> whiteActOpt = scopedActions.stream()
        .filter(act -> act.getId().equals(itemWithSentry.getId()))
        .findFirst();
    PlanDefinitionActionComponent whiteAct = whiteActOpt.orElseThrow();

    if (sourceRef.getDefinitionRef() instanceof TTask) {
      TTask srcTask = (TTask) sourceDef;
      PlanDefinitionActionComponent blackAct = scopedActions.stream()
          .filter(act -> act.getId().equals(srcTask.getId()))
          .findFirst().orElseThrow();
      whiteAct.addRelatedAction(
          new PlanDefinitionActionRelatedActionComponent()
              .setRelationship(ActionRelationshipType.AFTER)
              .setActionId(blackAct.getId()));
    }
  }

  private void processPlanItemOnPartWithPlanItemSource(TPlanItemDefinition itemWithSentry,
      List<PlanDefinitionActionComponent> scopedActions, TStage stage, TPlanItem sourceItem) {
    Object sourceDef = sourceItem.getDefinitionRef();

    Optional<PlanDefinitionActionComponent> whiteActOpt = scopedActions.stream()
        .filter(act -> act.getId().equals(itemWithSentry.getId()))
        .findFirst();
    PlanDefinitionActionComponent whiteAct = whiteActOpt.orElseThrow();

    if (sourceDef instanceof TPlanItem) {
      String refId = ((TPlanItem) sourceDef).getId();
      // act resulting from the mapping of the item with the while diamond sentry
      whiteAct.addRelatedAction(
          new PlanDefinitionActionRelatedActionComponent()
              .setRelationship(ActionRelationshipType.AFTER)
              .setActionId(refId));
    } else if (sourceDef instanceof TEventListener) {
      TEventListener eventListener = (TEventListener) sourceDef;
      whiteAct.addTriggerDefinition()
          .setEventName(eventListener.getName())
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
        throw new IllegalStateException(format(
            "Defensive! Milestone %s should be annotated with a semantic term, but none was found",
            milestone.getId()));
      }
      // model milestone as a state + enabler
      Term anno = annos.iterator().next();
      whiteAct.addCondition()
          .setKind(ActionConditionKind.START)
          .setLanguage(FHIRPath_STU1.getReferentId().toString())
          .setExpression("Resource.where(tag = '" + anno.getTag() + "').exists()");

      // model milestone as a trigger
      var dataRequirement = toSemanticDataRequirement(anno);
      whiteAct.addTriggerDefinition()
          .setType(TriggerType.DATAACCESSED)
          .setEventData(dataRequirement);
    } else {
      throw new UnsupportedOperationException(format(
          "Defensive! Unable to process plan item %s with source of type %s",
          sourceItem.getId(), sourceDef.getClass().getSimpleName()));
    }
  }

  private void processPlanItemOnPartWithSentrySource(
      TPlanItemDefinition itemWithSentry,
      List<PlanDefinitionActionComponent> scopedActions,
      TStage stage,
      TSentry sourceRef) {
    TSentry src = sourceRef;
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

  }


  private PlanDefinition.PlanDefinitionActionComponent processTask(
      TTask task,
      TDefinitions caseModel,
      TPlanItem planItem) {

    var planAction = new PlanDefinitionActionComponent();
    planAction.setId(task.getId());
    planAction.setTitle(sanitizeToUTF8(task.getName()));

    getTypeCode(task.getExtensionElements()).stream()
        .map(FHIRPlanDefinitionUtils::toCodeableConcept)
        .forEach(cd -> this.addCodeIfMissing(cd, planAction));

    getControls(planItem, task)
        .ifPresent(ctrl -> mapControls(ctrl, planAction));

    processAssociatedItems(planItem, caseModel, planAction, task);

    return planAction;
  }

  private void addCodeIfMissing(CodeableConcept cd, PlanDefinitionActionComponent planAction) {
    boolean hasCode = planAction
        .getCode().stream().anyMatch(c ->
            cd.getCoding().stream().anyMatch(d ->
                c.getCoding().stream().anyMatch(e ->
                    e.getCode().equals(d.getCode()))));
    if (!hasCode) {
      planAction.addCode(cd);
    }
  }

  private PlanDefinitionActionComponent processDiscretionaryTask(
      TTask task,
      TDefinitions caseModel,
      TDiscretionaryItem discretionaryItem) {

    var planAction = new PlanDefinitionActionComponent();
    planAction.setId(task.getId());
    planAction.setTitle(sanitizeToUTF8(task.getName()));

    getTypeCode(task.getExtensionElements()).stream()
        .map(FHIRPlanDefinitionUtils::toCodeableConcept)
        .forEach(cd -> this.addCodeIfMissing(cd, planAction));

    getControls(discretionaryItem, task)
        .ifPresent(ctrl -> mapControls(ctrl, planAction));

    processAssociatedItems(discretionaryItem, caseModel, planAction);

    discretionaryItem.getApplicabilityRuleRefs().stream()
        .flatMap(StreamUtil.filterAs(TApplicabilityRule.class))
        .forEach(app -> mapApplicabilityRule(app, caseModel, planAction));

    return planAction;
  }

  private void mapApplicabilityRule(TApplicabilityRule app, TDefinitions caseModel,
      PlanDefinitionActionComponent planAction) {
    if (!(app.getContextRef() instanceof TCaseFileItem)) {
      throw new UnsupportedOperationException(format(
          "Unable to process applicability rule %s with context %s",
          app.getId(), app.getContextRef()));
    }
    TCaseFileItem cfi = (TCaseFileItem) app.getContextRef();
    getSemanticAnnotation(cfi.getExtensionElements()).stream()
        .findFirst()
        .ifPresent(anno -> {
              planAction.addInput(toSemanticDataRequirement(anno));

              CodeableConcept cc = toCodeableConcept(anno);
              planAction.addCondition()
                  .setKind(ActionConditionKind.APPLICABILITY)
                  .setExpression(cc.getCodingFirstRep().getCode());
            }
        );
  }


  private PlanDefinitionActionComponent processHumanTask(
      TPlanItem planItem,
      THumanTask humanTask,
      TDefinitions caseModel) {
    PlanDefinition.PlanDefinitionActionComponent planAction
        = processTask(humanTask, caseModel, planItem);

    addAnnotations(humanTask.getExtensionElements(), planAction);

    planAction.setType(new Coding()
        .setSystem(
            KnowledgeRepresentationLanguageSerializationSeries.CMMN_1_1_XML_Syntax.getReferentId()
                .toString())
        .setCode("HumanTask")
    );
    return planAction;
  }

  private PlanDefinitionActionComponent processProcessTask(
      TPlanItem planItem,
      TProcessTask processTask,
      TDefinitions caseModel) {
    PlanDefinition.PlanDefinitionActionComponent planAction
        = processTask(processTask, caseModel, planItem);

    addAnnotations(processTask.getExtensionElements(), planAction);

    planAction.setType(new Coding()
        .setSystem(
            KnowledgeRepresentationLanguageSerializationSeries.CMMN_1_1_XML_Syntax.getReferentId()
                .toString())
        .setCode("ProcessTask")
    );

    if (processTask.getProcessRefExpression() != null
        && API4KP.getReferentId().toString().equals(
        processTask.getProcessRefExpression().getLanguage())) {
      var externalDef = newVersionId(
          URI.create(processTask.getProcessRefExpression().getContent().get(0).toString()
              .split(" ")[1]));
      planAction.setDefinition(new Reference()
          .setReference("#" + defaultCarrierUUID(externalDef, FHIR_STU3))
          .setDisplay(processTask.getName())
          .setIdentifier(toFHIRIdentifier(
              externalDef,
              newTerm(API4KP.getReferentId(), "KnowledgeAsset"))
          ));
    }
    return planAction;
  }

  private PlanDefinitionActionComponent processDiscretionaryHumanTask(
      TDiscretionaryItem planItem,
      THumanTask humanTask,
      TDefinitions caseModel) {
    PlanDefinitionActionComponent planAction
        = processDiscretionaryTask(humanTask, caseModel, planItem);

    addAnnotations(humanTask.getExtensionElements(), planAction);

    planAction.setType(new Coding()
        .setSystem(
            KnowledgeRepresentationLanguageSerializationSeries.CMMN_1_1_XML_Syntax.getReferentId()
                .toString())
        .setCode("HumanTask")
    );
    return planAction;
  }

  private PlanDefinitionActionComponent processDiscretionaryDecisionTask(
      TDiscretionaryItem planItem,
      TDecisionTask decisionTask,
      TDefinitions caseModel) {
    PlanDefinitionActionComponent planAction
        = processDiscretionaryTask(decisionTask, caseModel, planItem);

    addAnnotations(decisionTask.getExtensionElements(), planAction);

    addDefinition(planAction, decisionTask, caseModel);

    planAction.setType(new Coding()
        .setSystem(
            KnowledgeRepresentationLanguageSerializationSeries.CMMN_1_1_XML_Syntax.getReferentId()
                .toString())
        .setCode("DecisionTask")
    );
    return planAction;
  }

  private PlanDefinition.PlanDefinitionActionComponent processGenericTask(
      TPlanItem planItem,
      TTask task,
      TDefinitions caseModel) {
    PlanDefinition.PlanDefinitionActionComponent planAction
        = processTask(task, caseModel, planItem);

    addAnnotations(task.getExtensionElements(), planAction);

    planAction.setType(new Coding()
        .setSystem(
            KnowledgeRepresentationLanguageSerializationSeries.CMMN_1_1_XML_Syntax.getReferentId()
                .toString())
        .setCode("Generic Task")
    );
    return planAction;
  }

  private PlanDefinition.PlanDefinitionActionComponent processMilestone(
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

    PlanDefinition.PlanDefinitionActionComponent planAction
        = processTask(tDecisionTask, caseModel, planItem);

    addDefinition(planAction, tDecisionTask, caseModel);

    addAnnotations(tDecisionTask.getExtensionElements(), planAction);

    planAction.setType(new Coding()
        .setSystem(
            KnowledgeRepresentationLanguageSerializationSeries.CMMN_1_1_XML_Syntax.getReferentId()
                .toString())
        .setCode("DecisionTask")
    );
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
        .map(FHIRPlanDefinitionUtils::toCodeableConcept)
        .forEach(cd -> this.addCodeIfMissing(cd, planAction));
  }

  private void processAssociatedItems(TPlanItem planItem, TDefinitions caseModel,
      PlanDefinitionActionComponent planAction, TTask task) {
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
      processCaseFileItem((TCaseFileItem) assoc.getTargetRef(), planAction, caseModel,
          assoc.getSourceRef());
    } else if (assoc.getSourceRef() instanceof TCaseFileItem) {
      processCaseFileItem((TCaseFileItem) assoc.getSourceRef(), planAction, caseModel,
          assoc.getTargetRef());
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
            new Reference()
                .setReference(mapReference(dec.getExternalRef().getNamespaceURI()))
                .setDisplay(dec.getName())
                .setIdentifier(toFHIRIdentifier(
                    dec.getExternalRef().getLocalPart().replace("_", ""),
                    newTerm(API4KP.getReferentId(), "KnowledgeFragment"))
                ));
      }
    });
  }

  private String mapReference(String namespaceURI) {
    ResourceIdentifier original = SemanticIdentifier.newId(URI.create(namespaceURI));
    return DmnToPlanDefTranslator.mapArtifactToArtifactId(original).getResourceId().toString();
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
    var qname = tDecisionTask.getDecisionRef();
    if (qname == null) {
      log.error("Unlinked Decision task {}", tDecisionTask.getName());
      return Optional.empty();
    }
    return Optional.ofNullable(
        caseModel.getDecision().stream()
            .filter(dec -> dec.getId().equals(qname.getLocalPart()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(format(
                "Case %s : Decision Task %s has no associated decision model",
                caseModel.getName(), tDecisionTask.getName()))));
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
    return this.findAnnotation(extensionElements.getAny(), Has_Primary_Subject);
  }

  private Optional<ConceptIdentifier> findTopic(TExtensionElements extensionElements) {
    return this.findAnnotation(extensionElements.getAny(), Has_Focus);
  }

  private Optional<ConceptIdentifier> findAnnotation(
      List<Object> extensionElements, Term term) {
    if (extensionElements != null) {
      List<Annotation> annotations = extensionElements.stream()
          .flatMap(StreamUtil.filterAs(Annotation.class))
          .filter(annotation -> annotation.getRel() != null)
          .filter(annotation -> annotation.getRel().getConceptId()
              .equals(term.getConceptId()))
          .collect(Collectors.toList());

      if (annotations.size() > 1) {
        throw new IllegalStateException(format(
            "Model element cannot have more than one subject, found %s",
            annotations.stream()
                .map(ann -> ann.getRef().getTag())
                .collect(Collectors.joining(","))));
      }

      if (annotations.size() == 1) {
        var annotation = annotations.get(0);
        return Optional.of(annotation.getRef());
      }
    }
    return Optional.empty();
  }


  private DataRequirement toSemanticDataRequirement(Term anno) {
    var dataRequirement = new DataRequirement();
    dataRequirement.addCodeFilter().addValueCodeableConcept(toCodeableConcept(anno));
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