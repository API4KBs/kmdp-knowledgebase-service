package edu.mayo.kmdp.knowledgebase.introspectors.fhir.stu3;

import static edu.mayo.kmdp.util.DateTimeUtil.parseInstant;
import static edu.mayo.kmdp.util.DateTimeUtil.toLocalDate;
import static edu.mayo.kmdp.util.Util.isNotEmpty;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultArtifactId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.newSurrogate;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Depends_On;
import static org.omg.spec.api4kp._20200801.taxonomy.derivationreltype.DerivationTypeSeries.Is_Derived_From;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Plans_Processes_Pathways_And_Protocol_Definitions;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Description_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Draft;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Published;
import static org.omg.spec.api4kp._20200801.taxonomy.structuralreltype.StructuralPartTypeSeries.Has_Structural_Component;

import com.github.zafarkhaja.semver.Version;
import edu.mayo.kmdp.language.common.fhir.stu3.FHIRPlanDefinitionUtils;
import edu.mayo.kmdp.util.DateTimeUtil;
import edu.mayo.kmdp.util.StreamUtil;
import java.net.URI;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Named;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Enumerations.PublicationStatus;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Identifier.IdentifierUse;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPComponent;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.surrogate.Component;
import org.omg.spec.api4kp._20200801.surrogate.Dependency;
import org.omg.spec.api4kp._20200801.surrogate.Derivative;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.Link;
import org.omg.spec.api4kp._20200801.surrogate.Publication;
import org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetType;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries;

/**
 * Introspection class for fhir:PlanDefinition.
 * <p>
 * Generates the surrogate (KnowledgeAsset) for a KnowledgeCarrier-wrapped PlanDefinition.
 */
