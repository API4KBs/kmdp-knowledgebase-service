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
package edu.mayo.kmdp.examples._6.composite;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;
import static org.omg.spec.api4kp._20200801.taxonomy.lexicon.LexiconSeries.PCV;
import static org.omg.spec.api4kp._20200801.taxonomy.lexicon.LexiconSeries.SNOMED_CT;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Concrete_Knowledge_Expression;

import edu.mayo.kmdp.util.JenaUtil;
import edu.mayo.kmdp.util.fhir.fhir3.FHIR3JsonUtil;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.apache.jena.rdf.model.Model;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.id.IdentifierConstants;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder;

public class TransRepresentationChainTest extends CmmnToPlanDefIntegrationTestBase {

  @Override
  protected List<String> getXMLS() {
    return Arrays.asList(
        "/mock/Basic Case Model.cmmn.xml",
        "/mock/Basic Decision Model.dmn.xml"
    );
  }

  @Override
  protected ResourceIdentifier getCompositeAssetId() {
    return SemanticIdentifier.newId(
        UUID.fromString("4aab3ff1-bf86-40ec-868c-8fa5bf726c7f"),
        IdentifierConstants.VERSION_ZERO);
  }

  @Override
  protected UUID getRootAssetID() {
    return UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c");
  }

  @Override
  protected String getRootAssetVersion() {
    return "1.0.1";
  }


  @Test
  public void testStep1() {
    // The dependency-based constructor considers the given asset as the root of a tree-based knowledge base,
    // implicitly defined by the query { caseModel dependsOn* decisionModel }
    // In particular, in this case we have
    //    { caseModel dependsOn* decisionModel }
    // The operation then returns a 'structure', which is effectively an 'intensional' manifestation of a new, composite Asset
    Model struct =

        constructor.getKnowledgeBaseStructure(getRootAssetID(), getRootAssetVersion(), null)

            .flatOpt(kc -> kc.as(Model.class))
            .orElseGet(Assertions::fail);

    System.out.println("Structure Graph >>");
    System.out.println(JenaUtil.asString(struct));
  }

  @Test
  public void testStep2() {
    KnowledgeCarrier composite =

        constructor.getKnowledgeBaseStructure(getRootAssetID(), getRootAssetVersion(), null)
            .flatMap(kc -> assembler.assembleCompositeArtifact(kc, null))

            .orElseGet(Assertions::fail);

    System.out.println("Result >> " + composite.getClass());
    CompositeKnowledgeCarrier ckc = (CompositeKnowledgeCarrier) composite;
    System.out.println("Component # >> " + ckc.getComponent().size());

    System.out.println("Struct Type >> " + ckc.getStruct().getRepresentation().getLanguage());
  }

  @Test
  public void testStep3() {
    KnowledgeCarrier composite =

        constructor.getKnowledgeBaseStructure(getRootAssetID(), getRootAssetVersion(), null)
            .flatMap(kc -> assembler.assembleCompositeArtifact(kc, null))
            .flatMap(kc -> parser.applyLift(kc, Abstract_Knowledge_Expression.getTag()))

            .orElseGet(Assertions::fail);

    CompositeKnowledgeCarrier ckc = (CompositeKnowledgeCarrier) composite;
    ckc.getComponent().forEach(comp ->
        System.out.println(
            "Component : " + comp.getRepresentation().getLanguage() + " " + comp.getLevel()));
  }

  @Test
  public void testStep4() {
    KnowledgeCarrier parsedComposite =
        constructor.getKnowledgeBaseStructure(getRootAssetID(), getRootAssetVersion(), null)
            .flatMap(kc -> assembler.assembleCompositeArtifact(kc, null))
            .flatMap(kc -> parser.applyLift(kc, Abstract_Knowledge_Expression.getTag()))
            .orElseGet(Assertions::fail);

    KnowledgeCarrier dictionary =
        loadDictionary();

    KnowledgeCarrier decisionModelComponent
        = ((CompositeKnowledgeCarrier) parsedComposite).getComponent().get(1);
    parser.applyLower(decisionModelComponent, Concrete_Knowledge_Expression.getTag())
        .ifPresent(ec -> System.out.println(ec.getExpression()));
  }


  @Test
  public void testStep5() {
    Answer<KnowledgeCarrier> parsedComposite = Answer.of(
        constructor.getKnowledgeBaseStructure(getRootAssetID(), getRootAssetVersion(), null)
            .flatMap(kc -> assembler.assembleCompositeArtifact(kc, null))
            .flatMap(kc -> parser.applyLift(kc, Abstract_Knowledge_Expression.getTag()))
            .orElseGet(Assertions::fail));

    KnowledgeCarrier planDefinitionComposite =
        parsedComposite.flatMap(ckc ->
            translator.applyTransrepresent(
                ckc,
                ModelMIMECoder.encode(rep(FHIR_STU3, SNOMED_CT, PCV)),
                null))
            .orElseGet(Assertions::fail);

    CompositeKnowledgeCarrier ckc = (CompositeKnowledgeCarrier) planDefinitionComposite;
    ckc.getComponent().forEach(comp ->
        System.out.println(
            "Component : " + comp.getRepresentation().getLanguage() + " " + comp.getLevel()));
  }

  @Test
  public void testStep6() {
    KnowledgeCarrier planDefinitionComposite =
        constructor.getKnowledgeBaseStructure(getRootAssetID(), getRootAssetVersion(), null)
            .flatMap(kc -> assembler.assembleCompositeArtifact(kc, null))
            .flatMap(kc -> parser.applyLift(kc, Abstract_Knowledge_Expression.getTag()))
            .flatMap(ckc ->
                translator.applyTransrepresent(
                    ckc,
                    ModelMIMECoder.encode(rep(FHIR_STU3, SNOMED_CT, PCV)),
                    null))
            .orElseGet(Assertions::fail);

    KnowledgeCarrier flatPlanDef = flattener
        .flattenArtifact(planDefinitionComposite, getRootAssetID(), null)
        .orElseGet(Assertions::fail);

    System.out.println("Component : " + flatPlanDef.getRepresentation().getLanguage());
    System.out.println(
        FHIR3JsonUtil.instance.toJsonString(
            flatPlanDef.as(PlanDefinition.class)
                .orElse(null)));
  }


}
