package edu.mayo.kmdp.knowledgebase;

import edu.mayo.kmdp.util.Util;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Named;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.KnowledgePlatformOperator;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.CompositionalApiInternal._flattenArtifact;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedBind;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedExtract;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedSelect;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedWeave;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetRepositoryApiInternal;
import org.omg.spec.api4kp._20200801.datatypes.Bindings;
import org.omg.spec.api4kp._20200801.id.IdentifierConstants;
import org.omg.spec.api4kp._20200801.id.KeyIdentifier;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.CompositeStructType;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.omg.spec.api4kp._20200801.services.KnowledgeBase;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.springframework.beans.factory.annotation.Autowired;

@KPServer
@Named
public class KnowledgeBaseProvider
    implements KnowledgeBaseApiInternal {

  private Map<UUID, _applyNamedWeave> weavers = new HashMap<>();

  private Map<UUID, _applyNamedBind> binders = new HashMap<>();

  private Map<UUID, _applyNamedSelect> selectors = new HashMap<>();

  private Map<UUID, _flattenArtifact> flatteners = new HashMap<>();

  private Map<UUID, _applyNamedExtract> extractors = new HashMap<>();

  private Map<KeyIdentifier, KnowledgeBase> knowledgeBaseMap = new ConcurrentHashMap<>();

  private KnowledgeAssetRepositoryApiInternal assetRepository;

  public KnowledgeBaseProvider() {
  }

  public KnowledgeBaseProvider(
      KnowledgeAssetRepositoryApiInternal assetRepository) {
    this.assetRepository = assetRepository;
  }

  @Autowired(required = false)
  public KnowledgeBaseProvider(
      @KPServer KnowledgeAssetRepositoryApiInternal assetRepository,
      List<_applyNamedWeave> weavers,
      List<_applyNamedBind> binders,
      List<_applyNamedSelect> selectors,
      List<_flattenArtifact> flatteners) {
    this.assetRepository = assetRepository;
    weavers.forEach(this::withNamedWeaver);
    binders.forEach(this::withNamedBinder);
    selectors.forEach(this::withNamedSelector);
    flatteners.forEach(this::withNamedFlattener);
  }

  public KnowledgeBaseProvider withNamedWeaver(_applyNamedWeave weaver) {
    UUID key = ((KnowledgePlatformOperator<?>) weaver).getOperatorId().getUuid();
    this.weavers.put(key, weaver);
    return this;
  }

  public KnowledgeBaseProvider withNamedWeaver(
      Function<KnowledgeBaseProvider,_applyNamedWeave> weaver) {
    return withNamedWeaver(weaver.apply(this));
  }

  public KnowledgeBaseProvider withNamedBinder(_applyNamedBind binder) {
    UUID key = ((KnowledgePlatformOperator<?>) binder).getOperatorId().getUuid();
    this.binders.put(key, binder);
    return this;
  }
  
  public KnowledgeBaseProvider withNamedBinder(
      Function<KnowledgeBaseProvider,_applyNamedBind> binder) {
    return withNamedBinder(binder.apply(this));
  }

  public KnowledgeBaseProvider withNamedSelector(_applyNamedSelect selector) {
    UUID key = ((KnowledgePlatformOperator<?>) selector).getOperatorId().getUuid();
    this.selectors.put(key, selector);
    return this;
  }

  public KnowledgeBaseProvider withNamedSelector(
      Function<KnowledgeBaseProvider,_applyNamedSelect> selector) {
    return withNamedSelector(selector.apply(this));
  }

  public KnowledgeBaseProvider withNamedFlattener(_flattenArtifact flattener) {
    UUID key = ((KnowledgePlatformOperator<?>) flattener).getOperatorId().getUuid();
    this.flatteners.put(key, flattener);
    return this;
  }

  public KnowledgeBaseProvider withNamedFlattener(
      Function<KnowledgeBaseProvider,_flattenArtifact> flattener) {
    return withNamedFlattener(flattener.apply(this));
  }

  public KnowledgeBaseProvider withNamedExtractor(_applyNamedExtract extractor) {
    UUID key = ((KnowledgePlatformOperator<?>) extractor).getOperatorId().getUuid();
    this.extractors.put(key, extractor);
    return this;
  }

  public KnowledgeBaseProvider withNamedExtractor(
      Function<KnowledgeBaseProvider,_applyNamedExtract> extractor) {
    return withNamedExtractor(extractor.apply(this));
  }


  @Override
  public Answer<Void> deleteKnowledgeBase(final UUID kbaseId) {
    knowledgeBaseMap.keySet().stream()
        .filter(vid -> kbaseId.equals(vid.getUuid()))
        .forEach(knowledgeBaseMap::remove);
    return Answer.succeed();
  }

  private Answer<KnowledgeBase> deleteKnowledgeBaseVersion(KnowledgeBase kb) {
    knowledgeBaseMap.remove(kb.getKbaseId().asKey());
    return Answer.notFound();
  }

  @Override
  public Answer<KnowledgeBase> getKnowledgeBase(UUID kbaseId, String versionTag) {
    ResourceIdentifier vid = SemanticIdentifier.newId(kbaseId, versionTag);
    return Answer.ofNullable(knowledgeBaseMap.get(vid.asKey()));
  }

  @Override
  public Answer<KnowledgeCarrier> getKnowledgeBaseManifestation(UUID kbaseId, String versionTag) {
    return getKnowledgeBase(kbaseId, versionTag)
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
    return getKnowledgeBase(kbaseId, versionTag)
        .map(KnowledgeBase::getManifestation)
        .filter(CompositeKnowledgeCarrier.class::isInstance)
        .map(CompositeKnowledgeCarrier.class::cast)
        .map(CompositeKnowledgeCarrier::getStruct);
  }

  @Override
  public Answer<Pointer> initKnowledgeBase() {
    Pointer vid = SemanticIdentifier
        .newIdAsPointer(UUID.randomUUID(), IdentifierConstants.VERSION_ZERO);
    createEmptyKnowledgeBase(vid);
    return Answer.of(vid);
  }

  @Override
  public Answer<Pointer> initKnowledgeBase(KnowledgeCarrier initialComponent) {
    ResourceIdentifier kbaseId = initialComponent.getAssetId();

    if (!knowledgeBaseMap.containsKey(kbaseId.asKey())) {
      createEmptyKnowledgeBase(kbaseId);
    } else {
      return Answer.failed(
          new IllegalStateException("The KB with ID " + kbaseId.getTag() + ":"
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

    if (isLocal(kc) && assetRepository != null) {
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

    KnowledgeBase kBase = knowledgeBaseMap.get(versionedId.asKey());
    if (kBase == null) {
      return Answer.notFound();
    }
    if (kBase.getManifestation() == null) {
      initKBContent(kBase,sourceArtifact);
    } else {
      addToKBContent(kBase,sourceArtifact);
    }
    return Answer.of(versionedId);
  }

  @Override
  public Answer<Pointer> setKnowledgeBaseStructure(UUID kbaseId, String versionTag,
      KnowledgeCarrier struct) {
    return getKnowledgeBase(kbaseId,versionTag)
        .map(kb -> {
          if (kb.getManifestation() instanceof CompositeKnowledgeCarrier) {
            ((CompositeKnowledgeCarrier) kb.getManifestation())
                .withStruct(struct)
                .withStructType(CompositeStructType.GRAPH);
          }
          return kb.getKbaseId();
        });
  }

  @Override
  public Answer<List<Pointer>> getKnowledgeBaseComponents(UUID kbaseId, String versionTag) {
    return getKnowledgeBaseManifestation(kbaseId, versionTag)
        .map(kc -> kc.components()
            .map(KnowledgeCarrier::getAssetId)
            .map(SemanticIdentifier::toPointer)
            .collect(Collectors.toList()));
  }

  private void addToKBContent(KnowledgeBase kBase, KnowledgeCarrier sourceArtifact) {
    //TODO work in progress..
    KnowledgeCarrier kc = kBase.getManifestation();
    if (kc == null) {
      throw new IllegalStateException("The KB should not be null");
    }
    if (kc instanceof CompositeKnowledgeCarrier) {
      CompositeKnowledgeCarrier ckc = (CompositeKnowledgeCarrier) kc;
      if (ckc.getStructType() == CompositeStructType.SET) {
        ckc.getComponent().add(sourceArtifact);
      } else {
        throw new UnsupportedOperationException();
      }
    } else {
      kBase.setManifestation(
          AbstractCarrier.ofIdentifiableSet(
              Arrays.asList(kc, sourceArtifact)));
    }
  }

  private void initKBContent(KnowledgeBase kBase, KnowledgeCarrier sourceArtifact) {
    if (isLocal(sourceArtifact)) {
      configureAsLocalKB(kBase, sourceArtifact);
    } else {
      configureAsRemoteKB(kBase, sourceArtifact.getHref());
    }
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



  protected boolean isLocal(KnowledgeArtifact artifactSurrogate) {
    return artifactSurrogate.getLocator() == null
        || Util.isEmpty(artifactSurrogate.getLocator().getScheme());
  }

  protected boolean isLocal(KnowledgeCarrier carrier) {
    return carrier.getHref() == null
        || Util.isEmpty(carrier.getHref().getScheme())
        || "localhost" .equals(carrier.getHref().getHost());
  }

  @Override
  public Answer<KnowledgeBase> nextKnowledgeBaseVersion(UUID kbaseId, String baseVersionTag) {
    return getKnowledgeBase(kbaseId, baseVersionTag)
        .flatMap(currKB -> {
          Pointer vid = SemanticIdentifier.newIdAsPointer(kbaseId, UUID.randomUUID().toString());
          return Answer.of(createEmptyKnowledgeBase(vid)
              .withManifestation(currKB.getManifestation()));
        });
  }

  @Override
  public Answer<Pointer> namedWeave(UUID kbaseId, String versionTag, UUID operatorId,
      KnowledgeCarrier aspects, String xParams) {
    if (!weavers.containsKey(operatorId)) {
      return Answer.unsupported();
    }

    _applyNamedWeave weaver = weavers.get(operatorId);
    return nextKnowledgeBaseVersion(kbaseId, versionTag)
        .flatMap(kb ->
            weaver.applyNamedWeave(
                operatorId,
                aspects,
                kb.getKbaseId().getUuid(),
                kb.getKbaseId().getVersionTag(),
                xParams)
                .map(kb::withManifestation)
                .or(() -> deleteKnowledgeBaseVersion(kb))
        ).map(KnowledgeBase::getKbaseId);
  }


  @Override
  public Answer<Pointer> weave(UUID kbaseId, String versionTag, KnowledgeCarrier aspects, String xParams) {
    return Answer.anyDo(
        weavers.values(),
        weaverComponent ->
            namedWeave(
                kbaseId,
                versionTag,
                ((KnowledgePlatformOperator<?>) weaverComponent).getOperatorId().getUuid(),
                aspects,
                xParams));
  }



  @Override
  public Answer<Pointer> bind(UUID kbaseId, String versionTag, Bindings bindings, String xParams) {
    return Answer.anyDo(
        binders.values(),
        binderComponent ->
            namedBind(
                kbaseId,
                versionTag,
                ((KnowledgePlatformOperator<?>) binderComponent).getOperatorId().getUuid(),
                bindings,
                xParams));
  }

  @Override
  public Answer<Pointer> namedBind(UUID kbaseId, String versionTag, UUID operatorId,
      Bindings bindings, String xParams) {
    if (!binders.containsKey(operatorId)) {
      return Answer.unsupported();
    }

    _applyNamedBind binder = binders.get(operatorId);
    return nextKnowledgeBaseVersion(kbaseId, versionTag)
        .flatMap(kb ->
            binder.applyNamedBind(
                operatorId,
                bindings,
                kb.getKbaseId().getUuid(),
                kb.getKbaseId().getVersionTag(),
                xParams)
                .map(kb::withManifestation)
                .or(() -> deleteKnowledgeBaseVersion(kb))
        ).map(KnowledgeBase::getKbaseId);
  }

  @Override
  public Answer<Pointer> select(UUID kbaseId, String versionTag,
      KnowledgeCarrier selectDefinition, String xParams) {
    return Answer.anyDo(
        selectors.values(),
        selectorComponent ->
            namedSelect(
                kbaseId,
                versionTag,
                ((KnowledgePlatformOperator<?>) selectorComponent).getOperatorId().getUuid(),
                selectDefinition,
                xParams));
  }


  @Override
  public Answer<Pointer> namedSelect(UUID kbaseId, String versionTag, UUID operatorId,
      KnowledgeCarrier selectDefinition, String xParams) {
    if (!selectors.containsKey(operatorId)) {
      return Answer.unsupported();
    }

    _applyNamedSelect binder = selectors.get(operatorId);
    return nextKnowledgeBaseVersion(kbaseId, versionTag)
        .flatMap(kb ->
            binder.applyNamedSelect(
                operatorId,
                selectDefinition,
                kb.getKbaseId().getUuid(),
                kb.getKbaseId().getVersionTag(),
                xParams)
                .map(kb::withManifestation)
                .or(() -> deleteKnowledgeBaseVersion(kb))
        ).map(KnowledgeBase::getKbaseId);
  }

  @Override
  public Answer<Pointer> flatten(UUID kbaseId, String versionTag, String xParams) {
    Answer<Pointer> ptr = Answer.anyDo(
        flatteners.values(),
        flattenerComponent ->
            namedFlatten(
                kbaseId,
                versionTag,
                ((KnowledgePlatformOperator<?>) flattenerComponent).getOperatorId().getUuid(),
                xParams));
    if (ptr.isSuccess() && ! Util.isEmpty(xParams) && xParams.contains("clear")) {
      getKnowledgeBase(kbaseId,versionTag).ifPresent(this::deleteKnowledgeBaseVersion);
    }
    return ptr;
  }

  @Override
  public Answer<Pointer> namedFlatten(UUID kbaseId, String versionTag, UUID operatorId,
      String xParams) {
    if (!flatteners.containsKey(operatorId)) {
      return Answer.unsupported();
    }

    _flattenArtifact flattener = flatteners.get(operatorId);
    return nextKnowledgeBaseVersion(kbaseId, versionTag)
        .flatMap(kb ->
            flattener.flattenArtifact(
                kb.getManifestation(),
                null)
                .map(kb::withManifestation)
                .or(() -> deleteKnowledgeBaseVersion(kb))
        ).map(KnowledgeBase::getKbaseId);
  }

  @Override
  public Answer<Pointer> extract(UUID kbaseId, String versionTag, UUID rootAssetid,
      String xParams) {
    return Answer.anyDo(
        extractors.values(),
        extractorComponent ->
            namedExtract(
                kbaseId,
                versionTag,
                rootAssetid,
                ((KnowledgePlatformOperator<?>) extractorComponent).getOperatorId().getUuid(),
                xParams));
  }

  @Override
  public Answer<Pointer> namedExtract(UUID kbaseId, String versionTag, UUID rootAssetid,
      UUID operatorId, String xParams) {
    if (!extractors.containsKey(operatorId)) {
      return Answer.unsupported();
    }

    _applyNamedExtract extract = extractors.get(operatorId);
    return nextKnowledgeBaseVersion(kbaseId, versionTag)
        .flatMap(kb ->
            extract.applyNamedExtract(
                operatorId,
                kbaseId,
                versionTag,
                rootAssetid,
                xParams)
                .map(kb::withManifestation)
                .or(() -> deleteKnowledgeBaseVersion(kb))
        ).map(KnowledgeBase::getKbaseId);
  }
}
