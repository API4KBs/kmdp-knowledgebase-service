package edu.mayo.kmdp.kbase.inference.cql.v1_3;

import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HL7_CQL_1_3;

import edu.mayo.kmdp.kbase.inference.AbstractEvaluatorProvider;
import javax.inject.Named;
import org.omg.spec.api4kp._20200801.api.inference.v4.server.ReasoningApiInternal._evaluate;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.springframework.beans.factory.annotation.Autowired;

@Named
public class CQLEngineProvider
    extends AbstractEvaluatorProvider {

//  private final KnowledgeAssetRepositoryApiInternal repo;

  public CQLEngineProvider(
      @KPServer
      @Autowired
          KnowledgeBaseApiInternal kbaseManager) {
    super(kbaseManager);
  }

  @Override
  protected _evaluate getEvaluator(KnowledgeCarrier knowledgeAsset) {
    return kbase.initKnowledgeBase(knowledgeAsset, null)
        .flatMap(kbId -> kbase.getKnowledgeBase(kbId.getUuid(),kbId.getVersionTag()))
        .map(CQLEvaluator::new)
        .orElseThrow(UnsupportedOperationException::new);
  }

  @Override
  protected boolean supportsRepresentation(KnowledgeAsset knowledgeAsset) {
    return detectLanguage(knowledgeAsset)
        .map(lang -> lang.sameAs(HL7_CQL_1_3))
        .orElse(false);
  }
}
