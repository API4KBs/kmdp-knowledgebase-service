package edu.mayo.kmdp.terms;

import static edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries.ServiceUnavailable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.mayo.kmdp.terms.TermsTestUtil.MockRepo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetCatalogApi;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetRepositoryApi;
import edu.mayo.kmdp.api.terminology.v4.server.TermsApiInternal;

class OfflineModeTest {

  //Bug 1869360
  @Test
  void testRecoverFromOffline() {
    MockRepo repo = new MockRepo();
    TermsApiInternal terms = new TermsFHIRFacade(
        KnowledgeAssetCatalogApi.newInstance(repo),
        KnowledgeAssetRepositoryApi.newInstance(repo));

    var fail = terms.listTerminologies();
    assertTrue(ServiceUnavailable.sameAs(fail.getOutcomeType()));

    repo.setLoaded(true);

    assertTrue(terms.clearTerminologies().isSuccess());

    var size = terms.listTerminologies().orElseGet(Assertions::fail)
        .size();
    assertEquals(0, size);
  }


}
