package edu.mayo.kmdp.knowledgebase.constructors;

import static edu.mayo.kmdp.id.helper.DatatypeHelper.uri;
import static edu.mayo.ontology.taxonomies.api4kp.knowledgeoperations.KnowledgeProcessingOperationSeries.Knowledge_Resource_Construction_Task;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static org.omg.spec.api4kp._1_0.AbstractCarrier.ofAst;
import static org.omg.spec.api4kp._1_0.AbstractCarrier.rep;

import edu.mayo.kmdp.id.helper.DatatypeHelper;
import edu.mayo.kmdp.knowledgebase.v3.server.KnowledgeBaseApiInternal;
import edu.mayo.kmdp.metadata.surrogate.Dependency;
import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.repository.asset.v3.server.KnowledgeAssetCatalogApiInternal;
import edu.mayo.kmdp.repository.asset.v3.server.KnowledgeAssetRetrievalApiInternal;
import edu.mayo.kmdp.util.JenaUtil;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.Util;
import edu.mayo.ontology.taxonomies.kao.knowledgeassetrole.KnowledgeAssetRoleSeries;
import edu.mayo.ontology.taxonomies.kao.rel.dependencyreltype.DependencyTypeSeries;
import edu.mayo.ontology.taxonomies.kao.rel.structuralreltype.StructuralPartTypeSeries;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.omg.spec.api4kp._1_0.Answer;
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;
import org.omg.spec.api4kp._1_0.services.KPOperation;
import org.omg.spec.api4kp._1_0.services.KPSupport;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;

@KPOperation(Knowledge_Resource_Construction_Task)
@KPSupport(FHIR_STU3)
@Named
public class DependencyBasedConstructor implements KnowledgeBaseApiInternal._getKnowledgeBaseStructure {

  @Inject
  KnowledgeAssetCatalogApiInternal repo;

  public static KnowledgeBaseApiInternal._getKnowledgeBaseStructure newInstance(
      KnowledgeAssetCatalogApiInternal repo) {
    DependencyBasedConstructor constructor = new DependencyBasedConstructor();
    constructor.repo = repo;
    return constructor;
  }

  @Override
  public Answer<KnowledgeCarrier> getKnowledgeBaseStructure(UUID seedAssetId, String seedAssetVersionTag) {

    UUID compositeAssetId = Util.uuid(seedAssetId.toString());
    String compositeAssetVersion = seedAssetVersionTag;
    URIIdentifier compositeAssetVersionedURI = uri(
        compositeAssetId.toString(), compositeAssetVersion);

    // TODO Rather than getting ALL the assets,
    // there should be a query based on the assetId, or 'bundle' should be used
    // but getAssetBundle, unlike getArtifactBundle, is not implemented yet
    return repo.listKnowledgeAssets()
        .flatMap(ptrList ->
            ptrList.stream()
                .map(DatatypeHelper::deRef)
                .map(axId -> repo.getKnowledgeAsset(UUID.fromString(axId.getTag())))
                .collect(Answer.toList())
        )
        // Now we create a struct based on the analysis of the relationships between the assets
        .map(
            list -> {
              Model m = ModelFactory.createDefaultModel();

              m.add(JenaUtil.objA(
                  compositeAssetVersionedURI.getUri().toString(),
                  RDF.type,
                  KnowledgeAssetRoleSeries.Composite_Knowledge_Asset.getConceptId().toString()
              ));

              list.forEach(ax -> structure(compositeAssetVersionedURI, ax, m));

              return m;
            })
        .map(m -> {
          System.out.println(JenaUtil.asString(m));
          return m;
        })
        // And we return it
        // TODO This is really RDF, but RDF is not yet registered !! :
        .map(m -> ofAst(m, rep(OWL_2))
            // Need to Generate a new ID for the composite asset just constructed.
            .withAssetId(compositeAssetVersionedURI)
        );
  }

  private void structure(URIIdentifier rootAssetId, KnowledgeAsset componentAsset, Model m) {
    // TODO Use the versionUris when proper set in the XML
    m.add(JenaUtil.objA(
        getVersionURI(rootAssetId),
        StructuralPartTypeSeries.Has_Part.getConceptId().toString(),
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
                  componentAsset.getAssetId().getUri().toString(),
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

    componentAsset.getRelated().stream()
        .flatMap(StreamUtil.filterAs(Dependency.class))
        .filter(dep -> DependencyTypeSeries.Depends_On.sameAs(dep.getRel()))
        .forEach(dep -> {
              m.add(JenaUtil.objA(
                  getVersionURI(componentAsset.getAssetId()),
                  DependencyTypeSeries.Depends_On.getConceptId().toString(),
                  getVersionURI(((KnowledgeAsset) dep.getTgt()).getAssetId())
              ));
            }
        );
  }

  private String getVersionURI(URIIdentifier assetId) {
    return assetId.getVersionId() != null
        ? assetId.getVersionId().toString()
        : assetId.getUri().toString() + DatatypeHelper
            .getVersionSeparator(assetId.getUri().toString()) + "SNAPSHOT";
  }

}
