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
package edu.mayo.kmdp.language.translators.dmn.v1_2.fhir.stu3;

import static edu.mayo.kmdp.language.common.dmn.v1_2.DMN12Utils.asId;
import static edu.mayo.kmdp.language.common.dmn.v1_2.DMN12Utils.findBKM;
import static edu.mayo.kmdp.language.common.dmn.v1_2.DMN12Utils.findDecision;
import static edu.mayo.kmdp.language.common.dmn.v1_2.DMN12Utils.findInput;
import static edu.mayo.kmdp.language.common.dmn.v1_2.DMN12Utils.findKnowledgeSource;
import static edu.mayo.kmdp.language.common.dmn.v1_2.DMN12Utils.getSubDecisionsClosure;
import static edu.mayo.kmdp.language.common.dmn.v1_2.DMN12Utils.idToLocalRef;
import static edu.mayo.kmdp.language.common.dmn.v1_2.DMN12Utils.idToRef;
import static edu.mayo.kmdp.language.common.dmn.v1_2.DMN12Utils.joins;
import static edu.mayo.kmdp.language.common.dmn.v1_2.DMN12Utils.refToId;
import static edu.mayo.kmdp.language.common.dmn.v1_2.DMN12Utils.streamDecisionServices;
import static edu.mayo.kmdp.language.common.dmn.v1_2.DMN12Utils.streamDecisions;
import static edu.mayo.kmdp.language.common.fhir.stu3.FHIRPlanDefinitionUtils.setKnowledgeIdentifiers;
import static edu.mayo.kmdp.language.common.fhir.stu3.FHIRPlanDefinitionUtils.toCodeableConcept;
import static edu.mayo.kmdp.util.CharsetEncodingUtil.sanitizeToUTF8;
import static edu.mayo.kmdp.util.NameUtils.nameToIdentifier;
import static edu.mayo.kmdp.util.StreamUtil.filterAs;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.Captures;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.Defines;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.Has_Primary_Subject;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.In_Terms_Of;
import static java.util.Collections.singletonList;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newVersionId;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Contextualizable_Knowledge;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Decision_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Grounded_Knowledge;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Questionnaire;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeprocessingtechnique.KnowledgeProcessingTechniqueSeries.Query_Technique;

