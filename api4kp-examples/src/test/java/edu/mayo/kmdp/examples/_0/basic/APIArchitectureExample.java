package edu.mayo.kmdp.examples._0.basic;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetCatalogApi;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.client.ApiClientFactory;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetCatalogApiDelegate;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetCatalogApiInternal;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetCatalogApiInternalAdapter;
import org.omg.spec.api4kp._20200801.services.repository.KnowledgeAssetCatalog;

/**
 * This example uses the KnowledgeAssetRepositoryAPI to demonstrate how to implement a server, and
 * connect to it using a client
 */
public class APIArchitectureExample {


  /**
   * This test demonstrates various ways for a client to obtain an instance of an API4KP server As
   * various forms of proxying and/or delegation (including web-based services) are provided through
   * code-generation, the goal is to offer a single, transparent interface.
   */
  public void testLocalClientInitialization() {

    // The client instantiates the service provider directly
    KnowledgeAssetCatalogApi server = KnowledgeAssetCatalogApi.newInstance(newServerImplementation());
    assertTrue(server.getKnowledgeAssetCatalog().isSuccess());

    // The client can also use a delegate
    KnowledgeAssetCatalogApiDelegate delegate = new KnowledgeAssetCatalogApiInternalAdapter();
    KnowledgeAssetCatalogApi delegateClient = KnowledgeAssetCatalogApi.newInstance(delegate);
    assertTrue(delegateClient.getKnowledgeAssetCatalog().isSuccess());
  }

  public void testRemoteClientInitialization() {

    // Otherwise, the client can connect to a web service
    KnowledgeAssetCatalogApi restClient = KnowledgeAssetCatalogApi
        .newInstance(new ApiClientFactory("http://localhost:8080"));
    assertTrue(restClient.getKnowledgeAssetCatalog().isSuccess());

  }


  /**
   * A mock factory method that returns a server implementing an API4KP interface All the
   * operations, except for 'getAssetCatalog' are unsupported.
   *
   * @return A mock implementation of the KnowledgeAssetCatalog API
   */
  KnowledgeAssetCatalogApiInternal newServerImplementation() {
    return new KnowledgeAssetCatalogApiInternal() {
      @Override
      public Answer<KnowledgeAssetCatalog> getKnowledgeAssetCatalog() {
        return Answer.of(new KnowledgeAssetCatalog()
            .withName("Mock Catalog Descriptor"));
      }
    };
  }
}
