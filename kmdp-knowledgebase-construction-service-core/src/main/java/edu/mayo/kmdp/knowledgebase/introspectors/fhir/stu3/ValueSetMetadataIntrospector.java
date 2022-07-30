package edu.mayo.kmdp.knowledgebase.introspectors.fhir.stu3;

import static edu.mayo.kmdp.knowledgebase.introspectors.fhir.stu3.ValueSetMetadataIntrospector.ValueSetIdentifierIntrospector.getArtifactId;
import static edu.mayo.kmdp.knowledgebase.introspectors.fhir.stu3.ValueSetMetadataIntrospector.ValueSetIdentifierIntrospector.getAssetId;
import static edu.mayo.kmdp.knowledgebase.introspectors.fhir.stu3.ValueSetMetadataIntrospector.ValueSetIdentifierIntrospector.mapIdentifiers;
import static edu.mayo.kmdp.util.CharsetEncodingUtil.sanitizeToASCIItext;
import static org.omg.spec.api4kp._20200801.id.IdentifierConstants.VERSION_ZERO;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newVersionId;
import static org.omg.spec.api4kp._20200801.id.VersionIdentifier.toSemVer;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultSurrogateId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.newSurrogate;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Terminology_Ontology_And_Assertional_KBs;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Description_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.API4KP;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;
import static org.omg.spec.api4kp._20200801.taxonomy.structuralreltype.StructuralPartTypeSeries.Has_Proper_Part;

import edu.mayo.kmdp.registry.Registry;
import edu.mayo.kmdp.util.CharsetEncodingUtil;
import edu.mayo.kmdp.util.NameUtils;
import edu.mayo.kmdp.util.NameUtils.IdentifierType;
import edu.mayo.kmdp.util.Util;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Named;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.PrimitiveType;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KPComponent;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.surrogate.Component;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Introspector that generates {@link KnowledgeAsset} surrogates from FHIR STU3 {@link ValueSet}
 * artifacts.
 * <p>
 * Usage note: assumes that the ValueSet.url, ValueSet.version and ValueSet.date all refer to the
 * ValueSet as an asset - i.e. carry the identifier, version/revision and publication date from the
 * terminology publisher (e.g. VSAC), as opposed to the FHIR resource on a specific FHIR server.
 */
