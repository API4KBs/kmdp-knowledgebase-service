package edu.mayo.kmdp.ops.tranx.bpm;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;

import edu.mayo.kmdp.language.parsers.fhir.stu3.FHIR3Deserializer;
import java.net.URI;
import java.nio.charset.Charset;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.inference.v4.server.ReasoningApiInternal._askQuery;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetCatalogApi;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetRepositoryApi;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal._applyLower;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevel;
import org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries;

/**
 * Implementation of the {@link CcpmToPlanDefPipeline} based on the KARS
 * #getAnonymousCompositeKnowledgeAssetCarrier operation
 * <p>
 * Assumes connection to an Asset Repository that implements the operation
 */
public class KarsAnonymousCcpmToPlanDefPipeline extends CcpmToPlanDefPipeline {

  protected static final _applyLower serializer = new FHIR3Deserializer();

  public KarsAnonymousCcpmToPlanDefPipeline(
      KnowledgeAssetCatalogApi cat,
      KnowledgeAssetRepositoryApi repo,
      _askQuery dataShapeQuery,
      URI... annotationVocabularies) {
    super(cat, repo, dataShapeQuery, annotationVocabularies);
  }

  @Override
  public Answer<Pointer> initKnowledgeBase(KnowledgeCarrier kc, String params) {
    var ckc = (CompositeKnowledgeCarrier) kc;
    ResourceIdentifier rootId = ckc.getRootId();
    return repo.getAnonymousCompositeKnowledgeAssetCarrier(rootId.getUuid(), rootId.getVersionTag())
        .map(anon -> anon.withAssetId(ckc.getAssetId()))
        .flatWhole(m -> kbManager.initKnowledgeBase(m, params));
  }

  /**
   * Runs the pipeline, invoking KARS#getAnonymousCompositeKnowledgeAssetCarrier with the given root
   * ID to retrieve the input models, then returns the PlanDefinition at the given parsing level
   *
   * @param assetId the Asset ID of the transcreated composite PlanDefinition
   * @param rootId the Asset ID of the root Case Model
   * @param level  the Serialization level to return the PlanDefinition at
   * @return a composite PlanDefinition for the cCPM with the given root case model, serialized to
   * the given level
   */
  public Answer<KnowledgeCarrier> trigger(
      ResourceIdentifier assetId,
      ResourceIdentifier rootId,
      ParsingLevel level) {
    return initKnowledgeBase(
        new CompositeKnowledgeCarrier()
            .withAssetId(assetId)
            .withRootId(rootId), null)
        .flatMap(ptr ->
            this.applyNamedTransform(
                CcpmToPlanDefPipeline.id, ptr.getUuid(), ptr.getVersionTag(), null))
        .flatMap(pd -> serializer.applyLower(
            pd,
            level.getTag(),
            getRep(level),
            null));
  }

  private String getRep(ParsingLevel level) {
    switch (ParsingLevelSeries.asEnum(level)) {
      case Abstract_Knowledge_Expression:
        return codedRep(FHIR_STU3);
      case Concrete_Knowledge_Expression:
        return codedRep(FHIR_STU3, JSON);
      case Serialized_Knowledge_Expression:
        return codedRep(FHIR_STU3, JSON, Charset.defaultCharset());
      case Encoded_Knowledge_Expression:
        return codedRep(FHIR_STU3, JSON, Charset.defaultCharset(), Encodings.DEFAULT);
      default:
        throw new UnsupportedOperationException("Unknown parsing level : " + level);
    }
  }

}