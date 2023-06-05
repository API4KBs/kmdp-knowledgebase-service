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
package edu.mayo.kmdp.language;

import static java.util.Collections.singletonList;
import static org.omg.spec.api4kp._20200801.Answer.anyDo;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Lifting_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Lowering_Task;

import edu.mayo.kmdp.language.exceptions.ParsingException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Named;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.KnowledgePlatformComponent;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DiscoveryApiInternal;
import org.omg.spec.api4kp._20200801.id.KeyIdentifier;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.transrepresentation.DeserializationOperator;
import org.omg.spec.api4kp._20200801.services.transrepresentation.Deserializer;
import org.springframework.beans.factory.annotation.Autowired;

@Named
@KPServer
public class LanguageDeSerializer implements KnowledgePlatformComponent<Deserializer>,
    DeserializeApiInternal, DiscoveryApiInternal._getDeserializationComponent, DiscoveryApiInternal._listDeserializationComponents {

  private UUID id = UUID.randomUUID();
  private Deserializer descriptor;

  Map<KeyIdentifier, DeserializeApiOperator> deserializers;

  @Named
  public LanguageDeSerializer(@Autowired(required = false)
  @KPOperation(Lifting_Task)
  @KPOperation(Lowering_Task)
      List<DeserializeApiOperator> deserializers) {

    this.deserializers = deserializers.stream()
        .collect(Collectors.toMap(
            det -> det.getOperatorId().asKey(),
            det -> det
        ));

    this.descriptor = toKPComponent(getComponentId());
  }

  @Override
  public Answer<Deserializer> getDeserializationComponent(UUID componentId) {
    return Answer.of(getDescriptor());
  }

  @Override
  public Answer<List<Deserializer>> listDeserializationComponents(String from, String into, String method) {
    return Answer.of(singletonList(getDescriptor()));
  }

  @Override
  public Answer<KnowledgeCarrier> applyLift(KnowledgeCarrier sourceArtifact,
      String levelTag, String xAccept, String config) {
    return anyDo(
        getOperations(
            deserializers.values(),
            DeserializeApiOperator::can_applyLift,
            DeserializeApiOperator::as_applyLift),
        a -> a.applyLift(sourceArtifact, levelTag, xAccept, config),
        () -> Answer.failed(new ParsingException(sourceArtifact)));
  }

  @Override
  public Answer<KnowledgeCarrier> applyLower(KnowledgeCarrier sourceArtifact,
      String levelTag, String xAccept, String config) {
    return anyDo(
        getOperations(
            deserializers.values(),
            DeserializeApiOperator::can_applyLower,
            DeserializeApiOperator::as_applyLower),
        a -> a.applyLower(sourceArtifact, levelTag, xAccept, config),
        () -> Answer.failed(new ParsingException(sourceArtifact)));
  }

  @Override
  public Answer<KnowledgeCarrier> applyNamedLift(UUID operatorId, KnowledgeCarrier sourceArtifact,
      String levelTag, String xAccept, String config) {
    return Answer.ofTry(getDeserializer(operatorId))
        .flatOpt(DeserializeApiOperator::as_applyNamedLift)
        .flatMap(a -> a.applyNamedLift(operatorId, sourceArtifact, levelTag, xAccept, config));
  }

  @Override
  public Answer<KnowledgeCarrier> applyNamedLower(
      UUID operatorId,
      KnowledgeCarrier sourceArtifact,
      String levelTag, String xAccept, String config) {
    return Answer.ofTry(getDeserializer(operatorId))
        .flatOpt(DeserializeApiOperator::as_applyNamedLower)
        .flatMap(a -> a.applyNamedLower(operatorId, sourceArtifact, levelTag, xAccept, config));
  }



  @Override
  public Answer<DeserializationOperator> getLiftOperator(UUID operatorId) {
    return Answer.of(
        getDeserializer(operatorId)
            .filter(op -> op instanceof _applyLift)
            .map(DeserializeApiOperator::getDescriptor)
    );
  }

  @Override
  public Answer<DeserializationOperator> getLowerOperator(UUID operatorId) {
    return Answer.of(
        getDeserializer(operatorId)
            .filter(op -> op instanceof _applyLower)
            .map(DeserializeApiOperator::getDescriptor)
    );
  }

  @Override
  public Answer<List<DeserializationOperator>> listLiftOperators(String from, String into) {
    return Answer.of(
        deserializers.values().stream()
            .filter(op -> op instanceof _applyLift)
            .filter(op -> op.consumes(from))
            .filter(op -> op.produces(into))
            .map(DeserializeApiOperator::getDescriptor)
            .collect(Collectors.toList()));
  }

  @Override
  public Answer<List<DeserializationOperator>> listLowerOperators(String from, String into) {
    return Answer.of(
        deserializers.values().stream()
            .filter(op -> op instanceof _applyLower)
            .filter(op -> op.consumes(from))
            .filter(op -> op.produces(into))
            .map(DeserializeApiOperator::getDescriptor)
            .collect(Collectors.toList()));
  }

  @Override
  public UUID getComponentUuid() {
    return id;
  }

  @Override
  public Deserializer toKPComponent(ResourceIdentifier componentId) {
    return new Deserializer()
        .withInstanceId(getComponentId());
  }

  @Override
  public Deserializer getDescriptor() {
    return descriptor;
  }

  private Optional<DeserializeApiOperator> getDeserializer(UUID operatorId) {
    return deserializers.keySet().stream()
        .filter(key -> key.getUuid().equals(operatorId))
        .findFirst()
        .map(deserializers::get);
  }

}
