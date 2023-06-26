/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.mayo.kmdp.language.ccg.library.mock;

import static edu.mayo.kmdp.language.ccg.library.mock.MockVocabulary.Has_Hypertension;
import static edu.mayo.kmdp.language.ccg.library.mock.MockVocabulary.Has_Hypertension_Is;
import static edu.mayo.kmdp.registry.Registry.DID_URN_URI;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.newSurrogate;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper.toAnonymousCompositeAsset;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Effectuates;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Imports;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Includes_By_Reference;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Rules_Policies_And_Guidelines;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Structured_Information_And_Data_Capture_Models;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Information_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Service_Profile;

import java.net.URI;
import java.util.Arrays;
import java.util.regex.Pattern;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.id.Term;
import org.omg.spec.api4kp._20200801.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries;

public class MockOpDef {

  public static final ResourceIdentifier artifactId =
      SurrogateBuilder.artifactId(DID_URN_URI, "469dc12b-f20c-4c1f-9636-ac12caf1e38d", "0.0.1");

  public static final ResourceIdentifier assetId =
      SurrogateBuilder.assetId(DID_URN_URI, "05aec46b-05f3-483b-b283-6a2b76af1d8f", "0.0.0");

  public static final ResourceIdentifier coreId =
      SurrogateBuilder.assetId(DID_URN_URI, "0a61814a-a274-489a-8a48-4b66c5edf79f", "0.0.1");

  Term theFocalConcept = Term.sct("Hypertension (disorder)", "xyz");

  public static final MockVocabulary definedConcept =  Has_Hypertension_Is;
  private static final MockVocabulary theCorePC = Has_Hypertension;


  public KnowledgeCarrier buildArtifact() {
    return AbstractCarrier.of("test service profile".getBytes())
        .withAssetId(coreId)
        .withArtifactId(artifactId);
  }

  public CompositeKnowledgeCarrier buildSurrogate() {

    KnowledgeAsset mockApi = new MockAPISpec().buildSurrogate();

    KnowledgeAsset subReturnDatatype = new KnowledgeAsset()
        .withAssetId(SurrogateBuilder.randomAssetId())
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(fromSystemSpecificURI("http://foo/v2/Observation")));

    KnowledgeAsset masterReturnDatatype = new KnowledgeAsset()
        .withAssetId(SurrogateBuilder.randomAssetId())
        .withFormalCategory(Structured_Information_And_Data_Capture_Models)
        .withFormalType(Information_Model)
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(fromSystemSpecificURI("http://foo/v2/boolean")));

    KnowledgeAsset sub = newSurrogate(coreId, false)
        .withFormalType(Rules_Policies_And_Guidelines, KnowledgeAssetTypeSeries.Service_Profile)
        .withName(theFocalConcept.getPrefLabel(), "Service Profile for : Has Hypertension")
        .withQueryType()
        .asOperationalDefinition(theFocalConcept, theCorePC)
        .aaS()
        .withDependency(Imports, MockAPISpec.assetId)
        .withDependency(Effectuates, subReturnDatatype.getAssetId())
        .get()
        .withCarriers(new KnowledgeArtifact()
        .withArtifactId(artifactId)
        .withRepresentation(new SyntacticRepresentation()
            .withLanguage(Service_Profile)
            .withFormat(JSON)));

    // Identity of the Def as an Asset
    KnowledgeAsset master = newSurrogate(assetId)
        .withName(definedConcept.getLabel(), "Path Expr for : Has Hypertension - Is")
        .withExpressionType()
        .asOperationalDefinition(theFocalConcept, definedConcept)
        .withDependency(Includes_By_Reference, sub.getAssetId())
        .withInlinedFhirPath("X.exists()")
        .withDependency(Effectuates,masterReturnDatatype.getAssetId())
        .get();

    return
        toAnonymousCompositeAsset(
            master.getAssetId(),
            Arrays.asList(
                master,
                sub,
                masterReturnDatatype,
                subReturnDatatype,
                mockApi));
  }

  private ResourceIdentifier fromSystemSpecificURI(String id) {
    return SemanticIdentifier.newVersionId(URI.create(id),
        Pattern.compile("(.*)\\/(.*)\\/(.*)"),2,3 );
  }
}

