package edu.mayo.kmdp.examples;

import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newKey;

import edu.mayo.kmdp.language.DeserializeApiOperator;
import edu.mayo.kmdp.language.DetectApiOperator;
import edu.mayo.kmdp.language.LanguageDeSerializer;
import edu.mayo.kmdp.language.LanguageDetector;
import edu.mayo.kmdp.language.TransrepresentationExecutor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetCatalogApiInternal;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetRepositoryApiInternal;
import org.omg.spec.api4kp._20200801.id.KeyIdentifier;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;

public class MockAssetRepository implements KnowledgeAssetRepositoryApiInternal,
    KnowledgeAssetCatalogApiInternal {

  private final Map<KeyIdentifier, AssetPair> repo = new ConcurrentHashMap<>();

  private LanguageDeSerializer deSerializer;
  private LanguageDetector detector;
  private TransrepresentationExecutor translator;

  public MockAssetRepository() {

  }

  public MockAssetRepository(List<DeserializeApiOperator> parsers,
      List<DetectApiOperator> detectors) {
    deSerializer = new LanguageDeSerializer(parsers);
    detector = new LanguageDetector(detectors);
  }

  @Override
  public Answer<List<Pointer>> listKnowledgeAssetCarriers(UUID assetId, String versionTag) {
    var id = newKey(assetId, versionTag);
    return Answer.ofNullable(repo.get(id))
        .flatMap(ap -> Answer.ofNullable(ap.artifact))
        .map(l -> l.stream().map(a -> a.getArtifactId().toPointer()).collect(Collectors.toList()));
  }

  @Override
  public Answer<KnowledgeCarrier> getKnowledgeAssetVersionCanonicalCarrier(
      UUID assetId, String versionTag, String xAccept) {
    var id = newKey(assetId, versionTag);

    return Answer.ofNullable(repo.get(id))
        .flatMap(ap -> Answer.ofNullable(ap.artifact))
        .map(l -> l.get(0));
  }

  @Override
  public Answer<Void> addKnowledgeAssetCarrier(
      UUID assetId, String versionTag,
      KnowledgeCarrier carrier) {
    var id = newKey(assetId, versionTag);
    if (!repo.containsKey(id)) {
      return Answer.notFound();
    }
    repo.get(id).artifact.add(carrier);
    return Answer.succeed();
  }

  @Override
  public Answer<KnowledgeCarrier> getKnowledgeAssetCarrierVersion(UUID assetId, String versionTag,
      UUID artifactId, String artifactVersionTag, String xAccept) {
    var id = newId(assetId, versionTag);
    var cid = newId(artifactId, artifactVersionTag);
    if (!repo.containsKey(id.asKey())) {
      return Answer.notFound();
    }
    var chosen = repo.get(id.asKey()).artifact.stream()
        .filter(a -> a.getArtifactId().asKey().equals(cid.asKey()))
        .findFirst();
    return Answer.ofTry(chosen);
  }

  @Override
  public Answer<KnowledgeAsset> getKnowledgeAssetVersion(
      UUID assetId,
      String versionTag,
      String xAccept) {
    var id = newKey(assetId, versionTag);
    return Answer.ofNullable(repo.get(id)).map(ap -> ap.surrogate);
  }


  @Override
  public Answer<KnowledgeAsset> getKnowledgeAsset(
      UUID assetId,
      String xAccept) {
    var s = repo.entrySet().stream()
        .filter(e -> e.getKey().getUuid().equals(assetId))
        .map(e -> e.getValue().surrogate)
        .findFirst();
    return Answer.ofTry(s);
  }

  @Override
  public Answer<List<Pointer>> listKnowledgeAssets() {
    return Answer.of(repo.keySet().stream()
        .map(ki -> SemanticIdentifier.newId(ki.getUuid(), ki.getVersionTag()).toPointer())
        .collect(Collectors.toList()));
  }

  @Override
  public Answer<Void> setKnowledgeAssetVersion(
      UUID assetId,
      String versionTag,
      KnowledgeAsset surrogate) {
    var id = newKey(assetId, versionTag);
    repo.computeIfAbsent(id, x -> new AssetPair(x, surrogate));
    return Answer.succeed();
  }

  @Override
  public Answer<Void> setKnowledgeAssetCarrierVersion(UUID assetId, String versionTag,
      UUID artifactId, String artifactVersionTag, byte[] body) {
    var id = newId(assetId, versionTag);
    var cid = newId(artifactId, artifactVersionTag);
    var kc = AbstractCarrier.of(body)
        .withAssetId(id)
        .withArtifactId(cid);
    if (!repo.containsKey(id.asKey())) {
      return Answer.notFound();
    }
    if (detector != null) {
      kc = detector.applyDetect(kc).orElse(kc);
    }
    repo.get(id.asKey()).artifact.add(kc);
    return Answer.succeed();
  }



  public void publish(
      KnowledgeAsset surrogate,
      KnowledgeCarrier artifact) {

    ResourceIdentifier surrogateId = surrogate.getAssetId();

    this.setKnowledgeAssetVersion(
        surrogateId.getUuid(),
        surrogateId.getVersionTag(),
        surrogate);

    if (artifact != null) {
      repo.get(surrogateId.asKey()).artifact.add(artifact);
    }
  }


  private static class AssetPair {

    KeyIdentifier assetId;
    KnowledgeAsset surrogate;
    List<KnowledgeCarrier> artifact = new ArrayList<>();

    public AssetPair(KeyIdentifier assetId, KnowledgeAsset surrogate) {
      this.assetId = assetId;
      this.surrogate = surrogate;
    }
  }
}
