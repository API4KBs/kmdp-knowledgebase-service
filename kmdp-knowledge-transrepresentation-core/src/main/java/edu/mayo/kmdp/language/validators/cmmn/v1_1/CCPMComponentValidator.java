package edu.mayo.kmdp.language.validators.cmmn.v1_1;

import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.Has_Primary_Subject;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Case_Management_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Published;

import edu.mayo.kmdp.language.validators.AbstractValidator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.Severity;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.Term;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.Annotation;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.Publication;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetType;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeresourceoutcome.KnowledgeResourceOutcome;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeresourceoutcome.KnowledgeResourceOutcomeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatus;

public abstract class CCPMComponentValidator extends AbstractValidator {

  @Override
  public KnowledgeResourceOutcome getValidationType() {
    return KnowledgeResourceOutcomeSeries.Style_Conformance;
  }

  @Override
  protected Answer<Void> validateComponent(KnowledgeCarrier carrier, String xConfig) {
    if (carrier.is(KnowledgeAsset.class)) {
      return carrier.as(KnowledgeAsset.class)
          .map(ka -> this.validate(ka, carrier))
          .orElseGet(Answer::failed);
    } else if (carrier.is(org.omg.spec.dmn._20180521.model.TDefinitions.class)) {
      return carrier.as(org.omg.spec.dmn._20180521.model.TDefinitions.class)
          .map(kc -> this.validate(kc, carrier))
          .orElseGet(Answer::failed);
    } else if (carrier.is(org.omg.spec.cmmn._20151109.model.TDefinitions.class)) {
      return carrier.as(org.omg.spec.cmmn._20151109.model.TDefinitions.class)
          .map(kc -> this.validate(kc, carrier))
          .orElseGet(Answer::failed);
    } else if (carrier.is(PlanDefinition.class)) {
      return carrier.as(PlanDefinition.class)
          .map(kc -> this.validate(kc, carrier))
          .orElseGet(Answer::failed);
    } else {
      return Answer.failed();
    }
  }

  protected Answer<Void> validate(
      KnowledgeAsset knowledgeAsset, KnowledgeCarrier carrier) {
    return illegal();
  }

  protected Answer<Void> validate(
      org.omg.spec.dmn._20180521.model.TDefinitions decisionModel, KnowledgeCarrier carrier) {
    return illegal();
  }

  protected Answer<Void> validate(
      org.omg.spec.cmmn._20151109.model.TDefinitions caseModel, KnowledgeCarrier carrier) {
    return illegal();
  }

  protected Answer<Void> validate(
      PlanDefinition planDefinition, KnowledgeCarrier carrier) {
    return illegal();
  }

  private Answer<Void> illegal() {
    return Answer.failed(new IllegalStateException("This should not have been called"));
  }

  protected String impossible() {
    throw new IllegalStateException("This should not have been called");
  }

  /**
   * Validates the presence of an Asset ID
   * (not having one at this point is a catastrophic illegal state)
   * @param knowledgeAsset
   * @param carrier
   * @return
   */
  protected Answer<Void> validateAssetId(KnowledgeAsset knowledgeAsset, KnowledgeCarrier carrier) {
    boolean success = knowledgeAsset.getAssetId() != null;
    return validationResponse(
        carrier,
        success,
        "Asset ID",
        () -> "found " + knowledgeAsset.getAssetId().asKey(),
        () -> "MISSING AssetId"
    );
  }

  /**
   * Validates that the versionTag of an Asset is a full SemVer identifier
   * @param knowledgeAsset
   * @param carrier
   * @return
   */
  protected Answer<Void> validateAssetVersion(KnowledgeAsset knowledgeAsset, KnowledgeCarrier carrier) {
    Pattern semverPatter = Pattern.compile("\\d+\\.\\d+\\.\\d+(-\\d+)?");
    String assetVersion = knowledgeAsset.getAssetId().getVersionTag();
    boolean success = semverPatter.matcher(assetVersion).matches();
    return validationResponse(
        carrier,
        success,
        "Asset Version Tag",
        () -> "found Asset Version " + knowledgeAsset.getAssetId().getVersionTag(),
        () -> "Asset Version NOT a full SemVer " + assetVersion
    );
  }

  /**
   * Validates that the versionTag of an Artifact is a full SemVer identifier
   * @param knowledgeAsset
   * @param carrier
   * @return
   */
  protected Answer<Void> validateArtifactVersion(KnowledgeAsset knowledgeAsset, KnowledgeCarrier carrier) {
    Pattern semverPatter = Pattern.compile("\\d+\\.\\d+\\.\\d+(-\\d+)?");
    String artifactVersion = Optional.ofNullable(carrier.getArtifactId())
        .map(ResourceIdentifier::getVersionTag)
        .orElse("");
    boolean success = semverPatter.matcher(artifactVersion).matches();
    return validationResponse(
        carrier,
        success,
        "Artifact Version Tag",
        () -> "found Artifact Version " + carrier.getArtifactId().getVersionTag(),
        () -> "Asset Version NOT a full SemVer " + artifactVersion
    );
  }

