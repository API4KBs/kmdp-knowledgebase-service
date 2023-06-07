package edu.mayo.kmdp.examples._8.graphql;

import static edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryService.selfContainedRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.of;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.id.IdentifierConstants.VERSION_ZERO;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.GraphQL_Queries;

import edu.mayo.kmdp.examples._8.graphql.components.GraphQLEngine;
import edu.mayo.kmdp.examples._8.graphql.components.MockMetadataIntrospector;
import edu.mayo.kmdp.examples._8.graphql.components.PublicationHelper;
import edu.mayo.kmdp.knowledgebase.KnowledgeBaseProvider;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryService;
import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.inference.v4.server.ReasoningApiInternal._askQuery;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedIntrospectDirect;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetCatalogApi;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetRepositoryApi;
import org.omg.spec.api4kp._20200801.datatypes.Bindings;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;

/**
 * This example showcases an integration of GraphQL and SPARQL
 * <p>
 * A GraphQL schema is defined, and used in a GraphQL engine to submit GraphQL queries - The
 * definition is parsed as GRAPHQL_SCHEMA - Published (with Inferred metadata) to a Knowledge Asset
 * Repository
 * <p>
 * The GraphQL engine delegates the retrieval of data to a SPARQL-based Resolver - A SPARQL query is
 * parsed - then published (with Inferred metadata) to the Knowledge Asset Repository - An (asset to
 * asset) dependency is asserted between the GraphQL schema and the SPARQL Query
 * <p>
 * The GraphQL schema is used to initialize a Knowledge Base --> The dependency MAY be used to
 * construct a composite KB (not shown in this example)
 * <p>
 * The GraphQL KB is used to initialize a Query Engine - The engine uses the Metadata in the Asset
 * Repository to discover the dependency - Retrieves the SPARQL query artifact - Parses and inspects
 * the query to bind the Query as a Resolver of a specific Schema field
 * <p>
 * The client submits a GraphQL query to the Engine, passing the Query and a pointer to the GraphQL
 * KB - The engine API delegates the execution of the query to an out-of-the-box GraphQL engine -
 * The GraphQL engine makes a callback to the SPARQL engine, which is also wrapped with API4KP
 * <p>
 * The results are processed
 */
class GraphQLTest {

  KnowledgeAssetCatalogApi cat;
  KnowledgeAssetRepositoryApi repo;
  PublicationHelper helper;

  KnowledgeBaseApiInternal kbManager;

  _askQuery gqlQueryEngine;
  _applyNamedIntrospectDirect introspector;

  final ResourceIdentifier SPARQL_ASSET_ID =
      newId(UUID.fromString("0707be26-3835-4f8f-943a-921dec262bfa"), VERSION_ZERO);
  final String SPAQRL_QUERY_RESOLVER =
      "SELECT ?hello { BIND(\"world\" as ?hello) }";

  final ResourceIdentifier GQLS_ASSET_ID =
      newId(UUID.fromString("0c352277-66d5-45f8-99fc-76a7f4543371"), VERSION_ZERO);
  final String GQL_SCHEMA =
      "type Query{ hello: String }";

  final String GQL_QUERY =
      "{ hello }";

  @BeforeEach
  void setup() {
    KnowledgeAssetRepositoryService kars = selfContainedRepository();
    cat = KnowledgeAssetCatalogApi.newInstance(kars);
    repo = KnowledgeAssetRepositoryApi.newInstance(kars);
    introspector = new MockMetadataIntrospector();
    helper = new PublicationHelper(cat, repo, introspector);

    kbManager = new KnowledgeBaseProvider(repo);

    gqlQueryEngine = new GraphQLEngine(cat, repo, kbManager);
  }

  @Test
  void testGQLIntegrationProofOfConcept() {
    // Step 1a: Publish a 'binder' SPARQL Query
    KnowledgeCarrier kc1 = helper.publishSPARQLResolver(SPARQL_ASSET_ID, SPAQRL_QUERY_RESOLVER)
        .orElseGet(Assertions::fail);

    // Step 1b: Publish the GraphQL schema that defines the client-facing interface
    // assert a dependency on the SPARQL asset as the (definition of) a binder
    KnowledgeCarrier kc2 = helper.publishGraphQLSchema(GQLS_ASSET_ID, GQL_SCHEMA, kc1.getAssetId())
        .orElseGet(Assertions::fail);

    // Step 1c: Create a KnowledgeBase with the GraphQL Schema
    Answer<Pointer> gqlKBPointer = kbManager.initKnowledgeBase()
        .flatMap(kb -> kbManager.initKnowledgeBase(kc2, null));

    // ---------------------------------------------------------------------------------------- /

    // Step 2: Parse the client's GraphQL Query
    Answer<KnowledgeCarrier> gqlQuery = Answer.of(GQL_QUERY)
        .map(s -> of(s).withRepresentation(rep(GraphQL_Queries, TXT, Charset.defaultCharset())));

    // Step 3: Execute the Query
    // Pass the Pointer to the KBase, submit the Query
    List<Bindings> response =
        gqlQuery.flatMap(query ->
            gqlKBPointer.flatMap(ptr ->
                gqlQueryEngine.askQuery(ptr.getUuid(), ptr.getVersionTag(), query, null)))
            .orElseGet(Assertions::fail);

    // Step 4: Process Results
    assertEquals(1, response.size());
    System.out.println(response.get(0));
    assertEquals("world", response.get(0).get("hello"));
  }

}
