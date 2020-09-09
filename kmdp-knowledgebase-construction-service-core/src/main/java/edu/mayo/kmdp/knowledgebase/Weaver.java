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
package edu.mayo.kmdp.knowledgebase;

import static org.omg.spec.api4kp._20200801.Answer.anyDo;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Weaving_Task;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Named;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.KnowledgePlatformOperator;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal._namedWeave;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal._weave;
import org.omg.spec.api4kp._20200801.id.KeyIdentifier;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.services.KPComponent;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.springframework.beans.factory.annotation.Autowired;

@Named
@KPComponent
public class Weaver implements _weave, _namedWeave {

  Map<KeyIdentifier, _namedWeave> weavers;

  KnowledgeBaseApiInternal kbManager;

  public Weaver() {
    // empty constructor
  }

  public Weaver(
      @Autowired
      KnowledgeBaseApiInternal kbManager,
      @Autowired(required = false)
      @KPOperation(Weaving_Task)
          List<_namedWeave> weavers) {
    this.kbManager = kbManager;
    this.weavers = weavers.stream()
        .collect(Collectors.toMap(
            det -> getKey(det),
            det -> det
        ));
  }

  private KeyIdentifier getKey(_namedWeave det) {
    return ((KnowledgePlatformOperator<?>) det).getOperatorId().asKey();
  }

  public Weaver(KnowledgeBaseApiInternal kbManager) {
    this(kbManager, Collections.emptyList());
  }

  public void addNamedWeaver(_namedWeave weaver) {
    this.weavers.put(getKey(weaver), weaver);
  }

  @Override
  public Answer<Pointer> namedWeave(UUID kbaseId, java.lang.String versionTag, UUID operatorId,
      KnowledgeCarrier aspects) {
    return weavers.entrySet().stream()
        .filter(e -> e.getKey().getUuid().equals(operatorId))
        .map(e -> e.getValue().namedWeave(kbaseId, versionTag, operatorId, aspects))
        .findAny()
        .orElseGet(Answer::failed);
  }

  @Override
  public Answer<Pointer> weave(UUID kbaseId, java.lang.String versionTag,
      KnowledgeCarrier aspects) {
    return anyDo(weavers.entrySet(),
        w -> w.getValue().namedWeave(kbaseId, versionTag, w.getKey().getUuid(), aspects));
  }

}

