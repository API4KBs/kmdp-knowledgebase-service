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


import static org.omg.spec.api4kp._20200801.contrastors.SyntacticRepresentationContrastor.theRepContrastor;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Lifting_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Lowering_Task;

import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.TriFunction;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Predicate;
import javax.inject.Named;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormat;

@Named
@KPOperation(Lowering_Task)
@KPOperation(Lifting_Task)
public abstract class MultiFormatParser<T> extends AbstractDeSerializeOperator {

  private final List<AbstractDeSerializeOperator> parserSet;

  protected MultiFormatParser(XMLBasedLanguageParser<T> xmlParser,
      JSONBasedLanguageParser<T> jsonParser) {
    this.parserSet = Arrays.asList(xmlParser,jsonParser);
  }

  @Override
  public Optional<KnowledgeCarrier> innerAbstract(KnowledgeCarrier carrier, Properties config) {
    return processLift(
        carrier,
        Lifter::innerAbstract,
        config);
  }

  @Override
  public Optional<KnowledgeCarrier> innerDecode(KnowledgeCarrier carrier, Properties config) {
    return processLift(
        carrier,
        Lifter::innerDecode,
        config);
  }

  @Override
  public Optional<KnowledgeCarrier> innerDeserialize(KnowledgeCarrier carrier, Properties config) {
    return processLift(
        carrier,
        Lifter::innerDeserialize,
        config);
  }

  @Override
  public Optional<KnowledgeCarrier> innerParse(KnowledgeCarrier carrier, Properties config) {
    return processLift(
        carrier,
        Lifter::innerParse,
        config);
  }


  private boolean isLiftCandidate(AbstractDeSerializeOperator candidate,
      SyntacticRepresentation argumentRep) {
    return supportsFormat(candidate, argumentRep)
        && candidate.getSupportedRepresentations()
        .stream()
        .anyMatch(supportedRep -> theRepContrastor.isNarrowerOrEqual(supportedRep, argumentRep));
  }

  private boolean isLowerCandidate(AbstractDeSerializeOperator candidate,
      SyntacticRepresentation argumentRep) {
    return supportsFormat(candidate, argumentRep)
        && candidate.getSupportedRepresentations()
        .stream()
        .anyMatch(supportedRep -> theRepContrastor.isNarrowerOrEqual(supportedRep, argumentRep));
  }

  private boolean supportsFormat(AbstractDeSerializeOperator candidate, SyntacticRepresentation argumentRep) {
    return argumentRep.getFormat() == null || argumentRep.getFormat().sameAs(candidate.getDefaultFormat());
  }

  protected Optional<KnowledgeCarrier> processLift(KnowledgeCarrier source,
      TriFunction<AbstractDeSerializeOperator,KnowledgeCarrier,Properties,Optional<KnowledgeCarrier>> mapper,
      Properties config) {
    return process(
        source,
        mapper,
        parser -> isLiftCandidate(parser, source.getRepresentation()),
        config);
  }

  protected Optional<KnowledgeCarrier> processLower(KnowledgeCarrier source,
      SyntacticRepresentation into,
      TriFunction<AbstractDeSerializeOperator,KnowledgeCarrier,Properties,Optional<KnowledgeCarrier>> mapper,
      Properties config) {
    return process(
        source,
        mapper,
        parser -> isLowerCandidate(parser, source.getRepresentation()) && checkTargetCompatibility(into,parser.getDefaultFormat()),
        config);
  }

  protected Optional<KnowledgeCarrier> process(KnowledgeCarrier source,
      TriFunction<AbstractDeSerializeOperator,KnowledgeCarrier,Properties,Optional<KnowledgeCarrier>> mapper,
      Predicate<AbstractDeSerializeOperator> test,
      Properties config) {
    return parserSet.stream()
        .filter(test)
        .map(l -> mapper.apply(l,source, config))
        .flatMap(StreamUtil::trimStream)
        .findFirst();
  }


  @Override
  public Optional<KnowledgeCarrier> innerConcretize(KnowledgeCarrier carrier, SyntacticRepresentation into, Properties config) {
    return processLower(
        carrier,
        into,
        (ser,kc,cfg) -> ser.innerConcretize(kc,into,cfg),
        config);
  }

  @Override
  public Optional<KnowledgeCarrier> innerEncode(KnowledgeCarrier carrier, SyntacticRepresentation into, Properties config) {
    return processLower(
        carrier,
        into,
        (ser,kc, cfg) -> ser.innerEncode(kc,into,cfg),
        config);
  }

  @Override
  public Optional<KnowledgeCarrier> innerExternalize(KnowledgeCarrier carrier, SyntacticRepresentation into, Properties config) {
    return processLower(
        carrier,
        into,
        (ser,kc,cfg) -> ser.innerExternalize(kc,into,cfg),
        config);
  }

  @Override
  public Optional<KnowledgeCarrier> innerSerialize(KnowledgeCarrier carrier,
      SyntacticRepresentation into, Properties config) {
    return processLower(
        carrier,
        into,
        (ser,kc,cfg) -> ser.innerSerialize(kc,into,config),
        config);
  }

  @Override
  protected SerializationFormat getDefaultFormat() {
    return null;
  }

  protected boolean checkTargetCompatibility(SyntacticRepresentation into, SerializationFormat format) {
    return
        into.getLanguage().sameAs(getSupportedLanguage())
            &&
            (into.getFormat() == null || into.getFormat().sameAs(format));
  }
}