  /**
   * Validates that a model is in Published state
   * @param knowledgeAsset
   * @param carrier
   * @return
   */
  protected Answer<Void> validatePublicationStatus(KnowledgeAsset knowledgeAsset, KnowledgeCarrier carrier) {
    PublicationStatus status = Optional.ofNullable(knowledgeAsset.getLifecycle())
        .map(Publication::getPublicationStatus)
        .orElse(null);
    Severity valid;
    if (status == null) {
      valid = Severity.ERR;
    } else if (Published.sameAs(status)) {
      valid = Severity.OK;
    } else {
      valid = Severity.WRN;
    }
    return validationResponse(
        carrier,
        valid,
        "Status",
        Published::getLabel,
        () -> status != null ? status.getLabel().toUpperCase() : "NOT Published"
    );
  }

  /**
   * Validates the presence of the 'has_subject' annotation
   * @param knowledgeAsset
   * @param carrier
   * @return
   */
  protected Answer<Void> validateSubject(KnowledgeAsset knowledgeAsset, KnowledgeCarrier carrier) {
    if (Case_Management_Model.isAnyOf(knowledgeAsset.getFormalType())) {
      Optional<Annotation> subject = knowledgeAsset.getAnnotation().stream()
          .filter(ann -> Has_Primary_Subject.sameTermAs(ann.getRel()))
          .findFirst();
      Severity valid = subject.isPresent() ? Severity.OK : Severity.WRN;
      return validationResponse(
          carrier,
          valid,
          "Subject",
          () -> subject.get().getRef().getLabel(),
          () -> "Missing Subject Annotation on Case Surrogate"
      );
    } else return validationResponse(
        carrier,
        true,
        "Subject",
        () -> "N/A",
        () -> "N/A"
    );
  }

  /**
   * Checks for the present of a formal Asset Type term in the Surrogate
   * @param knowledgeAsset
   * @param carrier
   * @param types
   * @return
   */
  protected Answer<Void> validateAssetType(KnowledgeAsset knowledgeAsset, KnowledgeCarrier carrier,
      KnowledgeAssetType... types ) {
    List<KnowledgeAssetType> actualTypes = knowledgeAsset.getFormalType();
    List<KnowledgeAssetType> expectedTypes = Arrays.stream(types).collect(Collectors.toList());
    List<KnowledgeAssetType> foundTypes = expectedTypes.stream()
        .filter(type -> actualTypes.stream().anyMatch(t -> t.sameAs(type)))
        .collect(Collectors.toList());
    List<KnowledgeAssetType> missingTypes = new ArrayList<>(expectedTypes);
    missingTypes.removeAll(foundTypes);

    Severity status;
    if (actualTypes.isEmpty()) {
      status = Severity.ERR;
    } else if (foundTypes.containsAll(expectedTypes)) {
      status = Severity.OK;
    } else {
      status = Severity.WRN;
    }

    return validationResponse(
        carrier,
        status,
        "Asset Type",
        () -> "found " + toString(actualTypes, Term::getLabel),
        () -> "MISSING " + toString(missingTypes, Term::getLabel)
            + " BUT FOUND  " + toString(actualTypes, Term::getLabel));
  }


  protected <T> String toString(Collection<T> list, Function<T,String> mapper) {
    return list.stream()
        .map(mapper)
        .collect(Collectors.joining(","));
  }

  protected Answer<Void> validationResponse(KnowledgeCarrier carrier, boolean outcome,
      String ruleName, Supplier<String> successMsg, Supplier<String> failMsg) {
    return validationResponse(carrier, this::mapAssetId, outcome, ruleName, successMsg, failMsg);
  }

  protected Answer<Void> validationResponse(KnowledgeCarrier carrier, Severity outcome,
      String ruleName, Supplier<String> successMsg, Supplier<String> failMsg) {
    return validationResponse(carrier, this::mapAssetId, outcome, ruleName, successMsg, failMsg);
  }

  protected Answer<Void> validationResponse(
      KnowledgeCarrier carrier, Function<KnowledgeCarrier, String> keyMapper,
      boolean outcome,
      String ruleName, Supplier<String> successMsg, Supplier<String> failMsg) {
    return Answer.succeed().withExplanationDetail(
        format(
            keyMapper.apply(carrier),
            outcome ? Severity.OK : Severity.ERR,
            ruleName,
            outcome ? successMsg.get() : failMsg.get()));
  }


  protected Answer<Void> validationResponse(
      KnowledgeCarrier carrier, Function<KnowledgeCarrier, String> keyMapper,
      Severity outcome,
      String ruleName, Supplier<String> successMsg, Supplier<String> failMsg) {
    return Answer.succeed().withExplanationDetail(
        format(
            keyMapper.apply(carrier),
            outcome,
            ruleName,
            outcome == Severity.OK ? successMsg.get() : failMsg.get()));
  }

  @SuppressWarnings("unchecked")
  protected Answer<Void> allOf(Answer<Void>... partials) {
    return Stream.of(partials)
        .reduce(Answer::merge)
        .orElseGet(Answer::failed);
  }

}
