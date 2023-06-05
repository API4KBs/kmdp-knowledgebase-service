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
package edu.mayo.kmdp.language.parsers.html;


import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Lifting_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Lowering_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Serialized_Knowledge_Expression;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Encoded_Knowledge_Expression;

import edu.mayo.kmdp.language.parsers.AbstractDeSerializeOperator;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import javax.inject.Named;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormat;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;

@Named
@KPOperation(Lifting_Task)
@KPOperation(Lowering_Task)
@KPSupport(HTML)
public class HtmlDeserializer
    extends AbstractDeSerializeOperator {

  public static final UUID id = UUID.fromString("05e4999d-4e70-44de-b5c0-fdd511a62fe7");
  public static final String version = "1.0.0";

  public HtmlDeserializer() {
    this.operatorId = SemanticIdentifier.newId(id, version);
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return HTML;
  }

  @Override
  protected List<SyntacticRepresentation> getSupportedRepresentations() {
    return Arrays.asList(
        rep(HTML),
        rep(HTML,XML_1_1),
        rep(HTML,XML_1_1, Charset.defaultCharset()),
        rep(HTML,XML_1_1, Charset.defaultCharset(), Encodings.DEFAULT),
        rep(HTML,TXT),
        rep(HTML,TXT, Charset.defaultCharset()),
        rep(HTML,TXT, Charset.defaultCharset(), Encodings.DEFAULT)
    );
  }

  @Override
  protected SerializationFormat getDefaultFormat() {
    return TXT;
  }

  @Override
  public Optional<KnowledgeCarrier> innerDeserialize(KnowledgeCarrier carrier, Properties config) {
    return Optional.empty();
  }

  @Override
  public Optional<KnowledgeCarrier> innerParse(KnowledgeCarrier carrier, Properties config) {
    Document doh = carrier.asString()
        .map(Jsoup::parse)
        .orElseThrow(UnsupportedOperationException::new);
    return Optional
        .of(newVerticalCarrier(carrier, Abstract_Knowledge_Expression, rep(HTML), doh));
  }

  @Override
  public Optional<KnowledgeCarrier> innerAbstract(KnowledgeCarrier carrier, Properties config) {
    return Optional.empty();
  }

  @Override
  public Optional<KnowledgeCarrier> innerEncode(KnowledgeCarrier carrier,
      SyntacticRepresentation into, Properties config) {
    return carrier.asBinary()
        .map(bytes -> newVerticalCarrier(
            carrier,
            Encoded_Knowledge_Expression,
            getParseResultRepresentation(carrier,
                Encoded_Knowledge_Expression),
            bytes
        ));
  }

  @Override
  public Optional<KnowledgeCarrier> innerExternalize(KnowledgeCarrier carrier,
      SyntacticRepresentation into, Properties config) {
    return carrier.as(Document.class)
        .map(Document::outerHtml)
        .map(str -> newVerticalCarrier(carrier,
            Serialized_Knowledge_Expression,
            rep(HTML,TXT,Charset.defaultCharset()),
            str));
  }


  @Override
  public Optional<KnowledgeCarrier> innerSerialize(KnowledgeCarrier carrier,
      SyntacticRepresentation into, Properties config) {
    return Optional.empty();
  }

  @Override
  public Optional<KnowledgeCarrier> innerConcretize(KnowledgeCarrier carrier,
      SyntacticRepresentation into, Properties config) {
    return Optional.empty();
  }
}
