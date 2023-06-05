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

import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal._applyLower;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal._applyNamedLower;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevel;
import org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries;

public interface Lowerer extends _applyLower, _applyNamedLower {

  @Override
  default Answer<KnowledgeCarrier> applyNamedLower(UUID uuid, KnowledgeCarrier knowledgeCarrier,
      String parsingLevel, String xAccept, String props) {
    return Answer.ofTry(ParsingLevelSeries.resolveTag(parsingLevel))
        .flatMap(pl -> applyNamedLower(uuid, knowledgeCarrier, pl, xAccept, props));
  }

  default Answer<KnowledgeCarrier> applyNamedLower(UUID uuid, KnowledgeCarrier knowledgeCarrier,
      ParsingLevel parsingLevel, String xAccept, String props) {
    return uuid.equals(getOperatorId().getUuid())
        ? applyLower(knowledgeCarrier, parsingLevel, xAccept, props)
        : Answer.unsupported();
  }
  
  @Override
  default Answer<KnowledgeCarrier> applyLower(KnowledgeCarrier knowledgeCarrier,
      String parsingLevel, String xAccept, String props) {
    return Answer.ofTry(ParsingLevelSeries.resolveTag(parsingLevel))
        .flatMap(pl -> applyLower(knowledgeCarrier, pl, xAccept, props));
  }

  Answer<KnowledgeCarrier> applyLower(KnowledgeCarrier knowledgeCarrier,
      ParsingLevel parsingLevel, String xAccept, String props);

  ResourceIdentifier getOperatorId();

  /**
   * Lowers a concrete expression (String)
   * into a binary-encoded expression (byte[])
   *
   * @see Lifter#innerDecode(KnowledgeCarrier, Properties)
   * @param carrier A binary carrier
   * @param into A representation that provides the details of encoding
   *             Must be compatible with the source representation at the
   *             language/syntax/serialization level
   * @return A string carrier
   */
  Optional<KnowledgeCarrier> innerEncode(KnowledgeCarrier carrier, SyntacticRepresentation into, Properties config);

  /**
   * Lowers an abstract expression (abstract syntax tree)
   * into a concrete expression (string)
   *
   * @see Lifter#innerParse(KnowledgeCarrier, Properties)
   * @param carrier A parse tree carrier
   * @param into A representation that defines how to derive the serialized, concrete expression
   *             Must be compatible with the source representation at the language level
   *             (Note: only String is currently supported)
   * @return A string carrier
   */
  Optional<KnowledgeCarrier> innerExternalize(KnowledgeCarrier carrier, SyntacticRepresentation into, Properties config);

  /**
   * Lowers a parsed expression (parse tree)
   * into a concrete expression (string)
   *
   * @see Lifter#innerDeserialize(KnowledgeCarrier, Properties)
   * @param carrier A parse tree carrier
   * @param into A representation that defines how to derive the serialized, concrete expression
   *             Must be compatible with the source representation at the language/syntax level
   *             (Note: only String is currently supported)
   * @return A string carrier
   */
  Optional<KnowledgeCarrier> innerSerialize(KnowledgeCarrier carrier, SyntacticRepresentation into, Properties config);


  /**
   * Lowers an abstract expression (abstract syntax tree)
   * into a parsed expression for a given syntax (parse tree)
   *
   * @see Lifter#innerAbstract(KnowledgeCarrier, Properties)
   * @param carrier A parse tree carrier
   * @param into A representation that defines how to derive the concrete expression in a given syntax
   *             Must be compatible with the source representation at the language level
   * @return A string carrier
   */
  Optional<KnowledgeCarrier> innerConcretize(KnowledgeCarrier carrier, SyntacticRepresentation into, Properties config);


  default Optional<KnowledgeCarrier> innerEncode(KnowledgeCarrier carrier, Properties config) {
    return innerEncode(carrier,null, config);
  }

  default Optional<KnowledgeCarrier> innerExternalize(KnowledgeCarrier carrier, Properties config) {
    return innerExternalize(carrier,null, config);
  }

  default Optional<KnowledgeCarrier> innerSerialize(KnowledgeCarrier carrier, Properties config) {
    return innerSerialize(carrier,null, config);
  }

  default Optional<KnowledgeCarrier> innerConcretize(KnowledgeCarrier carrier, Properties config) {
    return innerConcretize(carrier,null, config);
  }
}
