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
package edu.mayo.kmdp.language.parsers;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Concrete_Knowledge_Expression;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Encoded_Knowledge_Expression;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Serialized_Knowledge_Expression;

import com.fasterxml.jackson.databind.JsonNode;
import edu.mayo.kmdp.util.JSonUtil;
import edu.mayo.kmdp.util.Util;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormat;

public abstract class JSONBasedLanguageParser<T> extends AbstractDeSerializeOperator {

  protected Class<T> root;

  @Override
  public Optional<KnowledgeCarrier> innerDeserialize(KnowledgeCarrier carrier, Properties config) {
    return carrier.asString()
        .flatMap(JSonUtil::readJson)
        .map(json -> newVerticalCarrier(carrier, Concrete_Knowledge_Expression, null, json));
  }

  @Override
  public Optional<KnowledgeCarrier> innerParse(KnowledgeCarrier carrier, Properties config) {
    return carrier.asString()
        .flatMap(str -> JSonUtil.parseJson(str, root))
        .map(ast -> newVerticalCarrier(carrier, Abstract_Knowledge_Expression, null, ast));
  }


  @Override
  public Optional<KnowledgeCarrier> innerAbstract(KnowledgeCarrier carrier, Properties config) {
    return carrier.as(JsonNode.class)
        .flatMap(jNode -> JSonUtil.parseJson(jNode, root))
        .map(ast -> newVerticalCarrier(carrier, Abstract_Knowledge_Expression, null, ast));
  }


  @Override
  public Optional<KnowledgeCarrier> innerEncode(KnowledgeCarrier carrier,
      SyntacticRepresentation into, Properties config) {
    return carrier.asBinary()
        .map(bytes -> newVerticalCarrier(carrier, Encoded_Knowledge_Expression, into, bytes));
  }

  @Override
  public Optional<KnowledgeCarrier> innerExternalize(KnowledgeCarrier carrier,
      SyntacticRepresentation into, Properties config) {
    return carrier.as(root)
        .flatMap(JSonUtil::writeJson)
        .flatMap(Util::asString)
        .map(str -> newVerticalCarrier(carrier, Serialized_Knowledge_Expression, into, str));
  }

  @Override
  public Optional<KnowledgeCarrier> innerSerialize(KnowledgeCarrier carrier,
      SyntacticRepresentation into, Properties config) {
    return carrier.as(JsonNode.class)
        .map(JsonNode::toString)
        .map(str -> newVerticalCarrier(carrier, Serialized_Knowledge_Expression, into, str));
  }

  @Override
  public Optional<KnowledgeCarrier> innerConcretize(KnowledgeCarrier carrier,
      SyntacticRepresentation into, Properties config) {
    return carrier.as(root)
        .flatMap(JSonUtil::toJsonNode)
        .map(json -> newVerticalCarrier(carrier, Concrete_Knowledge_Expression, into, json));
  }


  @Override
  public List<SyntacticRepresentation> getSupportedRepresentations() {
    return Arrays.asList(
        rep(getSupportedLanguage()),
        rep(getSupportedLanguage(), JSON),
        rep(getSupportedLanguage(), JSON, Charset.defaultCharset()),
        rep(getSupportedLanguage(), JSON, Charset.defaultCharset(), Encodings.DEFAULT));
  }

  @Override
  public SerializationFormat getDefaultFormat() {
    return JSON;
  }

}
