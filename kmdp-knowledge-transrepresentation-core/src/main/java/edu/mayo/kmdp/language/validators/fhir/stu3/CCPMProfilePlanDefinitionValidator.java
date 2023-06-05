package edu.mayo.kmdp.language.validators.fhir.stu3;

import static edu.mayo.kmdp.language.common.fhir.stu3.FHIRPlanDefinitionUtils.getNestedPlanDefs;
import static edu.mayo.kmdp.language.common.fhir.stu3.FHIRPlanDefinitionUtils.getSubActions;
import static edu.mayo.kmdp.language.common.fhir.stu3.FHIRPlanDefinitionUtils.joins;
import static edu.mayo.kmdp.language.common.fhir.stu3.FHIRPlanDefinitionUtils.toDisplayTerms;
import static edu.mayo.kmdp.util.CharsetEncodingUtil.sanitizeToASCIItext;
import static edu.mayo.kmdp.util.Util.isEmpty;
import static edu.mayo.kmdp.util.Util.isNotEmpty;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Cognitive_Care_Process_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Well_Formedness_Check_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;

import edu.mayo.kmdp.language.validators.cmmn.v1_1.CCPMComponentValidator;
import edu.mayo.kmdp.registry.Registry;
import edu.mayo.kmdp.util.Util;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Named;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.DataRequirement;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.PlanDefinition.PlanDefinitionActionComponent;
import org.hl7.fhir.dstu3.model.PrimitiveType;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.Severity;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;

@Named
@KPOperation(Well_Formedness_Check_Task)
@KPSupport(FHIR_STU3)
public class CCPMProfilePlanDefinitionValidator extends CCPMComponentValidator {

  public static final UUID id = UUID.fromString("7e2bceb4-632c-4361-b9db-ea8a84c09eff");
  public static final String version = "1.0.0";

  private final ResourceIdentifier operatorId;

  public CCPMProfilePlanDefinitionValidator() {
    this.operatorId = SemanticIdentifier.newId(id, version);
  }

  @Override
  public ResourceIdentifier getOperatorId() {
    return operatorId;
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return FHIR_STU3;
  }

  @Override
  public List<SyntacticRepresentation> getFrom() {
    return Arrays.asList(rep(FHIR_STU3), rep(Knowledge_Asset_Surrogate_2_0));
  }

  @Override
  protected Answer<Void> validate(KnowledgeAsset knowledgeAsset, KnowledgeCarrier carrier) {
    // validate the metadata in the Surrogate
    return Stream.of(
        validateAssetId(knowledgeAsset, carrier),
        validateAssetVersion(knowledgeAsset, carrier),
        validateArtifactVersion(knowledgeAsset, carrier),
        validateAssetType(knowledgeAsset, carrier, Cognitive_Care_Process_Model),
        validatePublicationStatus(knowledgeAsset, carrier),
        validateSubject(knowledgeAsset, carrier)
    ).reduce(Answer::merge).orElseGet(Answer::failed);
  }

  @Override
  protected Answer<Void> validate(PlanDefinition rootPlanDef,
      KnowledgeCarrier carrier) {
    return Stream.of(
        validateId(rootPlanDef, carrier),
        validateNameTitle(rootPlanDef, carrier),
        validateActionTitle(rootPlanDef, carrier),
        validateType(rootPlanDef, carrier),
        validateActionTypes(rootPlanDef, carrier),
        validateDefinitionRefs(rootPlanDef, carrier),
        validateRelatedAction(rootPlanDef, carrier),
        validateDocumentation(rootPlanDef, carrier),
        validateInputOutput(rootPlanDef, carrier),
        validateInputTechnique(rootPlanDef, carrier),
        validateSubActions(rootPlanDef, carrier),
        validateSubject(rootPlanDef, carrier)
    ).reduce(Answer::merge).orElseGet(Answer::failed);
  }


  /**
   * Ensure that the Asset ID is set as one of the PlanDef business Identifier
   *
   * @param rootPlanDef
   * @param carrier
   * @return
   */
  private Answer<Void> validateId(PlanDefinition rootPlanDef, KnowledgeCarrier carrier) {
    boolean success = getNestedPlanDefs(rootPlanDef)
        .allMatch(this::hasEnterpriseId);

    return validationResponse(
        carrier,
        success,
        "Component Asset IDs",
        () -> "all components have Asset ID",
        () -> "Missing Asset IDs"
    );
  }

  private boolean hasEnterpriseId(PlanDefinition pd) {
    if (pd.getIdentifier().isEmpty()) {
      return false;
    }
    return pd.getIdentifier().stream()
        .anyMatch(idtf ->
            Util.isNotEmpty(idtf.getValue())
                && idtf.getSystem().contains(Registry.MAYO_ASSETS_BASE_URI));
  }


