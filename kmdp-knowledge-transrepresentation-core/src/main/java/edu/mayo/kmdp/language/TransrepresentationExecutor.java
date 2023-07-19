/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.mayo.kmdp.language;

import static java.util.Collections.singletonList;
import static org.omg.spec.api4kp._20200801.Answer.anyDo;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Syntactic_Translation_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Transcreation_Task;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Named;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.KnowledgePlatformComponent;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DiscoveryApiInternal._getTxComponent;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DiscoveryApiInternal._listTxComponents;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.TransxionApiInternal;
import org.omg.spec.api4kp._20200801.id.KeyIdentifier;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.transrepresentation.TransrepresentationOperator;
import org.omg.spec.api4kp._20200801.services.transrepresentation.Transrepresentator;
import org.springframework.beans.factory.annotation.Autowired;

@Named
@KPServer
public class TransrepresentationExecutor implements KnowledgePlatformComponent<Transrepresentator>,
    TransxionApiInternal, _getTxComponent, _listTxComponents {

  private final UUID id = UUID.randomUUID();
  private final Transrepresentator descriptor;

  private final Map<KeyIdentifier, TransionApiOperator> translators;

  @Named
  public TransrepresentationExecutor(
      @Autowired(required = false)
      @KPOperation(Syntactic_Translation_Task)
          List<TransionApiOperator> translators) {

    this.translators = translators.stream()
        .collect(Collectors.toMap(
            tx -> tx.getOperatorId().asKey(),
            tx -> tx
        ));

    this.descriptor = toKPComponent(getComponentId());
  }

  @Override
  public Answer<Transrepresentator> getTxComponent(UUID componentId) {
    return Answer.of(getDescriptor());
  }

  @Override
  public Answer<List<Transrepresentator>> listTxComponents(String from, String into, String method) {
    return Answer.of(singletonList(getDescriptor()));
  }

  @Override
  public Answer<KnowledgeCarrier> applyTransrepresent(KnowledgeCarrier sourceArtifact, String xAccept, String cfg) {
    return anyDo(
        getOperations(
            translators.values(),
            TransionApiOperator::can_applyTransrepresent,
            TransionApiOperator::as_applyTransrepresent),
        a -> a.applyTransrepresent(sourceArtifact, xAccept, cfg));
  }

  @Override
  public Answer<KnowledgeCarrier> applyNamedTransrepresent(UUID operatorId,
      KnowledgeCarrier sourceArtifact, String xAccept, String cfg) {
    return Answer.ofTry(getTransrepresentator(operatorId))
        .flatOpt(TransionApiOperator::as_applyNamedTransrepresent)
        .flatMap(a -> a.applyNamedTransrepresent(operatorId, sourceArtifact, xAccept, cfg));
  }

  @Override
  public Answer<TransrepresentationOperator> getTxionOperator(UUID operatorId) {
    return Answer.ofTry(getTransrepresentator(operatorId)
        .map(TransionApiOperator::getDescriptor));
  }

  @Override
  public Answer<List<TransrepresentationOperator>> listTxionOperators(String from, String into) {
    return Answer.of(translators.values().stream()
        .filter(op -> op.consumes(from))
        .filter(op -> op.produces(into))
        .map(TransionApiOperator::getDescriptor)
        .collect(Collectors.toList()));
  }


  @Override
  public UUID getComponentUuid() {
    return id;
  }

  @Override
  public Transrepresentator toKPComponent(ResourceIdentifier componentId) {
    return new Transrepresentator()
        .withInstanceId(getComponentId());
  }

  @Override
  public Transrepresentator getDescriptor() {
    return descriptor;
  }

  private Optional<TransionApiOperator> getTransrepresentator(UUID operatorId) {
    return translators.keySet().stream()
        .filter(key -> key.getUuid().equals(operatorId))
        .findFirst()
        .map(translators::get);
  }

}
