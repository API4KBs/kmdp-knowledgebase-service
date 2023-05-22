package edu.mayo.kmdp.knowledgebase.introspectors.struct;

import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.rdf.model.ResourceFactory.createStatement;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newVersionId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.newSurrogate;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Imports;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetrole.KnowledgeAssetRoleSeries.Composite_Knowledge_Asset;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Description_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;
import static org.omg.spec.api4kp._20200801.taxonomy.structuralreltype.StructuralPartTypeSeries.Has_Structural_Component;
import static org.omg.spec.api4kp._20200801.taxonomy.structuralreltype.StructuralPartTypeSeries.Has_Structuring_Component;

import edu.mayo.kmdp.knowledgebase.AbstractKnowledgeBaseOperator;
import edu.mayo.kmdp.language.parsers.rdf.JenaRdfParser;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import javax.inject.Named;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedIntrospect;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedIntrospectDirect;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal._applyLift;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.KPComponent;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.Component;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;

@Named
@KPOperation(Description_Task)
@KPSupport(OWL_2)
@KPComponent
public class CompositeAssetMetadataIntrospector
    extends AbstractKnowledgeBaseOperator
    implements _applyNamedIntrospect, _applyNamedIntrospectDirect {

  public static final UUID id
      = UUID.fromString("d31acad8-a385-4acf-b30b-1dbb3342b8b4");
  public static final String version = "1.0.0";

  private _applyLift parser = new JenaRdfParser();

  public CompositeAssetMetadataIntrospector() {
    super(SemanticIdentifier.newId(id,version));
  }

  public CompositeAssetMetadataIntrospector(KnowledgeBaseApiInternal kbManager) {
    this();
    this.kbManager = kbManager;
  }

  @Override
  public Answer<KnowledgeCarrier> applyNamedIntrospect(UUID operatorId, UUID kbaseId,
      String versionTag, String xParams) {

    return kbManager.getKnowledgeBaseManifestation(kbaseId,versionTag)
        .flatMap(this::doIntrospect);
  }

  @Override
  public Answer<KnowledgeCarrier> applyNamedIntrospectDirect(UUID operatorId, KnowledgeCarrier artifact,
      String xParams) {
    return doIntrospect(artifact);
  }

  private Answer<KnowledgeCarrier>  doIntrospect(KnowledgeCarrier sourceArtifact) {
    CompositeKnowledgeCarrier ckc = (CompositeKnowledgeCarrier) sourceArtifact;
    return extractSurrogate(ckc, getStructGraph(ckc));
  }

  private Model getStructGraph(CompositeKnowledgeCarrier ckc) {
    return parser.applyLift(
        ckc.getStruct(),
        Abstract_Knowledge_Expression.getTag(),
        null,
        null)
        .flatOpt(kc -> kc.as(Model.class))
        .orElseThrow(UnsupportedOperationException::new);
  }

  private Answer<KnowledgeCarrier> extractSurrogate(
      CompositeKnowledgeCarrier carrier, Model graph) {
    ResourceIdentifier assetId = carrier.getAssetId();

    KnowledgeCarrier struct = carrier.getStruct();

    KnowledgeAsset surrogate = newSurrogate(assetId)
        .get()
        .withName(carrier.getLabel())
        .withRole(Composite_Knowledge_Asset);

    List<Statement> partStatements =
        graph.listStatements(
            null,
            createProperty(Imports.getReferentId().toString()),
            (RDFNode) null).toList();
    partStatements.addAll(
        graph.listStatements(
            createResource(assetId.getVersionId().toString()),
            createProperty(Has_Structural_Component.getReferentId().toString()),
            (RDFNode) null).toList());
    partStatements.add(
        createStatement(
            createResource(assetId.getVersionId().toString()),
            createProperty(Has_Structural_Component.getReferentId().toString()),
            createResource(carrier.getRootId().getVersionId().toString())));

    partStatements.stream()
        .map(st -> st.getObject().asResource().getURI())
        .map(URI::create)
        .filter(compUri -> ! compUri.equals(assetId.getVersionId()))
        .distinct()
        .forEach(uri -> surrogate.withLinks(
            new Component()
                .withRel(Has_Structural_Component)
                .withHref(newVersionId(uri))));

    surrogate.withLinks(
        new Component()
            .withRel(Has_Structuring_Component)
            .withHref(struct.getAssetId()));

    return Answer.of(AbstractCarrier.ofAst(surrogate)
        .withAssetId(carrier.getAssetId())
        .withRepresentation(rep(Knowledge_Asset_Surrogate_2_0))
    );
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return OWL_2;
  }


}


