package edu.mayo.kmdp.terms.impl.model;

import java.net.URI;
import java.util.Map;
import java.util.UUID;
import org.omg.spec.api4kp._20200801.terms.model.ConceptDescriptor;

/**
 * This class defines the parts of the terminology available for the service
 * <p>
 * In this context, a Terminology Scheme is a collection of Terms (Concept Descriptors) with
 * identity and version
 * <p>
 * Note that Concepts can be shared between Schemes, and belong to a 'namespace' which may not be
 * the same namespace that the Scheme is associated to
 */
public class TerminologyScheme {

  /**
   * A UUID-based tag of the terminology Scheme version - used for indexing
   */
  private UUID key;

  /**
   * A UUID-based
   */
  private UUID schemeUUID;

  /**
   * The 'tag' of the terminology Scheme May or may not be UUID based, and may not coincide with the
   * schemeUUID
   */
  private String tag;

  /**
   * The name of the terminology Scheme
   */
  private String name;

  /**
   * The version of the terminology Scheme
   */
  private String version;

  /**
   * The fully qualified and versioned URI of the terminology Scheme
   */
  private String schemeId;

  /**
   * The fully qualified, but unversioned, URI of the terminology Scheme
   */
  private URI seriesId;

  /**
   * The terms found in the terminology Scheme
   */
  private Map<UUID, ConceptDescriptor> terms;

  public UUID getKey() {
    return key;
  }

  public void setKey(UUID key) {
    this.key = key;
  }

  public String getTag() {
    return tag;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getSchemeId() {
    return schemeId;
  }

  public void setSchemeId(String schemeId) {
    this.schemeId = schemeId;
  }

  public URI getSeriesId() {
    return seriesId;
  }

  public void setSeriesId(URI seriesId) {
    this.seriesId = seriesId;
  }

  public Map<UUID, ConceptDescriptor> getTerms() {
    return terms;
  }

  public void setTerms(Map<UUID, ConceptDescriptor> terms) {
    this.terms = terms;
  }

  public TerminologyScheme() {
    super();
  }

  public UUID getSchemeUUID() {
    return schemeUUID;
  }

  public void setSchemeUUID(UUID schemeUUID) {
    this.schemeUUID = schemeUUID;
  }
}