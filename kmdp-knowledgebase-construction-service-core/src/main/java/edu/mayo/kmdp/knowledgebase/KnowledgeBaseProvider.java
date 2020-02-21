package edu.mayo.kmdp.knowledgebase;

import static edu.mayo.kmdp.id.helper.DatatypeHelper.toVersionIdentifier;
import static edu.mayo.kmdp.id.helper.DatatypeHelper.uri;
import static edu.mayo.kmdp.id.helper.DatatypeHelper.vid;
import static org.omg.spec.api4kp._1_0.AbstractCarrier.rep;

import edu.mayo.kmdp.id.VersionedIdentifier;
import edu.mayo.kmdp.id.helper.DatatypeHelper;
import edu.mayo.kmdp.knowledgebase.v3.server.KnowledgeBaseApiInternal;
import edu.mayo.kmdp.metadata.surrogate.ComputableKnowledgeArtifact;
import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.registry.Registry;
import edu.mayo.kmdp.repository.asset.v3.server.KnowledgeAssetRepositoryApiInternal;
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
import org.omg.spec.api4kp._1_0.identifiers.Pointer;
import org.omg.spec.api4kp._1_0.identifiers.VersionIdentifier;
import org.omg.spec.api4kp._1_0.services.BinaryCarrier;
import org.omg.spec.api4kp._1_0.services.KPServer;
import org.omg.spec.api4kp._1_0.services.KnowledgeBase;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;
import org.springframework.beans.factory.annotation.Autowired;

@KPServer
@Named
public class KnowledgeBaseProvider
    implements KnowledgeBaseApiInternal {

  private KnowledgeAssetRepositoryApiInternal assetRepository;

  private Map<VersionedIdentifier, KnowledgeBase> knowledgeBaseMap = new HashMap<>();

  @Autowired
  public KnowledgeBaseProvider(
      @KPServer KnowledgeAssetRepositoryApiInternal assetRepository) {
    this.assetRepository = assetRepository;
  }

  @Override
  public Answer<Void> deleteKnowledgeBase(UUID kbaseId) {
    knowledgeBaseMap.keySet().stream()
        .filter(vid -> kbaseId.equals(Util.toUUID(vid.getTag())))
        .forEach(knowledgeBaseMap::remove);
    return Answer.succeed();
  }

  @Override
  public Answer<KnowledgeBase> getKnowledgeBase(UUID kbaseId, String versionTag) {
    VersionedIdentifier vid = new VersionIdentifier()
        .withTag(kbaseId.toString())
        .withVersion(versionTag);
    return Answer.ofNullable(knowledgeBaseMap.get(vid));
  }

  @Override
  public Answer<List<Pointer>> getKnowledgeBaseSeries(UUID kbaseId) {
    return Answer.of(knowledgeBaseMap.keySet().stream()
        .filter(vid -> kbaseId.equals(Util.toUUID(vid.getTag())))
        .map(this::toPointer)
        .collect(Collectors.toList()));
  }

  @Override
  public Answer<KnowledgeCarrier> getKnowledgeBaseStructure(UUID kbaseId, String versionTag) {
    return Answer.unsupported();
  }

  @Override
  public Answer<Pointer> initKnowledgeBase() {
    VersionedIdentifier vid = vid(UUID.randomUUID().toString(), "0.0.0");
    createEmptyKnowledgeBase(vid);
    return Answer.of(toPointer(vid));
  }

  @Override
  public Answer<Pointer> initKnowledgeBase(KnowledgeAsset asset) {
    VersionedIdentifier kbaseId = toVersionIdentifier(asset.getAssetId());

    if (! knowledgeBaseMap.containsKey(kbaseId)) {
      createEmptyKnowledgeBase(kbaseId);
    } else {
      return Answer.failed(
          new IllegalStateException("The KB with ID " + kbaseId.getTag()  + ":"
              + kbaseId.getVersion() + " is already initialized"));
    }

    getCanonicalArtifact(asset)
        .ifPresent(artf -> populateKnowledgeBase(
            Util.toUUID(kbaseId.getTag()),
            kbaseId.getVersion(),
            artf
      ));

    return Answer.of(toPointer(kbaseId));
  }

  private Answer<KnowledgeCarrier> getCanonicalArtifact(KnowledgeAsset asset) {
    Answer<ComputableKnowledgeArtifact> artifactRef = Answer.of(asset.getCarriers().stream()
        .flatMap(StreamUtil.filterAs(ComputableKnowledgeArtifact.class))
        .findFirst());

    return artifactRef.flatMap(artifact -> {
      if (isLocal(artifact)) {
        VersionedIdentifier assetVid = toVersionIdentifier(asset.getAssetId());
        VersionedIdentifier artifVid = toVersionIdentifier(artifact.getArtifactId());
        return assetRepository.getKnowledgeAssetCarrierVersion(
            Util.toUUID(assetVid.getTag()),
            assetVid.getVersion(),
            Util.toUUID(artifVid.getTag()),
            artifVid.getVersion());
      } else {
        return Answer.of(new BinaryCarrier()
            .withRepresentation(rep(artifact.getRepresentation()))
            .withArtifactId(artifact.getArtifactId())
            .withAssetId(asset.getAssetId())
            .withLabel(asset.getName())
            .withHref(artifact.getLocator()));
      }
    });
  }

  private KnowledgeBase createEmptyKnowledgeBase(VersionedIdentifier vid) {
    KnowledgeBase newKBase = new KnowledgeBase()
        .withKbaseId(DatatypeHelper.uri(Registry.BASE_UUID_URN, vid.getTag(), vid.getVersion()));
    knowledgeBaseMap.put(vid, newKBase);
    return newKBase;
  }


  @Override
  public Answer<Pointer> populateKnowledgeBase(UUID kbaseId, String versionTag,
      KnowledgeCarrier sourceArtifact) {
    VersionedIdentifier versionedId = vid(kbaseId.toString(), versionTag);
    knowledgeBaseMap.computeIfPresent(versionedId,
        (id, kb) -> isLocal(sourceArtifact)
            ? configureAsLocalKB(kb, sourceArtifact)
            : configureAsRemoteKB(kb, sourceArtifact.getHref()));
    return Answer.of(toPointer(versionedId));
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

  protected Pointer toPointer(VersionedIdentifier versionedIdentifier) {
    return new Pointer()
        .withName("KBase " + versionedIdentifier.getTag() + ":" + versionedIdentifier.getVersion())
        .withEntityRef(uri(
            Registry.BASE_UUID_URN,
            versionedIdentifier.getTag(),
            versionedIdentifier.getVersion()));
  }

}
