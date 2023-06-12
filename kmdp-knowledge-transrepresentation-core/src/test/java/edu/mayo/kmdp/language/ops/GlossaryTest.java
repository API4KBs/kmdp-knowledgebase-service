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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.id.Term.mock;
import static org.omg.spec.api4kp._20200801.id.Term.sct;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Patient_Cohort_Definition;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Depends_On;
import static org.omg.spec.api4kp._20200801.taxonomy.derivationreltype.DerivationTypeSeries.Is_Adaptation_Of;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Assessment_Predictive_And_Inferential_Models;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Terminology_Ontology_And_Assertional_KBs;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetrole.KnowledgeAssetRoleSeries.Operational_Concept_Definition;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Value_Set;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.YAML_1_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_DSTU2;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HL7_CQL_1_3;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OpenAPI_2_X;
import static org.omg.spec.api4kp._20200801.taxonomy.languagerole.KnowledgeRepresentationLanguageRoleSeries.Schema_Language;
import static org.omg.spec.api4kp._20200801.taxonomy.lexicon.LexiconSeries.SNOMED_CT;

import edu.mayo.kmdp.ccg.model.GlossaryEntry;
import edu.mayo.kmdp.ccg.model.OperationalDefinition;
import edu.mayo.kmdp.util.JSonUtil;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.id.ConceptIdentifier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.surrogate.Annotation;
import org.omg.spec.api4kp._20200801.surrogate.Dependency;
import org.omg.spec.api4kp._20200801.surrogate.Derivative;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;

class GlossaryTest {

  ConceptIdentifier hypertense = mock("Has Hypertension", "h05hyp3r73n510n")
      .asConceptIdentifier();
  ConceptIdentifier diabetic = mock("Has Diabetes", "h05d14b3735")
      .asConceptIdentifier();

  ConceptIdentifier hypertension = sct("Hypertension", "38341003")
      .asConceptIdentifier();
  ConceptIdentifier diabetes = sct("Diabetes", "73211009")
      .asConceptIdentifier();

  @Test
  void testGeneratedModel() {

    var ge = new GlossaryEntry()
        .defines(hypertense.getConceptId().toString())
        .focus(hypertension.getConceptId().toString())
        .semanticDefinition(newId(URI.create("http://test.ckm.mock.edu/123456#1"))
            .getResourceId().toString())
        .def(List.of(
            new OperationalDefinition()
                .id(UUID.randomUUID().toString())
                .name("Hypertension")));

    Optional<String> x = JSonUtil.writeJsonAsString(ge);
    assertTrue(x.isPresent());

    assertTrue(x.get().contains(hypertense.getReferentId().toString()));
    assertTrue(x.get().contains("http://snomed.info/id"));

  }

  @Test
  void testGlossaryDefinitionAssets() {

    ConceptIdentifier has_subj = mock("has subject", "has-subject")
        .asConceptIdentifier();
    ConceptIdentifier op_def = mock("operationally defines", "op-defines")
        .asConceptIdentifier();

    KnowledgeAsset mcbt_diabetes_vs = new KnowledgeAsset()
        .withName("Hypertension Value Set")
        .withAssetId(
            newId(URI.create("http://terms.mayo.edu/valueset/123456789")))
        .withFormalCategory(Terminology_Ontology_And_Assertional_KBs)
        .withFormalType(Value_Set);
    KnowledgeAsset sem_def = new KnowledgeAsset()
        .withDescription(
            "The (optional) semantic definition, which could be resolved by dereferencing the concept URI")
        .withFormalCategory(Terminology_Ontology_And_Assertional_KBs)
        // TODO Add "Semantic Definition" or something like that to the asset types
        //.withType( KnowledgeAssetType.Patient_Cohort_Definition ) )
        .withAnnotation(new Annotation()
            .withRel(has_subj)
            .withRef(diabetes));

    // Identity of the Def as an Asset
    KnowledgeAsset def1 = new KnowledgeAsset()
        .withAssetId(newId(URI.create("http://test.ckm.mock.edu/9999-2")))
        .withName("Hypertension - BP - CQL")
        .withDescription(
            "Ref. to a CQL library that expresses the Blood Pressure criteria for 'hypertense'");

    // Knowledge Representation information
    // This is a case where the exact same work - Query for FHIR resources with a given code, etc. -
    // can be expressed either by means of a FHIR query, or a CQL expression
    def1.withFormalCategory(Assessment_Predictive_And_Inferential_Models)
        .withFormalType(Patient_Cohort_Definition)
        .withRole(Operational_Concept_Definition)
        .withCarriers(new KnowledgeArtifact()
            .withRepresentation(new SyntacticRepresentation()
                .withLanguage(HL7_CQL_1_3)
                .withFormat(XML_1_1)
                .withLexicon(SNOMED_CT)
                .withSubLanguage(new SyntacticRepresentation()
                    .withRole(Schema_Language)
                    .withLanguage(FHIR_DSTU2))))
        .withCarriers(new KnowledgeArtifact()
            .withRepresentation(new SyntacticRepresentation()
                .withLanguage(OpenAPI_2_X)
                .withFormat(YAML_1_2)
                .withLexicon(SNOMED_CT)));

    // Semantic information
    def1.withAnnotation(new Annotation()
        .withRel(has_subj)
        .withRef(diabetes))
        .withAnnotation(new Annotation()
            .withRel(op_def)
            .withRef(diabetic));

    // Related assets (dependencies, pedigree, etc.)

    // Not Strictly necessary, but here is how we would do the
    def1.withLinks(new Derivative()
        .withRel(Is_Adaptation_Of)
        .withHref(sem_def.getAssetId()));

    // Identity of the Def as an Asset
    KnowledgeAsset def2 = new KnowledgeAsset()
        .withAssetId(newId(URI.create("http://test.ckm.mock.edu/9999-1")))
        .withName("Hyperense - BP - FHIR")
        .withDescription(
            "Ref. to an OpenAPI spec of a FHIR query that looks for abnormal BP observations");

    // Knowledge Representation information
    def2.withFormalCategory(Assessment_Predictive_And_Inferential_Models)
        .withFormalType(Patient_Cohort_Definition)
        .withRole(Operational_Concept_Definition)
        .withCarriers(new KnowledgeArtifact()
            .withRepresentation(new SyntacticRepresentation()
                .withLanguage(OpenAPI_2_X)
                .withFormat(YAML_1_2)
                .withLexicon(SNOMED_CT)));
    // Semantic information
    def2.withAnnotation(
        new Annotation()
            .withRel(has_subj)
            .withRef(diabetes))
        .withAnnotation(
            new Annotation()
                .withRel(op_def)
                .withRef(diabetic));

    // Related assets (dependencies, pedigree, etc.)

    // Not Strictly necessary, but here is how we would do the
    def2.withLinks(new Derivative()
        .withRel(Is_Adaptation_Of)
        .withHref(sem_def.getAssetId()));

    def2.withLinks(new Dependency()
        .withRel(Depends_On)
        .withHref(mcbt_diabetes_vs.getAssetId()));

    assertNotNull(def1);
    assertTrue(JSonUtil.writeJson(def1).isPresent());
    assertNotNull(def2);
    assertTrue(JSonUtil.writeJson(def2).isPresent());

  }

}
