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
package edu.mayo.kmdp.language.parsers.mvf.v1_0;


import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Lifting_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Lowering_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.MVF_1_0;

import edu.mayo.kmdp.language.DeserializeApiOperator;
import edu.mayo.kmdp.language.parsers.XMLBasedLanguageParser;
import java.util.UUID;
import javax.inject.Named;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.omg.spec.mvf._20220702.mvf.MVFDictionary;
import org.omg.spec.mvf._20220702.mvf.ObjectFactory;

@Named
@KPOperation(Lifting_Task)
@KPOperation(Lowering_Task)
@KPSupport(MVF_1_0)
public class MVFParser extends XMLBasedLanguageParser<MVFDictionary>
    implements DeserializeApiOperator {

  public static final UUID OPERATOR_ID = UUID.fromString("57384716-c03b-4509-97c3-72265fe4ca51");
  public static final String OPERATOR_VERSION = "1.0.0";

  public MVFParser() {
    setId(SemanticIdentifier.newId(OPERATOR_ID, OPERATOR_VERSION));
    this.classContext.clear();
    this.classContext.add(ObjectFactory.class);
    configRootClass(MVFDictionary.class);
    this.mapper = new ObjectFactory()::createMVFDictionary;
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return MVF_1_0;
  }

}