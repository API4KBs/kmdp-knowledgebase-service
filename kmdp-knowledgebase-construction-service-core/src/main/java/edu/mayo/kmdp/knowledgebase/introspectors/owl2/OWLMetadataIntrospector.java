package edu.mayo.kmdp.knowledgebase.introspectors.owl2;


import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Description_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;

import edu.mayo.kmdp.knowledgebase.AbstractKnowledgeBaseOperator;
import edu.mayo.kmdp.language.parsers.owl2.OWLParser;
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
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.semanticweb.owlapi.model.OWLOntology;
import org.springframework.beans.factory.annotation.Autowired;

@Named
@KPOperation(Description_Task)
@KPSupport(OWL_2)
@KPComponent
public class OWLMetadataIntrospector
    extends AbstractKnowledgeBaseOperator
    implements _applyNamedIntrospect, _applyNamedIntrospectDirect {

  public static final UUID id
      = UUID.fromString("3c0683de-cb0f-45cf-ab6a-7964559ee91a");
  public static final String version = "1.0.0";

  @Autowired
  @KPSupport(OWL_2)
  private _applyLift parser;

  public OWLMetadataIntrospector() {
    super(SemanticIdentifier.newId(id, version));
    this.parser = new OWLParser();
  }

  public OWLMetadataIntrospector(KnowledgeBaseApiInternal kbManager) {
    this(kbManager, new OWLParser());
  }

  public OWLMetadataIntrospector(KnowledgeBaseApiInternal kbManager, _applyLift parser) {
    this();
    this.kbManager = kbManager;
    this.parser = parser;
  }

  @Override
  public Answer<KnowledgeCarrier> applyNamedIntrospect(UUID operatorId, UUID kbaseId,
      String versionTag, String xParams) {
    return kbManager.getKnowledgeBaseManifestation(kbaseId, versionTag)
        .flatMap(kc -> parser.applyLift(kc, Abstract_Knowledge_Expression, null, null))
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
          .applyLift(source, Abstract_Knowledge_Expression, null, null)
          .flatMap(this::innerIntrospect);
    }
  }

  private Answer<KnowledgeCarrier> innerIntrospect(KnowledgeCarrier carrier) {
    OWLOntology owl = carrier.mainComponentAs(OWLOntology.class);

//    KnowledgeAsset surrogate = newSurrogate(carrier.getAssetId()).get()
//        .withName(dmnModel.getName())
//        .withCarriers(new KnowledgeArtifact()
//            .withArtifactId(carrier.getArtifactId())
//            .withRepresentation(rep(DMN_1_1)));
//
//    return Answer.of(
//        AbstractCarrier.ofAst(surrogate)
//            .withAssetId(carrier.getAssetId())
//            .withLevel(Abstract_Knowledge_Expression)
//            // TODO improve...
//            .withArtifactId(surrogate.getSurrogate().get(0).getArtifactId())
//            .withRepresentation(new SyntacticRepresentation()
//                .withLanguage(Knowledge_Asset_Surrogate_2_0))
//    );
    return Answer.unsupported();
  }


  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return OWL_2;
  }

}