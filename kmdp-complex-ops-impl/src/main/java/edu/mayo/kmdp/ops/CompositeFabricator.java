package edu.mayo.kmdp.ops;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;

public class CompositeFabricator implements EphemeralAssetFabricator {

  private static final UUID OPERATOR_ID = UUID.fromString("c1f1cbf4-bb67-474b-bf1b-df72a1bd0b8d");

  protected Map<UUID,EphemeralAssetFabricator> delegates = new HashMap<>();

  @Override
  public UUID getId() {
    return OPERATOR_ID;
  }


  public CompositeFabricator(List<EphemeralAssetFabricator> delegates) {
    delegates.forEach(del -> this.delegates.put(del.getId(), del));
  }

  protected Answer<EphemeralAssetFabricator> chooseDelegate(@Nonnull Pointer source) {
    return Answer.ofTry(delegates.values().stream()
        .filter(candidate -> candidate.canFabricate(source))
        .findFirst());
  }

  protected Answer<EphemeralAssetFabricator> chooseDelegate(@Nonnull UUID ephemeralAssetId) {
    return Answer.ofTry(delegates.values().stream()
        .filter(candidate -> candidate.isFabricator(ephemeralAssetId))
        .findFirst());
  }

  @Override
  public Answer<KnowledgeCarrier> applyNamedTransform(
      UUID operatorId, UUID kbaseId,
      String versionTag, String xParams) {
    return delegates.get(operatorId)
        .applyNamedTransform(operatorId, kbaseId, versionTag, xParams);
  }

  @Override
  public Answer<KnowledgeCarrier> fabricate(
      @Nonnull UUID ephemeralAssetId,
      @Nonnull String ephemeralVersionTag) {
    return chooseDelegate(ephemeralAssetId)
        .flatMap(d -> d.fabricate(ephemeralAssetId, ephemeralVersionTag));
  }

  @Override
  public Answer<KnowledgeCarrier> fabricateSurrogate(
      @Nonnull UUID ephemeralAssetId,
      @Nonnull String ephemeralVersionTag) {
    return chooseDelegate(ephemeralAssetId)
        .flatMap(d -> d.fabricateSurrogate(ephemeralAssetId, ephemeralVersionTag));
  }


  @Override
  public Answer<String> getFabricatableVersion(
      @Nonnull UUID ephemeralAssetId) {
    return chooseDelegate(ephemeralAssetId)
        .flatMap(d -> d.getFabricatableVersion(ephemeralAssetId));
  }


  @Override
  public Optional<Pointer> pledge(
      @Nonnull Pointer source) {
    return chooseDelegate(source)
        .getOptionalValue()
        .flatMap(d -> d.pledge(source));
  }

  @Override
  public boolean canFabricate(@Nonnull Pointer source) {
    return delegates.values().stream()
        .anyMatch(d -> d.canFabricate(source));
  }

  @Override
  public boolean isFabricator(@Nonnull UUID ephemeralAssetId) {
    return delegates.values().stream()
        .anyMatch(d -> d.isFabricator(ephemeralAssetId));
  }
}
