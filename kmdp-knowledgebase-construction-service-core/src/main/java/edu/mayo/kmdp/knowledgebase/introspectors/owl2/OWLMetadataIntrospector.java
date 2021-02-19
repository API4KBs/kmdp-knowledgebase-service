package edu.mayo.kmdp.knowledgebase.introspectors.owl2;

import static edu.mayo.kmdp.knowledgebase.introspectors.owl2.OWLMetadataIntrospector.OWLIntrospectorConfiguration.OWLIntrospectorParams.IGNORES;
import static edu.mayo.kmdp.knowledgebase.introspectors.owl2.OWLMetadataIntrospector.OWLIntrospectorConfiguration.OWLIntrospectorParams.NS_INDEX;
import static edu.mayo.kmdp.knowledgebase.introspectors.owl2.OWLMetadataIntrospector.OWLIntrospectorConfiguration.OWLIntrospectorParams.TAG_INDEX;
import static edu.mayo.kmdp.knowledgebase.introspectors.owl2.OWLMetadataIntrospector.OWLIntrospectorConfiguration.OWLIntrospectorParams.VER_INDEX;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newVersionId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultArtifactId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.newSurrogate;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper.carry;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Imports;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Terminology_Ontology_And_Assertional_KBs;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Formal_Ontology;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Description_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Published;

import edu.mayo.kmdp.ConfigProperties;
import edu.mayo.kmdp.Opt;
import edu.mayo.kmdp.Option;
import edu.mayo.kmdp.knowledgebase.AbstractKnowledgeBaseOperator;
import edu.mayo.kmdp.knowledgebase.introspectors.owl2.OWLMetadataIntrospector.OWLIntrospectorConfiguration.OWLIntrospectorParams;
import edu.mayo.kmdp.language.parsers.owl2.OWLParser;
import edu.mayo.kmdp.language.parsers.owl2.OWLParser.OWLParserConfiguration;
import edu.mayo.kmdp.language.parsers.owl2.OWLParser.OWLParserConfiguration.OWLParserParams;
import edu.mayo.kmdp.util.Util;
import java.net.URI;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Named;
import org.apache.jena.vocabulary.RDFS;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedIntrospect;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedIntrospectDirect;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal._applyLift;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
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
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

