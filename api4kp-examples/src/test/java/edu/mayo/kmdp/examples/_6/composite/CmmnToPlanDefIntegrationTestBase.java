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

import static java.nio.charset.Charset.defaultCharset;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings.DEFAULT;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper.canonicalRepresentationOf;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Encoded_Knowledge_Expression;

import edu.mayo.kmdp.knowledgebase.KnowledgeBaseProvider;
import edu.mayo.kmdp.knowledgebase.assemblers.rdf.GraphBasedAssembler;
import edu.mayo.kmdp.knowledgebase.constructors.DependencyBasedConstructor;
import edu.mayo.kmdp.knowledgebase.flatteners.fhir.stu3.PlanDefinitionFlattener;
import edu.mayo.kmdp.language.LanguageDeSerializer;
import edu.mayo.kmdp.language.TransrepresentationExecutor;
import edu.mayo.kmdp.language.parsers.cmmn.v1_1.CMMN11Parser;
import edu.mayo.kmdp.language.parsers.dmn.v1_2.DMN12Parser;
import edu.mayo.kmdp.language.parsers.surrogate.v2.Surrogate2Parser;
import edu.mayo.kmdp.language.translators.cmmn.v1_1.fhir.stu3.CmmnToPlanDefTranslator;
import edu.mayo.kmdp.language.translators.dmn.v1_2.fhir.stu3.DmnToPlanDefTranslator;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryService;
import edu.mayo.kmdp.util.FileUtil;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.CompositionalApiInternal._assembleCompositeArtifact;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.CompositionalApiInternal._flattenArtifact;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal._getKnowledgeBaseStructure;
import org.omg.spec.api4kp._20200801.api.terminology.v4.server.TermsApiInternal;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.TransxionApiInternal;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.terms.model.ConceptDescriptor;

public abstract class CmmnToPlanDefIntegrationTestBase {

  KnowledgeAssetRepositoryService repo
      = KnowledgeAssetRepositoryService.selfContainedRepository();

  DeserializeApiInternal metadataParser =
      new LanguageDeSerializer(singletonList(new Surrogate2Parser()));

  DeserializeApiInternal parser =
      new LanguageDeSerializer(asList(new DMN12Parser(), new CMMN11Parser()));

  TransxionApiInternal translator = new TransrepresentationExecutor(
      asList(new CmmnToPlanDefTranslator(), new DmnToPlanDefTranslator())
  );

  KnowledgeBaseProvider kbManager
      = new KnowledgeBaseProvider(repo);

  _getKnowledgeBaseStructure constructor
      = DependencyBasedConstructor.newInstance(repo, getCompositeAssetId());

  _flattenArtifact flattener
      = new PlanDefinitionFlattener();

  _assembleCompositeArtifact assembler
      = GraphBasedAssembler.newInstance(repo);


  @BeforeEach
  void initializeTestRepository() {

    getXMLS().forEach(xml -> {
      String surr = getSurrogateSource(xml);
      String model = getModelSource(xml);

      KnowledgeAsset surrogate = readTestAssetMetadata(surr);
      KnowledgeCarrier carrier = readTestArtifact(model, surrogate);

      repo.publish(surrogate, carrier);
    });

    assertEquals(getXMLS().size(),
        repo.listKnowledgeAssets()
            .map(List::size)
            .orElse(-1));

    System.out.println("Asset repository initialized with content:");
    repo.listKnowledgeAssets()
        .ifPresent(ax -> ax.forEach(
            ptr -> System.out.println(ptr.getName() + " - " + ptr.getVersionId())));
    System.out.println("\n\n");
  }

  protected String getModelSource(String xml) {
    return xml;
  }

  protected String getSurrogateSource(String xml) {
    return xml.substring(0, xml.indexOf('.')) + ".surrogate.xml";
  }

  protected String getDictionarySource() {
    return "/mock/CS.dmn.xml";
  }

  private KnowledgeCarrier readTestArtifact(String xml, KnowledgeAsset surrogate) {
    Answer<KnowledgeCarrier> ans = Answer.of(
        FileUtil.readBytes(
            CmmnToPlanDefIntegrationTestBase.class.getResourceAsStream(xml)))
        .map(bytes -> AbstractCarrier.of(bytes)
            .withAssetId(surrogate.getAssetId())
            .withLevel(Encoded_Knowledge_Expression)
            .withArtifactId(surrogate.getCarriers().get(0).getArtifactId())
            .withHref(
                URI.create("file://" + xml.substring(xml.lastIndexOf('/') + 1)
                    .replace(" ", "")))
            .withRepresentation(canonicalRepresentationOf(surrogate)));
    assertTrue(ans.isSuccess());

    KnowledgeCarrier kc = ans.get();
    if (kc.getArtifactId().getVersionId() == null) {
      kc.setArtifactId(
          SemanticIdentifier.newId(URI.create(kc.getArtifactId().getResourceId().toString() + "/versions/0.0.0")));
    }
    return kc;
  }

  private KnowledgeAsset readTestAssetMetadata(String surrFilePath) {
    Answer<KnowledgeAsset> ans = Answer.of(
        FileUtil.readBytes(
            CmmnToPlanDefIntegrationTestBase.class.getResourceAsStream(surrFilePath)))
        .map(bytes -> AbstractCarrier.of(bytes)
            .withRepresentation(rep(Knowledge_Asset_Surrogate_2_0, XML_1_1, defaultCharset(), DEFAULT)))
        .flatMap(bc -> metadataParser.applyLift(bc, Abstract_Knowledge_Expression.getTag()))
        .flatOpt(astCarrier -> astCarrier.as(KnowledgeAsset.class));
    assertTrue(ans.isSuccess());
    return ans.get();
  }

  protected KnowledgeCarrier loadDictionary() {
    return Answer.of(
        FileUtil.readBytes(
            CmmnToPlanDefIntegrationTestBase.class.getResourceAsStream(getDictionarySource())))
        .map(bytes -> AbstractCarrier.of(bytes)
            .withLevel(Encoded_Knowledge_Expression)
            .withRepresentation(rep(DMN_1_2, XML_1_1)))
        .flatMap(kc -> parser.applyLift(kc, Abstract_Knowledge_Expression.getTag()))
        .get();
  }


  protected abstract List<String> getXMLS();

  protected abstract ResourceIdentifier getCompositeAssetId();

  protected abstract UUID getRootAssetID();

  protected abstract String getRootAssetVersion();

  protected String getDictionaryURI() {
    return "https://clinicalknowledgemanagement.mayo.edu/artifacts/deda9ce8-ca68-456e-b9d1-96d338469988";
  }


  // TODO This is a placeholder until we can implement the TerminologyProvider
  private TermsApiInternal getMockTermServer() {
    return new TermsApiInternal() {
      @Override
      public Answer<ConceptDescriptor> getTerm(UUID vocabularyId, String versionTag,
          String conceptId) {
        return Answer.unsupported();
      }

      @Override
      public Answer<List<Pointer>> listTerminologies() {
        return Answer.of(
            singletonList(new Pointer().withHref(URI.create(getDictionaryURI())))
        );
      }
    };
  }


}
