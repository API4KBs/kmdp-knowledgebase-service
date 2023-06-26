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
package edu.mayo.kmdp.language.ccg.library.mock;

import static edu.mayo.kmdp.language.ccg.library.mock.MockVocabulary.Has_Allergy_To_Statins;
import static edu.mayo.kmdp.language.ccg.library.mock.MockVocabulary.Has_Diabetes_Mellitus;
import static edu.mayo.kmdp.registry.Registry.DID_URN_URI;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.newSurrogate;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper.toAnonymousCompositeAsset;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Effectuates;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Imports;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Rules_Policies_And_Guidelines;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Service_Profile;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;

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
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries;

public class MockOpDef3 {

  public static final ResourceIdentifier artifactId =
      SurrogateBuilder.artifactId(DID_URN_URI, "fc965815-a927-443c-b433-c879d8ff93fb",
          "0.0.1");

  public static final ResourceIdentifier assetId =
      SurrogateBuilder.assetId(DID_URN_URI, "1eac8cde-ab6f-4741-9564-a8483e1b49db", "0.0.1");
  public static final MockVocabulary definedConcept = Has_Allergy_To_Statins;
  public static final MockVocabulary definedConcept2 = Has_Diabetes_Mellitus;
  Term theFocalConcept = Term.sct("Statin Allergy (disorder)", "stat");

  public KnowledgeCarrier buildArtifact() {
    return AbstractCarrier.of("test service profile 2".getBytes())
        .withAssetId(assetId)
        .withArtifactId(artifactId);
  }

  public CompositeKnowledgeCarrier buildSurrogate() {

    KnowledgeAsset mockApi = new MockAPISpec().buildSurrogate();

    KnowledgeAsset returnDatatype = new KnowledgeAsset()
        .withAssetId(SurrogateBuilder.randomAssetId())
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(fromSystemSpecificURI("http://foo/v2/Condition")));

    KnowledgeAsset master = newSurrogate(assetId, false)
        .withFormalType(Rules_Policies_And_Guidelines, Service_Profile)
        .withName(theFocalConcept.getPrefLabel(), "Service Profile for : Has Allergy to Statins")
        .withQueryType()
        .asOperationalDefinition(theFocalConcept, definedConcept)
        .asOperationalDefinition(null, definedConcept2)
        .aaS()
        .withDependency(Imports, MockAPISpec.assetId)
        .withDependency(Effectuates, returnDatatype.getAssetId())
        .get()
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(artifactId)
            .withRepresentation(new SyntacticRepresentation()
                .withLanguage(KnowledgeRepresentationLanguageSeries.Service_Profile)
                .withFormat(JSON)));

    CompositeKnowledgeCarrier ckc =
        toAnonymousCompositeAsset(
            master.getAssetId(),
            Arrays.asList(
                master,
                returnDatatype,
                mockApi));

    ckc.withRootId(master.getAssetId());
    return ckc;
  }

  private ResourceIdentifier fromSystemSpecificURI(String id) {
    return SemanticIdentifier.newVersionId(URI.create(id),
        Pattern.compile("(.*)\\/(.*)\\/(.*)"), 2, 3);
  }
}

