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
package edu.mayo.kmdp.language.ccg.library;

import static edu.mayo.kmdp.language.ccg.library.mock.MockVocabulary.Current_Caffeine_Use;
import static edu.mayo.kmdp.language.ccg.library.mock.MockVocabulary.Current_Caffeine_Use_NewVer;
import static edu.mayo.kmdp.language.ccg.library.mock.MockVocabulary.Currently_Dehydrated;
import static edu.mayo.kmdp.language.ccg.library.mock.MockVocabulary.Has_Allergy_To_Statins;
import static edu.mayo.kmdp.language.ccg.library.mock.MockVocabulary.Has_Diabetes_Mellitus;
import static edu.mayo.kmdp.language.ccg.library.mock.MockVocabulary.Has_Hypertension;
import static edu.mayo.kmdp.language.ccg.library.mock.MockVocabulary.Has_Hypertension_Is;
import static edu.mayo.kmdp.util.Util.uuid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newVersionId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.newSurrogate;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.randomArtifactId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.randomAssetId;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Effectuates;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetrole.KnowledgeAssetRoleSeries.Operational_Concept_Definition;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeprocessingtechnique.KnowledgeProcessingTechniqueSeries.Computational_Technique;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeprocessingtechnique.KnowledgeProcessingTechniqueSeries.Natural_Technique;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIRPath_STU1;

import edu.mayo.kmdp.ccg.model.GlossaryEntry;
import edu.mayo.kmdp.ccg.model.OperationalDefinition;
import edu.mayo.kmdp.language.ccg.library.mock.MockAPISpec;
import edu.mayo.kmdp.language.ccg.library.mock.MockOpDef;
import edu.mayo.kmdp.language.ccg.library.mock.MockOpDef2;
import edu.mayo.kmdp.language.ccg.library.mock.MockOpDef3;
import edu.mayo.kmdp.language.ccg.library.mock.MockVocabulary;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.Util;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.Term;
import org.omg.spec.api4kp._20200801.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder;
import org.omg.spec.api4kp._20200801.surrogate.Applicability;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;

class GlossaryLibraryServiceTest extends GlossaryLibraryTestBase {

  @Test
  void testListKnowledgeAssets() {
    MockOpDef entry = new MockOpDef();
    // Using a SemRepo client to upload an Asset + Surrogate (which *happens* to fit the criteria for a CCG entry)
    doPublish(entry.buildSurrogate(), entry.buildArtifact());

    List<Pointer> opDefs = semanticRepository
        .listKnowledgeAssets(
            Operational_Concept_Definition.getTag(),
            null,
            null,
            0, -1)
        .orElse(Collections.emptyList());
    assertEquals(2, opDefs.size());
    Pointer p = opDefs.iterator().next();

    assertNotNull(p.getHref());
  }


  @Test
  void testDiscoverEntriesWithKnownConcept() {
    MockOpDef entry = new MockOpDef();
    doPublish(entry.buildSurrogate(), entry.buildArtifact());

    var ptr = libraryApi
        .getGlossaryEntry(List.of("default"), Has_Hypertension.getUuid())
        .orElseGet(Assertions::fail);

    assertEquals(Has_Hypertension.getConceptId().toString(), ptr.getDefines());
    var pcId = MockVocabulary.resolveConceptId(ptr.getDefines())
        .orElseGet(Assertions::fail);

    Answer<GlossaryEntry> ge2 = libraryApi
        .getGlossaryEntry(List.of("default"), pcId.getUuid(), null, null, false, false, null);
    assertTrue(ge2.isSuccess());

    assertEquals(Has_Hypertension.getConceptId().toString(),
        ge2.map(GlossaryEntry::getDefines).orElse(null));
  }

  @Test
  void testDiscoverEntriesWithUnknownConcept() {
    MockOpDef entry = new MockOpDef();
    doPublish(entry.buildSurrogate(), entry.buildArtifact());

    Answer<GlossaryEntry> ge = libraryApi
        .getGlossaryEntry(List.of("default"), UUID.randomUUID(), null, null, false, false, null);

    assertFalse(ge.isSuccess());
    assertEquals(Answer.notFound().getOutcomeType(), ge.getOutcomeType());
  }

