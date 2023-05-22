package edu.mayo.kmdp.knowledgebase.flatteners.fhir.stu3;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.randomId;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;

import edu.mayo.kmdp.knowledgebase.AbstractKnowledgeBaseOperator;
import edu.mayo.kmdp.language.parsers.fhir.stu3.FHIR3Deserializer;
import edu.mayo.kmdp.util.StreamUtil;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.CompositionalApiInternal;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal._applyLift;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;

public abstract class AbstractFhirFlattener<T extends DomainResource>
    extends AbstractKnowledgeBaseOperator
    implements CompositionalApiInternal._flattenArtifact {

  private static final FHIR3Deserializer parser = new FHIR3Deserializer();

  protected AbstractFhirFlattener(ResourceIdentifier newId) {
    super(newId);
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return FHIR_STU3;
  }

  public Answer<KnowledgeCarrier> flattenArtifact(
      KnowledgeCarrier carrier, UUID rootAssetId, String params) {
    if (!(carrier instanceof CompositeKnowledgeCarrier)) {
      return Answer.of(carrier);
    }

    return Answer.of(carrier)
        .flatMap(
            ckc -> getParser().applyLift(ckc, Abstract_Knowledge_Expression.getTag(), codedRep(FHIR_STU3),
                null))
        .map(x -> x.components().collect(Collectors.toList()))
        .flatMap(this::flatten);
  }


  protected Answer<KnowledgeCarrier> flatten(List<KnowledgeCarrier> components) {
    Optional<ResourceIdentifier> assetId = combineAssetIds(components);

    if (assetId.isEmpty()) {
      return components.isEmpty()
          ? emptyResource()
          : Answer.failed(new IllegalArgumentException("Unable to determine a combined Asset ID"));
    }

    return emptyResource()
        .map(zero -> combine(zero, components))
        .map(flat -> wrap(assetId.orElse(randomId()), flat));
  }

  private T combine(KnowledgeCarrier zero, List<KnowledgeCarrier> components) {
    Class<T> klass = getComponentType();
    var z = zero.as(klass)
        .orElseThrow(IllegalStateException::new);
    return components.stream()
        .map(kc -> kc.as(klass))
        .flatMap(StreamUtil::trimStream)
        .reduce(z, this::merge);
  }

  private Answer<KnowledgeCarrier> emptyResource() {
    T empty = newInstance();
    return Answer.of(wrap(randomId(), empty));
  }

  protected abstract T merge(T target, T sources);

  protected Optional<ResourceIdentifier> combineAssetIds(List<KnowledgeCarrier> components) {
    return components.stream()
        .map(KnowledgeCarrier::getAssetId)
        .filter(Objects::nonNull)
        .reduce(SemanticIdentifier::hashIdentifiers);
  }

  protected KnowledgeCarrier wrap(ResourceIdentifier assetId, T flat) {
    return AbstractCarrier.ofAst(flat)
        .withAssetId(assetId)
        .withArtifactId(randomId())
        .withRepresentation(rep(FHIR_STU3));
  }

  protected _applyLift getParser() {
    return parser;
  }

  protected abstract Class<T> getComponentType();

  protected abstract T newInstance();
}
