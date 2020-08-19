package edu.mayo.kmdp.knowledgebase.constructors;

import static edu.mayo.kmdp.registry.Registry.MAYO_ASSETS_BASE_URI_URI;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.ofAst;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Knowledge_Resource_Construction_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;

import edu.mayo.kmdp.util.JenaUtil;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.Util;
import java.net.URI;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetCatalogApiInternal;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.Dependency;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetrole.KnowledgeAssetRoleSeries;
import org.omg.spec.api4kp._20200801.taxonomy.structuralreltype.StructuralPartTypeSeries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@KPOperation(Knowledge_Resource_Construction_Task)
@KPSupport(FHIR_STU3)
@Named
public class DependencyBasedConstructor implements
    KnowledgeBaseApiInternal._getKnowledgeBaseStructure {

  private static final Logger logger = LoggerFactory.getLogger(DependencyBasedConstructor.class);

  @Inject
  KnowledgeAssetCatalogApiInternal repo;

  public static KnowledgeBaseApiInternal._getKnowledgeBaseStructure newInstance(
      KnowledgeAssetCatalogApiInternal repo) {
    DependencyBasedConstructor constructor = new DependencyBasedConstructor();
    constructor.repo = repo;
    return constructor;
  }

  @Override
  public Answer<KnowledgeCarrier> getKnowledgeBaseStructure(UUID seedAssetId,
      String seedAssetVersionTag) {

    UUID compositeAssetId = Util.uuid(seedAssetId.toString());
    String compositeAssetVersion = seedAssetVersionTag;
    // need to specify asset base otherwise will default to urn:uuid
    ResourceIdentifier compositeAssetVersionedId = SemanticIdentifier
        .newId(MAYO_ASSETS_BASE_URI_URI, seedAssetId, seedAssetVersionTag);

    // TODO Rather than getting ALL the assets,
    // there should be a query based on the assetId, or 'bundle' should be used
    // but getAssetBundle, unlike getArtifactBundle, is not implemented yet
    return repo.listKnowledgeAssets()
        .flatMap(ptrList ->
            ptrList.stream()
                .map(axId -> repo.getKnowledgeAsset(axId.getUuid()))
                .collect(Answer.toList())
        )
        // Now we create a struct based on the analysis of the relationships between the assets
        .map(
            list -> {
              Model m = ModelFactory.createDefaultModel();

              m.add(JenaUtil.objA(
                  compositeAssetVersionedId.getResourceId().toString(),
                  RDF.type,
                  KnowledgeAssetRoleSeries.Composite_Knowledge_Asset.getConceptId().toString()
              ));

              list.forEach(ax -> structure(compositeAssetVersionedId, ax, m));

              return m;
            })
        .map(m -> {
          logger.info(JenaUtil.asString(m));
          return m;
        })
        // And we return it
        // TODO This is really RDF, but RDF is not yet registered !! :
        .map(m -> ofAst(m, rep(OWL_2))
            // Need to Generate a new ID for the composite asset just constructed.
            .withAssetId(compositeAssetVersionedId)
        );
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

}
