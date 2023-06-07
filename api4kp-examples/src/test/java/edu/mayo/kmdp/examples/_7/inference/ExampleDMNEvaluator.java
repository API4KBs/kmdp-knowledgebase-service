package edu.mayo.kmdp.examples._7.inference;

/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import static edu.mayo.kmdp.util.NameUtils.camelCase;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Inference_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_1;

import edu.mayo.kmdp.util.Util;
import edu.mayo.kmdp.util.fhir.fhir3.FHIR3DataTypeConstructor;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.Base;
import org.hl7.fhir.dstu3.model.DataElement;
import org.hl7.fhir.dstu3.model.ElementDefinition;
import org.hl7.fhir.dstu3.model.ElementDefinition.TypeRefComponent;
import org.hl7.fhir.dstu3.model.Enumerations.DataType;
import org.hl7.fhir.dstu3.model.Type;
import org.kie.api.KieServices;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.Message;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieContainer;
import org.kie.dmn.api.core.DMNContext;
import org.kie.dmn.api.core.DMNModel;
import org.kie.dmn.api.core.DMNResult;
import org.kie.dmn.api.core.DMNRuntime;
import org.kie.dmn.api.core.ast.DecisionNode;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.PlatformComponentHelper;
import org.omg.spec.api4kp._20200801.api.inference.v4.server.ReasoningApiInternal._evaluate;
import org.omg.spec.api4kp._20200801.datatypes.Bindings;
import org.omg.spec.api4kp._20200801.id.ConceptIdentifier;
import org.omg.spec.api4kp._20200801.id.Term;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeBase;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.terms.ConceptScheme;


@KPSupport(DMN_1_1)
@KPOperation(Inference_Task)
public class ExampleDMNEvaluator implements _evaluate {

  private final DMNRuntime runtime;

  public ExampleDMNEvaluator(KnowledgeBase knowledgeBase) {
    runtime = initRuntime(knowledgeBase);
  }

  @Override
  public Answer<Bindings> evaluate(UUID modelId, String versionTag,
      Bindings features, String params) {
    DMNModel model = resolveModel(modelId, versionTag);

    Map<Term, Object> concepts = resolveConcepts(features);
    DMNContext ctx = bindInputs(concepts, model);
    DMNResult res = runtime.evaluateAll(model, ctx);
//    Map<Term, Object> out = bindOutputs(res.getContext());
    Map<String, Object> out = extractOutputs(res.getContext());
    return Answer.of(PlatformComponentHelper.asBindings(out));
  }


  /**
   * Resolve model by versioned ID Assumption : the ID resolves to a Semantic Decision Model Asset
   * expressed in DMN
   */
  private DMNModel resolveModel(UUID modelId, String versionTag) {
    // TODO
    return runtime.getModels().get(0);
  }

  /**
   * Resolve the URIs of the input data concepts Maybe TODO : Validate that the 'Object' value
   * conforms to the canonical schema associated to its data concept
   */
  private java.util.Map<Term, Object> resolveConcepts(
      ConceptScheme<Term> scheme,
      java.util.Map<String, Object> features) {
    java.util.Map<Term, Object> resolved = new HashMap<>();

    features.forEach((k, v) -> {
      Optional<Term> pc = resolveConceptByUUID(scheme, k);
      if (pc.isPresent()) {
        resolved.put(pc.get(), v);
      } else {
        resolveConceptByLabel(scheme, k)
            .ifPresent(pcl -> resolved.put(pcl, v));
      }
    });

    return resolved;
  }

  private java.util.Map<Term, Object> resolveConcepts(Map<String, Object> features) {
    return features.keySet().stream()
        .collect(Collectors.toMap(
            this::toConcept,
            features::get
        ));
  }

  private Term toConcept(String k) {
    return new ConceptIdentifier()
        .withName(k)
        .withUuid(Util.uuid(k))
        .withTag(k);
  }

