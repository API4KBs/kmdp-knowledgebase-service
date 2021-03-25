package edu.mayo.kmdp.knowledgebase.constructors.dmn.v1_2;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Knowledge_Resource_Construction_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;

import edu.mayo.kmdp.knowledgebase.AbstractKnowledgeBaseOperator;
import edu.mayo.kmdp.knowledgebase.constructors.JenaOwlImportConstructor;
import edu.mayo.kmdp.util.JenaUtil;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.URIUtil;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Named;
import javax.xml.bind.JAXBElement;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal._getKnowledgeBaseStructure;
import org.omg.spec.api4kp._20200801.id.KeyIdentifier;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
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
import org.omg.spec.dmn._20180521.model.TDMNElementReference;
import org.omg.spec.dmn._20180521.model.TDRGElement;
import org.omg.spec.dmn._20180521.model.TDecision;
import org.omg.spec.dmn._20180521.model.TDecisionService;
import org.omg.spec.dmn._20180521.model.TDefinitions;
import org.omg.spec.dmn._20180521.model.TInputData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@KPOperation(Knowledge_Resource_Construction_Task)
@KPSupport(DMN_1_2)
@Named
public class DMN12ImportConstructor
    extends AbstractKnowledgeBaseOperator
    implements _getKnowledgeBaseStructure {

  private static final Logger logger = LoggerFactory.getLogger(JenaOwlImportConstructor.class);

  public static final UUID id = UUID.fromString("6b791758-d044-4010-bb8d-944325d1b66a");
  public static final String version = "1.0.0";

  public DMN12ImportConstructor() {
    super(newId(id,version));
  }

  public DMN12ImportConstructor(KnowledgeBaseApiInternal kbManager) {
    this();
    this.kbManager = kbManager;
  }

  @Override
  public Answer<KnowledgeCarrier> getKnowledgeBaseStructure(UUID kbId,
      String kbVersionTag, String params) {

    Answer<CompositeKnowledgeCarrier> ckc =
        kbManager.getKnowledgeBaseManifestation(kbId, kbVersionTag)
        .map(CompositeKnowledgeCarrier.class::cast);

    List<KnowledgeCarrier> components = ckc
        .filter(CompositeKnowledgeCarrier.class::isInstance)
        .map(CompositeKnowledgeCarrier.class::cast)
        .map(CompositeKnowledgeCarrier::getComponent)
        .orElse(Collections.emptyList());

    Map<KeyIdentifier,ResourceIdentifier> a2aMap
        = getArtifactToAssetMap(ckc.map(AbstractCarrier::components).orElseGet(Stream::empty));

    Model struct = ModelFactory.createDefaultModel();
    components.stream()
        .flatMap(comp -> this.getDependencies(comp, a2aMap))
        .forEach(struct::add);

    return Answer.of(AbstractCarrier.ofAst(struct,rep(OWL_2)));
  }

  private Map<KeyIdentifier, ResourceIdentifier> getArtifactToAssetMap(Stream<KnowledgeCarrier> components) {
    return components.collect(Collectors.toMap(
        kc -> kc.getArtifactId().asKey(),
        KnowledgeCarrier::getAssetId
    ));
  }

  private Stream<Statement> getDependencies(KnowledgeCarrier kc,
      Map<KeyIdentifier, ResourceIdentifier> a2aMap) {
    Optional<TDefinitions> m = kc.as(TDefinitions.class);
    if (m.isEmpty()) {
      return Stream.empty();
    }
    return m.stream().flatMap(dmn -> dmn.getDrgElement().stream())
        .map(JAXBElement::getValue)
        .flatMap(this::getExternalRequirements)
        .map(imp -> JenaUtil.objA(kc.getAssetId().getVersionId(),
            DependencyTypeSeries.Imports.getReferentId(),
            resolveArtifactDependency(imp, a2aMap).getVersionId()));
  }

  private Stream<String> getExternalRequirements(TDRGElement tdrgElement) {
    List<String> refs = new ArrayList<>();
    if (tdrgElement instanceof TDecision) {
      TDecision td = (TDecision) tdrgElement;
      td.getInformationRequirement()
          .forEach(ir -> {
            addIfExternal(ir.getRequiredDecision(), refs);
            addIfExternal(ir.getRequiredInput(), refs);
          });
      td.getKnowledgeRequirement()
          .forEach(kr -> addIfExternal(kr.getRequiredKnowledge(), refs));
    } else if (tdrgElement instanceof TInputData) {
      // nothing
    } else if (tdrgElement instanceof TDecisionService) {
      TDecisionService td = (TDecisionService) tdrgElement;
      td.getInputData().forEach(in -> addIfExternal(in,refs));
      td.getInputDecision().forEach(in -> addIfExternal(in,refs));
      td.getOutputDecision().forEach(out -> addIfExternal(out,refs));
    }
    return refs.stream();
  }

  private void addIfExternal(TDMNElementReference ref, List<String> refs) {
    if (ref != null && ! ref.getHref().startsWith("#")) {
      refs.add(URIUtil.normalizeURIString(URI.create(ref.getHref())));
    }
  }

  private ResourceIdentifier resolveArtifactDependency(String importedUri, Map<KeyIdentifier, ResourceIdentifier> a2aMap) {
    URI tgt = URI.create(importedUri);
    KeyIdentifier key = newId(tgt).asKey();
    if (! a2aMap.containsKey(key)) {
      throw new IllegalStateException("Unrecognized imported artifact " + tgt);
    }
    return a2aMap.get(key);
  }

  //TODO integrate
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
        : newId(assetId.getNamespaceUri(), assetId.getTag(), "SNAPSHOT")
            .getVersionId();
    return versionURI.toString();
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return DMN_1_2;
  }
}
