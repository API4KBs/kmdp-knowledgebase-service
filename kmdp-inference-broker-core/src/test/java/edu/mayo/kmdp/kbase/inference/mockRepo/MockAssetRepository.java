package edu.mayo.kmdp.kbase.inference.mockRepo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetCatalogApiInternal;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetRepositoryApiInternal;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;

public class MockAssetRepository implements KnowledgeAssetRepositoryApiInternal,
    KnowledgeAssetCatalogApiInternal {

  private final Map<String,KnowledgeCarrier> carriers = new HashMap<>();
  private final Map<String, KnowledgeAsset> surrogates = new HashMap<>();

  public MockAssetRepository() {

  }

  public void addContent(List<KnowledgeAsset> surrogates, List<KnowledgeCarrier> carriers) {
    surrogates.forEach(s -> {
      ResourceIdentifier rid = s.getAssetId();
      String key = getKey(rid.getUuid(),rid.getVersionTag());
      this.surrogates.put(key, s);
    });

    carriers.forEach(c -> {
      String key = getKey(c.getAssetId().getUuid(),c.getAssetId().getVersionTag());
      this.carriers.put(key, c);
    });
  }

  @Override
  public Answer<KnowledgeCarrier> getKnowledgeAssetVersionCanonicalCarrier(UUID assetId, String versionTag,
      String xAccept) {
    return Answer.of(carriers.get(getKey(assetId,versionTag)));
  }


  @Override
  public Answer<KnowledgeAsset> getKnowledgeAssetVersion(UUID assetId, String versionTag) {
    return Answer.of(surrogates.get(getKey(assetId,versionTag)));
  }


  @Override
  public Answer<KnowledgeCarrier> getKnowledgeAssetCarrierVersion(UUID assetId, String versionTag,
      UUID artifactId, String artifactVersionTag) {
    return Answer.of(carriers.get(getKey(assetId,versionTag)));
  }

  @Override
  public Answer<List<Pointer>> listKnowledgeAssets(String assetTypeTag, String assetAnnotationTag,
      String assetAnnotationConcept, Integer offset, Integer limit) {
    return Answer.of(surrogates.values().stream()
        .map(ax -> ax.getAssetId().toPointer())
        .collect(Collectors.toList()));
  }


  private String getKey(UUID uuid, String versionTag) {
    return uuid + ":" + versionTag;
  }


  /********************************************************************************************/

 
}
