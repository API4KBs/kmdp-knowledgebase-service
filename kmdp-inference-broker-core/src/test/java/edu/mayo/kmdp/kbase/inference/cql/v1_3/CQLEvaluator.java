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
package edu.mayo.kmdp.kbase.inference.cql.v1_3;

import static org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper.canonicalRepresentationOf;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Inference_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HL7_CQL_1_3;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.asEnum;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.cqframework.cql.elm.execution.AccessModifier;
import org.cqframework.cql.elm.execution.FunctionDef;
import org.cqframework.cql.elm.execution.Library;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.PlatformComponentHelper;
import org.omg.spec.api4kp._20200801.api.inference.v4.server.ReasoningApiInternal._evaluate;
import org.omg.spec.api4kp._20200801.datatypes.Bindings;
import org.omg.spec.api4kp._20200801.id.KeyIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeBase;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.opencds.cqf.cql.execution.Context;


@KPOperation(Inference_Task)
@KPSupport(HL7_CQL_1_3)
public class CQLEvaluator
    implements _evaluate {

  private Map<KeyIdentifier, byte[]> artifactCache = new HashMap<>();

  private CQL2ELMTranslatorHelper translator;

  private KnowledgeRepresentationLanguage modelLanguage;

  public CQLEvaluator(KnowledgeBase knowledgeBase) {
    KnowledgeCarrier carrier = knowledgeBase.getManifestation();

    artifactCache.put(
        carrier.getAssetId().asKey(),
        carrier.asBinary().orElseThrow(IllegalStateException::new));

    this.translator = new CQL2ELMTranslatorHelper();
    modelLanguage = detectInformationModel(carrier.getRepresentation())
        .orElse(FHIR_STU3);
  }

  @Override
  public Answer<Bindings> evaluate(UUID modelId, String versionTag, Bindings features, String params) {
    Bindings out = getCarrier(modelId, versionTag)
        .flatMap(translator::cqlToExecutableLibrary)
        .map(lib -> internalEvaluate(lib, initExecutableKB(lib, features).orElse(null)))
        .map(PlatformComponentHelper::asBindings)
        .orElse(new Bindings());

    return Answer.of(out);
  }

  private Optional<KnowledgeRepresentationLanguage> detectInformationModel(
      SyntacticRepresentation asset) {
    // TODO Actually inspect..
    return Optional.of(FHIR_STU3);
  }

  private KnowledgeRepresentationLanguage detectRepresentationLanguage(KnowledgeAsset asset) {
    return canonicalRepresentationOf(asset).getLanguage();
  }

  private Map<String, Object> internalEvaluate(Library lib, Context ctx) {
    Map<String, Object> out = new HashMap<>();
    if (ctx != null) {
      lib.getStatements().getDef().stream()
          .filter(def -> !(def instanceof FunctionDef))
          .forEach(
              def -> {
                Object val = def.evaluate(ctx);
                if (val != null && def.getAccessLevel() == AccessModifier.PUBLIC) {
                  out.put(def.getName(), val);
                }
              });
    }
    return out;
  }


  private Optional<Context> initExecutableKB(Library elm, Map features) {
    Context ctx = new Context(elm);
    ctx.setExpressionCaching(true);

    ctx.registerLibraryLoader(new DefaultFHIRHelperLibraryLoader(translator));

    switch (asEnum(modelLanguage)) {
      case FHIR_STU3:
        ctx.registerDataProvider("http://hl7.org/fhir", new InMemoryFhir3DataProvider(features));
        break;
     default:
        throw new UnsupportedOperationException(
            "Unable to use " + modelLanguage + " in combination with CQL");
    }
    return Optional.of(ctx);
  }


  private Optional<KnowledgeCarrier> getCarrier(UUID modelId, String versionTag) {
    KeyIdentifier key = SemanticIdentifier.newId(modelId, versionTag).asKey();
    return Optional.ofNullable(
        artifactCache.getOrDefault(key, null))
        .map(AbstractCarrier::of);
  }

}
