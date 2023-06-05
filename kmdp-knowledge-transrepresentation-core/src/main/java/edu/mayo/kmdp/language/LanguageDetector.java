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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Named;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.KnowledgePlatformComponent;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DetectApiInternal;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DiscoveryApiInternal;
import org.omg.spec.api4kp._20200801.id.KeyIdentifier;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.transrepresentation.DetectionOperator;
import org.omg.spec.api4kp._20200801.services.transrepresentation.Detector;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries;
import org.springframework.beans.factory.annotation.Autowired;

@Named
@KPServer
public class LanguageDetector implements KnowledgePlatformComponent<Detector>,
    DetectApiInternal, DiscoveryApiInternal._getDetectComponent, DiscoveryApiInternal._listDetectComponents {

  private UUID id = UUID.randomUUID();
  private Detector descriptor;

  Map<KeyIdentifier,DetectApiOperator> detectors;

  @Named
  public LanguageDetector(@Autowired(required = false)
  @KPOperation(KnowledgeProcessingOperationSeries.Language_Information_Detection_Task)
      List<DetectApiOperator> detectors) {

    this.detectors = detectors.stream()
        .collect(Collectors.toMap(
            det -> det.getOperatorId().asKey(),
            det -> det
        ));

    this.descriptor = toKPComponent(getComponentId());
  }

  @Override
  public Answer<Detector> getDetectComponent(UUID componentId) {
    return Answer.of(getDescriptor());
  }

  @Override
  public Answer<List<Detector>> listDetectComponents(String into, String method) {
    return Answer.of(singletonList(getDescriptor()));
  }

  @Override
  public Answer<KnowledgeCarrier> applyDetect(KnowledgeCarrier sourceArtifact, String config) {
    return anyDo(
        getOperations(
            detectors.values(),
            Operator::can_applyDetect,
            DetectApiOperator::as_applyDetect),
        a -> a.applyDetect(sourceArtifact, config));
  }

  @Override
  public Answer<KnowledgeCarrier> applyNamedDetect(UUID operatorId,
      KnowledgeCarrier sourceArtifact, String config) {
    return Answer.of(getDetector(operatorId))
        .flatOpt(DetectApiOperator::as_applyNamedDetect)
        .flatMap(a -> a.applyNamedDetect(operatorId, sourceArtifact, config));
  }

  @Override
  public Answer<DetectionOperator> getDetectionOperator(UUID operatorId) {
    return Answer.of(getDetector(operatorId)
        .map(DetectApiOperator::getDescriptor));
  }

  @Override
  public Answer<List<DetectionOperator>> listDetectionOperators(String into) {
    return Answer.of(detectors.values().stream()
        .filter(op -> op.produces(into))
        .map(DetectApiOperator::getDescriptor)
        .collect(Collectors.toList()));
  }


  @Override
  public UUID getComponentUuid() {
    return id;
  }

  @Override
  public Detector toKPComponent(ResourceIdentifier componentId) {
    return new Detector()
        .withInstanceId(getComponentId());
  }

  @Override
  public Detector getDescriptor() {
    return descriptor;
  }

  private Optional<DetectApiOperator> getDetector(UUID operatorId) {
    return detectors.keySet().stream()
        .filter(key -> key.getUuid().equals(operatorId))
        .findFirst()
        .map(detectors::get);
  }
}