  @Test
  void testDiscoverEntriesWithAlternativeDefinitions() {
    KnowledgeAsset opdef1 = newSurrogate(uuid("c1def1"))
        .withName("Test", "Test")
        .asOperationalDefinition(
            Term.mock("a", "123"),
            Current_Caffeine_Use)
        .get();
    KnowledgeAsset opdef2 = newSurrogate(uuid("c1def2"))
        .withName("Test2", "Test2")
        .asOperationalDefinition(
            Term.mock("a", "123"),
            Current_Caffeine_Use)
        .get();
    KnowledgeAsset opdef3 = newSurrogate(uuid("c2def3"))
        .withName("Test3", "Test3")
        .asOperationalDefinition(
            Term.mock("b", "456"),
            Currently_Dehydrated)
        .get();

    semanticRepository.publish(opdef1, null);
    semanticRepository.publish(opdef2, null);
    semanticRepository.publish(opdef3, null);

    var entryPtrs = libraryApi.listGlossaryEntries(List.of("default"))
        .orElse(Collections.emptyList());

    assertEquals(2, entryPtrs.size());

    assertTrue(entryPtrs.stream()
        .map(GlossaryEntry::getDefines)
        .anyMatch(href -> href
            .contains(Current_Caffeine_Use.getConceptId().toString())));

    assertTrue(entryPtrs.stream()
        .map(GlossaryEntry::getDefines)
        .anyMatch(href -> href
            .contains(Currently_Dehydrated.getConceptId().toString())));
  }


  @Test
  void testgetGlossaryEntryWithAlternatives() {

    KnowledgeAsset opdef1 = newSurrogate(uuid("c1def1"))
        .withName("Test", "Test")
        .asOperationalDefinition(
            Term.mock("a", "123"),
            Current_Caffeine_Use)
        .withDependency(Effectuates, randomAssetId())
        .withCarriers(randomArtifactId(), null)
        .get();
    KnowledgeAsset opdef2 = newSurrogate(uuid("c1def2"))
        .withName("Test2", "Test2")
        .asOperationalDefinition(
            Term.mock("a", "123"),
            Current_Caffeine_Use)
        .withDependency(Effectuates, randomAssetId())
        .withCarriers(randomArtifactId(), null)
        .get();
    KnowledgeAsset opdef3 = newSurrogate(uuid("c2def3"))
        .withName("Test3", "Test3")
        .asOperationalDefinition(
            Term.mock("b", "456"),
            Currently_Dehydrated)
        .withDependency(Effectuates, randomAssetId())
        .get();

    semanticRepository.publish(opdef1, null);
    semanticRepository.publish(opdef2, null);
    semanticRepository.publish(opdef3, null);

    Answer<GlossaryEntry> gEntry = libraryApi
        .getGlossaryEntry(List.of("default"), Current_Caffeine_Use.getUuid(), null, null, false, false,
            null);
    assertNotNull(gEntry);
    assertTrue(gEntry.isSuccess());
    assertEquals(2, gEntry.map(GlossaryEntry::getDef).map(List::size).orElse(-1));
  }


  @Test
  void testBuildOfCCGEntryInternal() {
    MockOpDef entry = new MockOpDef();

    // Using a SemRepo client to upload an Asset + Surrogate (which *happens* to fit the criteria for a CCG entry)
    doPublish(entry.buildSurrogate(), entry.buildArtifact());

    Optional<GlossaryEntry> ccgEntryOp = libraryApi
        .getGlossaryEntry(List.of("default"), Has_Hypertension_Is.getUuid(), null, null, false, false, null)
        .getOptionalValue();

    assertTrue(ccgEntryOp.isPresent());
    GlossaryEntry ccgEntry = ccgEntryOp.get();

    assertEquals(Has_Hypertension_Is.getConceptId().toString(),
        ccgEntry.getDefines());
    assertNull(ccgEntry.getRelatedEntries());

    assertEquals(1, ccgEntry.getDef().size());
    var def = ccgEntry.getDef().get(0);
    assertNotNull(def);

    assertNotNull(def.getDefines());
    assertEquals(Has_Hypertension_Is.getUuid(),
        def.getDefines());

    assertFalse(def.getIncludes().isEmpty());

    assertNotNull(def.getComputableSpec());
    assertEquals("X.exists()", def.getComputableSpec().getInlinedExpr());
    var rep = ModelMIMECoder.decode(def.getComputableSpec().getMimeCode())
        .orElseGet(Assertions::fail);
    assertTrue(FHIRPath_STU1.sameAs(rep.getLanguage()));

    // assertNotNull(opDef.getDatatype());

  }


