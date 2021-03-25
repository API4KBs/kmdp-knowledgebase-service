package edu.mayo.kmdp.knowledgebase.constructors;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Knowledge_Resource_Construction_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;

import edu.mayo.kmdp.knowledgebase.AbstractKnowledgeBaseOperator;
import edu.mayo.kmdp.util.JenaUtil;
import edu.mayo.kmdp.util.StreamUtil;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import javax.inject.Named;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal._getKnowledgeBaseStructure;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.Dependency;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetrole.KnowledgeAssetRoleSeries;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.omg.spec.api4kp._20200801.taxonomy.structuralreltype.StructuralPartTypeSeries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@KPOperation(Knowledge_Resource_Construction_Task)
@KPSupport(OWL_2)
@Named
public class JenaOwlImportConstructor
  extends AbstractKnowledgeBaseOperator
  implements _getKnowledgeBaseStructure {

  private static final Logger logger = LoggerFactory.getLogger(JenaOwlImportConstructor.class);

  public static final UUID id = UUID.fromString("04bca4bb-edd2-4680-a67a-20684cc8dd97");
  public static final String version = "1.0.0";

  public JenaOwlImportConstructor() {
    super(SemanticIdentifier.newId(id,version));
  }

  public JenaOwlImportConstructor(KnowledgeBaseApiInternal kbManager) {
    this();
    this.kbManager = kbManager;
  }

  @Override
  public Answer<KnowledgeCarrier> getKnowledgeBaseStructure(UUID kbId,
      String kbVersionTag, String params) {

    List<KnowledgeCarrier> components = kbManager.getKnowledgeBaseManifestation(kbId, kbVersionTag)
        .filter(CompositeKnowledgeCarrier.class::isInstance)
        .map(CompositeKnowledgeCarrier.class::cast)
        .map(CompositeKnowledgeCarrier::getComponent)
        .orElse(Collections.emptyList());

    Model struct = ModelFactory.createDefaultModel();
    components.stream()
        .flatMap(this::getDependencies)
        .forEach(struct::add);

    return Answer.of(AbstractCarrier.ofAst(struct,rep(OWL_2)));
  }

  private Stream<Statement> getDependencies(KnowledgeCarrier kc) {
    Optional<Model> m = kc.as(Model.class);
    if (m.isEmpty()) {
      return Stream.empty();
    }
    Set<Statement> imports = new HashSet<>();
    if (m.get() instanceof OntModel) {
      OntModel om = (OntModel) m.get();
      om.listOntologies()
          .mapWith(Ontology::getImport)
          .filterKeep(Objects::nonNull)
          .mapWith(or -> URI.create(or.toString()))
          .mapWith(or -> JenaUtil.objA(kc.getAssetId().getVersionId(),
              DependencyTypeSeries.Imports.getReferentId(),
              or))
          .forEachRemaining(imports::add);
    }
    return imports.stream();
  }

  private void structure(ResourceIdentifier rootAssetId, KnowledgeAsset componentAsset, Model m) {
    // TODO Use the versionUris when proper set in the XML
    m.add(JenaUtil.objA(
        getVersionURI(rootAssetId),
        StructuralPartTypeSeries.Has_Structural_Component.getConceptId().toString(),
        getVersionURI(componentAsset.getAssetId())
    ));
    m.add(JenaUtil.objA(
        getVersionURI(componentAsset.getAssetId()),
        RDF.type,
        KnowledgeAssetRoleSeries.Component_Knowledge_Asset.getConceptId().toString()
    ));

    componentAsset.getFormalType()
        .forEach(type -> {
              m.add(JenaUtil.objA(
                  componentAsset.getAssetId().getResourceId().toString(),
                  RDF.type,
                  type.getConceptId().toString())
              );

              m.add(JenaUtil.datA(
                  type.getConceptId().toString(),
                  RDFS.label,
                  type.getLabel())
              );
            }
        );

    componentAsset.getLinks().stream()
        .flatMap(StreamUtil.filterAs(Dependency.class))
        .filter(dep -> DependencyTypeSeries.Depends_On.sameAs(dep.getRel()))
        .forEach(dep -> m.add(JenaUtil.objA(
            getVersionURI(componentAsset.getAssetId()),
            DependencyTypeSeries.Depends_On.getConceptId().toString(),
            getVersionURI(dep.getHref()))));
  }

  private String getVersionURI(ResourceIdentifier assetId) {
    URI versionURI = assetId.getVersionId() != null
        ? assetId.getVersionId()
        : SemanticIdentifier.newId(assetId.getNamespaceUri(), assetId.getTag(), "SNAPSHOT")
            .getVersionId();
    return versionURI.toString();
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return OWL_2;
  }
}
