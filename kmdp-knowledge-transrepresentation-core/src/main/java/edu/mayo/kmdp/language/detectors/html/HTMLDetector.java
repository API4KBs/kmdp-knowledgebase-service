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
package edu.mayo.kmdp.language.detectors.html;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Language_Information_Detection_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;

import edu.mayo.kmdp.language.DetectApiOperator;
import edu.mayo.kmdp.language.detectors.AbstractLanguageDetector;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Named;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@KPOperation(Language_Information_Detection_Task)
@KPSupport(HTML)
public class HTMLDetector
  extends AbstractLanguageDetector
    implements DetectApiOperator {

  static UUID id = UUID.randomUUID();
  static String version = "1.0.0";

  protected static final Logger logger = LoggerFactory.getLogger(HTMLDetector.class);

  public HTMLDetector() {
    setId(SemanticIdentifier.newId(id,version));
  }

  @Override
  public List<SyntacticRepresentation> getInto() {
    return getSupportedRepresentations();
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return HTML;
  }

  @Override
  public List<SyntacticRepresentation> getSupportedRepresentations() {
    return Arrays.asList(
        rep(HTML),
        rep(HTML,TXT));
  }

  @Override
  protected Optional<SyntacticRepresentation> detectBinary(byte[] bytes) {
    return detectString(new String(bytes));
  }

  @Override
  protected Optional<SyntacticRepresentation> detectString(String str) {
    if (isHtml(str)) {
      return Optional.of(new SyntacticRepresentation()
          .withLanguage(HTML)
          .withFormat(TXT));
    } else {
      return Optional.empty();
    }
  }

  @Override
  protected Optional<SyntacticRepresentation> detectTree(Object ast) {
    return detectAST(ast);
  }

  @Override
  protected Optional<SyntacticRepresentation> detectAST(Object parseTree) {
    return (parseTree instanceof Document)
        ? Optional.of(rep(HTML))
        : Optional.empty();
  }

  private boolean isHtml(String s) {
    Document dirtyDoc = Jsoup.parse(s);
    Document dox = new Cleaner(Whitelist.relaxed()).clean(dirtyDoc);
    return ! (dox.body().children().isEmpty() && dox.head().children().isEmpty());
  }

}
