package edu.mayo.kmdp.knowledgebase.introspectors.fhir.stu3;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.newSurrogate;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Cognitive_Care_Process_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Description_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;

import edu.mayo.kmdp.knowledgebase.AbstractKnowledgeBaseOperator;
import java.util.UUID;
import javax.inject.Named;
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
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries;

/**
 * Introspection class for PlanDefinitions. Generates the surrogate (KnowledgeAsset) for the
 * PlanDefinition given the KnowledgeCarrier.
 */
@Named
@KPOperation(Description_Task)
@KPSupport(FHIR_STU3)
@KPComponent
public class PlanDefinitionMetadataIntrospector
    extends AbstractKnowledgeBaseOperator
    implements _applyNamedIntrospect, _applyNamedIntrospectDirect {

  public static final UUID id
      = UUID.fromString("ae1302ac-fafa-46e5-8ceb-b59b6959aa9d");
  public static final String version = "1.0.0";

  public PlanDefinitionMetadataIntrospector() {
    super(SemanticIdentifier.newId(id, version));
  }

  public PlanDefinitionMetadataIntrospector(KnowledgeBaseApiInternal kbManager) {
    this();
    this.kbManager = kbManager;
  }


  @Override
  public Answer<KnowledgeCarrier> applyNamedIntrospect(UUID operatorId, UUID kbaseId,
      String versionTag, String xParams) {

    return kbManager.getKnowledgeBaseManifestation(kbaseId, versionTag)
        .map(AbstractCarrier::mainComponent)
        .flatMap(this::doIntrospect);
  }

  @Override
  public Answer<KnowledgeCarrier> applyNamedIntrospectDirect(UUID operatorId,
      KnowledgeCarrier artifact, String xParams) {
    return doIntrospect(artifact);
  }

  private Answer<KnowledgeCarrier> doIntrospect(KnowledgeCarrier knowledgeCarrier) {

    // ignore 's' for now
    ResourceIdentifier uri = knowledgeCarrier.getAssetId();
    KnowledgeAsset surrogate = newSurrogate(uri).get()
        .withName(knowledgeCarrier.getLabel())
        .withFormalType(Cognitive_Care_Process_Model)
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(knowledgeCarrier.getArtifactId())
            .withRepresentation(rep(FHIR_STU3)));

    return Answer.of(
        AbstractCarrier.ofAst(surrogate)
            .withAssetId(knowledgeCarrier.getAssetId())
            .withLevel(ParsingLevelSeries.Abstract_Knowledge_Expression)
            // TODO improve...
            .withArtifactId(surrogate.getSurrogate().get(0).getArtifactId())
            .withRepresentation(rep(Knowledge_Asset_Surrogate_2_0))
    );

  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return FHIR_STU3;
  }


}
