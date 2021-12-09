package edu.mayo.kmdp.knowledgebase.introspectors.fhir.stu3;

import static edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries.BadRequest;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.id.VersionIdentifier.toSemVer;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultSurrogateId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.newSurrogate;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Structured_Information_And_Data_Capture_Models;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Questionnaire;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Description_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Archived;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Draft;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Published;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Unpublished;

import edu.mayo.kmdp.knowledgebase.AbstractKnowledgeBaseOperator;
import edu.mayo.kmdp.language.parsers.fhir.stu3.FHIR3Deserializer;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Named;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.Questionnaire;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedIntrospect;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedIntrospectDirect;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPComponent;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatus;

/**
 * Introspection class for PlanDefinitions. Generates the surrogate (KnowledgeAsset) for the
 * PlanDefinition given the KnowledgeCarrier.
 */
@Named
@KPOperation(Description_Task)
@KPSupport(FHIR_STU3)
@KPComponent
public class QuestionnaireMetadataIntrospector
    extends AbstractKnowledgeBaseOperator
    implements _applyNamedIntrospect, _applyNamedIntrospectDirect {

  public static final UUID id
      = UUID.fromString("9dec1fce-a567-4c71-b4c1-c75091683c3f");
  public static final String VERSION = "1.0.0";

  public QuestionnaireMetadataIntrospector() {
    super(newId(id, VERSION));
  }

  public QuestionnaireMetadataIntrospector(KnowledgeBaseApiInternal kbManager) {
    this();
    this.kbManager = kbManager;
  }


  @Override
  public Answer<KnowledgeCarrier> applyNamedIntrospect(UUID operatorId, UUID kbaseId,
      String versionTag, String xParams) {

    return kbManager.getKnowledgeBaseManifestation(kbaseId, versionTag)
        .map(AbstractCarrier::mainComponent)
        .flatMap(kc -> this.doIntrospect(kc, kc.getRepresentation()));
  }

  @Override
  public Answer<KnowledgeCarrier> applyNamedIntrospectDirect(UUID operatorId,
      KnowledgeCarrier artifact, String xParams) {
    return new FHIR3Deserializer()
        .applyLift(artifact, Abstract_Knowledge_Expression, codedRep(FHIR_STU3),null)
        .flatMap(parsed -> doIntrospect(parsed, artifact.getRepresentation()));
  }

  private Answer<KnowledgeCarrier> doIntrospect(KnowledgeCarrier knowledgeCarrier,
      SyntacticRepresentation original) {
    Optional<org.hl7.fhir.dstu3.model.Questionnaire> questOpt = knowledgeCarrier.as(
        org.hl7.fhir.dstu3.model.Questionnaire.class);
    if (questOpt.isEmpty()) {
      return Answer.failed(BadRequest);
    }
    return doIntrospect(questOpt.get(), original);
  }

  private Answer<KnowledgeCarrier> doIntrospect(Questionnaire quest,
      SyntacticRepresentation original) {
    ResourceIdentifier assetId = SemanticIdentifier.newVersionId(URI.create(quest.getUrl()));
    assetId.withVersionTag(toSemVer(assetId.getVersionTag()));

    SyntacticRepresentation rep = (SyntacticRepresentation) original.clone();
    if (rep.getLanguage() == null) {
      rep.setLanguage(FHIR_STU3);
    }

    ResourceIdentifier artifactId =
        defaultSurrogateId(assetId, FHIR_STU3, toSemVer(quest.getVersion()));

    KnowledgeAsset surrogate = newSurrogate(assetId)
        .withPublicationStatus(mapStatus(quest.getStatus()))
        .get()
        .withName(quest.getName())
        .withFormalCategory(Structured_Information_And_Data_Capture_Models)
        .withFormalType(Questionnaire)
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(artifactId)
            .withRepresentation(rep));

    return Answer.of(
        AbstractCarrier.ofAst(surrogate)
            .withAssetId(surrogate.getAssetId())
            .withLevel(Abstract_Knowledge_Expression)
            .withArtifactId(surrogate.getSurrogate().get(0).getArtifactId())
            .withLabel(surrogate.getName())
            .withRepresentation(rep(Knowledge_Asset_Surrogate_2_0))
    );

  }

  private PublicationStatus mapStatus(Enumerations.PublicationStatus status) {
    switch (status) {
      case DRAFT:
        return Draft;
      case ACTIVE:
        return Published;
      case RETIRED:
        return Archived;
      case NULL:
      case UNKNOWN:
      default:
        return Unpublished;
    }
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return FHIR_STU3;
  }


}
