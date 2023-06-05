package edu.mayo.kmdp.language.validators;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newVersionId;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Well_Formedness_Check_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.CMMN_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.structuralreltype.StructuralPartTypeSeries.Has_Structural_Component;

import edu.mayo.kmdp.language.validators.cmmn.v1_1.CCPMComponentValidator;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Named;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.Severity;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.surrogate.Component;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.omg.spec.cmmn._20151109.model.TDefinitions;
import org.omg.spec.dmn._20180521.model.TImport;

@Named
@KPOperation(Well_Formedness_Check_Task)
@KPSupport({DMN_1_2, CMMN_1_1})
public class CCPMCompositeValidator extends CCPMComponentValidator {

  public static final UUID id = UUID.fromString("9b73e435-4fd6-4a86-817f-b4dabbbe9ba9");
  public static final String version = "1.0.0";

  // this may be overridden by configuration - for now, set defensively.
  public static final Severity FAIL_SEVERITY = Severity.FATAL;

  private final ResourceIdentifier operatorId;

  public CCPMCompositeValidator() {
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
    return Arrays.asList(rep(CMMN_1_1), rep(DMN_1_2), rep(Knowledge_Asset_Surrogate_2_0));
  }

  @Override
  public Answer<Void> applyValidate(KnowledgeCarrier knowledgeCarrier, String xConfig) {
    return validateComposite(knowledgeCarrier);
  }

  protected Answer<Void> validateComposite(KnowledgeCarrier knowledgeCarrier) {
    Optional<KnowledgeAsset> compositeSurrogate = knowledgeCarrier.components()
        .map(KnowledgeCarrier::getExpression)
        .flatMap(StreamUtil.filterAs(KnowledgeAsset.class))
        .findFirst();
    Optional<CompositeKnowledgeCarrier> compositeArtifact = knowledgeCarrier.components()
        .flatMap(StreamUtil.filterAs(CompositeKnowledgeCarrier.class))
        .findFirst();
    if (compositeArtifact.isEmpty() || compositeSurrogate.isEmpty()) {
      return Answer.failed(ResponseCodeSeries.UnprocessableEntity);
    }
    return validateComposite(compositeSurrogate.get(), compositeArtifact.get());
  }

  private Answer<Void> validateComposite(
      KnowledgeAsset metadata, CompositeKnowledgeCarrier components) {
    return Stream.of(
            validateMetadataLinks(metadata, components),
            validateImports(metadata, components)
        ).reduce(Answer::merge)
        .orElseGet(Answer::succeed);
  }

  private Answer<Void> validateMetadataLinks(
      KnowledgeAsset metadata, CompositeKnowledgeCarrier components) {
    List<Component> parts = metadata.getLinks().stream()
        .flatMap(StreamUtil.filterAs(Component.class))
        .filter(c -> Has_Structural_Component.sameAs(c.getRel()))
        .collect(Collectors.toList());

    if (parts.size() != components.components().count()) {
      return validationResponse(
          components,
          FAIL_SEVERITY,
          "Consistency",
          this::impossible,
          () -> "Metadata not consistent with components"
      );
    }

    List<ResourceIdentifier> brokenLinks = parts.stream()
        .map(Component::getHref)
        .filter(compId -> unknownAsset(compId, components))
        .collect(Collectors.toList());

    return validationResponse(
        components,
        brokenLinks.isEmpty() ? Severity.OK : FAIL_SEVERITY,
        "Consistency",
        () -> "Assets components resolved",
        () -> "Unresolved components " + brokenLinks.stream()
            .map(ResourceIdentifier::toString).collect(Collectors.joining(","))
    );
  }

  private Answer<Void> validateImports(
      KnowledgeAsset metadata, CompositeKnowledgeCarrier components) {
    return components.components()
        .map(comp -> validateImports(metadata, comp, components))
        .reduce(Answer::merge)
        .orElseGet(Answer::succeed);
  }

  private Answer<Void> validateImports(
      KnowledgeAsset metadata, KnowledgeCarrier comp, CompositeKnowledgeCarrier components) {
    if (comp.is(TDefinitions.class)) {
      return validateCMMNImports(metadata,
          comp.as(TDefinitions.class).orElseThrow(),
          components);
    } else if (comp.is(org.omg.spec.dmn._20180521.model.TDefinitions.class)) {
      return validateDMNImports(metadata,
          comp.as(org.omg.spec.dmn._20180521.model.TDefinitions.class).orElseThrow(),
          components);
    } else if (comp.is(PlanDefinition.class)) {
      // native FHIR components are validated elsewhere
      return Answer.succeed();
    } else {
      return validationResponse(
          components,
          FAIL_SEVERITY,
          "Artifact Type",
          this::impossible,
          () -> "Unable to handle " + comp.getExpression().getClass().getName());
    }
  }

  private Answer<Void> validateDMNImports(
      KnowledgeAsset metadata,
      org.omg.spec.dmn._20180521.model.TDefinitions model,
      CompositeKnowledgeCarrier components) {
    List<ResourceIdentifier> brokenLinks = model.getImport().stream()
        .map(TImport::getNamespace)
        .map(uri -> newVersionId(URI.create(uri)))
        .filter(uri -> unknownArtifact(uri, components))
        .collect(Collectors.toList());
    return validationResponse(
        components,
        brokenLinks.isEmpty() ? Severity.OK : FAIL_SEVERITY,
        "Artifact Consistency",
        () -> "Artifact components resolved " + metadata.getAssetId(),
        () -> metadata.getAssetId() + " Unresolved components " + brokenLinks.stream()
            .map(ResourceIdentifier::toString).collect(Collectors.joining(","))
    );
  }

  private Answer<Void> validateCMMNImports(
      KnowledgeAsset metadata,
      TDefinitions model,
      CompositeKnowledgeCarrier components) {
    List<ResourceIdentifier> brokenLinks = model.getImport().stream()
        .map(org.omg.spec.cmmn._20151109.model.TImport::getNamespace)
        .map(uri -> newVersionId(URI.create(uri)))
        .filter(uri -> unknownArtifact(uri, components))
        .collect(Collectors.toList());
    return validationResponse(
        components,
        brokenLinks.isEmpty() ? Severity.OK : FAIL_SEVERITY,
        "Artifact Consistency",
        () -> "Artifact components resolved " + metadata.getAssetId(),
        () -> metadata.getAssetId() + " Unresolved components " + brokenLinks.stream()
            .map(ResourceIdentifier::toString).collect(Collectors.joining(","))
    );
  }

  private boolean unknownAsset(ResourceIdentifier compId, CompositeKnowledgeCarrier components) {
    return components.components()
        .noneMatch(x -> x.getAssetId().getUuid().equals(compId.getUuid()));
  }

  private boolean unknownArtifact(ResourceIdentifier compId, CompositeKnowledgeCarrier components) {
    return components.components()
        .noneMatch(x -> x.getArtifactId().getUuid().equals(compId.getUuid()));
  }


}
