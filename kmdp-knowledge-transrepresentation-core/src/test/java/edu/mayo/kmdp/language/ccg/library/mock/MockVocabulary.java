package edu.mayo.kmdp.language.ccg.library.mock;

import edu.mayo.kmdp.util.DateTimeUtil;
import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.id.Term;
import org.omg.spec.api4kp._20200801.terms.ConceptTerm;
import org.omg.spec.api4kp._20200801.terms.TermDescription;
import org.omg.spec.api4kp._20200801.terms.model.TermImpl;

public class MockVocabulary implements ConceptTerm {

  private static final Map<UUID,ConceptTerm> index = new HashMap<>();

  public static MockVocabulary Current_Caffeine_Use = new MockVocabulary("1", "Curr Caffeine Use");
  public static MockVocabulary Current_Caffeine_Use_NewVer = new MockVocabulary("1", "Curr Caffeine Use", "0.0.1");
  public static MockVocabulary Currently_Dehydrated = new MockVocabulary("2", "Curr Dehydrated");
  public static MockVocabulary Has_Hypertension = new MockVocabulary("3", "Has Hypertension");
  public static MockVocabulary Has_Hypertension_Is = new MockVocabulary("4", "Hypertense - Is");
  public static MockVocabulary Has_Allergy_To_Statins = new MockVocabulary("5", "Has Statin Allergy");
  public static MockVocabulary Has_Diabetes_Mellitus = new MockVocabulary("6", "Has Diabetes");
  public static MockVocabulary Most_Recent_Blood_Pressure = new MockVocabulary("7", "Most Recent BP");

  private final TermDescription termImpl;

  public MockVocabulary(String i, String name) {
    this(i, name, "0.0.0");
  }

  public MockVocabulary(String i, String name, String versionTag) {
    index.put(UUID.nameUUIDFromBytes(i.getBytes()), this);
    termImpl = new TermImpl(
        "urn:mock:"+i,
        UUID.nameUUIDFromBytes(i.getBytes()).toString(),
        versionTag,
        i,
        Collections.emptyList(),
        name,
        "urn:mock:"+i,
        new Term[0],
        new Term[0],
        new Date()
    );
  }

  public static Optional<ConceptTerm> resolveUUID(UUID id) {
    return Optional.ofNullable(index.get(id));
  }


  public static Optional<ConceptTerm> resolveConceptId(String conceptId) {
    return index.values().stream()
        .filter(c -> c.getConceptId().toString().equals(conceptId))
        .findFirst();
  }

  @Override
  public TermDescription getDescription() {
    return this.termImpl;
  }

  @Override
  public ResourceIdentifier getNamespace() {
    return SemanticIdentifier
        .newId(URI.create("http://foo.bar/mock"))
        .withTag("baz")
        .withEstablishedOn(DateTimeUtil.today());
  }

  @Override
  public URI getVersionId() {
    return termImpl.getVersionId();
  }

  @Override
  public URI getResourceId() {
    return termImpl.getResourceId();
  }

  @Override
  public UUID getUuid() {
    return termImpl.getUuid();
  }

  @Override
  public URI getNamespaceUri() {
    return getNamespace().getResourceId();
  }

  @Override
  public String getVersionTag() {
    return "0";
  }

  @Override
  public Date getEstablishedOn() {
    return DateTimeUtil.today();
  }
}