  /**
   * Resolve a PC URI to its corresponding term
   */
  private Optional<Term> resolveConceptByUUID(ConceptScheme<Term> scheme, String key) {
    return scheme.getConcepts()
        .filter(t -> t.getTag().equals(key))
        .findFirst();
  }

  /**
   * Resolve a PC (normalized) label to its corresponding term
   */
  private Optional<Term> resolveConceptByLabel(ConceptScheme<Term> scheme, String label) {
    return scheme.getConcepts()
        .filter(pc -> label.equals(toVariableName(pc))
            || label.equals(pc.getLabel()))
        .findAny();
  }


  /**
   * Maps a 'natural' PC label to its normalized variable name counterpart
   */
  private String toVariableName(Term pc) {
    String label = pc.getLabel();
    if (label.contains(" ")) {
      return camelCase(label);
    } else {
      return Character.toLowerCase(label.charAt(0)) + label.substring(1);
    }
  }

  /**
   *
   */
  private DMNContext bindInputs(java.util.Map<Term, ?> inputs, DMNModel model) {
    DMNContext ctx = runtime.newContext();
    inputs.forEach((pc, value) -> ctx.set(toVariableName(pc), castIntoDMN(value, pc, model)));
    return ctx;
  }


  /**
   * Inverse mapping
   */
  private Map<Term, Object> bindOutputs(DMNContext result) {
    Map<Term, Object> map = new HashMap<>();
    result.getAll().forEach((k, v) -> {
      Optional<Term> pc = Optional.empty(); //resolveConceptByLabel(k);
      pc.ifPresent(term -> map.put(
          term,
          castIntoFHIR(v, term)));
    });
    return map;
  }

  private Map<String, Object> extractOutputs(DMNContext result) {
    Map<String, Object> map = new HashMap<>();
    result.getAll().forEach((k, v) ->
        map.put(k, v instanceof Base ? v : castIntoFHIR(v, null)));
    return map;
  }

  private Type castIntoFHIR(final Object x, final Term concept) {
    Optional<DataElement> schema = getType(concept);
    Optional<Type> cast = schema.flatMap(de -> FHIR3DataTypeConstructor.construct(de, x));
    return cast.orElseThrow(IllegalStateException::new);
  }

  private static Optional<DataElement> getType(Term concept) {
    return Optional.ofNullable(
        new DataElement().addElement(
            new ElementDefinition().addType(
                new TypeRefComponent().setCode(DataType.QUANTITY.toCode()))));
  }

  private Object castIntoDMN(Object value, Term pc, DMNModel model) {
    if (needUnwrapping(value, pc, model)) {
      return value; //FHIR3DataTypeConstructor.destruct(value);
    } else {
      return value;
    }
  }

  private boolean needUnwrapping(Object value, Term pc, DMNModel model) {
    DecisionNode dec = model.getDecisionByName(toVariableName(pc));
    return dec != null
        && !dec.getResultType().isAssignableValue(value);
  }

  public static DMNRuntime initRuntime(KnowledgeBase knowledgeBase) {
    KieServices kieServices = KieServices.Factory.get();
    KnowledgeCarrier carrier = knowledgeBase.getManifestation();

    KieFileSystem kfs = kieServices.newKieFileSystem()
        .write(kieServices.getResources()
            .newInputStreamResource(
                new ByteArrayInputStream(carrier.asBinary()
                    .orElseThrow(UnsupportedOperationException::new)))
            .setTargetPath(
                "/" + carrier.getAssetId().getTag() + "/versions/" + carrier.getAssetId()
                    .getVersionTag())
            .setResourceType(ResourceType.DMN));

    KieModule km = kieServices.newKieBuilder(kfs).buildAll().getKieModule();
    KieContainer kieContainer = kieServices.newKieContainer(km.getReleaseId());

    kieContainer.verify().getMessages(Message.Level.ERROR)
        .forEach(msg -> System.err.println(msg.getText()));

    return kieContainer.newKieSession().getKieRuntime(DMNRuntime.class);
  }
}

