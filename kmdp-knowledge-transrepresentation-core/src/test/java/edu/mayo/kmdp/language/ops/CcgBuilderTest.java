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
package edu.mayo.kmdp.language.ops;

import static edu.mayo.kmdp.language.ccg.library.mock.MockVocabulary.Current_Caffeine_Use;
import static edu.mayo.kmdp.registry.Registry.BASE_UUID_URN_URI;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.Defines;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.ofAst;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.assetId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.newSurrogate;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Clinical_Concept_Glossary;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;

import edu.mayo.kmdp.ccg.model.GlossaryEntry;
import edu.mayo.kmdp.ccg.model.OperationalDefinition;
import edu.mayo.kmdp.language.ccg.library.mock.MockVocabulary;
import edu.mayo.kmdp.language.translators.surrogate.v2.SurrogateV2ToCcgEntry;
import edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;

class CcgBuilderTest {

  @Test
  void testIdInline() {
    SurrogateV2ToCcgEntry toCcgEntry = new SurrogateV2ToCcgEntry();

    final ResourceIdentifier id =
        assetId(BASE_UUID_URN_URI, UUID.randomUUID().toString(), "1");

    KnowledgeAsset surrogate = newSurrogate(id,true)
        .withAnnotation(Defines,
            Current_Caffeine_Use)
        .withName("Test", "test desc")
        .withQueryType()
        .aaS()
        .withInlinedFhirPath("x + 1")
        .get();

    KnowledgeCarrier opDefCarrier = toCcgEntry
        .applyTransrepresent(
            ofAst(surrogate)
            .withRepresentation(rep(Knowledge_Asset_Surrogate_2_0)),
            codedRep(Clinical_Concept_Glossary),
            null)
        .orElseGet(Assertions::fail);

    assertTrue(opDefCarrier.is(GlossaryEntry.class));
    GlossaryEntry ge = opDefCarrier.as(GlossaryEntry.class)
        .orElseGet(Assertions::fail);
    assertFalse(ge.getDef().isEmpty());
    assertNotNull(ge.getDef().get(0));
    OperationalDefinition opDef = ge.getDef().get(0);
    assertEquals(id.getVersionId().toString(), opDef.getComputableSpec().getAssetId());
  }


  @Test
  public void testWithInputs() {
    SurrogateV2ToCcgEntry toCcgEntry = new SurrogateV2ToCcgEntry();

    final ResourceIdentifier id =
        assetId(BASE_UUID_URN_URI, UUID.randomUUID().toString(), "1");

    KnowledgeAsset surrogate = newSurrogate(id, true)
        .withAnnotation(Defines,
            Current_Caffeine_Use)
        .withAnnotation(SemanticAnnotationRelTypeSeries.In_Terms_Of,
            MockVocabulary.Most_Recent_Blood_Pressure)
        .withName("Test", "test desc")
        .aaS()
        .get();

    KnowledgeCarrier opDefCarrier =  toCcgEntry
        .applyTransrepresent(
            ofAst(surrogate)
                .withRepresentation(rep(Knowledge_Asset_Surrogate_2_0)),
            codedRep(Clinical_Concept_Glossary),
            null)
        .orElseGet(Assertions::fail);

    GlossaryEntry ge = opDefCarrier.as(GlossaryEntry.class)
        .orElseGet(Assertions::fail);
    OperationalDefinition opDef = (OperationalDefinition) ge.getDef().get(0);
    assertEquals(1, opDef.getInTermsOf().size());
    assertEquals(MockVocabulary.Most_Recent_Blood_Pressure.getConceptId().toString(),
        opDef.getInTermsOf().get(0));
  }

}