  @Test
  void testBundleCCGEntry() {
    MockOpDef entry = new MockOpDef();

    MockAPISpec api = new MockAPISpec();
    ResourceIdentifier apiAssetId = api.buildSurrogate().getAssetId();
    KnowledgeCarrier apiArtifact = api.buildArtifact();

    // Using a SemRepo client to upload an Asset + Surrogate (which *happens* to fit the criteria for a CCG entry)
    doPublish(entry.buildSurrogate(), entry.buildArtifact());
    semanticRepository.setKnowledgeAssetCarrierVersion(
        apiAssetId.getUuid(), apiAssetId.getVersionTag(),
        apiArtifact.getArtifactId().getUuid(), apiArtifact.getArtifactId().getVersionTag(),
        apiArtifact.asBinary().orElse(null));

    // Using the SemRepo API to check that OpDefs can be generated transparently on the fly
    // This should point to the same as repoApi
    Optional<GlossaryEntry> ccgEntryOp = libraryApi
        .getGlossaryEntry(List.of("default"), Has_Hypertension_Is.getUuid(), null, null, false, false, null)
        .getOptionalValue();
    assertTrue(ccgEntryOp.isPresent());
    GlossaryEntry ccgEntry = ccgEntryOp.get();

    assertNotNull(ccgEntry.getDef().get(0));
    OperationalDefinition master = ccgEntry.getDef().get(0);
    assertEquals(1, master.getIncludes().size());
    OperationalDefinition sub = master.getIncludes().get(0);
    assertNull(sub.getIncludes());

    var idStr = ccgEntry.getDef().get(0).getComputableSpec().getAssetId();
    var id = newVersionId(URI.create(idStr));

    List<KnowledgeCarrier> bundle =
        semanticRepository.getAnonymousCompositeKnowledgeAssetCarrier(
                id.getUuid(), id.getVersionTag())
            .flatOpt(Util.as(CompositeKnowledgeCarrier.class))
            .map(CompositeKnowledgeCarrier::getComponent)
            .orElse(Collections.emptyList());

    assertEquals(3, bundle.size());
    Set<String> exprs = bundle.stream()
        .map(AbstractCarrier::asString)
        .flatMap(StreamUtil::trimStream)
        .collect(Collectors.toSet());

    assertTrue(exprs.containsAll(Arrays.asList("X.exists()", "test service profile", "apiTest")));
  }


  @Test
  void testListKnowledgeAssetsWithApplicability() {
    MockOpDef entry = new MockOpDef();
    CompositeKnowledgeCarrier complexAsset = entry.buildSurrogate();

    KnowledgeAsset asset = complexAsset.mainComponent()
        .as(KnowledgeAsset.class)
        .orElseGet(Assertions::fail);

    asset.setApplicableIn(new Applicability()
        .withSituation(Has_Hypertension.asConceptIdentifier()));

    doPublish(complexAsset, entry.buildArtifact());

    var opDefs2 = libraryApi.listGlossaryEntries(
            List.of("default"), Has_Hypertension.getUuid(), null, false, false, null)
        .orElse(Collections.emptyList());
    assertEquals(1, opDefs2.size());

    var opDefs3 = libraryApi.listGlossaryEntries(
            List.of("default"), Has_Diabetes_Mellitus.getUuid(), null, false, false, null)
        .orElse(Collections.emptyList());
    assertEquals(0, opDefs3.size());
  }

  @Test
  void testgetGlossaryEntryWithApplicabilityFilter() {

    KnowledgeAsset returnType = mockReturnType();

    KnowledgeAsset opdef1 = newSurrogate(uuid("c1def1"))
        .withName("Test", "Test")
        .withApplicability(Has_Allergy_To_Statins)
        .asOperationalDefinition(
            Term.mock("a", "123"),
            Current_Caffeine_Use)
        .withDependency(Effectuates, returnType.getAssetId())
        .get();
    KnowledgeAsset opdef2 = newSurrogate(uuid("c1def2"))
        .withName("Test2", "Test2")
        .withApplicability(Has_Hypertension)
        .asOperationalDefinition(
            Term.mock("a", "123"),
            Current_Caffeine_Use)
        .withDependency(Effectuates, returnType.getAssetId())
        .get();
    KnowledgeAsset opdef3 = newSurrogate(uuid("c2def3"))
        .withName("Test3", "Test3")
        .withApplicability(Has_Allergy_To_Statins)
        .asOperationalDefinition(
            Term.mock("b", "456"),
            Currently_Dehydrated)
        .withDependency(Effectuates, returnType.getAssetId())
        .get();

    semanticRepository.publish(opdef1, null);
    semanticRepository.publish(opdef2, null);
    semanticRepository.publish(opdef3, null);

    Answer<GlossaryEntry> gEntry = libraryApi
        .getGlossaryEntry(List.of("default"), Current_Caffeine_Use.getUuid(),
            Has_Allergy_To_Statins.getUuid(),
            null, false, false, null);
    assertNotNull(gEntry);
    assertTrue(gEntry.isSuccess());
    assertEquals(1, gEntry.map(GlossaryEntry::getDef).map(List::size).orElse(-1));

  }

