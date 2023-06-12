package edu.mayo.kmdp.language.ccg.library.mock;

import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.Defines;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newKey;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;

import edu.mayo.kmdp.language.parsers.surrogate.v2.Surrogate2Parser;
import edu.mayo.kmdp.util.StreamUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.AbstractCompositeCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetCatalogApiInternal;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetRepositoryApiInternal;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal._applyLift;
import org.omg.spec.api4kp._20200801.id.KeyIdentifier;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.Dependency;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper;

public class MockGLAssetRepo implements KnowledgeAssetRepositoryApiInternal,
    KnowledgeAssetCatalogApiInternal {

  private final Map<KeyIdentifier, AssetPair> repo = new ConcurrentHashMap<>();

  private _applyLift parser = new Surrogate2Parser();

  public MockGLAssetRepo() {

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
    if (!repo.containsKey(id)) {
      return Answer.notFound();
    }
    var ap = repo.get(id);
    return this.getCarrier(ap)
        .or(() -> this.getInline(ap));
  }

  private Answer<? extends KnowledgeCarrier> getInline(AssetPair ap) {
    return Answer.ofNullable(ap.surrogate.getCarriers())
        .filter(k -> !k.isEmpty())
        .map(k -> k.get(0))
        .flatMap(k -> Answer.ofNullable(k.getInlinedExpression())
            .map(AbstractCarrier::of));
  }

  private Answer<KnowledgeCarrier> getCarrier(AssetPair ap) {
    return Answer.ofNullable(ap.artifact)
        .map(l -> l.get(0));
  }

  @Override
  public Answer<Void> addCanonicalKnowledgeAssetSurrogate(UUID assetId, String versionTag,
      KnowledgeCarrier body) {
    body.components()
        .map(kc -> parser.applyLift(kc, Abstract_Knowledge_Expression.getTag(),
            codedRep(Knowledge_Asset_Surrogate_2_0), null).orElseGet(Assertions::fail))
        .forEach(kc -> {
          var surr = kc.as(KnowledgeAsset.class)
              .orElseGet(Assertions::fail);
          setKnowledgeAssetVersion(
              surr.getAssetId().getUuid(),
              surr.getAssetId().getVersionTag(),
              surr);
        });
    return Answer.succeed();
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
  public Answer<List<Pointer>> listKnowledgeAssets(
      String typeTag,
      String anno, String annValue,
      Integer offset, Integer limit
  ) {
    var ids = repo.entrySet().stream()
        .filter(x -> typeTag == null
            || x.getValue().surrogate.getRole().stream()
            .anyMatch(role -> role.getTag().equals(typeTag))
            || x.getValue().surrogate.getFormalType().stream()
            .anyMatch(type -> type.getTag().equals(typeTag)));

    if (Defines.getTag().equals(anno)) {
      ids = ids.filter(e -> e.getValue().surrogate.getAnnotation().stream()
          .anyMatch(ann -> ann.getRef().getResourceId().toString().equals(annValue)));
    }
    var ptrs = ids
        .map(Entry::getKey)
        .map(ki -> SemanticIdentifier.newId(ki.getUuid(), ki.getVersionTag()))
        .map(id -> id.toPointer()
            .withHref(id.getVersionId()));
    return Answer.of(ptrs.collect(Collectors.toList()));
  }

  @Override
  public Answer<CompositeKnowledgeCarrier> getAnonymousCompositeKnowledgeAssetSurrogate(
      UUID assetId, String versionTag, String xAccept) {
    var id = newId(assetId, versionTag);
    var surrogates = getWithDependencies(assetId, versionTag)
        .map(SurrogateHelper::carry)
        .collect(Collectors.toList());
    return Answer.of(AbstractCompositeCarrier.ofUniformAggregate(surrogates)
        .withRootId(id));
  }

  private Stream<KnowledgeAsset> getWithDependencies(UUID assetId, String versionTag) {
    var root = getKnowledgeAssetVersion(assetId, versionTag);
    if (root.isFailure()) {
      return Stream.empty();
    }
    var deps = root.get().getLinks().stream()
        .flatMap(StreamUtil.filterAs(Dependency.class))
        .map(Dependency::getHref)
        .flatMap(r -> getWithDependencies(r.getUuid(), r.getVersionTag()))
        .filter(Objects::nonNull);
    return Stream.concat(
        Stream.of(root.get()), deps);
  }

  public Answer<CompositeKnowledgeCarrier> getAnonymousCompositeKnowledgeAssetCarrier(
      UUID assetId, String versionTag, String xAccept) {
    var id = newId(assetId, versionTag);
    var carriers = getWithDependencies(assetId, versionTag)
        .map(ka -> getKnowledgeAssetVersionCanonicalCarrier(
            ka.getAssetId().getUuid(), ka.getAssetId().getVersionTag(), null).orElse(null))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    return Answer.of(AbstractCompositeCarrier.ofMixedAnonymousComposite(carriers)
        .withRootId(id));
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
    } else if (!surrogate.getCarriers().isEmpty()
        && surrogate.getCarriers().get(0).getInlinedExpression() != null) {
      var ka = surrogate.getCarriers().get(0);
      var kc = AbstractCarrier.of(ka.getInlinedExpression())
          .withAssetId(surrogateId)
          .withArtifactId(ka.getArtifactId())
          .withRepresentation(ka.getRepresentation());
      repo.get(surrogateId.asKey()).artifact.add(kc);
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
