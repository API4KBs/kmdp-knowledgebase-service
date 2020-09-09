package edu.mayo.kmdp.knowledgebase;

import edu.mayo.kmdp.util.Util;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Named;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetRepositoryApiInternal;
import org.omg.spec.api4kp._20200801.datatypes.Bindings;
import org.omg.spec.api4kp._20200801.id.IdentifierConstants;
import org.omg.spec.api4kp._20200801.id.KeyIdentifier;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.KPComponent;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.omg.spec.api4kp._20200801.services.KnowledgeBase;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.springframework.beans.factory.annotation.Autowired;

@KPServer
@Named
public class KnowledgeBaseProvider
    implements KnowledgeBaseApiInternal {


  @Autowired(required = false)
  private Weaver weaver;

  private Map<KeyIdentifier, KnowledgeBase> knowledgeBaseMap = new HashMap<>();

  @KPServer @Autowired
  private KnowledgeAssetRepositoryApiInternal assetRepository;

  public KnowledgeBaseProvider() {
  }

  public KnowledgeBaseProvider(
      KnowledgeAssetRepositoryApiInternal assetRepository) {
    this.assetRepository = assetRepository;
    this.weaver = new Weaver(this, Collections.emptyList());
  }

  public Weaver getWeaver() {
    return weaver;
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
  public Answer<KnowledgeCarrier> getKnowledgeBaseManifestation(UUID kbaseId, String versionTag) {
    return getKnowledgeBase(kbaseId,versionTag)
        .map(KnowledgeBase::getManifestation);
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
    return getKnowledgeBase(kbaseId,versionTag)
        .map(KnowledgeBase::getManifestation)
        .filter(CompositeKnowledgeCarrier.class::isInstance)
        .map(CompositeKnowledgeCarrier.class::cast)
        .map(CompositeKnowledgeCarrier::getStruct);
  }

  @Override
  public Answer<Pointer> initKnowledgeBase() {
    Pointer vid = SemanticIdentifier.newIdAsPointer(UUID.randomUUID(), IdentifierConstants.VERSION_ZERO);
    createEmptyKnowledgeBase(vid);
    return Answer.of(vid);
  }

  @Override
  public Answer<Pointer> initKnowledgeBase(KnowledgeCarrier initialComponent) {
    ResourceIdentifier kbaseId = initialComponent.getAssetId();

    if (! knowledgeBaseMap.containsKey(kbaseId.asKey())) {
      createEmptyKnowledgeBase(kbaseId);
    } else {
      return Answer.failed(
          new IllegalStateException("The KB with ID " + kbaseId.getTag()  + ":"
              + kbaseId.getVersionTag() + " is already initialized"));
    }

    if (initialComponent.getExpression() == null
        && initialComponent.getArtifactId() != null) {
      getCanonicalArtifact(initialComponent)
          .ifPresent(resolvedComponent
              -> populateKnowledgeBase(
              kbaseId.getUuid(),
              kbaseId.getVersionTag(),
              resolvedComponent));
    } else {
      populateKnowledgeBase(
          kbaseId.getUuid(),
          kbaseId.getVersionTag(),
          initialComponent);
    }

    return Answer.of(kbaseId.toPointer());
  }

  private Answer<KnowledgeCarrier> getCanonicalArtifact(KnowledgeCarrier kc) {
    ResourceIdentifier assetVid = kc.getAssetId();
    ResourceIdentifier artifVid = kc.getArtifactId();

    if (isLocal(kc)) {
      return assetRepository.getKnowledgeAssetCarrierVersion(
          assetVid.getUuid(),
          assetVid.getVersionTag(),
          artifVid.getUuid(),
          artifVid.getVersionTag());
    } else {
      return Answer.of(kc);
    }
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
        .withKbaseId(kBase.getKbaseId().withHref(endpoint));
  }

  protected KnowledgeBase configureAsLocalKB(KnowledgeBase kBase, KnowledgeCarrier kc) {
    return kBase
        .withManifestation(kc)
        .withKbaseId(kBase.getKbaseId().withHref(null));
  }


  @Override
  public Answer<Pointer> bind(UUID kbaseId, String versionTag, Bindings bindings) {
    return null;
  }

  @Override
  public Answer<Pointer> namedBind(UUID kbaseId, String versionTag, UUID operatorId,
      Bindings bindings) {
    return null;
  }

  @Override
  public Answer<Pointer> namedWeave(UUID kbaseId, String versionTag, UUID operatorId,
      KnowledgeCarrier aspects) {
    return weaver.namedWeave(kbaseId,versionTag,operatorId,aspects);
  }

  @Override
  public Answer<Pointer> weave(UUID kbaseId, String versionTag, KnowledgeCarrier aspects) {
    return weaver.weave(kbaseId,versionTag,aspects);
  }

  protected boolean isLocal(KnowledgeArtifact artifactSurrogate) {
    return artifactSurrogate.getLocator() == null
        || Util.isEmpty(artifactSurrogate.getLocator().getScheme());
  }
  protected boolean isLocal(KnowledgeCarrier carrier) {
    return carrier.getHref() == null
        || Util.isEmpty(carrier.getHref().getScheme())
        || "localhost".equals(carrier.getHref().getHost());
  }

  protected KnowledgeAssetRepositoryApiInternal getAssetRepository() {
    return assetRepository;
  }

}
