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

import static edu.mayo.kmdp.language.ccg.library.mock.MockVocabulary.Has_Diabetes_Mellitus;
import static edu.mayo.kmdp.registry.Registry.DID_URN_URI;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.Defines;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.Has_Primary_Subject;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.In_Terms_Of;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.newSurrogate;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Includes_By_Reference;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Assessment_Predictive_And_Inferential_Models;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetrole.KnowledgeAssetRoleSeries.Operational_Concept_Definition;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Assessment_Model;

import java.net.URI;
import java.util.regex.Pattern;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.id.Term;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder;

public class MockComplexDef {

  public static final ResourceIdentifier assetId =
      SurrogateBuilder.assetId(DID_URN_URI, "a1a19e62-72b9-478f-ba3a-ace681c61665", "0.0.1");

  Term theFocalConcept = Term.sct("Complex diabetes case (disorder)", "dbm");

  public static final MockVocabulary definedConcept = Has_Diabetes_Mellitus;

  public KnowledgeAsset buildSurrogate() {

    KnowledgeAsset master = newSurrogate(assetId)
        .withName("Diabetes", "Complex Operational Definition")
        .withFormalType(Assessment_Predictive_And_Inferential_Models, Assessment_Model)
        .withAnnotation(Has_Primary_Subject,theFocalConcept)
        .withAnnotation(Defines, definedConcept)
        .withAnnotation(In_Terms_Of, MockOpDef.definedConcept)
        .withAnnotation(In_Terms_Of, MockOpDef2.definedConcept)
        .withDependency(Includes_By_Reference,MockOpDef.assetId)
        .withDependency(Includes_By_Reference,MockOpDef2.assetId)
        .get()
        .withRole(Operational_Concept_Definition);

    return master;
  }

  private ResourceIdentifier fromSystemSpecificURI(String id) {
    return SemanticIdentifier.newVersionId(URI.create(id),
        Pattern.compile("(.*)\\/(.*)\\/(.*)"),2,3 );
  }
}

