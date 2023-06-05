package edu.mayo.kmdp.language;

import static org.omg.spec.api4kp._20200801.contrastors.SyntacticRepresentationContrastor.theRepContrastor;

import edu.mayo.kmdp.util.Util;
import java.util.List;
import org.omg.spec.api4kp._20200801.KnowledgePlatformOperator;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.ValidateApiInternal;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder;
import org.omg.spec.api4kp._20200801.services.transrepresentation.ValidationOperator;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeresourceoutcome.KnowledgeResourceOutcome;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeresourceoutcome.KnowledgeResourceOutcomeSeries;

public interface ValidateApiOperator
    extends ValidateApiInternal.Operator, KnowledgePlatformOperator<ValidationOperator> {

  List<SyntacticRepresentation> getFrom();

  default ValidationOperator getDescriptor() {
    return new ValidationOperator()
        .withFrom(getFrom())
        .withOperatorId(getOperatorId());
  }

  default KnowledgeResourceOutcome getValidationType() {
    // TODO FUTURE should be inferred from getDescriptor
    return KnowledgeResourceOutcomeSeries.Grammaticality;
  }

  default boolean consumes(String from) {
    return Util.isEmpty(from) ||
        ModelMIMECoder.decode(from)
        .map(fromRep -> getFrom().stream()
            .anyMatch(supported -> theRepContrastor.isBroaderOrEqual(supported, fromRep)))
        .orElse(false);
  }

}
