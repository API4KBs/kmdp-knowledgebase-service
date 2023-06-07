package edu.mayo.kmdp.ops.tranx.mvf.owl2.cso;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultArtifactId;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Formal_Ontology;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Lexicon;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.MVF_1_0;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Encoded_Knowledge_Expression;

import edu.mayo.kmdp.knowledgebase.introspectors.owl2.OWLMetadataIntrospector;
import edu.mayo.kmdp.language.parsers.owl2.OWLParser;
import edu.mayo.kmdp.ops.EphemeralAssetFabricator;
import edu.mayo.kmdp.ops.tranx.mvf.owl2.cso.components.VernacularConceptGenerator;
import edu.mayo.kmdp.registry.Registry;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.joda.time.LocalDate;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.AbstractCompositeCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedIntrospectDirect;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedTransformDirect;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetCatalogApi;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetRepositoryApi;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal._applyLower;
import org.omg.spec.api4kp._20200801.id.IdentifierConstants;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;

public class CSOFabricator implements EphemeralAssetFabricator {

  public static final UUID OPERATOR_ID = UUID.fromString("08b4a37b-63c8-4a53-9fcc-34a68dc268db");

  public static final URI CSO_NS = URI.create(
      "http://ontology.mayo.edu/ontologies/clinicalsituationontology/");
  public static final URI CSO_NS_SNAPSHOT = URI.create(
      "http://ontology.mayo.edu/ontologies/SNAPSHOT/clinicalsituationontology/");
  public static final URI CSV_NS = URI.create(
      "https://ontology.mayo.edu/taxonomies/clinicalsituations");

  public static final URI CSV_PATTERN = URI.create(
      "https://ontology.mayo.edu/taxonomies/clinicalsituationpatterns#");

  public static final UUID CSO_ID = UUID.fromString("c4aa4f82-42cf-317d-98f1-cce4ca234de2");
  public static final UUID CSOVer_ID = UUIDEncrypter.encrypt(CSO_ID, OPERATOR_ID);

  private final KnowledgeAssetCatalogApi cat;
  private final KnowledgeAssetRepositoryApi repo;

  private final _applyNamedIntrospectDirect introspector;
  private final _applyNamedTransformDirect transformer;

  private final _applyLower serializer;

  private final String csoUrl;

  public CSOFabricator(
      @Nonnull final KnowledgeAssetCatalogApi cat,
      @Nonnull final KnowledgeAssetRepositoryApi repo,
      @Nonnull final String csoURL) {
    this.cat = cat;
    this.repo = repo;

    this.introspector = new OWLMetadataIntrospector();
    this.transformer = new VernacularConceptGenerator();
    this.serializer = new OWLParser();

    this.csoUrl = csoURL;
  }

  @Override
  public Answer<KnowledgeCarrier> applyNamedTransform(
      UUID operatorId,
      UUID kbaseId, String versionTag,
      String xParams) {
    return fabricate(kbaseId, versionTag);
  }

  @Override
  public UUID getId() {
    return OPERATOR_ID;
  }

  @Override
  public Answer<KnowledgeCarrier> fabricate(
      @Nonnull UUID ephemeralAssetId,
      @Nonnull String ephemeralVersionTag) {
    return innerFabricate()
        .flatMap(cso -> serializer.applyLower(
            cso,
            Encoded_Knowledge_Expression.getTag(),
            codedRep(OWL_2, XML_1_1, Charset.defaultCharset(), Encodings.DEFAULT),
            null));
  }

  @Override
  public Answer<KnowledgeCarrier> fabricateSurrogate(
      @Nonnull UUID ephemeralAssetId,
      @Nonnull String ephemeralVersionTag) {
    return innerFabricate()
        .flatMap(kc -> introspector.applyNamedIntrospectDirect(
            OWLMetadataIntrospector.OP_ID,
            kc,
            null));
  }

  protected Answer<KnowledgeCarrier> innerFabricate() {
    return cat.listKnowledgeAssets(Lexicon.getTag(), null, null, 0, -1)
        .flatList(Pointer.class,
            ptr -> repo.getKnowledgeAssetCanonicalCarrier(ptr.getUuid()))
        .map(this::compose)
        .flatWhole(this::toOntology);
  }


  private CompositeKnowledgeCarrier compose(List<KnowledgeCarrier> knowledgeCarriers) {
    var assetId = getCSOAssetId();
    return AbstractCompositeCarrier.ofUniformAggregate(knowledgeCarriers)
        .withAssetId(assetId)
        .withArtifactId(defaultArtifactId(assetId, MVF_1_0))
        .withRepresentation(rep(MVF_1_0, XML_1_1, Charset.defaultCharset(), Encodings.DEFAULT));
  }


  private Answer<KnowledgeCarrier> toOntology(CompositeKnowledgeCarrier ckc) {
    return transformer.applyNamedTransformDirect(
        VernacularConceptGenerator.OPERATOR_ID,
        ckc,
        csoUrl
    );
  }


  @Override
  public Answer<String> getFabricatableVersion(
      @Nonnull UUID ephemeralAssetId) {
    return Answer.of(IdentifierConstants.VERSION_LATEST);
  }


  @Override
  public Optional<Pointer> pledge(
      @Nonnull Pointer source) {
    if (!canFabricate(source)) {
      return Optional.empty();
    }
    var ptr = getCSOAssetId()
        .toPointer()
        .withType(Formal_Ontology.getReferentId())
        .withMimeType(codedRep(OWL_2, XML_1_1, Charset.defaultCharset(), Encodings.DEFAULT))
        .withName("Clinical Situation Vernacular (Ephemeral Fragment)");
    if (source.getHref() != null) {
      var url = source.getHref().toString()
          .replace(source.getTag(), ptr.getTag())
          .replace(source.getVersionTag(), ptr.getVersionTag());
      ptr.withHref(URI.create(url));
    }
    return Optional.of(ptr);
  }

  private ResourceIdentifier getCSOAssetId() {
    var now = new LocalDate();
    return SemanticIdentifier.newId(
        Registry.MAYO_ASSETS_BASE_URI_URI,
        CSOVer_ID,
        String.format("%4d.%d.1", now.getYear(), now.getMonthOfYear()));
  }

  @Override
  public boolean canFabricate(
      @Nonnull Pointer source) {
    return Lexicon.getReferentId()
        .equals(source.getType());
  }

  @Override
  public boolean isFabricator(
      @Nonnull UUID ephemeralAssetId) {
    return CSOVer_ID.equals(ephemeralAssetId);
  }
}
