/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package edu.mayo.kmdp.kbase.inference;

import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Inference_Task;

import edu.mayo.kmdp.util.StreamUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import javax.inject.Named;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.inference.v4.server.ModelApiInternal;
import org.omg.spec.api4kp._20200801.api.inference.v4.server.ReasoningApiInternal;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetCatalogApiInternal;
import org.omg.spec.api4kp._20200801.datatypes.Bindings;
import org.omg.spec.api4kp._20200801.id.KeyIdentifier;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.springframework.beans.factory.annotation.Autowired;

@KPServer
@Named
@KPOperation(Inference_Task)
public class StandardsInferenceBroker implements ReasoningApiInternal, ModelApiInternal {

  private java.util.Map<KeyIdentifier, KnowledgeAsset> knownModels = new HashMap<>();
  private KnowledgeAssetCatalogApiInternal assetCatalog;

  private Set<Function<KnowledgeAsset, Optional<_evaluate>>> evaluatorProviders;

  @Autowired
  public StandardsInferenceBroker(
      @KPServer KnowledgeAssetCatalogApiInternal assetCatalog,
      Set<Function<KnowledgeAsset, Optional<_evaluate>>> evaluatorProviders) {
    this.assetCatalog = assetCatalog;
    this.evaluatorProviders = evaluatorProviders;
  }

  @Override
  @SuppressWarnings("rawtypes")
  public Answer<Bindings> evaluate(UUID modelId, String versionTag, Bindings inputFeatures, String params) {
    // Broker pattern
    return getEvaluator(modelId, versionTag)
        .flatMap(evaluator -> evaluator.evaluate(modelId, versionTag, inputFeatures, params));
  }


  private Answer<_evaluate> getEvaluator(UUID modelId, String versionTag) {
    KeyIdentifier vid = SemanticIdentifier.newId(modelId, versionTag).asKey();
    KnowledgeAsset asset;

    if (! knownModels.containsKey(vid)) {
      Answer<KnowledgeAsset> surr = assetCatalog
          .getKnowledgeAssetVersion(modelId, versionTag);
      if (! surr.isSuccess()) {
        return Answer.notFound();
      }
      asset = surr.get();
      knownModels.put(vid,asset);
    } else {
      asset = knownModels.get(vid);
    }

    return Answer.of(
        evaluatorProviders.stream()
            .map(provider -> provider.apply(asset))
            .flatMap(StreamUtil::trimStream)
            .findFirst());
  }





  @Override
  public Answer<KnowledgeCarrier> getModel(UUID modelId, String versionTag) {
    return Answer.unsupported();
  }


  @Override
  public Answer<List<Pointer>> listModels() {
    return assetCatalog.listKnowledgeAssets();
  }
}
