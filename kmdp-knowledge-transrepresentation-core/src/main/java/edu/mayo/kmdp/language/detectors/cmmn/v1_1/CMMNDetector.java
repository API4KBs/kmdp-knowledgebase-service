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
package edu.mayo.kmdp.language.detectors.cmmn.v1_1;

import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Language_Information_Detection_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.CMMN_1_1;

import edu.mayo.kmdp.language.DetectApiOperator;
import edu.mayo.kmdp.language.detectors.XMLBasedLanguageDetector;
import java.util.List;
import java.util.UUID;
import javax.inject.Named;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.omg.spec.cmmn._20151109.model.TDefinitions;

@Named
@KPOperation(Language_Information_Detection_Task)
@KPSupport(CMMN_1_1)
public class CMMNDetector
    extends XMLBasedLanguageDetector<TDefinitions>
    implements DetectApiOperator {

  static UUID id = UUID.randomUUID();
  static String version = "1.0.0";

  public CMMNDetector() {
    setId(SemanticIdentifier.newId(id,version));
    this.root = TDefinitions.class;
  }

  @Override
  public List<SyntacticRepresentation> getInto() {
    return getSupportedRepresentations();
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return CMMN_1_1;
  }

}