@Named
@KPOperation(Description_Task)
@KPSupport(FHIR_STU3)
@KPComponent
public class PlanDefinitionMetadataIntrospector
    extends AbstractFhirIntrospector<PlanDefinition> {

  public static final UUID id
      = UUID.fromString("ae1302ac-fafa-46e5-8ceb-b59b6959aa9d");
  public static final String OP_VERSION = "1.0.0";

  public PlanDefinitionMetadataIntrospector() {
    super(newId(id, OP_VERSION));
  }

  public PlanDefinitionMetadataIntrospector(KnowledgeBaseApiInternal kbManager) {
    this();
    this.kbManager = kbManager;
  }

  protected KnowledgeAsset innerIntrospect(
      PlanDefinition planDef,
      SyntacticRepresentation original,
      Properties props) {

    // ignore 's' for now
    var assetId = getAssetId(planDef, true);
    var artifactId = getArtifactId(planDef, assetId);

    return newSurrogate(assetId).get()
        .withName(planDef.getName())
        .withFormalCategory(Plans_Processes_Pathways_And_Protocol_Definitions)
        .withFormalType(detectType(planDef))
        .withLifecycle(detectAssetStatus(assetId))
        .withLinks(getAssetLinks(planDef))
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(artifactId)
            .withMimeType(codedRep(original))
            .withRepresentation(original)
            .withLinks(getArtifactLinks(planDef))
            .withLifecycle(detectLifeCycle(planDef.getStatus(), planDef.getDate(), artifactId)));
  }

  private Collection<Link> getAssetLinks(PlanDefinition planDef) {
    return Stream.concat(
            // the root Case Model asset ID is a secondary ID on the flat PlanDef
            Stream.of(getAssetId(planDef, false)),
            // decision Asset IDs are primary IDs on the contained components
            FHIRPlanDefinitionUtils.getNestedPlanDefs(planDef)
                .filter(pd -> pd != planDef)
                .map(pd -> getAssetId(pd, true))
        ).map(x -> new Component()
            .withHref(x)
            .withRel(Has_Structural_Component))
        .sorted(Comparator.comparing(l -> l.getHref().getUuid()))
        .collect(Collectors.toList());
  }

  private Collection<Link> getArtifactLinks(PlanDefinition planDef) {
    return planDef.getRelatedArtifact().stream()
        .map(this::getLink)
        .flatMap(StreamUtil::trimStream)
        .sorted(Comparator.comparing(l -> l.getHref().getUuid()))
        .collect(Collectors.toList());
  }

  private Optional<Link> getLink(RelatedArtifact relatedArtifact) {
    var ref = newId(URI.create(relatedArtifact.getUrl()));
    Link link = null;
    switch (relatedArtifact.getType()) {
      case DEPENDSON:
        link = new Dependency()
            .withHref(ref)
            .withRel(Depends_On);
        break;
      case DERIVEDFROM:
        link = new Derivative()
            .withHref(ref)
            .withRel(Is_Derived_From);
        break;
      case COMPOSEDOF:
        link = new Component()
            .withHref(ref)
            .withRel(Has_Structural_Component);
        break;
      default:
    }
    return Optional.ofNullable(link);
  }

  private Collection<KnowledgeAssetType> detectType(PlanDefinition planDef) {
    return planDef.getType().getCoding().stream()
        .filter(x -> x.getSystem().contains("KnowledgeAssetType"))
        .map(Coding::getCode)
        .map(cd -> KnowledgeAssetTypeSeries.resolve(cd)
            .or(() -> ClinicalKnowledgeAssetTypeSeries.resolve(cd)))
        .flatMap(StreamUtil::trimStream)
        .collect(Collectors.toList());
  }

  private Publication detectAssetStatus(ResourceIdentifier assetId) {
    var ver = Version.valueOf(assetId.getVersionTag());
    if (ver.getPreReleaseVersion() != null
        || ver.getBuildMetadata() != null
        || ver.getMajorVersion() == 0) {
      return new Publication()
          .withPublicationStatus(Draft);
    } else {
      return new Publication()
          .withPublicationStatus(Published);
    }
  }

  private Publication detectLifeCycle(
      PublicationStatus fhirStatus,
      Date planDefLastModified,
      ResourceIdentifier artifactId) {
    var status = detectStatus(fhirStatus, artifactId);
    var lastReview = detectLastReview(planDefLastModified, artifactId);
    return new Publication()
        .withPublicationStatus(status)
        .withLastReviewedOn(lastReview);
  }

  private Date detectLastReview(Date planDefLastModified, ResourceIdentifier artifactId) {
    if (planDefLastModified != null) {
      return planDefLastModified;
    }
    var ver = Version.valueOf(artifactId.getVersionTag());
    if (ver.getPreReleaseVersion() != null) {
      try {
        var info = ver.getPreReleaseVersion();
        // remove any RC et similar
        info = info.substring(info.lastIndexOf('.') + 1);
        return DateTimeUtil.fromLocalDateTime(toLocalDate(parseInstant(info)).atStartOfDay());
      } catch (Exception e) {
        // best effort
      }
    }
    return null;
  }

  private org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatus
  detectStatus(PublicationStatus fhirStatus, ResourceIdentifier artifactId) {
    if (fhirStatus != null) {
      switch (fhirStatus) {
        case ACTIVE:
          return Published;
        case DRAFT:
          return Draft;
        default:
          // other statuses not mapped
      }
    }
    var ver = Version.valueOf(artifactId.getVersionTag());
    if (ver.getPreReleaseVersion() != null
        || ver.getBuildMetadata() != null) {
      return Draft;
    } else {
      return Published;
    }
  }

  private ResourceIdentifier getAssetId(PlanDefinition planDef, boolean primary) {
    return planDef.getIdentifier().stream()
        .filter(x -> x.getType().getCoding().stream()
            .anyMatch(cd -> "https://www.omg.org/spec/API4KP/".equals(cd.getSystem())
                && "KnowledgeAsset".equals(cd.getCode())))
        .filter(x -> primary
            ? x.getUse() == IdentifierUse.OFFICIAL || x.getUse() == null
            : x.getUse() == IdentifierUse.SECONDARY)
        .flatMap(x -> parseId(x).stream())
        .findFirst()
        .orElseGet(() -> SemanticIdentifier.newVersionId(URI.create(planDef.getUrl())));
  }

  private ResourceIdentifier getArtifactId(PlanDefinition planDef, ResourceIdentifier assetId) {
    return planDef.getIdentifier().stream()
        .filter(x -> x.getType().getCoding().stream()
            .anyMatch(cd -> "https://www.omg.org/spec/API4KP/".equals(cd.getSystem())
                && "KnowledgeArtifact".equals(cd.getCode())))
        .flatMap(x -> parseId(x).stream())
        .findFirst()
        .orElseGet(() -> defaultArtifactId(assetId, FHIR_STU3));
  }

  private Optional<ResourceIdentifier> parseId(Identifier id) {
    try {
      var sys = id.getSystem();
      var ix = id.getValue().indexOf('|');
      var val = id.getValue().substring(0, ix);
      var ver = id.getValue().substring(ix + 1);
      if (isNotEmpty(sys) && isNotEmpty(val) && isNotEmpty(ver)) {
        return Optional.of(newId(URI.create(sys), val, ver));
      }
    } catch (Exception e) {
      // ignore
    }
    return Optional.empty();
  }


  @Override
  protected Class<PlanDefinition> getTypeClass() {
    return PlanDefinition.class;
  }


}
