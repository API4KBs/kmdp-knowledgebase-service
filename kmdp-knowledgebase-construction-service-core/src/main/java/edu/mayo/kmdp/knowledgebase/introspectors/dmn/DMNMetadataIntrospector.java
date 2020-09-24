package edu.mayo.kmdp.knowledgebase.introspectors.dmn;


import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Description_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;

import edu.mayo.kmdp.knowledgebase.KnowledgeBaseProvider;
import edu.mayo.kmdp.knowledgebase.introspectors.dmn.v1_1.DMN11MetadataIntrospector;
import edu.mayo.kmdp.knowledgebase.introspectors.dmn.v1_2.DMN12MetadataIntrospector;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Named;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedIntrospect;
import org.omg.spec.api4kp._20200801.services.KPComponent;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;

@KPServer
@KPOperation(Description_Task)
@KPSupport({DMN_1_1,DMN_1_2})
@Named
public class DMNMetadataIntrospector implements _applyNamedIntrospect {

  @Inject
  @KPComponent
  @KPSupport(DMN_1_1)
  _applyNamedIntrospect dmn11Introspector;

  @Inject
  @KPComponent
  @KPSupport(DMN_1_2)
  _applyNamedIntrospect dmn12Introspector;

  public DMNMetadataIntrospector() {

  }

  public DMNMetadataIntrospector(KnowledgeBaseApiInternal kbManager) {
    this.dmn11Introspector = new DMN11MetadataIntrospector(kbManager);
    this.dmn12Introspector = new DMN12MetadataIntrospector(kbManager);
  }

  @Override
  public Answer<KnowledgeCarrier> applyNamedIntrospect(UUID operatorId, UUID kbaseId,
      String versionTag, String xParams) {

    // TODO use OperatorID...
    if (DMN11MetadataIntrospector.id.equals(operatorId)) {
      return dmn11Introspector
          .applyNamedIntrospect(
              DMN11MetadataIntrospector.id,
              kbaseId,
              versionTag,
              xParams);
    } else if (DMN12MetadataIntrospector.id.equals(operatorId)) {
      return dmn12Introspector
          .applyNamedIntrospect(
              DMN12MetadataIntrospector.id,
              kbaseId,
              versionTag,
              xParams);
    }
    return Answer.unsupported();
  }

}