@Named
@KPOperation(Description_Task)
@KPSupport(OWL_2)
@KPComponent
public class OWLMetadataIntrospector
    extends AbstractKnowledgeBaseOperator
    implements _applyNamedIntrospect, _applyNamedIntrospectDirect {

  private static final Logger logger = LoggerFactory.getLogger(OWLMetadataIntrospector.class);

  public static final UUID id
      = UUID.fromString("3c0683de-cb0f-45cf-ab6a-7964559ee91a");
  public static final String version = "1.0.0";

  @Autowired
  @KPSupport(OWL_2)
  @KPComponent(implementation = "owlapi")
  private _applyLift parser;

  OWLParserConfiguration opc = new OWLParserConfiguration()
      .with(OWLParserParams.IGNORE_IMPORTS, "true");

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
        .flatMap(kc -> parser.applyLift(kc, Abstract_Knowledge_Expression, null, opc.encode()))
        .map(AbstractCarrier::mainComponent)
        .flatMap(x -> doIntrospect(x, new OWLIntrospectorConfiguration(xParams)));
  }

  @Override
  public Answer<KnowledgeCarrier> applyNamedIntrospectDirect(UUID operatorId,
      KnowledgeCarrier artifact, String xParams) {
    return doIntrospect(artifact, new OWLIntrospectorConfiguration(xParams));
  }

  private Answer<KnowledgeCarrier> doIntrospect(KnowledgeCarrier source,
      OWLIntrospectorConfiguration cfg) {
    if (Abstract_Knowledge_Expression.sameAs(source.getLevel())) {
      return innerIntrospect(source, source.getRepresentation(), cfg);
    } else if (parser == null) {
      return Answer.unsupported();
    } else {
      return parser
          .applyLift(source, Abstract_Knowledge_Expression, null, opc.encode())
          .flatMap(parsed -> this.innerIntrospect(parsed, source.getRepresentation(), cfg));
    }
  }

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
    OntoIdentifiers o = new OntoIdentifiers(ontoId, cfg);

    KnowledgeArtifact artf = new KnowledgeArtifact()
        .withArtifactId(defaultArtifactId(o.assetId, OWL_2))
        .withLocator(URI.create(o.baseURI))
        .withRepresentation(rep);

    KnowledgeAsset surrogate = newSurrogate(o.assetId).get()
        .withSecondaryId(o.ontologyId)
        .withFormalCategory(Terminology_Ontology_And_Assertional_KBs)
        .withFormalType(Formal_Ontology)
        .withLifecycle(new Publication().withPublicationStatus(Published))
        .withCarriers(artf);

    owl.importsDeclarations()
        .map(OWLImportsDeclaration::getIRI)
        .filter(iri -> ! ignores.contains(iri))
        .map(iri -> new OntoIdentifiers(iri, cfg))
        .forEach(oid -> {
          surrogate.withLinks(new Dependency().withRel(Imports).withHref(oid.assetId));
          surrogate.getCarriers().get(0)
              .withLinks(new Dependency().withRel(Imports).withHref(oid.ontologyId));
        });

    owl.annotations()
        .filter(p -> p.getProperty().getIRI().equals(IRI.create(RDFS.label.getURI())))
        .map(OWLAnnotation::getValue)
        .flatMap(v -> v.asLiteral().stream())
        .map(OWLLiteral::getLiteral)
        .findFirst()
        .ifPresent(name -> {
          surrogate.setName(name);
          artf.setName(name);
        });

    return Answer.of(carry(surrogate));
  }


  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return OWL_2;
  }

  private static class OntoIdentifiers {

    String baseURI;
    ResourceIdentifier assetId;
    ResourceIdentifier ontologyId;

    public OntoIdentifiers(OWLOntologyID ontoId, OWLIntrospectorConfiguration cfg) {
      this(
          ontoId.getVersionIRI()
              .or(ontoId::getOntologyIRI)
              .map(IRI::toString).orElseThrow(),
          cfg);
      logger.trace("DECLAR {} :: {} ", ontologyId.getResourceId(), ontologyId.getVersionId());
    }

    public OntoIdentifiers(IRI ontoId, OWLIntrospectorConfiguration cfg) {
      this(ontoId.toString(), cfg);
      logger.trace("IMPORT {} :: {} ", ontologyId.getResourceId(), ontologyId.getVersionId());
    }

    protected OntoIdentifiers(String targetURI, OWLIntrospectorConfiguration cfg) {
      Pattern versionPattern = Pattern.compile(cfg.getTyped(OWLIntrospectorParams.VERSION_PATTERN));
      String versionTag;
      int nsIdx = cfg.getTyped(NS_INDEX, Integer.class);
      int verIdx = cfg.getTyped(VER_INDEX, Integer.class);
      int tagIdx = cfg.getTyped(TAG_INDEX, Integer.class);

      baseURI = targetURI;
      Matcher m = versionPattern.matcher(targetURI);
      if (! m.matches()) {
        throw new UnsupportedOperationException("Unable to match URI " + targetURI);
      }
      if (Util.isNotEmpty(m.group(verIdx))) {
        URI versionURI = URI.create(targetURI);
        versionTag = m.group(verIdx);
        URI seriesURI = URI.create(m.group(nsIdx) + m.group(tagIdx) + trail(targetURI));
        ontologyId = newVersionId(seriesURI, versionURI);
      } else {
        URI seriesURI = URI.create(targetURI);
        versionTag = cfg.getTyped(OWLIntrospectorParams.DEFAULT_VERSION);
        URI versionURI = URI.create(
            m.group(nsIdx) + versionTag + "/" + m.group(tagIdx) + trail(targetURI));
        ontologyId = newVersionId(seriesURI, versionURI);
      }
      assetId = newId(Util.uuid(ontologyId.getResourceId().toString()), versionTag);
    }

    private String trail(String targetURI) {
      if (targetURI.endsWith("/")) {
        return "/";
      } else if (targetURI.endsWith("#")) {
        return "#";
      } else {
        return "";
      }
    }
  }

  public static class OWLIntrospectorConfiguration
      extends
      ConfigProperties<OWLMetadataIntrospector.OWLIntrospectorConfiguration, OWLIntrospectorParams> {

    private static final Properties DEFAULTS =
        defaulted(OWLMetadataIntrospector.OWLIntrospectorConfiguration.OWLIntrospectorParams.class);

    public OWLIntrospectorConfiguration() {
      super(DEFAULTS);
    }

    public OWLIntrospectorConfiguration(Properties defaults) {
      super(defaults);
    }

    public OWLIntrospectorConfiguration(String cfg) {
      super(cfg);
    }

    @Override
    public OWLMetadataIntrospector.OWLIntrospectorConfiguration.OWLIntrospectorParams[] properties() {
      return OWLMetadataIntrospector.OWLIntrospectorConfiguration.OWLIntrospectorParams.values();
    }

    public enum OWLIntrospectorParams implements
        Option<OWLIntrospectorConfiguration.OWLIntrospectorParams> {

      DEFAULT_VERSION(Opt.of(
          "defaultVersionTag",
          null,
          "",
          String.class,
          false)),
      VERSION_PATTERN(Opt.of(
          "versionPattern",
          null,
          "A 2/3-part RegEx that determines the boundaries and order of a "
              + "(i) namespace, (ii) tag and (iii) version components of the URI, "
              + "where the version is optional",
          String.class,
          false)),
      NS_INDEX(Opt.of(
          "namespacePatternIndex",
          "1",
          "",
          Integer.class,
          false)),
      VER_INDEX(Opt.of(
          "versionPatternIndex",
          "2",
          "",
          Integer.class,
          false)),
      TAG_INDEX(Opt.of(
          "tagPatternIndex",
          "3",
          "",
          Integer.class,
          false)),
      IGNORES(Opt.of(
          "importIgnores",
          "",
          "",
          String.class,
          false)
      );

      private Opt<OWLMetadataIntrospector.OWLIntrospectorConfiguration.OWLIntrospectorParams> opt;

      OWLIntrospectorParams(
          Opt<OWLMetadataIntrospector.OWLIntrospectorConfiguration.OWLIntrospectorParams> opt) {
        this.opt = opt;
      }

      @Override
      public Opt<OWLMetadataIntrospector.OWLIntrospectorConfiguration.OWLIntrospectorParams> getOption() {
        return opt;
      }
    }
  }
}