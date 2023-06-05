package edu.mayo.kmdp.kbase.inference.mockRepo;

import java.util.UUID;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetCatalogApiInternal;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetRepositoryApiInternal;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;

public class MockSingletonAssetRepository implements KnowledgeAssetRepositoryApiInternal,
    KnowledgeAssetCatalogApiInternal {

  private UUID modelId;
  private String version;
  private KnowledgeCarrier carrier;
  private KnowledgeAsset surrogate;

  public MockSingletonAssetRepository(UUID modelId, String version,
      KnowledgeCarrier carrier, KnowledgeAsset surrogate) {
    this.carrier = carrier;
    this.modelId = modelId;
    this.version = version;
    this.surrogate = surrogate;
  }

  @Override
  public Answer<KnowledgeCarrier> getKnowledgeAssetVersionCanonicalCarrier(UUID assetId, String versionTag,
      String xAccept) {
    if (assetId.equals(this.modelId) && versionTag.equals(this.version)) {
      return Answer.of(carrier);
    } else {
      return Answer.notFound();
    }
  }

  @Override
  public Answer<KnowledgeAsset> getKnowledgeAssetVersion(UUID assetId, String versionTag) {
    if (assetId.equals(this.modelId) && versionTag.equals(this.version)) {
      return Answer.of(surrogate);
    } else {
      return Answer.notFound();
    }
  }

  @Override
  public Answer<KnowledgeCarrier> getKnowledgeGraph(String xAccept) {
    return Answer.unsupported();
  }

  @Override
  public Answer<KnowledgeCarrier> getKnowledgeAssetCarrierVersion(UUID assetId, String versionTag,
      UUID artifactId, String artifactVersionTag) {
    if (assetId.equals(this.modelId) && versionTag.equals(this.version)) {
      return Answer.of(carrier);
    } else {
      return Answer.notFound();
    }
  }


}