  @Test
  void testgetGlossaryEntryWithMethod() {

    KnowledgeAsset returnType = mockReturnType();

    KnowledgeAsset opdef1 = newSurrogate(uuid("c1def1"))
        .asOperationalDefinition(
            Term.mock("a", "123"),
            Current_Caffeine_Use)
        .aaS()
        .withDependency(Effectuates, returnType.getAssetId())
        .get()
        .withProcessingMethod(Computational_Technique);

    KnowledgeAsset opdef2 = newSurrogate(uuid("c1def2"))
        .asOperationalDefinition(
            Term.mock("a", "123"),
            Current_Caffeine_Use)
        .withDependency(Effectuates, returnType.getAssetId())
        .get()
        .withProcessingMethod(Computational_Technique);

    KnowledgeAsset opdef3 = newSurrogate(uuid("c1def3"))
        .asOperationalDefinition(
            Term.mock("a", "123"),
            Current_Caffeine_Use)
        .withDependency(Effectuates, returnType.getAssetId())
        .get()
        .withProcessingMethod(Natural_Technique);

    KnowledgeAsset opdef4 = newSurrogate(uuid("c2def4"))
        .asOperationalDefinition(
            Term.mock("b", "456"),
            Current_Caffeine_Use)
        .withDependency(Effectuates, returnType.getAssetId())
        .get()
        .withProcessingMethod(Computational_Technique);

    semanticRepository.publish(opdef1, null);
    semanticRepository.publish(opdef2, null);
    semanticRepository.publish(opdef3, null);
    semanticRepository.publish(opdef4, null);

    Answer<GlossaryEntry> gEntry = libraryApi
        .getGlossaryEntry(List.of("default"), Current_Caffeine_Use.getUuid(),
            null,
            Computational_Technique.getTag(),
            false, false,
            null);

    assertNotNull(gEntry);
    assertTrue(gEntry.isSuccess());
    assertEquals(3, gEntry.map(GlossaryEntry::getDef).map(List::size).orElse(-1));

  }


  @Test
  void testDefinitionsWithConceptSeries() {

    KnowledgeAsset returnType = mockReturnType();

    Term t1 = Current_Caffeine_Use.asConceptIdentifier().withVersionTag("1.0.0");
    Term t2 = Current_Caffeine_Use_NewVer.asConceptIdentifier().withVersionTag("2.0.0");

    KnowledgeAsset opdef1 = newSurrogate(uuid("c1def1"))
        .asOperationalDefinition(
            Term.mock("a", "123"),
            t1)
        .aaS()
        .withDependency(Effectuates, returnType.getAssetId())
        .get();

    KnowledgeAsset opdef2 = newSurrogate(uuid("c1def2"))
        .asOperationalDefinition(
            Term.mock("a", "123"),
            t2)
        .withDependency(Effectuates, returnType.getAssetId())
        .get();

    semanticRepository.publish(opdef1, null);
    semanticRepository.publish(opdef2, null);

    assertEquals(t1.getUuid(), t2.getUuid());
    Answer<GlossaryEntry> gEntry = libraryApi
        .getGlossaryEntry(List.of("default"), t1.getUuid());

    assertNotNull(gEntry);
    assertTrue(gEntry.isSuccess());
    assertEquals(2,
        gEntry.map(GlossaryEntry::getDef).map(List::size).orElse(-1));

  }


  private KnowledgeAsset mockReturnType() {
    return new KnowledgeAsset()
        .withAssetId(randomAssetId())
        .withCarriers(
            new KnowledgeArtifact()
                .withArtifactId(randomArtifactId()));
  }


  @Test
  void testDatatype() {
    MockOpDef2 entry2 = new MockOpDef2();
    doPublish(entry2.buildSurrogate(), entry2.buildArtifact());

    Answer<GlossaryEntry> ansGLEntry = this.libraryApi
        .getGlossaryEntry(List.of("default"), MockOpDef2.definedConcept.getUuid());
    assertTrue(ansGLEntry.isSuccess());

    GlossaryEntry glEntry = ansGLEntry.get();

    assertEquals(1, glEntry.getDef().size());

    assertEquals("http://foo#Condition",
        glEntry.getDef().get(0).getEffectuates());
  }


  MockOpDef2 createMockOpDefinition() {
    MockOpDef2 operationalDefinition = new MockOpDef2();
    doPublish(operationalDefinition.buildSurrogate(), operationalDefinition.buildArtifact());

    return operationalDefinition;
  }

  @Test
  void testEntriesWManyToManyDefinitions() {
    MockOpDef2 entry1 = new MockOpDef2();
    MockOpDef3 entry2 = new MockOpDef3();
    doPublish(entry1.buildSurrogate(), entry1.buildArtifact());
    doPublish(entry2.buildSurrogate(), entry2.buildArtifact());

    Answer<GlossaryEntry> ge = libraryApi
        .getGlossaryEntry(List.of("default"), Has_Allergy_To_Statins.getUuid(), null, null, false, false,
            null);

    assertTrue(ge.isSuccess());
    assertEquals(2, ge.get().getDef().size());
  }


}