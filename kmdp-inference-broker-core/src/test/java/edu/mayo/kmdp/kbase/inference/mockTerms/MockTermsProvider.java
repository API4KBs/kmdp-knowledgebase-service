package edu.mayo.kmdp.kbase.inference.mockTerms;

import java.util.List;
import java.util.UUID;
import javax.inject.Named;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.terminology.v4.server.TermsApiInternal;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.terms.model.ConceptDescriptor;

@KPServer
@Named
public class MockTermsProvider implements TermsApiInternal {

  @Override
  public Answer<ConceptDescriptor> getTerm(UUID uuid, String s, String s1) {
    return Answer.unsupported();
  }

  @Override
  public Answer<List<ConceptDescriptor>> getTerms(UUID uuid, String s, String s1) {
    return Answer.unsupported();
  }

  @Override
  public Answer<KnowledgeCarrier> getVocabulary(UUID uuid, String s, String s1) {
    return Answer.unsupported();
  }

  @Override
  public Answer<Boolean> isAncestor(UUID vocabularyId, String versionTag, String conceptId, String testConceptId) {
    return Answer.unsupported();
  }

  @Override
  public Answer<Void> isMember(UUID uuid, String s, String s1) {
    return Answer.unsupported();
  }

  @Override
  public Answer<List<ConceptDescriptor>> listAncestors(UUID vocabularyId, String versionTag, String conceptId) {
    return Answer.unsupported();
  }

  @Override
  public Answer<List<Pointer>> listTerminologies() {
    return Answer.unsupported();
  }

}
