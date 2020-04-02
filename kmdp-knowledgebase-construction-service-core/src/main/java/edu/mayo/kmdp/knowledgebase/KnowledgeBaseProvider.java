package edu.mayo.kmdp.knowledgebase;

import static org.omg.spec.api4kp._1_0.AbstractCarrier.rep;

import edu.mayo.kmdp.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import edu.mayo.kmdp.metadata.v2.surrogate.ComputableKnowledgeArtifact;
import edu.mayo.kmdp.metadata.v2.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.repository.asset.v4.server.KnowledgeAssetRepositoryApiInternal;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.Util;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Named;
import org.omg.spec.api4kp._1_0.Answer;
import org.omg.spec.api4kp._1_0.id.IdentifierConstants;
import org.omg.spec.api4kp._1_0.id.KeyIdentifier;
import org.omg.spec.api4kp._1_0.id.Pointer;
import org.omg.spec.api4kp._1_0.id.ResourceIdentifier;
import org.omg.spec.api4kp._1_0.id.SemanticIdentifier;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;
import org.omg.spec.api4kp._1_0.services.KPServer;
import org.omg.spec.api4kp._1_0.services.KnowledgeBase;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;
import org.springframework.beans.factory.annotation.Autowired;

@KPServer
@Named
public class KnowledgeBaseProvider
    implements KnowledgeBaseApiInternal {

  private KnowledgeAssetRepositoryApiInternal assetRepository;

  private Map<KeyIdentifier, KnowledgeBase> knowledgeBaseMap = new HashMap<>();

  @Autowired
  public KnowledgeBaseProvider(
      @KPServer KnowledgeAssetRepositoryApiInternal assetRepository) {
    this.assetRepository = assetRepository;
  }

  @Override
  public Answer<Void> deleteKnowledgeBase(final UUID kbaseId) {
    knowledgeBaseMap.keySet().stream()
        .filter(vid -> kbaseId.equals(vid.getUuid()))
        .forEach(knowledgeBaseMap::remove);
    return Answer.succeed();
  }

  @Override
  public Answer<KnowledgeBase> getKnowledgeBase(UUID kbaseId, String versionTag) {
    ResourceIdentifier vid = SemanticIdentifier.newId(kbaseId,versionTag);
    return Answer.ofNullable(knowledgeBaseMap.get(vid.asKey()));
  }

  @Override
  public Answer<List<Pointer>> getKnowledgeBaseSeries(UUID kbaseId) {
    return Answer.of(
        knowledgeBaseMap.keySet().stream()
            .filter(vid -> kbaseId.equals(vid.getUuid()))
            .map(vid -> knowledgeBaseMap.get(vid))
            .map(KnowledgeBase::getKbaseId)
            .collect(Collectors.toList()));
  }

  @Override
  public Answer<KnowledgeCarrier> getKnowledgeBaseStructure(UUID kbaseId, String versionTag) {
    return Answer.unsupported();
  }

  @Override
  public Answer<Pointer> initKnowledgeBase() {
    Pointer vid = SemanticIdentifier.newIdAsPointer(UUID.randomUUID(), IdentifierConstants.VERSION_ZERO);
    createEmptyKnowledgeBase(vid);
    return Answer.of(vid);
  }

  @Override
  public Answer<Pointer> initKnowledgeBase(KnowledgeAsset asset) {
    Pointer kbaseId = asset.getAssetId().toPointer();

    if (! knowledgeBaseMap.containsKey(kbaseId.asKey())) {
      createEmptyKnowledgeBase(kbaseId);
    } else {
      return Answer.failed(
          new IllegalStateException("The KB with ID " + kbaseId.getTag()  + ":"
              + kbaseId.getVersionTag() + " is already initialized"));
    }

    getCanonicalArtifact(asset)
        .ifPresent(artf -> populateKnowledgeBase(
            kbaseId.getUuid(),
            kbaseId.getVersionTag(),
            artf
      ));

    return Answer.of(kbaseId);
  }

  private Answer<KnowledgeCarrier> getCanonicalArtifact(KnowledgeAsset asset) {
    Answer<ComputableKnowledgeArtifact> artifactRef = Answer.of(asset.getCarriers().stream()
        .flatMap(StreamUtil.filterAs(ComputableKnowledgeArtifact.class))
        .findFirst());

    return artifactRef.flatMap(artifact -> {
      ResourceIdentifier assetVid = asset.getAssetId();
      ResourceIdentifier artifVid = artifact.getArtifactId();
      if (isLocal(artifact)) {
        return assetRepository.getKnowledgeAssetCarrierVersion(
            assetVid.getUuid(),
            assetVid.getVersionTag(),
            artifVid.getUuid(),
            artifVid.getVersionTag());
      } else {
        return Answer.of(new KnowledgeCarrier()
            .withRepresentation(rep(artifact.getRepresentation()))
            .withArtifactId(artifVid)
            .withAssetId(assetVid)
            .withLabel(asset.getName())
            .withHref(artifact.getLocator()));
      }
    });
  }

  private KnowledgeBase createEmptyKnowledgeBase(ResourceIdentifier vid) {
    KnowledgeBase newKBase = new KnowledgeBase()
        .withKbaseId(vid.toInnerPointer());
    knowledgeBaseMap.put(vid.asKey(), newKBase);
    return newKBase;
  }


  @Override
  public Answer<Pointer> populateKnowledgeBase(UUID kbaseId, String versionTag,
      KnowledgeCarrier sourceArtifact) {
    Pointer versionedId = SemanticIdentifier.newIdAsPointer(kbaseId, versionTag);
    knowledgeBaseMap.computeIfPresent(versionedId.asKey(),
        (id, kb) -> isLocal(sourceArtifact)
            ? configureAsLocalKB(kb, sourceArtifact)
            : configureAsRemoteKB(kb, sourceArtifact.getHref()));
    return Answer.of(versionedId);
  }

  protected KnowledgeBase configureAsRemoteKB(KnowledgeBase kBase, URI endpoint) {
    return kBase
        .withManifestation(null)
        .withEndpoint(endpoint);
  }

  protected KnowledgeBase configureAsLocalKB(KnowledgeBase kBase, KnowledgeCarrier kc) {
    return kBase
        .withManifestation(kc)
        .withEndpoint(null);
  }



  protected boolean isLocal(ComputableKnowledgeArtifact artifactSurrogate) {
    return artifactSurrogate.getLocator() == null
        || Util.isEmpty(artifactSurrogate.getLocator().getScheme());
  }
  protected boolean isLocal(KnowledgeCarrier carrier) {
    return carrier.getHref() == null
        || Util.isEmpty(carrier.getHref().getScheme());
  }

  protected KnowledgeAssetRepositoryApiInternal getAssetRepository() {
    return assetRepository;
  }

}
