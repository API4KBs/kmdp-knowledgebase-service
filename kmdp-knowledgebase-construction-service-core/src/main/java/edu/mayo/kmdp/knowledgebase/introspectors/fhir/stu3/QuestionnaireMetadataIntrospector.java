package edu.mayo.kmdp.knowledgebase.introspectors.fhir.stu3;

import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.id.VersionIdentifier.toSemVer;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultSurrogateId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.newSurrogate;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Structured_Information_And_Data_Capture_Models;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Questionnaire;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Description_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;

import java.net.URI;
import java.util.Properties;
import java.util.UUID;
import javax.inject.Named;
import org.hl7.fhir.dstu3.model.Questionnaire;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPComponent;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;

/**
 * Introspection class for fhir:Questionnaire.
 * <p>
 * Generates the surrogate (KnowledgeAsset) for a KnowledgeCarrier-wrapped Questionnaire.
 */
@Named
@KPOperation(Description_Task)
@KPSupport(FHIR_STU3)
@KPComponent
public class QuestionnaireMetadataIntrospector
    extends AbstractFhirIntrospector<Questionnaire> {

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


  protected KnowledgeAsset innerIntrospect(
      Questionnaire quest,
      SyntacticRepresentation original,
      Properties props) {
    ResourceIdentifier assetId = SemanticIdentifier.newVersionId(URI.create(quest.getUrl()));
    assetId.withVersionTag(toSemVer(assetId.getVersionTag()));

    SyntacticRepresentation rep = (SyntacticRepresentation) original.clone();
    if (rep.getLanguage() == null) {
      rep.setLanguage(FHIR_STU3);
    }

    ResourceIdentifier artifactId =
        defaultSurrogateId(assetId, FHIR_STU3, toSemVer(quest.getVersion()));

    return newSurrogate(assetId)
        .withPublicationStatus(mapStatus(quest.getStatus()))
        .get()
        .withName(quest.getName())
        .withFormalCategory(Structured_Information_And_Data_Capture_Models)
        .withFormalType(Questionnaire)
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(artifactId)
            .withRepresentation(rep));
  }

  @Override
  protected Class<Questionnaire> getTypeClass() {
    return Questionnaire.class;
  }
}
