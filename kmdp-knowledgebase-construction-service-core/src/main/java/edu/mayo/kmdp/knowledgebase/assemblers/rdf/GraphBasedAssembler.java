package edu.mayo.kmdp.knowledgebase.assemblers.rdf;

import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Knowledge_Resource_Composition_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;

import edu.mayo.kmdp.knowledgebase.AbstractKnowledgeBaseOperator;
import edu.mayo.kmdp.registry.Registry;
import edu.mayo.ontology.taxonomies.kao.rel.dependencyreltype.DependencyTypeSeries;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.CompositionalApiInternal;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetRepositoryApiInternal;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;

@KPOperation(Knowledge_Resource_Composition_Task)
@KPSupport(FHIR_STU3)
@Named
public class GraphBasedAssembler
    extends AbstractKnowledgeBaseOperator
    implements CompositionalApiInternal._assembleCompositeArtifact {

  public static final UUID id = UUID.fromString("284e63d4-84ff-42a1-b4ea-5e27a434d3c0");
  public static final String version = "1.0.0";

  // TODO this should be a parameter of the operation
  private static final String ASSET_BASE_URI = Registry.MAYO_ASSETS_BASE_URI;

  @Inject
  KnowledgeAssetRepositoryApiInternal repo;

  public GraphBasedAssembler() {
    super(SemanticIdentifier.newId(id,version));
  }

  public GraphBasedAssembler(KnowledgeAssetRepositoryApiInternal repo) {
    this();
    this.repo = repo;
  }

  public static CompositionalApiInternal._assembleCompositeArtifact newInstance(
      KnowledgeAssetRepositoryApiInternal repo) {
    GraphBasedAssembler assembler = new GraphBasedAssembler();
    assembler.repo = repo;
    return assembler;
  }

  @Override
  public Answer<KnowledgeCarrier> assembleCompositeArtifact(KnowledgeCarrier struct) {
    Set<ResourceIdentifier> componentIds = getComponentAssets(struct);

    Set<KnowledgeCarrier> components = componentIds.stream()
        .map(cid -> repo.getCanonicalKnowledgeAssetCarrier(cid.getUuid(),cid.getVersionTag()))
        .collect(Answer.toSet())
        .orElse(Collections.emptySet())
        ;

    return Answer.of(new CompositeKnowledgeCarrier()
            .withAssetId(struct.getAssetId())
            .withStruct(struct)
            .withComponent(components));
  }

  private Set<ResourceIdentifier> getComponentAssets(KnowledgeCarrier struct) {
    Model graph = struct.as(Model.class)
        .orElseThrow(IllegalArgumentException::new);

    Set<Resource> resources = getResources(graph);

    return resources.stream()
        .filter(res -> res.getURI().startsWith(ASSET_BASE_URI))
        .map(Resource::getURI)
        .map(URI::create)
        .map(SemanticIdentifier::newVersionId)
        .collect(Collectors.toSet());
  }

  private ResourceIdentifier getRootAsset(KnowledgeCarrier struct) {
    Model graph = struct.as(Model.class)
        .orElseThrow(IllegalArgumentException::new);

    Set<Resource> resources = getResources(graph);

    Set<Resource> assets = resources.stream()
        .filter(res -> res.getURI().startsWith(ASSET_BASE_URI))
        .collect(Collectors.toSet());

    Property dependsOn = new PropertyImpl(
        DependencyTypeSeries.Depends_On.getConceptId().toString());
    Set<Resource> dependents = getResources(
        graph.query(
            new SimpleSelector(null, dependsOn, (RDFNode) null)
        ));

    Set<Resource> roots = new HashSet<>(assets);
    roots.removeAll(dependents);

    if (roots.size() != 1) {
      throw new UnsupportedOperationException(
          "Unable to detect a single root asset in the structure, which is expected to be tree-based");
    }
    return SemanticIdentifier.newVersionId(URI.create(roots.iterator().next().getURI()));
  }

  private Set<Resource> getResources(Model graph) {
    Set<Resource> resources = new HashSet<>();
    graph.listObjects().forEachRemaining(node -> { if (node.canAs(Resource.class)) { resources.add(node.asResource()); }});
    return resources;
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return FHIR_STU3;
  }
}
