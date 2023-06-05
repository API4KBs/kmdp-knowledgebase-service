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
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal._applyLift;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal._applyNamedLift;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevel;
import org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries;

public interface Lifter extends _applyLift, _applyNamedLift {


  @Override
  default Answer<KnowledgeCarrier> applyNamedLift(UUID uuid, KnowledgeCarrier knowledgeCarrier,
      String parsingLevel, String xAccept, String props) {
    return Answer.ofTry(ParsingLevelSeries.resolveTag(parsingLevel))
        .flatMap(pl -> applyNamedLift(uuid, knowledgeCarrier, pl, xAccept, props));
  }

  default Answer<KnowledgeCarrier> applyNamedLift(UUID uuid, KnowledgeCarrier knowledgeCarrier,
      ParsingLevel parsingLevel, String xAccept, String props) {
    return uuid.equals(getOperatorId().getUuid())
        ? applyLift(knowledgeCarrier, parsingLevel, xAccept, props)
        : Answer.unsupported();
  }

  @Override
  default Answer<KnowledgeCarrier> applyLift(KnowledgeCarrier knowledgeCarrier,
      String parsingLevel, String xAccept, String props) {
    return Answer.ofTry(ParsingLevelSeries.resolveTag(parsingLevel))
        .flatMap(pl -> applyLift(knowledgeCarrier, pl, xAccept, props));
  }

  Answer<KnowledgeCarrier> applyLift(KnowledgeCarrier knowledgeCarrier,
      ParsingLevel parsingLevel, String xAccept, String props);

  ResourceIdentifier getOperatorId();

  /**
   * Lifts a binary-encoded expression (byte[])
   * into an serialized expression (String)
   *
   * @see Lowerer#innerEncode(KnowledgeCarrier, Properties)
   * @param carrier A binary carrier
   * @return A string carrier
   */
  Optional<KnowledgeCarrier> innerDecode(KnowledgeCarrier carrier, Properties config);

  /**
   * Lifts a serialized expression (String)
   * into a concrete expression (parse tree)
   * @param carrier A string carrier
   * @return A parse tree carrier
   */
  Optional<KnowledgeCarrier> innerDeserialize(KnowledgeCarrier carrier, Properties config);

  /**
   * Lifts a serialized expression (String)
   * into an abstract expression (abstract syntax tree)
   * @param carrier A string carrier
   * @return An abstract syntax tree carrier
   */
  Optional<KnowledgeCarrier> innerParse(KnowledgeCarrier carrier, Properties config);


  /**
   * Lifts a concrete expression (parse tree)
   * into an abstract expression (abstract syntax tree)
   * @param carrier A parse tree carrier
   * @return An abstract syntax tree carrier
   */
  Optional<KnowledgeCarrier> innerAbstract(KnowledgeCarrier carrier, Properties config);
}