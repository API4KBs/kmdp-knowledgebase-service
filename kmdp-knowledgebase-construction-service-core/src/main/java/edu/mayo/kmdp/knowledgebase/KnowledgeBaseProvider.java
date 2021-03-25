package edu.mayo.kmdp.knowledgebase;

import static org.omg.spec.api4kp._20200801.AbstractCompositeCarrier.ofUniformAggregate;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Description_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Knowledge_Resource_Flattening_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Selection_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Transcreation_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Weaving_Task;

import edu.mayo.kmdp.util.Util;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Named;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.KnowledgePlatformOperator;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.CompositionalApiInternal._flattenArtifact;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedBind;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedExtract;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedIntrospect;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedSelect;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedTransform;
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
import org.omg.spec.api4kp._20200801.services.KPComponent;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.omg.spec.api4kp._20200801.services.KnowledgeBase;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.springframework.beans.factory.annotation.Autowired;

@KPServer
@Named
public class KnowledgeBaseProvider
    implements KnowledgeBaseApiInternal {

  private Set<UUID> componentRegistry = new HashSet<>();

  private Map<UUID, _applyNamedWeave> weavers;

  private Map<UUID, _applyNamedBind> binders;

  private Map<UUID, _applyNamedSelect> selectors;

  private Map<UUID, _flattenArtifact> flatteners;

  private Map<UUID, _applyNamedExtract> extractors;

  private Map<UUID, _applyNamedIntrospect> introspectors;

  private Map<UUID, _applyNamedTransform> transcreators;

  private Map<KeyIdentifier, KnowledgeBase> knowledgeBaseMap = new ConcurrentHashMap<>();

  private KnowledgeAssetRepositoryApiInternal assetRepository;

  public KnowledgeBaseProvider(
      KnowledgeAssetRepositoryApiInternal assetRepository) {
    this.assetRepository = assetRepository;
    this.weavers = new ConcurrentHashMap<>();
    this.binders = new ConcurrentHashMap<>();
    this.selectors = new ConcurrentHashMap<>();
    this.flatteners = new ConcurrentHashMap<>();
    this.extractors = new ConcurrentHashMap<>();
    this.introspectors = new ConcurrentHashMap<>();
    this.transcreators = new ConcurrentHashMap<>();
  }

  @Named
  @Autowired
  public KnowledgeBaseProvider(
      @Autowired(required = false) @KPServer
          KnowledgeAssetRepositoryApiInternal assetRepository,
      @Autowired(required = false) @KPOperation(Weaving_Task)
          List<_applyNamedWeave> wvr,
      @Autowired(required = false)
          List<_applyNamedBind> bnd,
      @Autowired(required = false) @KPOperation(Selection_Task)
          List<_applyNamedSelect> sel,
      @Autowired(required = false) @KPOperation(Knowledge_Resource_Flattening_Task)
          List<_flattenArtifact> flt,
      @Autowired(required = false) @KPOperation(Description_Task) @KPComponent
          List<_applyNamedIntrospect> inx,
      @Autowired(required = false) @KPOperation(Transcreation_Task) @KPComponent
          List<_applyNamedTransform> trx) {
    this(assetRepository);
    if (wvr != null) {
      wvr.forEach(this::withNamedWeaver);
    }
    if (bnd != null) {
      bnd.forEach(this::withNamedBinder);
    }
    if (sel != null) {
      sel.forEach(this::withNamedSelector);
    }
    if (flt != null) {
      flt.forEach(this::withNamedFlattener);
    }
    if (inx != null) {
      inx.forEach(this::withNamedIntrospector);
    }
    if (trx != null) {
      trx.forEach(this::withNamedTranscreator);
    }
  }

  public KnowledgeBaseProvider withNamedWeaver(_applyNamedWeave weaver) {
    return register(weaver,this.weavers);
  }

  public KnowledgeBaseProvider withNamedWeaver(
      Function<KnowledgeBaseProvider,_applyNamedWeave> weaver) {
    return withNamedWeaver(weaver.apply(this));
  }

  public KnowledgeBaseProvider withNamedBinder(_applyNamedBind binder) {
    return register(binder,this.binders);
  }
  
  public KnowledgeBaseProvider withNamedBinder(
      Function<KnowledgeBaseProvider,_applyNamedBind> binder) {
    return withNamedBinder(binder.apply(this));
  }

  public KnowledgeBaseProvider withNamedSelector(_applyNamedSelect selector) {
    return register(selector,this.selectors);
  }

  public KnowledgeBaseProvider withNamedSelector(
      Function<KnowledgeBaseProvider,_applyNamedSelect> selector) {
    return withNamedSelector(selector.apply(this));
  }

  public KnowledgeBaseProvider withNamedFlattener(_flattenArtifact flattener) {
    return register(flattener,this.flatteners);
  }

  public KnowledgeBaseProvider withNamedFlattener(
      Function<KnowledgeBaseProvider,_flattenArtifact> flattener) {
    return withNamedFlattener(flattener.apply(this));
  }

  public KnowledgeBaseProvider withNamedExtractor(_applyNamedExtract extractor) {
    return register(extractor,this.extractors);
  }

  public KnowledgeBaseProvider withNamedExtractor(
      Function<KnowledgeBaseProvider,_applyNamedExtract> extractor) {
    return withNamedExtractor(extractor.apply(this));
  }

  public KnowledgeBaseProvider withNamedIntrospector(_applyNamedIntrospect introspector) {
    return register(introspector,this.introspectors);
  }

  public KnowledgeBaseProvider withNamedIntrospector(
      Function<KnowledgeBaseProvider,_applyNamedIntrospect> introspector) {
    return withNamedIntrospector(introspector.apply(this));
  }

  public KnowledgeBaseProvider withNamedTranscreator(_applyNamedTransform transcreator) {
    return register(transcreator,this.transcreators);
  }

  public KnowledgeBaseProvider withNamedTranscreator(
      Function<KnowledgeBaseProvider,_applyNamedTransform> transcreator) {
    return withNamedTranscreator(transcreator.apply(this));
  }

  private <T> KnowledgeBaseProvider register(T comp, Map<UUID, T> map) {
    AbstractKnowledgeBaseOperator op = (AbstractKnowledgeBaseOperator) comp;
    UUID key = op.getOperatorId().getUuid();
    map.put(key, comp);
    op.withKBManager(this);
    componentRegistry.add(key);
    return this;
  }


  @Override
  public Answer<Void> deleteKnowledgeBase(final UUID kbaseId, String params) {
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
  public Answer<KnowledgeBase> getKnowledgeBase(UUID kbaseId, String versionTag, String params) {
    ResourceIdentifier vid = SemanticIdentifier.newId(kbaseId, versionTag);
    return Answer.ofNullable(knowledgeBaseMap.get(vid.asKey()));
  }

  @Override
  public Answer<KnowledgeCarrier> getKnowledgeBaseManifestation(UUID kbaseId, String versionTag, String params) {
    return getKnowledgeBase(kbaseId, versionTag, params)
        .map(KnowledgeBase::getManifestation);
  }

  @Override
  public Answer<List<Pointer>> getKnowledgeBaseSeries(UUID kbaseId, String params) {
    return Answer.of(
        knowledgeBaseMap.keySet().stream()
            .filter(vid -> kbaseId.equals(vid.getUuid()))
            .map(vid -> knowledgeBaseMap.get(vid))
            .map(KnowledgeBase::getKbaseId)
            .collect(Collectors.toList()));
  }

  @Override
  public Answer<KnowledgeCarrier> getKnowledgeBaseStructure(UUID kbaseId, String versionTag, String params) {
    return getKnowledgeBase(kbaseId, versionTag)
        .map(KnowledgeBase::getManifestation)
        .filter(CompositeKnowledgeCarrier.class::isInstance)
        .map(CompositeKnowledgeCarrier.class::cast)
        .map(CompositeKnowledgeCarrier::getStruct);
  }


  @Override
  public Answer<Void> hasKnowledgeBase(UUID kbaseId, String versionTag, String params) {
    return knowledgeBaseMap.containsKey(SemanticIdentifier.newId(kbaseId,versionTag).asKey())
        ? Answer.succeed()
        : Answer.notFound();
  }

  @Override
  public Answer<Pointer> initKnowledgeBase(KnowledgeCarrier initialComponent, String params) {
    return initialComponent != null
        ? seedKnowledgeBase(initialComponent)
        : initBlankKnowledgeBase();
  }

  protected Answer<Pointer> initBlankKnowledgeBase() {
    Pointer vid = SemanticIdentifier
        .newIdAsPointer(UUID.randomUUID(), IdentifierConstants.VERSION_ZERO);
    createEmptyKnowledgeBase(vid);
    return Answer.of(vid);
  }

  public Answer<Pointer> seedKnowledgeBase(KnowledgeCarrier initialComponent) {
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
      KnowledgeCarrier sourceArtifact, String params) {
    Pointer versionedId = SemanticIdentifier.newIdAsPointer(kbaseId, versionTag)
        .withName(sourceArtifact.getLabel());

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
      KnowledgeCarrier struct, String params) {
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
  public Answer<List<Pointer>> getKnowledgeBaseComponents(UUID kbaseId, String versionTag, String params) {
    return getKnowledgeBaseManifestation(kbaseId, versionTag, params)
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
      if (ckc.getStructType() == CompositeStructType.NONE) {
        ckc.getComponent().add(sourceArtifact);
      } else {
        throw new UnsupportedOperationException();
      }
    } else {
      kBase.setManifestation(
          ofUniformAggregate(
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
  public Answer<KnowledgeBase> nextKnowledgeBaseVersion(UUID kbaseId, String baseVersionTag, String params) {
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
    return nextKnowledgeBaseVersion(kbaseId, versionTag, null)
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
    return nextKnowledgeBaseVersion(kbaseId, versionTag, null)
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
    return nextKnowledgeBaseVersion(kbaseId, versionTag, null)
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
    return nextKnowledgeBaseVersion(kbaseId, versionTag, null)
        .flatMap(kb ->
            flattener.flattenArtifact(
                kb.getManifestation(),
                null, null)
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
    return nextKnowledgeBaseVersion(kbaseId, versionTag, null)
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


  @Override
  public Answer<Pointer> introspect(UUID kbaseId, String versionTag, String xParams) {
    return Answer.anyDo(
        introspectors.values(),
        introspectorComponent ->
            introspect(
                kbaseId,
                versionTag,
                xParams));
  }

  @Override
  public Answer<Pointer> namedIntrospect(UUID kbaseId, String versionTag, UUID operatorId,
      String xParams) {
    if (!introspectors.containsKey(operatorId)) {
      return Answer.unsupported();
    }

    _applyNamedIntrospect introspect = introspectors.get(operatorId);
    return nextKnowledgeBaseVersion(kbaseId, versionTag, null)
        .flatMap(kb ->
            introspect.applyNamedIntrospect(
                operatorId,
                kbaseId,
                versionTag,
                xParams)
                .map(kb::withManifestation)
                .or(() -> deleteKnowledgeBaseVersion(kb))
        ).map(KnowledgeBase::getKbaseId);
  }


  @Override
  public Answer<Pointer> transform(UUID kbaseId, String versionTag, String xParams) {
    return Answer.anyDo(
        transcreators.values(),
        transcreatorComponent ->
            transform(
                kbaseId,
                versionTag,
                xParams));
  }

  @Override
  public Answer<Pointer> namedTransform(UUID kbaseId, String versionTag, UUID operatorId,
      String xParams) {
    if (!transcreators.containsKey(operatorId)) {
      return Answer.unsupported();
    }

    _applyNamedTransform introspect = transcreators.get(operatorId);
    return nextKnowledgeBaseVersion(kbaseId, versionTag, null)
        .flatMap(kb ->
            introspect.applyNamedTransform(
                operatorId,
                kbaseId,
                versionTag,
                xParams)
                .map(kb::withManifestation)
                .or(() -> deleteKnowledgeBaseVersion(kb))
        ).map(KnowledgeBase::getKbaseId);
  }

  public boolean hasNamedComponent(UUID id) {
    return componentRegistry.contains(id);
  }
}
