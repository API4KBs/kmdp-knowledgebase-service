package edu.mayo.kmdp.knowledgebase.weavers.fhir.stu3;

import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Weaving_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;

import edu.mayo.kmdp.knowledgebase.AbstractKnowledgeBaseOperator;
import java.util.UUID;
import javax.inject.Named;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedWeaveDirect;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KPComponent;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;

@Named
@KPSupport({FHIR_STU3})
@KPComponent
@KPOperation(Weaving_Task)
public class PlanDefDataReqProfileWeaver
    extends AbstractKnowledgeBaseOperator
    implements _applyNamedWeaveDirect {



  protected PlanDefDataReqProfileWeaver(ResourceIdentifier opId) {
    super(opId);
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return FHIR_STU3;
  }

  @Override
  public Answer<KnowledgeCarrier> applyNamedWeaveDirect(UUID operatorId, KnowledgeCarrier artifact,
      KnowledgeCarrier aspects, String xParams) {
    return Answer.of(artifact);
  }
}
