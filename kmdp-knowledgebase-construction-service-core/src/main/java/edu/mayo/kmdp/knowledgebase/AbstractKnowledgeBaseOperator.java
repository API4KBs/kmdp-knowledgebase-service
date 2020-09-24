package edu.mayo.kmdp.knowledgebase;

import org.omg.spec.api4kp._20200801.KnowledgePlatformOperator;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeProcessingOperator;

public abstract class AbstractKnowledgeBaseOperator implements
    KnowledgePlatformOperator<KnowledgeProcessingOperator> {

  private ResourceIdentifier operatorId;

  protected KnowledgeBaseApiInternal kbManager;

  protected AbstractKnowledgeBaseOperator(ResourceIdentifier opId) {
    this.operatorId = opId;
  }

  @Override
  public ResourceIdentifier getOperatorId() {
    return operatorId;
  }

  @Override
  public KnowledgeProcessingOperator getDescriptor() {
    throw new UnsupportedOperationException();
  }

  public AbstractKnowledgeBaseOperator withKBManager(KnowledgeBaseApiInternal kbManager) {
    this.kbManager = kbManager;
    return this;
  }

}
