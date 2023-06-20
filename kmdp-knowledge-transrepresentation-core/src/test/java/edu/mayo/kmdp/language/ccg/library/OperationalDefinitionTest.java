package edu.mayo.kmdp.language.ccg.library;

import static edu.mayo.kmdp.language.ccg.library.mock.MockVocabulary.Currently_Dehydrated;
import static edu.mayo.kmdp.language.ccg.library.mock.MockVocabulary.Has_Diabetes_Mellitus;
import static edu.mayo.kmdp.language.ccg.library.mock.MockVocabulary.Has_Hypertension;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.Defines;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.randomAssetId;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetrole.KnowledgeAssetRoleSeries.Operational_Concept_Definition;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeprocessingtechnique.KnowledgeProcessingTechniqueSeries.Natural_Technique;

import edu.mayo.kmdp.ccg.model.GlossaryEntry;
import edu.mayo.kmdp.ccg.model.OperationalDefinition;
import edu.mayo.kmdp.util.StreamUtil;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.surrogate.Annotation;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder;

class OperationalDefinitionTest extends GlossaryLibraryTestBase {

  @Test
  void testNaturalisticDefinition() {
    KnowledgeAsset surrogate = SurrogateBuilder.newSurrogate(randomAssetId())
        .asOperationalDefinition(null, Currently_Dehydrated)
        .get()
        .withProcessingMethod(Natural_Technique);
    semanticRepository.publish(surrogate, null);

    GlossaryEntry entry =
        libraryApi.getGlossaryEntry(List.of("default"), Currently_Dehydrated.getUuid())
            .orElseGet(Assertions::fail);

    assertEquals(Currently_Dehydrated.getConceptId().toString(), entry.getDefines());
    assertTrue(entry.getDef().stream()
        .allMatch(Objects::nonNull));
    assertTrue(entry.getDef().stream()
        .flatMap(StreamUtil.filterAs(OperationalDefinition.class))
        .allMatch(
            od -> od.getProcessingMethod().contains(Natural_Technique.getTag())));
  }

  @Test
  void testDefinitionsOfMultipleConcepts() {
    KnowledgeAsset surrogate = new KnowledgeAsset()
        .withAssetId(randomAssetId())
        .withRole(Operational_Concept_Definition)
        .withProcessingMethod(Natural_Technique)
        .withAnnotation(
            new Annotation()
                .withRef(Has_Hypertension.asConceptIdentifier())
                .withRel(Defines.asConceptIdentifier()),
            new Annotation()
                .withRef(Has_Diabetes_Mellitus.asConceptIdentifier())
                .withRel(Defines.asConceptIdentifier()));

    semanticRepository.publish(surrogate, null);

    GlossaryEntry entry1 =
        libraryApi.getGlossaryEntry(List.of("default"), Has_Hypertension.getUuid())
            .orElseGet(Assertions::fail);

    assertEquals(Has_Hypertension.getConceptId().toString(), entry1.getDefines());
    assertTrue(entry1.getDef().stream()
        .allMatch(e -> Has_Hypertension.getUuid().equals(e.getDefines())));
    var opDefId1 = entry1.getDef().get(0).getId();

    GlossaryEntry entry2 =
        libraryApi.getGlossaryEntry(List.of("default"), Has_Diabetes_Mellitus.getUuid())
            .orElseGet(Assertions::fail);

    assertEquals(Has_Diabetes_Mellitus.getConceptId().toString(), entry2.getDefines());
    assertTrue(entry2.getDef().stream()
        .allMatch(e -> Has_Diabetes_Mellitus.getUuid().equals(e.getDefines())));
    var opDefId2 = entry2.getDef().get(0).getId();

    assertEquals(opDefId1, opDefId2);

  }
}

