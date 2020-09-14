package edu.mayo.kmdp.knowledgebase;

import org.omg.spec.api4kp._20200801.KnowledgePlatformOperator;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeProcessingOperator;

public abstract class AbstractKnowledgeBaseOperator implements
    KnowledgePlatformOperator<KnowledgeProcessingOperator> {

  private ResourceIdentifier operatorId;

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

}
