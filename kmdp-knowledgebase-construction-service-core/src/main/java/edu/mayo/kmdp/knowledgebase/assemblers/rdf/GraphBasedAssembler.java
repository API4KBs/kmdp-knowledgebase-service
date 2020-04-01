package edu.mayo.kmdp.knowledgebase.assemblers.rdf;

import static edu.mayo.ontology.taxonomies.api4kp.knowledgeoperations.KnowledgeProcessingOperationSeries.Knowledge_Resource_Composition_Task;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;

import edu.mayo.kmdp.knowledgebase.v4.server.CompositionalApiInternal;
import edu.mayo.kmdp.registry.Registry;
import edu.mayo.kmdp.repository.asset.v4.server.KnowledgeAssetRetrievalApiInternal;
import edu.mayo.ontology.taxonomies.kao.rel.dependencyreltype.DependencyTypeSeries;
import java.net.URI;
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
import org.omg.spec.api4kp._1_0.Answer;
import org.omg.spec.api4kp._1_0.id.ResourceIdentifier;
import org.omg.spec.api4kp._1_0.id.SemanticIdentifier;
import org.omg.spec.api4kp._1_0.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._1_0.services.KPOperation;
import org.omg.spec.api4kp._1_0.services.KPSupport;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;

@KPOperation(Knowledge_Resource_Composition_Task)
@KPSupport(FHIR_STU3)
@Named
public class GraphBasedAssembler implements CompositionalApiInternal._assembleCompositeArtifact {

  // TODO this should be a parameter of the operation
  private static final String ASSET_BASE_URI = Registry.MAYO_ASSETS_BASE_URI;

  @Inject
  KnowledgeAssetRetrievalApiInternal repo;

  public static CompositionalApiInternal._assembleCompositeArtifact newInstance(
      KnowledgeAssetRetrievalApiInternal repo) {
    GraphBasedAssembler assembler = new GraphBasedAssembler();
    assembler.repo = repo;
    return assembler;
  }

  @Override
  public Answer<KnowledgeCarrier> assembleCompositeArtifact(KnowledgeCarrier struct) {
    ResourceIdentifier rootAsset = getRootAsset(struct);
    return repo.getKnowledgeArtifactBundle(UUID.fromString(rootAsset.getTag()), rootAsset.getVersionTag())
        .map(parts -> new CompositeKnowledgeCarrier()
            .withAssetId(struct.getAssetId())
            .withStruct(struct)
            .withComponent(parts));
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

}