  /**
   * Ensure that Title exists, and Name is a sanitized String
   *
   * @param rootPlanDef
   * @param carrier
   * @return
   */
  private Answer<Void> validateNameTitle(PlanDefinition rootPlanDef, KnowledgeCarrier carrier) {
    return getNestedPlanDefs(rootPlanDef)
        .map(pd -> {
          boolean hasTitle = pd.getTitle() != null;
          boolean hasName = pd.getName() != null
              && sanitizeToASCIItext(pd.getName()).equals(pd.getName());

          return validationResponse(
              carrier,
              this::mapAssetId,
              hasTitle && hasName ? Severity.OK : Severity.ERR,
              "PD Name / Title - " + pd.getTitle(),
              () -> "title: " + pd.getTitle(),
              () -> "invalid name: " + pd.getName());
        }).reduce(Answer::merge)
        .orElseGet(Answer::failed);
  }

  /**
   * Ensure that Title exists, and Name is a sanitized String
   *
   * @param rootPlanDef
   * @param carrier
   * @return
   */
  private Answer<Void> validateActionTitle(PlanDefinition rootPlanDef, KnowledgeCarrier carrier) {
    return getNestedPlanDefs(rootPlanDef)
        .map(pd -> {
          List<PlanDefinitionActionComponent> untitledActions =
              getSubActions(pd)
                  .filter(act -> isEmpty(act.getTitle()))
                  .collect(Collectors.toList());
          Severity valid;
          if (untitledActions.isEmpty()) {
            valid = Severity.OK;
          } else if (untitledActions.contains(rootPlanDef.getActionFirstRep())) {
            valid = Severity.ERR;
          } else {
            valid = Severity.WRN;
          }

          return validationResponse(
              carrier,
              this::mapAssetId,
              valid,
              "Action Titles - " + pd.getTitle(),
              () -> "All actions have titles",
              () -> "Missing action (stage/task) titles");
        }).reduce(Answer::merge)
        .orElseGet(Answer::failed);
  }


  /**
   * Ensure that that PlanDefinition is Typed
   *
   * @param rootPlanDef
   * @param carrier
   * @return
   */
  private Answer<Void> validateType(PlanDefinition rootPlanDef, KnowledgeCarrier carrier) {
    return getNestedPlanDefs(rootPlanDef)
        .map(pd -> {
          boolean success = !pd.getType().getCoding().isEmpty();

          return validationResponse(
              carrier,
              this::mapAssetId,
              success,
              "PlanDef Type - " + pd.getTitle(),
              () -> toDisplayTerms(pd.getType()),
              () -> "NO PlanDef Type found");
        }).reduce(Answer::merge)
        .orElseGet(Answer::failed);
  }

  /**
   * Ensure that Actions are Typed
   *
   * @param rootPlanDef
   * @param carrier
   * @return
   */
  private Answer<Void> validateActionTypes(PlanDefinition rootPlanDef, KnowledgeCarrier carrier) {
    return getNestedPlanDefs(rootPlanDef)
        .map(pd -> {
          List<PlanDefinitionActionComponent> untypedActions =
              getSubActions(pd)
                  .filter(act -> act.getType().isEmpty())
                  .collect(Collectors.toList());

          return validationResponse(
              carrier,
              this::mapAssetId,
              untypedActions.isEmpty(),
              "Action Type - " + pd.getTitle(),
              () -> "All actions have types",
              () -> "Untyped, " + untypedActions.stream()
                  .map(PlanDefinitionActionComponent::getTitle).collect(Collectors.joining(",")));
        }).reduce(Answer::merge)
        .orElseGet(Answer::failed);
  }

  /**
   * Ensure that cross-PD 'definitions' are valid and resolvable
   *
   * @param rootPlanDef
   * @param carrier
   * @return
   */
  private Answer<Void> validateDefinitionRefs(PlanDefinition rootPlanDef,
      KnowledgeCarrier carrier) {
    return getNestedPlanDefs(rootPlanDef)
        .map(pd -> {
          List<PlanDefinitionActionComponent> definedActs =
              getSubActions(pd)
                  .filter(act -> !act.getDefinition().isEmpty())
                  .collect(Collectors.toList());
          List<PlanDefinitionActionComponent> brokenReferences = definedActs.stream()
              .filter(act -> {
                Reference ref = act.getDefinition();
                String ptr = ref.getReference();
                if (!(ref.getResource() instanceof PlanDefinition)) {
                  return false;
                }
                PlanDefinition refPD = (PlanDefinition) ref.getResource();
                return getSubActions(refPD).anyMatch(x -> x.getId().equals(ptr));
              }).collect(Collectors.toList());

          return validationResponse(
              carrier,
              this::mapAssetId,
              brokenReferences.isEmpty(),
              "Definition Ref - " + pd.getTitle(),
              () -> definedActs.isEmpty() ? "No Definition References"
                  : "Valid Definition References",
              () -> "Pending Definition Refs, " + brokenReferences.stream()
                  .map(PlanDefinitionActionComponent::getTitle).collect(Collectors.joining(",")));
        }).reduce(Answer::merge)
        .orElseGet(Answer::failed);
  }

