package edu.mayo.kmdp.examples._7.inference;


import static edu.mayo.kmdp.id.helper.DatatypeHelper.resolveTerm;

import edu.mayo.kmdp.util.Util;
import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.omg.spec.api4kp._20200801.id.Term;

public enum MockVocab implements Term {


  Survival_Rate(
      "b7cdb1b3-8e96-419a-ac08-01d9b38de3e8",
      "hodgkinLymphoma5YearSurvivalRate",
      "Hodgkin's Lymphoma Survival Rate (5 Year)"
  ),

  Current_Caffeine_User(
      "currentCaffeineUser",
      "Current Caffeine User"
  ),

  Current_Chronological_Age(
      "01aaaf22-ca7f-42d0-a1a2-f027fbf81fa6",
      "currentChronologicalAge",
      "Current Chronological Age"
  );


  public static final Map<UUID, MockVocab> INDEX = Arrays.stream(MockVocab.values())
      .collect(Collectors.toConcurrentMap(Term::getUuid, Function.identity()));

  private final UUID uuid;
  private final String tag;
  private final String label;

  MockVocab(final String code,
      final String displayName) {
    this(Util.uuid(code).toString(), code, displayName);
  }

  MockVocab(final String conceptUUID,
      final String code,
      final String displayName) {
    this.uuid = UUID.fromString(conceptUUID);
    this.tag = code;
    this.label = displayName;
  }


  public static Optional<MockVocab> resolve(final String tag) {
    return resolveTag(tag);
  }

  public static Optional<MockVocab> resolveTag(final String tag) {
    return resolveTerm(tag, MockVocab.values(), Term::getTag);
  }

  public static Optional<MockVocab> resolveUUID(final UUID conceptId) {
    return Optional.ofNullable(INDEX.get(conceptId));
  }


  @Override
  public String getTag() {
    return tag;
  }

  @Override
  public String getName() {
    return label;
  }

  @Override
  public Date getEstablishedOn() {
    return new Date();
  }

  @Override
  public URI getReferentId() {
    return URI.create("uri:urn:" + tag);
  }

  @Override
  public URI getResourceId() {
    return URI.create("urn:uuid:" + uuid.toString());
  }

  @Override
  public UUID getUuid() {
    return uuid;
  }

  @Override
  public URI getNamespaceUri() {
    return URI.create("urn:uuid");
  }

  @Override
  public URI getVersionId() {
    return URI.create("urn:uuid:" + uuid.toString() + ":" + getVersionTag());
  }

  @Override
  public String getVersionTag() {
    return "0.0.0";
  }
}
