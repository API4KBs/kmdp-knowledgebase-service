package edu.mayo.kmdp.examples._7.inference;

import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_1;

import edu.mayo.kmdp.kbase.inference.AbstractEvaluatorProvider;
import org.omg.spec.api4kp._20200801.api.inference.v4.server.ReasoningApiInternal._evaluate;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.springframework.beans.factory.annotation.Autowired;

public class DMNEngineProvider
    extends AbstractEvaluatorProvider {

  public DMNEngineProvider(
      @KPServer
      @Autowired
          KnowledgeBaseApiInternal kbaseManager) {
    super(kbaseManager);
  }

  protected boolean supportsRepresentation(KnowledgeAsset knowledgeAsset) {
    return detectLanguage(knowledgeAsset)
        .map(lang -> lang.sameAs(DMN_1_1))
        .orElse(false);
  }

  @Override
  protected _evaluate getEvaluator(KnowledgeCarrier knowledgeAsset) {
    ResourceIdentifier assetId = knowledgeAsset.getAssetId();
    if (kbase.hasKnowledgeBase(assetId.getUuid(),assetId.getVersionTag()).isFailure()) {
      kbase.initKnowledgeBase(knowledgeAsset, null);
    }
    return kbase.getKnowledgeBase(assetId.getUuid(),assetId.getVersionTag())
        .map(ExampleDMNEvaluator::new)
        .orElseThrow(UnsupportedOperationException::new);
  }
}
