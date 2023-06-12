package edu.mayo.kmdp.language.ccg.library.mock;

import java.util.UUID;
import javax.inject.Named;
import org.omg.spec.api4kp._20200801.Answer;
import edu.mayo.kmdp.api.terminology.v4.server.TermsApiInternal;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.omg.spec.api4kp._20200801.terms.model.ConceptDescriptor;

@KPServer
@Named
public class MockTermProvider implements TermsApiInternal {

  @Override
  public Answer<ConceptDescriptor> lookupTerm(String conceptId, String xAccept) {
    return Answer.ofTry(MockVocabulary.resolveUUID(UUID.fromString(conceptId))
        .map(ConceptDescriptor::toConceptDescriptor));
  }

}
