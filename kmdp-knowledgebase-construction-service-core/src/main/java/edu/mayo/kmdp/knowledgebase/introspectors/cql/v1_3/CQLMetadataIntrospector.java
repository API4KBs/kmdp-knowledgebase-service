package edu.mayo.kmdp.knowledgebase.introspectors.cql.v1_3;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.newSurrogate;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Description_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HL7_CQL;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.languagerole.KnowledgeRepresentationLanguageRoleSeries.Schema_Language;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;

import edu.mayo.kmdp.knowledgebase.AbstractKnowledgeBaseOperator;
import java.util.UUID;
import javax.inject.Named;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedIntrospect;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPComponent;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;

@Named
@KPOperation(Description_Task)
@KPSupport(HL7_CQL)
@KPComponent
public class CQLMetadataIntrospector
    extends AbstractKnowledgeBaseOperator
    implements _applyNamedIntrospect {

  public static final UUID id
      = UUID.fromString("8969059a-0f3e-41c6-83be-fe0d14bdbfc6");
  public static final String version = "1.0.0";

  public CQLMetadataIntrospector() {
    super(SemanticIdentifier.newId(id,version));
  }

  public CQLMetadataIntrospector(KnowledgeBaseApiInternal kbManager) {
    this();
    this.kbManager = kbManager;
  }

  @Override
  public Answer<KnowledgeCarrier> applyNamedIntrospect(UUID operatorId, UUID kbaseId,
      String versionTag, String xParams) {

    return kbManager.getKnowledgeBaseManifestation(kbaseId,versionTag)
        .map(AbstractCarrier::mainComponent)
        .flatMap(this::doIntrospect);
  }

  private Answer<KnowledgeCarrier> doIntrospect(KnowledgeCarrier sourceArtifact) {
    KnowledgeAsset surrogate = newSurrogate(sourceArtifact.getAssetId()).get()
        .withName("TODO")
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(sourceArtifact.getArtifactId())
            .withRepresentation(rep(HL7_CQL)
                .withSubLanguage(rep(FHIR_STU3).withRole(Schema_Language))));

    return Answer.of(
        AbstractCarrier.ofAst(surrogate)
            .withAssetId(sourceArtifact.getAssetId())
            .withLevel(Abstract_Knowledge_Expression)
            // TODO Improve..
            .withArtifactId(surrogate.getSurrogate().get(0).getArtifactId())
            .withRepresentation(new SyntacticRepresentation()
                .withLanguage(Knowledge_Asset_Surrogate_2_0)));
  }


  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return HL7_CQL;
  }

}