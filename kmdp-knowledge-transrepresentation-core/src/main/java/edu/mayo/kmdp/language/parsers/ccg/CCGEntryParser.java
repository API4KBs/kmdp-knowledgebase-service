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
package edu.mayo.kmdp.language.parsers.ccg;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Lifting_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Lowering_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Clinical_Concept_Glossary;

import edu.mayo.kmdp.ccg.model.GlossaryEntry;
import edu.mayo.kmdp.language.parsers.JSONBasedLanguageParser;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.inject.Named;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;

@Named
@KPOperation(Lowering_Task)
@KPOperation(Lifting_Task)
public class CCGEntryParser extends JSONBasedLanguageParser<GlossaryEntry> {

  public static final UUID id = UUID.fromString("469abd00-a398-40fb-a4ee-5dd9a8ab5f43");
  public static final String version = "1.0.0";

  private final List<SyntacticRepresentation> supportedRepresentations = Arrays.asList(
      rep(Clinical_Concept_Glossary, JSON, Charset.defaultCharset()));

  public CCGEntryParser() {
    setId(SemanticIdentifier.newId(id, version));
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return Clinical_Concept_Glossary;
  }

  @Override
  public List<SyntacticRepresentation> getSupportedRepresentations() {
    return supportedRepresentations;
  }

}