  /**
   * Ensure that intra-PD 'related actions' are valid and resolvable
   *
   * @param rootPlanDef
   * @param carrier
   * @return
   */
  private Answer<Void> validateRelatedAction(PlanDefinition rootPlanDef, KnowledgeCarrier carrier) {
    return getNestedPlanDefs(rootPlanDef)
        .map(pd -> {
          List<PlanDefinitionActionComponent> relatedActs =
              getSubActions(pd)
                  .filter(act -> !act.getRelatedAction().isEmpty())
                  .collect(Collectors.toList());
          List<PlanDefinitionActionComponent> brokenRelationships = relatedActs.stream()
              .filter(act -> act.getRelatedAction().stream().anyMatch(
                  rel -> rel.getRelationship() == null || rel.getActionId() == null
                      || getSubActions(pd).noneMatch(a -> joins(a.getId(), rel.getActionId())))
              ).collect(Collectors.toList());

          return validationResponse(
              carrier,
              this::mapAssetId,
              brokenRelationships.isEmpty(),
              "Related Ref - " + pd.getTitle(),
              () -> relatedActs.isEmpty() ? "No Related Acts" : "Valid Related acts",
              () -> "Unresolved Related Actions, " + relatedActs.stream()
                  .map(PlanDefinitionActionComponent::getTitle).collect(Collectors.joining(",")));
        }).reduce(Answer::merge)
        .orElseGet(Answer::failed);
  }

  /**
   * Ensure that Attachments are fully annotated
   *
   * @param rootPlanDef
   * @param carrier
   * @return
   */
  private Answer<Void> validateDocumentation(PlanDefinition rootPlanDef, KnowledgeCarrier carrier) {
    return getNestedPlanDefs(rootPlanDef)
        .map(pd -> {
          List<RelatedArtifact> relatedArtifacts =
              getSubActions(pd)
                  .flatMap(act -> act.getDocumentation().stream())
                  .collect(Collectors.toList());
          Set<RelatedArtifact> brokenArtifacts = relatedArtifacts.stream()
              .filter(art -> isEmpty(art.getDocument().getContentType())
                  && art.getExtension().isEmpty())
              .collect(Collectors.toSet());
          Set<RelatedArtifact> reallyBrokenArtifacts = relatedArtifacts.stream()
              .filter(art -> isEmpty(art.getUrl()) || isEmpty(art.getDocument().getUrl()))
              .collect(Collectors.toSet());

          Severity status;
          if (!reallyBrokenArtifacts.isEmpty()) {
            status = Severity.ERR;
          } else if (!brokenArtifacts.isEmpty()) {
            status = Severity.WRN;
          } else {
            status = Severity.OK;
          }
          brokenArtifacts.addAll(reallyBrokenArtifacts);

          return validationResponse(
              carrier,
              this::mapAssetId,
              status,
              "K-Sources - " + pd.getTitle(),
              () -> relatedArtifacts.isEmpty() ? "No Attachments" : "Valid Attachments",
              () -> "Attachment w/ missing Metadata, " + brokenArtifacts.stream()
                  .map(RelatedArtifact::getDisplay).collect(Collectors.joining(",")));
        }).reduce(Answer::merge)
        .orElseGet(Answer::failed);
  }

  /**
   * Ensure that I/O Data Requirements are codified
   *
   * @param rootPlanDef
   * @param carrier
   * @return
   */
  private Answer<Void> validateInputOutput(PlanDefinition rootPlanDef, KnowledgeCarrier carrier) {
    return getNestedPlanDefs(rootPlanDef)
        .map(pd -> {
          List<DataRequirement> ioRequirements = Stream.concat(
                  getSubActions(pd).flatMap(act -> act.getInput().stream()),
                  getSubActions(pd).flatMap(act -> act.getOutput().stream()))
              .collect(Collectors.toList());

          Set<DataRequirement> brokenRequirements = ioRequirements.stream()
              .filter(dr -> dr.getProfile().isEmpty() || dr.getCodeFilterFirstRep()
                  .getValueCodeableConcept().isEmpty()
                  || dr.getCodeFilterFirstRep().getValueCodeableConceptFirstRep().getCoding()
                  .isEmpty())
              .collect(Collectors.toSet());

          return validationResponse(
              carrier,
              this::mapAssetId,
              brokenRequirements.isEmpty(),
              "I/O Reqs - " + pd.getTitle(),
              () -> ioRequirements.isEmpty() ? "No Input/Output" : "Annotated Input/Output",
              () -> "Missing Concept or Datatype, " + brokenRequirements.stream()
                  .map(this::getConceptLabel)
                  .collect(Collectors.joining(",")));
        }).reduce(Answer::merge)
        .orElseGet(Answer::failed);
  }

