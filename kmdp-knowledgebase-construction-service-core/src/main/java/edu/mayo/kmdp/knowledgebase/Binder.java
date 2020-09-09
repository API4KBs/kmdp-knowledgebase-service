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
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Injection_Task;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Named;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.KnowledgePlatformOperator;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal._bind;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal._namedBind;
import org.omg.spec.api4kp._20200801.datatypes.Bindings;
import org.omg.spec.api4kp._20200801.id.KeyIdentifier;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.services.KPComponent;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.springframework.beans.factory.annotation.Autowired;

@Named
@KPComponent
public class Binder implements _bind, _namedBind {

  Map<KeyIdentifier, _namedBind> binders;

  KnowledgeBaseApiInternal kbManager;

  public Binder() {
    // empty constructor
  }

  public Binder(
      @Autowired
      KnowledgeBaseApiInternal kbManager,
      @Autowired(required = false)
      //TODO Define 'BIND' as a task
      @KPOperation(Injection_Task)
          List<_namedBind> binders) {
    this.kbManager = kbManager;
    this.binders = binders.stream()
        .collect(Collectors.toMap(
            det -> getKey(det),
            det -> det
        ));
  }

  private KeyIdentifier getKey(_namedBind det) {
    return ((KnowledgePlatformOperator<?>) det).getOperatorId().asKey();
  }

  public Binder(KnowledgeBaseApiInternal kbManager) {
    this(kbManager, Collections.emptyList());
  }

  public void addNamedBinder(_namedBind binder) {
    this.binders.put(getKey(binder), binder);
  }

  @Override
  public Answer<Pointer> namedBind(UUID kbaseId, String versionTag, UUID operatorId,
      Bindings bindings) {
    return binders.entrySet().stream()
        .filter(e -> e.getKey().getUuid().equals(operatorId))
        .map(e -> e.getValue().namedBind(kbaseId, versionTag, operatorId, bindings))
        .findAny()
        .orElseGet(Answer::failed);
  }

  @Override
  public Answer<Pointer> bind(UUID kbaseId, String versionTag,
      Bindings bindings) {
    return anyDo(binders.entrySet(),
        w -> w.getValue().namedBind(kbaseId, versionTag, w.getKey().getUuid(), bindings));
  }

}

