package edu.mayo.kmdp.knowledgebase.introspectors.owl2.internal;


import static edu.mayo.kmdp.knowledgebase.introspectors.owl2.internal.OWLIntrospectorConfiguration.OWLIntrospectorParams.ASSET_NS;
import static edu.mayo.kmdp.knowledgebase.introspectors.owl2.internal.OWLIntrospectorConfiguration.OWLIntrospectorParams.NS_INDEX;
import static edu.mayo.kmdp.knowledgebase.introspectors.owl2.internal.OWLIntrospectorConfiguration.OWLIntrospectorParams.TAG_INDEX;
import static edu.mayo.kmdp.knowledgebase.introspectors.owl2.internal.OWLIntrospectorConfiguration.OWLIntrospectorParams.VER_INDEX;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newVersionId;
import static org.omg.spec.api4kp._20200801.id.VersionIdentifier.toSemVer;

import edu.mayo.kmdp.knowledgebase.introspectors.owl2.internal.OWLIntrospectorConfiguration.OWLIntrospectorParams;
import edu.mayo.kmdp.util.DateTimeUtil;
import edu.mayo.kmdp.util.Util;
import java.net.URI;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Descriptive metadata for Ontology Assets, assumed to have at least one OWL2 manifestation used to
 * introspect the metadata from
 * <p>
 * Preserves the ontology IRIs as a secondary ID, while constructing a primary, normalized platform
 * Asset ID from the deconstruction of the ontology (version) IRI into three components: a base
 * namespace, a version tag and a local identifier.
 * <p>
 * The version tag is tentatively parsed as a date (year/month/day), considered to be the ontology
 * publication (creation) date. (Note: the distinction between creation, publication and release
 * dates may be refined in future versions)
 */
public class OntoIdentifiers {

  private static final Logger logger = LoggerFactory.getLogger(OntoIdentifiers.class);

  private final String baseURI;
  private ResourceIdentifier assetId;

  private String versionTag;

  private Date ontologyCreationDate = null;

  private ResourceIdentifier ontologyId;

  public OntoIdentifiers(OWLOntologyID ontoId,
      OWLIntrospectorConfiguration cfg) {
    this(getOntologyIRI(ontoId), cfg);
  }

  public OntoIdentifiers(
      IRI ontoId, OWLIntrospectorConfiguration cfg) {
    this(ontoId.toString(), cfg);
  }


  protected OntoIdentifiers(String ontologyIri, OWLIntrospectorConfiguration cfg) {
    this.baseURI = ontologyIri;
    decodeOntologyIri(ontologyIri, cfg);
  }

  private static String getOntologyIRI(OWLOntologyID ontoId) {
    return ontoId.getVersionIRI()
        .or(ontoId::getOntologyIRI)
        .map(IRI::toString).orElseThrow();
  }

  protected void decodeOntologyIri(String ontologyIri, OWLIntrospectorConfiguration cfg) {
    String[] parts = doDecode(ontologyIri, cfg);

    if (parts.length == 0) {
      initFallback(ontologyIri);
    } else if (Util.isNotEmpty(parts[1])) {
      initVersioned(ontologyIri, parts, cfg);
    } else {
      initUnversioned(ontologyIri, parts, cfg);
    }
  }

  private void initUnversioned(
      String ontologyIri, String[] parts,
      OWLIntrospectorConfiguration cfg) {
    URI seriesURI = URI.create(ontologyIri);
    String defVersion = cfg.getTyped(OWLIntrospectorParams.DEFAULT_VERSION);
    URI versionURI = URI.create(
        parts[0] + versionTag + "/" + parts[2] + trail(ontologyIri));
    ontologyId = newVersionId(seriesURI, versionURI)
        .withEstablishedOn(null);
    tryDecodeVersionTag(defVersion);

    assetId = newId(
        cfg.getTyped(ASSET_NS, URI.class),
        Util.uuid(ontologyId.getResourceId().toString()),
        versionTag)
        // clear the default ID date -
        .withEstablishedOn(null);
  }

  private void initVersioned(
      String ontologyIri, String[] parts,
      OWLIntrospectorConfiguration cfg) {
    URI versionURI = URI.create(ontologyIri);
    URI seriesURI = URI.create(parts[0] + parts[2] + trail(ontologyIri));
    ontologyId = newVersionId(seriesURI, versionURI).withEstablishedOn(null);
    tryDecodeVersionTag(parts[1]);

    assetId = newId(
        cfg.getTyped(ASSET_NS, URI.class),
        Util.uuid(ontologyId.getResourceId().toString()),
        versionTag)
        // clear the default ID date -
        .withEstablishedOn(null);
  }

  private void tryDecodeVersionTag(String tag) {
    var tagDate = DateTimeUtil.tryParseDate(tag, "yyyyMMdd")
        .or(() -> DateTimeUtil.tryParseDate(tag, DateTimeUtil.DEFAULT_DATE_PATTERN));
    if (tagDate.isPresent()) {
      ontologyCreationDate = tagDate.get();
      versionTag = toCalver(tagDate.get());
    } else {
      versionTag = toSemVer(tag);
    }
  }

  private String toCalver(Date date) {
    var ld = DateTimeUtil.toLocalDate(date);
    return String.format("%d.%d.%d", ld.getYear(), ld.getMonthValue(), ld.getDayOfMonth());
  }

  private void initFallback(String ontologyIri) {
    logger.warn(
        "Unable to decode ontology IRI {} using the provided pattern, fallback to default",
        ontologyIri);
    var ontologyUri = URI.create(ontologyIri);
    assetId = newVersionId(ontologyUri, ontologyUri).withEstablishedOn(null);
    ontologyId = assetId;
  }

  private String[] doDecode(String ontologyIri, OWLIntrospectorConfiguration cfg) {
    Pattern versionPattern = Pattern.compile(cfg.getTyped(OWLIntrospectorParams.VERSION_PATTERN));
    int nsIdx = cfg.getTyped(NS_INDEX, Integer.class);
    int verIdx = cfg.getTyped(VER_INDEX, Integer.class);
    int tagIdx = cfg.getTyped(TAG_INDEX, Integer.class);

    Matcher m = versionPattern.matcher(ontologyIri);
    if (!m.matches()) {
      return new String[0];
    }
    return new String[]{m.group(nsIdx), m.group(verIdx), m.group(tagIdx)};
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

  public String getBaseURI() {
    return baseURI;
  }

  public ResourceIdentifier getAssetId() {
    return assetId;
  }

  public String getVersionTag() {
    return versionTag;
  }

  public Date getOntologyCreationDate() {
    return ontologyCreationDate;
  }

  public ResourceIdentifier getOntologyId() {
    return ontologyId;
  }
}
