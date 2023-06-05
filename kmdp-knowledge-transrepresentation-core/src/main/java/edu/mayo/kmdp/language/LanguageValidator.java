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
package edu.mayo.kmdp.language;

import static java.util.Collections.singletonList;
import static org.omg.spec.api4kp._20200801.Answer.anyDo;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Well_Formedness_Check_Task;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Named;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.KnowledgePlatformComponent;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DiscoveryApiInternal;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.ValidateApiInternal;
import org.omg.spec.api4kp._20200801.id.KeyIdentifier;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.transrepresentation.ValidationOperator;
import org.omg.spec.api4kp._20200801.services.transrepresentation.Validator;
import org.springframework.beans.factory.annotation.Autowired;

@Named
@KPServer
public class LanguageValidator implements KnowledgePlatformComponent<Validator>,
    ValidateApiInternal, DiscoveryApiInternal._getValidationComponent, DiscoveryApiInternal._listValidationComponents {

  private UUID id = UUID.randomUUID();
  private Validator descriptor;

  Map<KeyIdentifier,ValidateApiOperator> validators;

  @Named
  public LanguageValidator(@Autowired(required = false)
  @KPOperation(Well_Formedness_Check_Task)
      List<ValidateApiOperator> validators) {

    this.validators = validators.stream()
        .collect(Collectors.toMap(
            det -> det.getOperatorId().asKey(),
            det -> det
        ));

    this.descriptor = toKPComponent(getComponentId());
  }


  @Override
  public Answer<Validator> getValidationComponent(UUID componentId) {
    return Answer.of(getDescriptor());
  }

  @Override
  public Answer<List<Validator>> listValidationComponents(String from, String methodTag) {
    return Answer.of(singletonList(getDescriptor()));
  }

  @Override
  public Answer<ValidationOperator> getValidationOperator(UUID operatorId) {
    return Answer.of(getValidator(operatorId)
        .map(ValidateApiOperator::getDescriptor));
  }

  @Override
  public Answer<List<ValidationOperator>> listValidationOperators(String from) {
    return Answer.of(validators.values().stream()
        .filter(op -> op.consumes(from))
        .map(ValidateApiOperator::getDescriptor)
        .collect(Collectors.toList()));
  }

  @Override
  public Answer<Void> applyValidate(KnowledgeCarrier sourceArtifact, String config) {
    return anyDo(
        getOperations(
            validators.values(),
            ValidateApiOperator::can_applyValidate,
            ValidateApiOperator::as_applyValidate),
        a -> a.applyValidate(sourceArtifact, config));
  }

  @Override
  public Answer<Void> applyNamedValidate(UUID operatorId,
      KnowledgeCarrier sourceArtifact, String config) {
    return Answer.of(getValidator(operatorId))
        .flatOpt(ValidateApiOperator::as_applyNamedValidate)
        .flatMap(v -> v.applyNamedValidate(operatorId, sourceArtifact, config));
  }


  @Override
  public UUID getComponentUuid() {
    return id;
  }

  @Override
  public Validator toKPComponent(ResourceIdentifier componentId) {
    return new Validator()
        .withInstanceId(getComponentId());
  }

  @Override
  public Validator getDescriptor() {
    return descriptor;
  }


  private Optional<ValidateApiOperator> getValidator(UUID operatorId) {
    return validators.keySet().stream()
        .filter(key -> key.getUuid().equals(operatorId))
        .findFirst()
        .map(validators::get);
  }
}