import edu.mayo.kmdp.language.common.fhir.stu3.FHIRPlanDefinitionUtils;
import edu.mayo.kmdp.util.NameUtils.IdentifierType;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.Util;
import edu.mayo.ontology.taxonomies.kao.decisiontype.DecisionTypeSeries;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.CodeType;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DataRequirement;
import org.hl7.fhir.dstu3.model.DataRequirement.DataRequirementCodeFilterComponent;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.PlanDefinition.ActionRelationshipType;
import org.hl7.fhir.dstu3.model.PlanDefinition.PlanDefinitionActionComponent;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType;
import org.hl7.fhir.dstu3.model.UriType;
import org.omg.spec.api4kp._20200801.id.ConceptIdentifier;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.Term;
import org.omg.spec.api4kp._20200801.surrogate.Annotation;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries;
import org.omg.spec.dmn._20180521.model.TAuthorityRequirement;
import org.omg.spec.dmn._20180521.model.TDMNElement.ExtensionElements;
import org.omg.spec.dmn._20180521.model.TDecision;
import org.omg.spec.dmn._20180521.model.TDecisionService;
import org.omg.spec.dmn._20180521.model.TDefinitions;
import org.omg.spec.dmn._20180521.model.TInputData;
import org.omg.spec.dmn._20180521.model.TKnowledgeRequirement;
import org.omg.spec.dmn._20180521.model.TKnowledgeSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DmnToPlanDef {

  private static final Logger log = LoggerFactory.getLogger(DmnToPlanDef.class);

  public DmnToPlanDef() {
    // nothing to do
  }

  public PlanDefinition transform(
      ResourceIdentifier assetId,
      ResourceIdentifier srcArtifactId,
      ResourceIdentifier tgtArtifactId,
      TDefinitions decisionModel) {
    log.debug("Called Translator DMN to PDF  for {}", decisionModel.getName());
    var cpm = new PlanDefinition();

    mapIdentity(cpm, assetId, srcArtifactId, tgtArtifactId);
    mapName(cpm, decisionModel);
    mapSubject(cpm, decisionModel);

    mapDecisions(cpm, decisionModel);

    log.debug("Finished DMN to PDef  for {} ", decisionModel.getName());
    return cpm;
  }

  private void mapDecisions(PlanDefinition cpm,
      TDefinitions decisionModel) {

    Collection<TDecisionService> dmnDecisionServices = streamDecisionServices(decisionModel)
        .collect(Collectors.toList());

    Collection<TDecision> dmnDecisions = streamDecisions(decisionModel)
        .filter(dec -> dmnDecisionServices.stream()
            .noneMatch(ds -> isDecisionServiceScoped(dec, ds)))
        .collect(Collectors.toList());

    Map<String, PlanDefinitionActionComponent> mappedDecisions = new HashMap<>();
    dmnDecisions.stream()
        .map(decision -> processDecision(cpm, cpm::addAction, decisionModel, decision))
        .forEach(act -> mappedDecisions.putIfAbsent(act.getId(), act));

    Map<String, PlanDefinitionActionComponent> mappedDecisionServices = new HashMap<>();
    dmnDecisionServices.stream()
        .map(decisionService -> processDecisionService(cpm, decisionModel, decisionService))
        .forEach(act -> mappedDecisionServices.putIfAbsent(act.getId(), act));

    dmnDecisions
        .forEach(decision -> processDecisionToDecisionDependencies(
            cpm,
            mappedDecisions,
            decision,
            decisionModel));

    dmnDecisions
        .forEach(decision -> processDecisionToDecisionServiceDependencies(
            cpm,
            mappedDecisions,
            mappedDecisionServices,
            decision,
            decisionModel));

    dmnDecisionServices
        .forEach(ds -> processDecisionServiceToDecisionServiceDependencies(
            cpm,
            mappedDecisions,
            mappedDecisionServices,
            ds,
            decisionModel));
  }

  private boolean isDecisionServiceScoped(TDecision dec, TDecisionService ds) {
    boolean isOutput = ds.getOutputDecision().stream()
        .anyMatch(ref -> joins(dec.getId(), ref.getHref()));
    boolean isEncapsulated = ds.getEncapsulatedDecision().stream()
        .anyMatch(ref -> joins(dec.getId(), ref.getHref()));
    boolean isInput = ds.getInputDecision().stream()
        .anyMatch(ref -> joins(dec.getId(), ref.getHref()));
    return isOutput || isEncapsulated || isInput;
  }

  private void processDecisionToDecisionDependencies(
      PlanDefinition cpm,
      Map<String, PlanDefinitionActionComponent> mappedDecisions,
      TDecision dmnDecision,
      TDefinitions decisionModel) {

    PlanDefinitionActionComponent srcAction =
        mappedDecisions.get(asId(dmnDecision.getId()));

    dmnDecision.getInformationRequirement().stream()
        .filter(info -> info.getRequiredDecision() != null)
        .map(info -> URI.create(info.getRequiredDecision().getHref()))
        .forEach(tgtActionId -> srcAction.addRelatedAction()
            .setRelationship(ActionRelationshipType.AFTER)
            .setActionId(idToLocalRef(tgtActionId)));
  }

  private void processDecisionToDecisionServiceDependencies(
      PlanDefinition cpm,
      Map<String, PlanDefinitionActionComponent> mappedDecisions,
      Map<String, PlanDefinitionActionComponent> mappedDecisionServices,
      TDecision dmnDecision,
      TDefinitions decisionModel) {

    PlanDefinitionActionComponent srcAction =
        mappedDecisions.get(asId(dmnDecision.getId()));

    List<PlanDefinitionActionComponent> serviceClientAction = dmnDecision.getKnowledgeRequirement()
        .stream()
        .filter(know -> know.getRequiredKnowledge() != null)
        .flatMap(ref -> lookupAsDecisionService(ref, mappedDecisionServices)
            .or(() -> lookupViaBKM(decisionModel, ref, mappedDecisionServices))
            .stream())
        .collect(Collectors.toList());

    serviceClientAction.forEach(subAct -> {
      addToScope(subAct, srcAction.getAction(), srcAction::addAction);
      subAct.getOutput().forEach(out -> srcAction.getInput().add(out));
    });
  }

  private void processDecisionServiceToDecisionServiceDependencies(
      PlanDefinition cpm,
      Map<String, PlanDefinitionActionComponent> mappedDecisions,
      Map<String, PlanDefinitionActionComponent> mappedDecisionServices,
      TDecisionService dmnDecisionService,
      TDefinitions decisionModel) {

    PlanDefinitionActionComponent srcAction =
        mappedDecisionServices.get(asId(dmnDecisionService.getId()));

    List<TDecision> serviceDecisions = dmnDecisionService.getOutputDecision().stream()
        .map(out -> streamDecisions(decisionModel)
            .filter(dec -> joins(dec.getId(), out.getHref()))
            .findFirst().orElseThrow())
        .flatMap(dec -> getSubDecisionsClosure(dec, decisionModel))
        .distinct()
        .collect(Collectors.toList());

    serviceDecisions
        .forEach(subDec -> subDec.getKnowledgeRequirement().stream()
            .filter(know -> know.getRequiredKnowledge() != null)
            .flatMap(ref -> lookupAsDecisionService(ref, mappedDecisionServices)
                .or(() -> lookupViaBKM(decisionModel, ref, mappedDecisionServices))
                .stream())
            .forEach(subAct -> {
              addToScope(subAct, srcAction.getAction(), srcAction::addAction);
              // remap the children's outputs as parent's inputs
              subAct.getOutput().forEach(out -> srcAction.getInput().add(out));
            }));
  }


  private Optional<PlanDefinitionActionComponent> lookupViaBKM(
      TDefinitions decisionModel,
      TKnowledgeRequirement ref,
      Map<String, PlanDefinitionActionComponent> mappedDecisionServices) {
    return findBKM(ref.getRequiredKnowledge(), decisionModel).stream()
        .flatMap(bkm -> bkm.getKnowledgeRequirement().stream())
        .map(TKnowledgeRequirement::getRequiredKnowledge)
        .filter(Objects::nonNull)
        .map(info -> mappedDecisionServices.get(
            refToId(info.getHref())))
        .findAny();
  }

  private Optional<PlanDefinitionActionComponent> lookupAsDecisionService(TKnowledgeRequirement ref,
      Map<String, PlanDefinitionActionComponent> mappedDecisionServices) {
    return Optional
        .ofNullable(mappedDecisionServices.get(refToId(ref.getRequiredKnowledge().getHref())));
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

    cpm.setType(toCodeableConcept(Decision_Model));
    cpm.setVersion(tgtArtifactId.getVersionTag());
    cpm.setId(tgtArtifactId.getUuid().toString());
  }

  private PlanDefinitionActionComponent processDecision(
      PlanDefinition cpm,
      Consumer<PlanDefinitionActionComponent> cpmScope,
      TDefinitions decisionModel, TDecision decision) {
    var decisionAction = new PlanDefinitionActionComponent();
    decisionAction.setTitle(sanitizeToUTF8(decision.getName()));
    decisionAction.setId(asId(decision.getId()));

    decisionAction.setType(new Coding()
        .setSystem(
            KnowledgeRepresentationLanguageSerializationSeries.DMN_1_2_XML_Syntax.getReferentId()
                .toString())
        .setCode("Decision")
    );

    getSemanticAnnotation(decision.getExtensionElements()).stream()
        .map(FHIRPlanDefinitionUtils::toCodeableConcept)
        .forEach(decisionAction::addCode);

    decision.getAuthorityRequirement()
        .forEach(know -> mapKnowledgeSource(know, cpm, decisionAction, decisionModel));

    decision.getInformationRequirement().stream()
        .filter(info -> info.getRequiredInput() != null)
        .map(info -> findInput(info.getRequiredInput(), decisionModel))
        .flatMap(StreamUtil::trimStream)
        .map(this::mapInput)
        .forEach(decisionAction::addInput);

    addToScope(decisionAction, cpm.getAction(), cpmScope);
    return decisionAction;
  }


  private PlanDefinitionActionComponent processDecisionService(PlanDefinition cpm,
      TDefinitions decisionModel, TDecisionService decisionService) {
    var serviceAction = new PlanDefinitionActionComponent();
    serviceAction.setTitle(sanitizeToUTF8(decisionService.getName()));
    serviceAction.setId(asId(decisionService.getId()));

    serviceAction.setType(new Coding()
        .setSystem(
            KnowledgeRepresentationLanguageSerializationSeries.DMN_1_2_XML_Syntax.getReferentId()
                .toString())
        .setCode("DecisionService")
    );

    getSemanticAnnotation(decisionService.getExtensionElements()).stream()
        .map(FHIRPlanDefinitionUtils::toCodeableConcept)
        .forEach(serviceAction::addCode);

    decisionService.getOutputDecision().forEach(
        out -> {
          var outputDecision = findDecision(out, decisionModel)
              .orElseThrow();
          mapOutput(outputDecision, serviceAction);
          outputDecision.getAuthorityRequirement().forEach(
              ks -> mapKnowledgeSource(ks, cpm, serviceAction, decisionModel)
          );
        }
    );

    decisionService.getInputData().forEach(
        input -> {
          TInputData inputData = findInput(input, decisionModel)
              .orElseThrow();
          serviceAction.addInput(mapInput(inputData));
        }
    );

    return serviceAction;
  }


  private DataRequirement mapInput(TInputData input) {
    var dataRequirement = new DataRequirement();
    var codeFilters = new DataRequirementCodeFilterComponent();
    dataRequirement.addCodeFilter(codeFilters);

    getSemanticAnnotation(input.getExtensionElements()).stream()
        .map(FHIRPlanDefinitionUtils::toCodeableConcept)
        .forEach(codeFilters::addValueCodeableConcept);

    var dataType = input.getVariable().getTypeRef();
    if (dataType != null && dataType.startsWith("fhir")) {
      var type = dataType.substring(4).toLowerCase();
      var baseProfile = "https://www.hl7.org/fhir/STU3/" + type + ".profile.json";
      dataRequirement.setType(type);
      dataRequirement.setProfile(List.of(new UriType(baseProfile)));
      dataRequirement.addExtension(
          "https://www.omg.org/spec/API4KP/api4kp-kao/Technique",
          new CodeType(Query_Technique.getTag()));
    }
    return dataRequirement;
  }

  private DataRequirement mapOutput(TDecision output,
      PlanDefinitionActionComponent serviceAction) {
    var dataRequirement = new DataRequirement();
    var codeFilters = new DataRequirementCodeFilterComponent();
    dataRequirement.addCodeFilter(codeFilters);

    getSemanticAnnotation(output.getExtensionElements()).stream()
        .map(FHIRPlanDefinitionUtils::toCodeableConcept)
        .filter(cd -> !isKMConcept(cd))
        .forEach(codeFilters::addValueCodeableConcept);
    getSemanticAnnotation(output.getExtensionElements()).stream()
        .map(FHIRPlanDefinitionUtils::toCodeableConcept)
        .filter(this::isKMConcept)
        .forEach(serviceAction::addCode);

    if (!codeFilters.getValueCodeableConcept().isEmpty()) {
      serviceAction.addOutput(dataRequirement);
    }
    return dataRequirement;
  }

  private boolean isKMConcept(CodeableConcept cd) {
    String codeSystem = cd.getCodingFirstRep().getSystem();
    boolean isDecisionType = DecisionTypeSeries.schemeSeriesIdentifier.getNamespaceUri().toString()
        .equals(codeSystem);
    boolean isKnowledgeAssetType = KnowledgeAssetTypeSeries.schemeSeriesIdentifier.getNamespaceUri()
        .toString()
        .equals(codeSystem);
    return isDecisionType || isKnowledgeAssetType;
  }

  private void mapKnowledgeSource(TAuthorityRequirement know,
      PlanDefinition cpm, PlanDefinitionActionComponent decisionAction,
      TDefinitions decisionModel) {
    findKnowledgeSource(know.getRequiredAuthority(), decisionModel)
        .ifPresent(knowledgeSource ->
            mapKnowledgeSource(knowledgeSource, cpm, decisionAction, decisionModel));
  }

  private void mapKnowledgeSource(TKnowledgeSource knowledgeSource,
      PlanDefinition cpm, PlanDefinitionActionComponent decisionAction,
      TDefinitions decisionModel) {
    if (Util.isEmpty(knowledgeSource.getLocationURI())) {
      // ignore placeholders that do not actually point to a Knowledge Source
      return;
    }

    // Build the link between the PD Action and the KS content
    var relatedArtifact = new RelatedArtifact()
        .setUrl(knowledgeSource.getLocationURI())
        .setDisplay(knowledgeSource.getName())
        .setDocument(new Attachment()
            .setTitle(sanitizeToUTF8(knowledgeSource.getName()))
            .setUrl(knowledgeSource.getLocationURI())
            .setContentType(knowledgeSource.getType()));
    decisionAction.addDocumentation(relatedArtifact);

    // Build a self-contained metadata surrogate, in FHIR form, for the KS Content
    Library lib = (Library) new Library()
        .setTitle(knowledgeSource.getName() + " (Metadata)")
        .setId(idToRef(knowledgeSource.getId()));
    knowledgeSource.getAuthorityRequirement().stream()
        .flatMap(
            auth -> findKnowledgeSource(auth.getRequiredAuthority(), decisionModel).stream())
        .forEach(
            ks -> lib.addRelatedArtifact()
                .setType(RelatedArtifactType.COMPOSEDOF)
                .setUrl(
                    newVersionId(URI.create(ks.getLocationURI())).getVersionId().toString()));
    relatedArtifact.setResource(
        new Reference(lib)
            .setDisplay(lib.getTitle()));

    // Handles asset ID and type, if any (non-KMDP content does not have asset status)
    List<Term> types = Optional.ofNullable(knowledgeSource.getExtensionElements())
        .stream()
        .flatMap(x -> x.getAny().stream())
        .flatMap(filterAs(Annotation.class))
        .map(Annotation::getRef)
        .collect(Collectors.toList());
    if (types.isEmpty()) {
      inferType(knowledgeSource.getType())
          .ifPresent(types::add);
    }

    if (!types.isEmpty()) {
      ResourceIdentifier knowAssetId = newVersionId(URI.create(knowledgeSource.getLocationURI()));
      lib.addIdentifier(new Identifier()
              .setSystem(knowAssetId.getNamespaceUri().toString())
              .setValue(knowAssetId.getTag() + ":" + knowAssetId.getVersionTag()))
          .setUrl(knowAssetId.getVersionId() != null
              ? knowAssetId.getVersionId().toString()
              : knowAssetId.getResourceId().toString());

      var typesCC = toCodeableConcept(types);
      lib.setType(toCodeableConcept(types));
      relatedArtifact.addExtension(new Extension()
          .setUrl("http://kmd.mayo.edu/fhirExtensions/knowledgeAssetType")
          .setValue(typesCC));
    }

    cpm.addContained(lib);
  }

  private Optional<Term> inferType(String type) {
    switch (type) {
      case "application/dita+xml":
        return Optional.of(Contextualizable_Knowledge);
      case "text/html":
        return Optional.of(Grounded_Knowledge);
      case "application/fhir+json":
        // this is an imperfect mapping - could also be "IKR"
        return Optional.of(Questionnaire);
      case "*/*":
      default:
        return Optional.empty();
    }
  }

  private void mapSubject(PlanDefinition cpm, TDefinitions dmnModel) {
    this.findSubject(dmnModel.getExtensionElements())
        .map(FHIRPlanDefinitionUtils::toCodeableConcept)
        .map(Collections::singletonList)
        .ifPresent(cpm::setTopic);
  }


  private void mapName(PlanDefinition cpm, TDefinitions dmnModel) {
    cpm.setName(nameToIdentifier(dmnModel.getName(), IdentifierType.CLASS));
    cpm.setTitle(sanitizeToUTF8(dmnModel.getName()));
  }


  private static Collection<Term> getSemanticAnnotation(ExtensionElements extensionElements) {
    return extensionElements == null
        ? Collections.emptyList()
        : getSemanticAnnotation(extensionElements.getAny());
  }

  private static List<Term> getSemanticAnnotation(List<Object> extensionElements) {
    if (extensionElements == null || extensionElements.isEmpty()) {
      return Collections.emptyList();
    }

    return extensionElements.stream()
        .flatMap(filterAs(Annotation.class))
        .filter(ann ->
            ann.getRel() == null
                || Defines.getTag().equals(ann.getRel().getTag())
                || Captures.getTag().equals(ann.getRel().getTag())
                || In_Terms_Of.getTag().equals(ann.getRel().getTag())
        )
        .map(Annotation::getRef)
        .map(Term.class::cast)
        .collect(Collectors.toList());
  }


  private Optional<ConceptIdentifier> findSubject(ExtensionElements extensionElements) {
    return Optional.ofNullable(extensionElements)
        .map(ExtensionElements::getAny)
        .flatMap(this::findSubject);
  }

  private Optional<ConceptIdentifier> findSubject(List<Object> extensionElements) {
    if (extensionElements != null) {
      List<Annotation> annotations = extensionElements.stream()
          .filter(e -> e instanceof Annotation)
          .map(e -> (Annotation) e)
          .filter(
              annotation -> annotation.getRel() != null
                  && annotation.getRel().getReferentId() != null)
          .filter(annotation -> annotation.getRel().getReferentId()
              .equals(Has_Primary_Subject.getReferentId()))
          .collect(Collectors.toList());

      if (annotations.size() > 1) {
        throw new IllegalStateException("Cannot have more than one subject, found "
            + annotations.stream()
            .map(ann -> ann.getRef().getTag())
            .collect(Collectors.joining(",")));
      }

      if (annotations.size() == 1) {
        return Optional.of(annotations.get(0).getRef());
      }
    }

    return Optional.empty();
  }

  private void addToScope(PlanDefinitionActionComponent action,
      List<PlanDefinitionActionComponent> context,
      Consumer<PlanDefinitionActionComponent> scope) {
    // consider adding checks
    scope.accept(action);
  }

}