  /**
   * Ensure that Input Data Requirements have a known Technique Extension
   *
   * @param rootPlanDef
   * @param carrier
   * @return
   */
  private Answer<Void> validateInputTechnique(
      PlanDefinition rootPlanDef,
      KnowledgeCarrier carrier) {
    return getNestedPlanDefs(rootPlanDef)
        .map(pd -> {
          List<DataRequirement> inputRequirements =
              getSubActions(pd).flatMap(act -> act.getInput().stream())
                  .collect(Collectors.toList());

          Set<DataRequirement> danglingRequirements = inputRequirements.stream()
              .filter(dr -> dr.getExtension().stream()
                  .noneMatch(x -> "https://www.omg.org/spec/API4KP/api4kp-kao/Technique"
                      .equals(x.getUrl())))
              .collect(Collectors.toSet());

          return validationResponse(
              carrier,
              this::mapAssetId,
              danglingRequirements.isEmpty(),
              "OpDef Reqs " + pd.getTitle(),
              () -> inputRequirements.isEmpty() ? "No Inputs" : "All Inputs with Technique",
              () -> "Missing OpDef Technique, " + danglingRequirements.stream()
                  .map(this::getConceptLabel)
                  .collect(Collectors.joining(",")));
        }).reduce(Answer::merge)
        .orElseGet(Answer::failed);
  }

  /**
   * Ensure that I/O Data Requirements are codified
   *
   * @param rootPlanDef
   * @param carrier
   * @return
   */
  private Answer<Void> validateSubActions(PlanDefinition rootPlanDef, KnowledgeCarrier carrier) {
    return getNestedPlanDefs(rootPlanDef)
        .map(pd -> {
          List<PlanDefinitionActionComponent> subActions = getSubActions(pd)
              .filter(act -> "Decision".equals(act.getType().getCode())
                  || "DecisionService".equals(act.getType().getCode()))
              .collect(Collectors.toList());

          Set<PlanDefinitionActionComponent> brokenSubDecisionServices = subActions.stream()
              .filter(act -> act.getAction().stream().anyMatch(sub -> {
                if (!"DecisionService".equals(sub.getType().getCode())) {
                  return true;
                }
                // broken if any sub.output
                // does not match any of the sup act.input
                return sub.getOutput().stream()
                    .anyMatch(out -> act.getInput().stream().noneMatch(
                        in -> matches(in, out)));
              }))
              .collect(Collectors.toSet());

          return validationResponse(
              carrier,
              this::mapAssetId,
              brokenSubDecisionServices.isEmpty(),
              "SubAction Services - " + pd.getTitle(),
              () -> subActions.isEmpty() ? "No SubActions" : "Linked SubAction Services",
              () -> "Improper SubAction, " + brokenSubDecisionServices.stream()
                  .map(PlanDefinitionActionComponent::getTitle)
                  .collect(Collectors.joining(",")));
        }).reduce(Answer::merge)
        .orElseGet(Answer::failed);
  }

  private boolean matches(DataRequirement in, DataRequirement out) {
    var inProfiles = in.getProfile().stream()
        .map(PrimitiveType::getValue).collect(Collectors.toSet());
    var outProfiles = out.getProfile().stream()
        .map(PrimitiveType::getValue).collect(Collectors.toSet());
    boolean profileEquals = inProfiles.equals(outProfiles);

    boolean codeEquals = in.hasCodeFilter() &&
        in.getCodeFilterFirstRep().equalsDeep(out.getCodeFilterFirstRep());
    boolean typeEquals = in.getType() != null &&
        in.getType().equals(out.getType());

    return profileEquals && codeEquals && typeEquals;
  }

  private String getConceptLabel(DataRequirement d) {
    CodeableConcept cc = d.getCodeFilterFirstRep().getValueCodeableConceptFirstRep();
    return isNotEmpty(cc.getText())
        ? cc.getText()
        : cc.getCodingFirstRep().getDisplay();
  }

  private Answer<Void> validateSubject(PlanDefinition rootPlanDef, KnowledgeCarrier carrier) {
    Optional<CodeableConcept> subject =
        rootPlanDef.getActionFirstRep().getReason().stream().findFirst();
    Severity valid = subject.isPresent() ? Severity.OK : Severity.ERR;
    return validationResponse(
        carrier,
        valid,
        "Subject",
        () -> rootPlanDef.getActionFirstRep().getReasonFirstRep().getCodingFirstRep().getDisplay(),
        () -> "Missing Subject Annotation on Case Model"
    );
  }

}
