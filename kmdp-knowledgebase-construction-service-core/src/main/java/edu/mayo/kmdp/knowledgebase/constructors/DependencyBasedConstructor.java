package edu.mayo.kmdp.knowledgebase.constructors;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.ofAst;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Depends_On;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Imports;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Knowledge_Resource_Construction_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;

import edu.mayo.kmdp.knowledgebase.AbstractKnowledgeBaseOperator;
import edu.mayo.kmdp.registry.Registry;
import edu.mayo.kmdp.util.JenaUtil;
import edu.mayo.kmdp.util.StreamUtil;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.inject.Named;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.RDF;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal._getKnowledgeBaseStructure;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetCatalogApiInternal;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPComponent;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.Dependency;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetrole.KnowledgeAssetRoleSeries;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.omg.spec.api4kp._20200801.taxonomy.structuralreltype.StructuralPartTypeSeries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@KPOperation(Knowledge_Resource_Construction_Task)
@KPSupport(FHIR_STU3)
@KPComponent
@Named
public class DependencyBasedConstructor
  extends AbstractKnowledgeBaseOperator
    implements _getKnowledgeBaseStructure {

  private static final Logger logger = LoggerFactory.getLogger(DependencyBasedConstructor.class);

  public static final UUID id = UUID.fromString("13881270-6556-4cb1-99b9-5a3dacff96f6");
  public static final String version = "1.0.0";

  @Autowired(required = false)
  KnowledgeAssetCatalogApiInternal repo;

  ResourceIdentifier compositeId;

  public DependencyBasedConstructor() {
    super(SemanticIdentifier.newId(id,version));
  }

  public static KnowledgeBaseApiInternal._getKnowledgeBaseStructure newInstance(
      KnowledgeAssetCatalogApiInternal repo, ResourceIdentifier compositeId) {
    DependencyBasedConstructor constructor = new DependencyBasedConstructor();
    constructor.repo = repo;
    constructor.compositeId = compositeId;
    return constructor;
  }

  @Override
  public Answer<KnowledgeCarrier> getKnowledgeBaseStructure(UUID rootId,
      String rootVersionTag, String params) {

    Answer<List<Pointer>> allAssets = repo.listKnowledgeAssets();
    if (! allAssets.isSuccess()) {
      return Answer.failed(allAssets);
    }

    ResourceIdentifier compositeAssetVersionedId;
    if (compositeId != null) {
      compositeAssetVersionedId = compositeId;
    } else {
      compositeAssetVersionedId = allAssets.get().stream()
          .map(ResourceIdentifier.class::cast)
          .reduce(SemanticIdentifier::hashIdentifiers)
          .orElseThrow();
    }


    // TODO Rather than getting ALL the assets,
    // there should be a query based on the assetId,
    return allAssets
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
                  KnowledgeAssetRoleSeries.Composite_Knowledge_Asset.getReferentId().toString()
              ));

              list.forEach(ax -> structure(compositeAssetVersionedId, ax, m));

              return m;
            })
        .map(m -> {
          String s = beautifyTuples(JenaUtil.asString(m));
          if (logger.isInfoEnabled()) {
            logger.info(s);
          }
          return m;
        })
        // And we return it
        // TODO This is really RDF, but RDF is not yet registered !! :
        .map(m -> ofAst(m, rep(OWL_2))
            // Need to Generate a new ID for the composite asset just constructed.
            .withAssetId(compositeAssetVersionedId)
        );
  }

  private void structure(ResourceIdentifier compositeAssetId, KnowledgeAsset componentAsset, Model m) {
    // TODO Use the versionUris when proper set in the XML
    m.add(JenaUtil.objA(
        getVersionURI(compositeAssetId),
        StructuralPartTypeSeries.Has_Structural_Component.getReferentId().toString(),
        getVersionURI(componentAsset.getAssetId())
    ));
    m.add(JenaUtil.objA(
        getVersionURI(componentAsset.getAssetId()),
        RDF.type,
        KnowledgeAssetRoleSeries.Component_Knowledge_Asset.getReferentId().toString()
    ));

    componentAsset.getFormalType()
        .forEach(type ->
            m.add(JenaUtil.objA(
                componentAsset.getAssetId().getResourceId().toString(),
                RDF.type,
                type.getReferentId().toString())
            )
        );

    componentAsset.getLinks().stream()
        .flatMap(StreamUtil.filterAs(Dependency.class))
        .filter(dep -> Depends_On.sameAs(dep.getRel()))
        .forEach(dep -> m.add(JenaUtil.objA(
            getVersionURI(componentAsset.getAssetId()),
            Depends_On.getReferentId().toString(),
            getVersionURI(dep.getHref()))));

    componentAsset.getLinks().stream()
        .flatMap(StreamUtil.filterAs(Dependency.class))
        .filter(dep -> Imports.sameAs(dep.getRel()))
        .forEach(dep -> m.add(JenaUtil.objA(
            getVersionURI(componentAsset.getAssetId()),
            Imports.getReferentId().toString(),
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
    return null;
  }

  static Pattern uuidPattern = Pattern.compile(
      "^([A-Fa-f0-9]{8})-([A-Fa-f0-9]{4})-([A-Fa-f0-9]{4})-([A-Fa-f0-9]{4})-([A-Fa-f0-9]{12})$");

  public static String beautifyTuples(String s) {
    s = s.replace(Registry.BASE_UUID_URN, "");
    s = s.replace(Registry.MAYO_ARTIFACTS_BASE_URI, "a:");
    s = s.replace(Registry.MAYO_ASSETS_BASE_URI, "x:");
    s = s.replace("https://www.omg.org/spec/API4KP/api4kp-kao/", "x:");
    s = s.replace("https://www.omg.org/spec/API4KP/api4kp-rel/", "x:");
    s = s.replace("https://www.omg.org/spec/API4KP/api4kp/", "x:");
    s = s.replaceAll("/versions/\\d+\\.\\d+\\.\\d+", "");
    s = s.replaceAll("/versions/\\d+\\.\\d+\\.\\d+", "");
    s = uuidPattern.matcher(s).replaceAll("$1");
    return s;
  }
}
