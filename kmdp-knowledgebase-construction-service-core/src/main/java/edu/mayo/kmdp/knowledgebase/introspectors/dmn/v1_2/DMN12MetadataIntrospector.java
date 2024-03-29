package edu.mayo.kmdp.knowledgebase.introspectors.dmn.v1_2;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.newSurrogate;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Description_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;

import edu.mayo.kmdp.knowledgebase.AbstractKnowledgeBaseOperator;
import edu.mayo.kmdp.language.parsers.dmn.v1_2.DMN12Parser;
import java.util.UUID;
import javax.inject.Named;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedIntrospect;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedIntrospectDirect;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal._applyLift;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPComponent;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.omg.spec.dmn._20180521.model.TDefinitions;
import org.springframework.beans.factory.annotation.Autowired;

@Named
@KPOperation(Description_Task)
@KPSupport(DMN_1_2)
@KPComponent
public class DMN12MetadataIntrospector
    extends AbstractKnowledgeBaseOperator
    implements _applyNamedIntrospect, _applyNamedIntrospectDirect {

  public static final UUID id
      = UUID.fromString("78d054a8-ba2d-4b6f-93a7-667cfe0820ee");
  public static final String version = "1.0.0";

  @Autowired
  @KPSupport(DMN_1_2)
  private _applyLift parser;

  public DMN12MetadataIntrospector() {
    super(SemanticIdentifier.newId(id,version));
    this.parser = new DMN12Parser();
  }

  public DMN12MetadataIntrospector(KnowledgeBaseApiInternal kbManager) {
    this(kbManager, new DMN12Parser());
  }

  public DMN12MetadataIntrospector(KnowledgeBaseApiInternal kbManager, _applyLift parser) {
    this();
    this.kbManager = kbManager;
    this.parser = parser;
  }

  @Override
  public Answer<KnowledgeCarrier> applyNamedIntrospect(UUID operatorId, UUID kbaseId,
      String versionTag, String xParams) {

    return kbManager.getKnowledgeBaseManifestation(kbaseId,versionTag)
        .map(AbstractCarrier::mainComponent)
        .flatMap(this::doIntrospect);
  }

  @Override
  public Answer<KnowledgeCarrier> applyNamedIntrospectDirect(UUID operatorId,
      KnowledgeCarrier artifact, String xParams) {
    return doIntrospect(artifact);
  }

  private Answer<KnowledgeCarrier> doIntrospect(KnowledgeCarrier source) {
    if (parser == null && ! Abstract_Knowledge_Expression.sameAs(source.getLevel())) {
      return Answer.unsupported();
    } else if (parser == null) {
      return innerIntrospect(source);
    } else {
      return parser
          .applyLift(source, Abstract_Knowledge_Expression.getTag(), null, null)
          .flatMap(this::innerIntrospect);
    }
  }

  private Answer<KnowledgeCarrier> innerIntrospect(KnowledgeCarrier carrier) {
    TDefinitions dmnModel = carrier.as(TDefinitions.class)
        .orElseThrow(IllegalStateException::new);

    KnowledgeAsset surrogate = newSurrogate(carrier.getAssetId()).get()
        .withName(dmnModel.getName())
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(carrier.getArtifactId())
            .withRepresentation(rep(DMN_1_2)));

    return Answer.of(
        AbstractCarrier.ofAst(surrogate)
            .withAssetId(carrier.getAssetId())
            .withLevel(Abstract_Knowledge_Expression)
            // TODO improve...
            .withArtifactId(surrogate.getSurrogate().get(0).getArtifactId())
            .withRepresentation(rep(Knowledge_Asset_Surrogate_2_0))
    );
  }


  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return DMN_1_2;
  }
}