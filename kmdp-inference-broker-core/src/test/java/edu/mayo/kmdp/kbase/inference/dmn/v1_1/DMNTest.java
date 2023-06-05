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
package edu.mayo.kmdp.kbase.inference.dmn.v1_1;

import static edu.mayo.kmdp.util.NameUtils.camelCase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_1;

import ca.uhn.fhir.context.FhirContext;
import edu.mayo.kmdp.kbase.inference.InferenceBaseTest;
import edu.mayo.kmdp.kbase.inference.dmn.KieDMNHelper;
import edu.mayo.kmdp.kbase.inference.mockTerms.PCO;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.IntegerType;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.Type;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.kie.dmn.api.core.DMNContext;
import org.kie.dmn.api.core.DMNModel;
import org.kie.dmn.api.core.DMNRuntime;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetRepositoryApiInternal;
import org.omg.spec.api4kp._20200801.datatypes.Map;
import org.omg.spec.api4kp._20200801.services.KnowledgeBase;

@Disabled("DMN engine need upgrade")

public class DMNTest extends InferenceBaseTest {

  static final FhirContext fhirContext = FhirContext.forDstu3();

  @Test
  public void testEngine() {
    DMNRuntime runtime = initRuntime("/Scorecipe.dmn");
    DMNModel model = runtime.getModels().get(0);

    DMNContext ctx = runtime.newContext();
    ctx.set("spaghetti", new Quantity().setCode("Kg").setValue(1).setCode("kg"));
    ctx.set("tagliatelle", new Foo());
    ctx.set("linguine", "aaa");

    ctx = runtime.evaluateAll(model, ctx).getContext();
    ctx.getAll().forEach((k, v) ->
        System.out.println(k + " \t :: \t " + toString(v)));

    assertEquals("400", toString(ctx.get("calories")));
    assertEquals("0.42", toString(ctx.get("sauce")));
  }


  @Test
  @SuppressWarnings("unchecked")
  public void testEngineComplexOutput() {
    UUID id = UUID.randomUUID();

    KnowledgeAssetRepositoryApiInternal semRepo =
        initMockRepo(id, VTAG, "/MockPredictor2.dmn", rep(DMN_1_1, XML_1_1));

    DMNRuntime runtime = KieDMNHelper
        .initRuntime(new KnowledgeBase().withManifestation(
            semRepo.getKnowledgeAssetVersionCanonicalCarrier(id, VTAG).get()));
    DMNModel model = runtime.getModels().get(0);

    DMNContext ctx = runtime.newContext();
    ctx.set(camelCase(PCO.Current_Caffeine_User.getLabel()), new BooleanType().setValue(true));
    ctx.set(camelCase(PCO.Current_Chronological_Age.getLabel()), new IntegerType().setValue(37));
    ctx = runtime.evaluateAll(model, ctx).getContext();

    assertTrue(ctx.get("riskOfHeartFailure") instanceof java.util.Map);

    java.util.Map<String, Object> obj =
        (java.util.Map<String, Object>) ctx.get("riskOfHeartFailure");
    assertEquals("mock", obj.get("newElement"));
    assertEquals(BigDecimal.valueOf(42), obj.get("newElement2"));
    assertEquals("%", obj.get("newElement3"));
  }


  public static String toString(Object v) {
    if (v instanceof Type) {
      return fhirContext.newJsonParser()
          .encodeResourceToString(new Observation().setValue((Type) v));
    }
    if (v instanceof Map) {
      return ((Map) v).keySet().stream()
          .collect(Collectors.toMap(k -> k, DMNTest::toString)).toString();
    }
    return v != null ? v.toString() : null;
  }

  public static class Foo {

    public Quantity amount = new Quantity()
        .setUnit("Kg")
        .setValue(44)
        .setCode("kg")
        .setSystem("ucum");

    public Quantity getAmount() {
      return amount;
    }

    public void setAmount(Quantity amount) {
      this.amount = amount;
    }

    @Override
    public String toString() {
      return "Foo{" +
          "amount=" + DMNTest.toString(amount) +
          '}';
    }
  }

}