@Named
@KPOperation(Description_Task)
@KPSupport(FHIR_STU3)
@KPComponent
public class ValueSetMetadataIntrospector
    extends AbstractFhirIntrospector<ValueSet> {

  public static final Logger LOGGER = LoggerFactory.getLogger(ValueSetMetadataIntrospector.class);

  public static final String IGNORE_VERSION_FLAG = "ignoreVersion";

  /**
   * 'Asset ID' of the Introspector logic/methodology, of which this Java class is a manifestation
   */
  public static final UUID OPERATOR_ID
      = UUID.fromString("ea200c2f-27b8-4a24-99ea-5349152d4505");
  /**
   * 'Asset Version' of the Introspector logic/methodology
   */
  public static final String VERSION = "1.0.0";

  public ValueSetMetadataIntrospector() {
    super(newId(OPERATOR_ID, VERSION));
  }

  public ValueSetMetadataIntrospector(KnowledgeBaseApiInternal kbManager) {
    this();
    this.kbManager = kbManager;
  }


  /**
   * Analyzes the elements of a ValueSet to extract minimal metadata elements: identifiers
   * (delegates to {@link ValueSetIdentifierIntrospector}), formal type, formal representation,
   * name/description, publication status.
   *
   * @param valueSet               the ValueSet to extract metadata from
   * @param originalRepresentation the SyntacticRepresentation of the original artifact (before
   *                               parsing)
   * @param props                  additional configuration
   * @return a Surrogate for the original ValueSet artifact
   */
  protected KnowledgeAsset innerIntrospect(
      ValueSet valueSet,
      SyntacticRepresentation originalRepresentation,
      Properties props) {
    ResourceIdentifier assetId = tryAssetId(valueSet)
        .orElseGet(() -> getAssetId(
            valueSet.getUrl(), extractVersion(valueSet, props), valueSet.getDate()));
    ResourceIdentifier artifactId = getArtifactId(assetId);
    List<ResourceIdentifier> nativeId = mapIdentifiers(valueSet.getIdentifier());

    SyntacticRepresentation rep = (SyntacticRepresentation) originalRepresentation.clone();
    if (rep.getLanguage() == null) {
      rep.setLanguage(FHIR_STU3);
    }

    var meta = newSurrogate(assetId)
        .withPublicationStatus(mapStatus(valueSet.getStatus()))
        .get()
        .withName(valueSet.getName())
        .withDescription(CharsetEncodingUtil.sanitizeToASCIItext(valueSet.getDescription()))
        .withSecondaryId(nativeId)
        .withFormalCategory(Terminology_Ontology_And_Assertional_KBs)
        .withFormalType(KnowledgeAssetTypeSeries.Value_Set)
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(artifactId)
            .withRepresentation(rep));

    valueSet.getCompose().getInclude().stream()
        .flatMap(cs -> cs.getValueSet().stream())
        .map(PrimitiveType::getValue)
        .map(vsUri -> ValueSetIdentifierIntrospector.getAssetId(vsUri, VERSION_ZERO, null))
        .forEach(ref ->
            meta.withLinks(new Component()
                .withHref(ref)
                .withRel(Has_Proper_Part)));

    return meta;
  }

  /**
   * Looks up the Asset Id asserted on a ValueSet as an {@link Identifier, if any}
   * @param valueSet the ValueSet
   * @return the optional Asset Id
   */
  private Optional<ResourceIdentifier> tryAssetId(ValueSet valueSet) {
    return valueSet.getIdentifier().stream()
        .filter(id -> Objects.equals(API4KP.getReferentId().toString(),
            id.getType().getCodingFirstRep().getSystem())
            && Objects.equals("KnowledgeAsset",
            id.getType().getCodingFirstRep().getCode()))
        .map(id -> {
          String[] s = id.getValue().split("\\|");
          UUID u = UUID.fromString(s[0]);
          String v = s.length > 1 ? s[1] : VERSION_ZERO;
          return newId(URI.create(id.getSystem()), u, v);
        })
        .findFirst();
  }

  /**
   * Allows to ignore the ValueSet (asset) version.
   * <p>
   * ValueSet versions tend to vary, and clients tend not to refer to specific ValueSet versions. If
   * no version handling strategy can be implemented consistently, this configuration allows to use
   * default version 0.0.0 as 'no version / latest version'.
   *
   * @param valueSet the ValueSet
   * @param props    configuration
   * @return the ValueSet.version, unless ignored by configuration
   */
  private String extractVersion(ValueSet valueSet, Properties props) {
    boolean ignoreVersion = Optional.ofNullable(props)
        .map(p -> p.get(IGNORE_VERSION_FLAG))
        .map(x -> Boolean.valueOf(x.toString()))
        .orElse(false);
    return ignoreVersion ? null : valueSet.getVersion();
  }


  /**
   * Internal method used to reify the generic type T
   *
   * @return Class<ValueSet>
   */
  @Override
  protected Class<ValueSet> getTypeClass() {
    return ValueSet.class;
  }

  /**
   * Sub-introspector that maps the FHIR identifiers used in ValueSets to Knowledge Platform Asset
   * and Artifact Identifiers.
   * <p>
   * Note: Assumes that ValueSet.url is a 'logical URI', that is transparent to location and
   * implementation. I.e., assumes that this URI persists across the source/publisher (e.g. VSAC),
   * and carries over across FHIR servers, essentially making this an Asset ID in the platform
   * sense.
   * <p>
   * Note: Tries to handle versioning in the process. While various revision and versioning
   * strategies MAY be in place across different organizations, the information may or may not be
   * carried through the FHIR resource.
   */
  public static class ValueSetIdentifierIntrospector {

    /**
     * Empty private constructor - functional class
     */
    private ValueSetIdentifierIntrospector() {
      // nothing to do
    }

    /**
     * Maps a ValueSet URI (url) + version to a primary Asset Identifier.
     * <p>
     * If the original URI is based on an universal identifier (OID or UUID), that sub-identifier
     * will be mapped to a UUID, otherwise the whole url string will be used to hash a new UUID.
     *
     * @param valueSetUri the ValueSet.url
     * @param version     the ValueSet.version
     * @param date        the Date corresponding to the ValueSet publication / last revision
     * @return an Asset ID {@link ResourceIdentifier}
     * @see ValueSetIdentifierIntrospector#mapVersion(String)
     */
    public static ResourceIdentifier getAssetId(String valueSetUri, String version, Date date) {
      String trail = NameUtils.getTrailingPart(valueSetUri);
      UUID guid = tryOid(trail)
          .or(() -> tryGuid(trail))
          .or(() -> tryString(valueSetUri))
          .orElseGet(() -> {
            LOGGER.warn("Unable to process ");
            return UUID.randomUUID();
          });

      return SurrogateBuilder.assetId(
              Registry.MAYO_ASSETS_BASE_URI_URI,
              guid,
              mapVersion(version))
          .withEstablishedOn(date != null ? date : new Date());
    }

    /**
     * Maps a ValueSet.version to a versionTag, returning the generic version 0.0.0 if absent.
     * <p>
     * This method does not try to detect/enforce any specific pattern, but removes special
     * characters and whitespaces.
     *
     * @param version the ValueSet.version
     * @return a Version tag
     * @see org.omg.spec.api4kp._20200801.id.VersionIdentifier#toSemVer(String)
     */
    public static String mapVersion(String version) {
      if (Util.isEmpty(version)) {
        return VERSION_ZERO;
      }
      try {
        var tmp = NameUtils.nameToIdentifier(version, IdentifierType.VARIABLE);
        tmp = sanitizeToASCIItext(tmp)
            .replaceAll("\\s+", "");
        return toSemVer(tmp);
      } catch (Exception e) {
        return VERSION_ZERO;
      }
    }

    /**
     * Maps a FHIR business Identifier to a platform (secondary) ResourceIdentifier, composed from
     * the Identifier's system (as namespace) and value (as tag).
     * <p>
     * Assumes that no version is present, given that FHIR handles version tags separately.
     * <p>
     * If the Identifier.system is set to "urn:ietf:rfc:3986" (URIs), parses the value as a full
     * system+tag pair instead.
     *
     * @param fhirBusinessId the FHIR {@link Identifier}
     * @return a {@link ResourceIdentifier}
     */
    public static ResourceIdentifier mapIdentifier(Identifier fhirBusinessId) {
      if ("urn:ietf:rfc:3986".equals(fhirBusinessId.getSystem())) {
        URI vuri = URI.create(fhirBusinessId.getValue());
        return newVersionId(vuri, vuri);
      } else {
        return newId(URI.create(fhirBusinessId.getSystem()), fhirBusinessId.getValue());
      }
    }

    /**
     * Maps a collection of FHIR business Identifiers to a List of platform ResourceIdentifiers.
     * Iterates over the collection and maps each individual member.
     *
     * @param fhirBusinessIds the Collection of FHIR Identifiers
     * @return a List of platform {@link ResourceIdentifier}
     * @see ValueSetIdentifierIntrospector#mapIdentifier(Identifier)
     */
    public static List<ResourceIdentifier> mapIdentifiers(Collection<Identifier> fhirBusinessIds) {
      return fhirBusinessIds.stream()
          .filter(id -> !API4KP.getReferentId().toString()
              .equals(id.getType().getCodingFirstRep().getSystem()))
          .map(ValueSetIdentifierIntrospector::mapIdentifier)
          .collect(Collectors.toList());
    }

    /**
     * Provides a default artifactId for the ValueSet with a given assetId.
     * <p>
     * Assuming that the ValueSet.url (URI) is an Asset ID, derives a default artifact ID for the
     * asset + language (FHIR STU3) combination.
     *
     * @param assetId the Asset ID of the ValueSet
     * @return A default Artifact ID for the ValueSet
     * @see SurrogateBuilder#defaultSurrogateId(ResourceIdentifier, KnowledgeRepresentationLanguage)
     * @see #mapVersion(String)
     */
    public static ResourceIdentifier getArtifactId(ResourceIdentifier assetId) {
      return defaultSurrogateId(assetId, FHIR_STU3, VERSION_ZERO);
    }

    /**
     * Attempts to generate an UUID from a ValueSet URI.
     *
     * @param valueSetUri the ValueSet URI
     * @return the UUID, if successful
     */
    private static Optional<UUID> tryString(String valueSetUri) {
      if (Util.isEmpty(valueSetUri)) {
        return Optional.empty();
      }
      try {
        return Optional.of(UUID.nameUUIDFromBytes(valueSetUri.getBytes(StandardCharsets.UTF_8)));
      } catch (Exception e) {
        return Optional.empty();
      }
    }

    /**
     * Attempts to generate an UUID from the trailing part of ValueSet URI.
     *
     * @param candidateUUID an identifier that MAY conform to the UUID structure
     * @return the UUID, if successful
     */
    private static Optional<UUID> tryGuid(String candidateUUID) {
      return Util.isUUID(candidateUUID)
          ? Optional.of(UUID.fromString(candidateUUID))
          : Optional.empty();
    }

    /**
     * Attempts to extract an OID from the ValueSet URI. If successful, rehashes the OID into a
     * GUID, deterministically
     *
     * @param candidateOID an identifier that MAY conform to the OID structure
     * @return a GUID derived from the OID, if successful
     */
    private static Optional<UUID> tryOid(String candidateOID) {
      return Util.isOID(candidateOID)
          ? Optional.of(Util.uuid(candidateOID))
          : Optional.empty();
    }

  }

}
