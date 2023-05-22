package edu.mayo.kmdp.knowledgebase.introspectors.owl2;

import static edu.mayo.kmdp.knowledgebase.introspectors.owl2.internal.OWLIntrospectorConfiguration.OWLIntrospectorParams.IGNORES;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.newSurrogate;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper.carry;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Imports;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Terminology_Ontology_And_Assertional_KBs;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Formal_Ontology;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Description_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Draft;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Published;

import edu.mayo.kmdp.knowledgebase.AbstractKnowledgeBaseOperator;
import edu.mayo.kmdp.knowledgebase.introspectors.owl2.internal.OWLIntrospectorConfiguration;
import edu.mayo.kmdp.knowledgebase.introspectors.owl2.internal.OntoArtfactIdentifiers;
import edu.mayo.kmdp.knowledgebase.introspectors.owl2.internal.OntoIdentifiers;
import edu.mayo.kmdp.language.detectors.owl2.OWLDetector;
import edu.mayo.kmdp.language.parsers.owl2.OWLParser;
import edu.mayo.kmdp.language.parsers.owl2.OWLParser.OWLParserConfiguration;
import edu.mayo.kmdp.language.parsers.owl2.OWLParser.OWLParserConfiguration.OWLParserParams;
import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Named;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedIntrospect;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedIntrospectDirect;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal._applyLift;
import org.omg.spec.api4kp._20200801.id.IdentifierConstants;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPComponent;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.surrogate.Dependency;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.Publication;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatus;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Metadata Introspector for Ontologies with an OWL/RDF based representation
 * <p>
 * Preserves the {@link OWLOntologyID} (series + version) as a {@link KnowledgeAsset} secondaryId,
 * while parsing the IRIs to extract an Asset ID's ResourceIdentifier namespace, tag and version.
 * Assumes a common pattern e.g. 'http://base.host/namespace/version/localName', where version is
 * typically a date in the format yyyyMMMMdd, or 'SNAPSHOT'.
 * <p>
 * Uses the {@link OWLImportsDeclaration} to establish dependencies to other Ontology assets
 * <p>
 * Supports annotations on the {@link OWLOntology} to drive the Artifact's version (via
 * owl:versionInfo), creation and last modification date (via dct:created and dct:modified
 * respectively).
 * <p>
 * Uses the ontology IRI date pattern to derive the Asset version's creation date, and falls back to
 * the Artifact version if unable to. This allows the same Ontology Asset to have more than one
 * representation, where the representations can evolve independently. Moreover, it is common
 * practice to 'future date' ontologies based on their release to the public (a creation from the
 * consumer's perspective), while tracking the actual creation of the OWL documents.
 * <p>
 * Infers the publication status from the version (assumes published, unless 'SNAPSHOT').
 * <p>
 * Assumes that the {@link SyntacticRepresentation} metadata is provided by the client, otherwise
 * uses and {@link edu.mayo.kmdp.language.detectors.owl2.OWLDetector} to infer the necessary
 * information.
 */
@Named
@KPOperation(Description_Task)
@KPSupport(OWL_2)
@KPComponent
public class OWLMetadataIntrospector
    extends AbstractKnowledgeBaseOperator
    implements _applyNamedIntrospect, _applyNamedIntrospectDirect {

  /**
   * The unique ID of this class as an API4KP operator
   */
  public static final UUID OP_ID
      = UUID.fromString("3c0683de-cb0f-45cf-ab6a-7964559ee91a");
  /**
   * The version of this class as an API4KP operator
   */
  public static final String OP_VERSION = "2.0.0";

  /**
   * The parser used to deserialize the OWL ontologu to be introspected, so that it can be queried
   * for content and annotations
   */
  @Autowired
  @KPSupport(OWL_2)
  @KPComponent(implementation = "owlapi")
  private _applyLift parser;

  /**
   * The default configuration used by the {@link #parser}
   */
  private OWLParserConfiguration opc = new OWLParserConfiguration()
      .with(OWLParserParams.IGNORE_IMPORTS, "true");

  /**
   * Default constructor for direct introspection
   */
  public OWLMetadataIntrospector() {
    super(SemanticIdentifier.newId(OP_ID, OP_VERSION));
    this.parser = new OWLParser();
  }

  /**
   * Constructor for introspection of ontologies managed via a KnowledgeBase
   *
   * @param kbManager the API used to access the ontology to be introspected
   */
  public OWLMetadataIntrospector(KnowledgeBaseApiInternal kbManager) {
    this(kbManager, new OWLParser());
  }

  /**
   * Constructor for introspection of ontologies managed via a KnowledgeBase
   *
   * @param kbManager the API used to access the ontology to be introspected
   * @param parser    a client-provided Ontology document parser
   */
  public OWLMetadataIntrospector(KnowledgeBaseApiInternal kbManager, _applyLift parser) {
    this();
    this.kbManager = kbManager;
    this.parser = parser;
  }

  /**
   * Supported Language
   *
   * @return OWL_2
   */
  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return OWL_2;
  }

  /**
   * Introspects a KnowledgeBase, where the main component of that KnowledgeBase is expected to be
   * an OWL ontology.
   * <p>
   * Retrieves the ontology from the KB, then introspects it directly
   *
   * @param operatorId the operator to be used for introspection (should be OP_ID)
   * @param kbaseId    the KnowledgeBase Id
   * @param versionTag the KnowledgeBase version
   * @param xParams    an optional serialized {@link OWLIntrospectorConfiguration}
   * @return the {@link KnowledgeAsset} for the main component of the KB, carried and wrapped
   */
  @Override
  public Answer<KnowledgeCarrier> applyNamedIntrospect(UUID operatorId, UUID kbaseId,
      String versionTag, String xParams) {

    return kbManager.getKnowledgeBaseManifestation(kbaseId, versionTag)
        .flatMap(kc -> parser.applyLift(kc, Abstract_Knowledge_Expression.getTag(), null, opc.encode()))
        .map(AbstractCarrier::mainComponent)
        .flatMap(x -> doIntrospect(x, new OWLIntrospectorConfiguration(xParams)));
  }

  /**
   * Introspects an OWL ontology, carried
   *
   * @param operatorId the operator to be used for introspection (should be OP_ID)
   * @param artifact   the OWL Ontology document, carried
   * @param xParams    an optional serialized {@link OWLIntrospectorConfiguration}
   * @return the {@link KnowledgeAsset} for the main component of the KB, carried and wrapped
   */
  @Override
  public Answer<KnowledgeCarrier> applyNamedIntrospectDirect(UUID operatorId,
      KnowledgeCarrier artifact, String xParams) {
    return doIntrospect(artifact, new OWLIntrospectorConfiguration(xParams));
  }

  /**
   * Introspects an OWL ontology, carried, lifting it if necessary
   *
   * @param source the OWL Ontology document, carried
   * @param cfg    an optional serialized {@link OWLIntrospectorConfiguration}
   * @return the {@link KnowledgeAsset} for the main component of the KB, carried and wrapped
   */
  private Answer<KnowledgeCarrier> doIntrospect(KnowledgeCarrier source,
      OWLIntrospectorConfiguration cfg) {
    if (source.getRepresentation() == null) {
      new OWLDetector().applyDetect(source, null)
          .map(inferredKC -> source.withRepresentation(inferredKC.getRepresentation()));
    }
    if (Abstract_Knowledge_Expression.sameAs(source.getLevel())) {
      return innerIntrospect(source, source.getRepresentation(), cfg);
    } else if (parser == null) {
      return Answer.unsupported();
    } else {
      return parser
          .applyLift(source, Abstract_Knowledge_Expression.getTag(), null, opc.encode())
          .flatMap(parsed -> this.innerIntrospect(parsed, source.getRepresentation(), cfg));
    }
  }

  /**
   * Introspects the OWL ontology
   *
   * @param carrier the OWL Ontology document, carried
   * @param rep     the Ontology representation metadata
   * @param cfg     an optional serialized {@link OWLIntrospectorConfiguration}
   * @return the {@link KnowledgeAsset} for the ontology, carried and wrapped
   */
  private Answer<KnowledgeCarrier> innerIntrospect(
      KnowledgeCarrier carrier,
      SyntacticRepresentation rep,
      OWLIntrospectorConfiguration cfg) {
    OWLOntology owl = carrier.mainComponentAs(OWLOntology.class);
    OWLOntologyID ontoId = owl.getOntologyID();

    String ignoreString = cfg.getTyped(IGNORES);
    Set<IRI> ignores = Arrays.stream(ignoreString.split(","))
        .map(String::trim)
        .map(IRI::create)
        .collect(Collectors.toSet());

    if (ontoId.getOntologyIRI().isEmpty()) {
      return Answer.failed(new UnsupportedOperationException("Missing ontology ID"));
    }
    if (ontoId.getVersionIRI().isEmpty()) {
      return Answer.failed(new UnsupportedOperationException("Missing ontology Version ID"));
    }

    var ontoIdentifiers = new OntoIdentifiers(ontoId, cfg);
    var artifactIdentifiers = new OntoArtfactIdentifiers(ontoIdentifiers, owl);
    var assetId = ontoIdentifiers.getAssetId()
        .withName(artifactIdentifiers.getOntologyName());

    KnowledgeArtifact artf = new KnowledgeArtifact()
        .withArtifactId(artifactIdentifiers.getOwlArtifactId())
        .withLocator(URI.create(ontoIdentifiers.getBaseURI()))
        .withLifecycle(inferArtifactPublication(artifactIdentifiers))
        .withRepresentation(rep);

    KnowledgeAsset surrogate = newSurrogate(assetId).get()
        .withSecondaryId(ontoIdentifiers.getOntologyId())
        .withFormalCategory(Terminology_Ontology_And_Assertional_KBs)
        .withFormalType(Formal_Ontology)
        .withLifecycle(inferAssetPublication(ontoIdentifiers, artifactIdentifiers))
        .withCarriers(artf);

    owl.importsDeclarations()
        .map(OWLImportsDeclaration::getIRI)
        .filter(iri -> !ignores.contains(iri))
        .map(iri -> new OntoIdentifiers(iri, cfg))
        .forEach(oid -> {
          surrogate.withLinks(new Dependency().withRel(Imports).withHref(oid.getAssetId()));
          surrogate.getCarriers().get(0)
              .withLinks(new Dependency().withRel(Imports).withHref(oid.getOntologyId()));
        });

    Optional.ofNullable(artifactIdentifiers.getOntologyName())
        .ifPresent(name -> {
          surrogate.setName(name);
          artf.setName(name);
        });

    return Answer.of(carry(surrogate));
  }

  /**
   * Determines the Ontology Asset publication status and date.
   * <p>
   * Determines the status from the Sem/CalVer pattern of the asset version tag.
   * <p>
   * Tries to use the version tag (e.g. 20210101) as a creation date, otherwise tries to use the OWL
   * document creation date, otherwise falls back to 'now()'.
   *
   * @param assetInfo    ontology asset id/version metadata
   * @param artifactInfo ontology document id/version metadata
   * @return the asset {@link Publication} metadata
   */
  private Publication inferAssetPublication(
      OntoIdentifiers assetInfo, OntoArtfactIdentifiers artifactInfo) {
    var status = inferPublicationStatus(assetInfo.getVersionTag());
    var creationDate = Optional.ofNullable(assetInfo.getOntologyCreationDate())
        .or(() -> Optional.ofNullable(artifactInfo.getOwlCreationDate()))
        .or(() -> Optional.ofNullable(artifactInfo.getOwlLastModifiedDate()))
        .orElse(new Date());
    return new Publication()
        .withPublicationStatus(status)
        .withCreatedOn(creationDate);
  }

  /**
   * Determines the Ontology OWL Document (artifact) publication status and date.
   * <p>
   * Determines the status from the Sem/CalVer pattern of the artifact version tag.
   * <p>
   * Uses the (optional) information derived from the annotations in the OWL ontology to determine
   * creation and last modified dates
   *
   * @param artifactInfo ontology document id/version metadata
   * @return the asset {@link Publication} metadata
   * @see OntoArtfactIdentifiers
   */
  private Publication inferArtifactPublication(
      OntoArtfactIdentifiers artifactInfo) {
    var status = inferPublicationStatus(artifactInfo.getOwlVersionTag());
    return new Publication()
        .withPublicationStatus(status)
        .withCreatedOn(artifactInfo.getOwlCreationDate())
        .withLastReviewedOn(artifactInfo.getOwlLastModifiedDate());
  }

  /**
   * Maps a Sem/CalVer version tag to a publication status.
   * <p>
   * Assumes Draft if 'SNAPSHOT', Published otherwise.
   * <p>
   * Note that pre-published is not supported, as it is not a common practice to have 'release
   * candidate' ontologies
   *
   * @param versionTag the ontology Resource version tag
   * @return a {@link PublicationStatus} consistent with the tag
   */
  private PublicationStatus inferPublicationStatus(String versionTag) {
    return versionTag.endsWith(IdentifierConstants.SNAPSHOT)
        ? Draft
        : Published;
  }

}