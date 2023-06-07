package edu.mayo.kmdp.examples._8.graphql.components;

import static edu.mayo.kmdp.examples._8.graphql.components.MockSPARQLAdapter.DATABASE_ID;
import static edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries.InternalServerError;
import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper.links;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Imports;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.GraphQL_Schemas;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.SPARQL_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;

import edu.mayo.kmdp.language.parsers.sparql.SparqlLifter;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.RuntimeWiring.Builder;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.jena.query.Query;
import org.junit.jupiter.api.Assertions;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.ServerSideException;
import org.omg.spec.api4kp._20200801.api.inference.v4.server.ReasoningApiInternal._askQuery;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetCatalogApi;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetRepositoryApi;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal._applyLift;
import org.omg.spec.api4kp._20200801.datatypes.Bindings;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.Dependency;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;

public class GraphQLEngine implements _askQuery {

  private final KnowledgeAssetCatalogApi cat;
  private final KnowledgeAssetRepositoryApi repo;
  private final KnowledgeBaseApiInternal kbManager;

  private final _applyLift gqlSchemaParser = new GraphQLDSLParser();
  private final _applyLift sqlLifter = new SparqlLifter();

  private final MockSPARQLAdapter sparqlBackend;

  public GraphQLEngine(
      KnowledgeAssetCatalogApi cat,
      KnowledgeAssetRepositoryApi repo,
      KnowledgeBaseApiInternal kbManager) {
    this.cat = cat;
    this.repo = repo;
    this.kbManager = kbManager;
    this.sparqlBackend = new MockSPARQLAdapter(kbManager);
  }

  @Override
  public Answer<List<Bindings>> askQuery
      (UUID kBaseId, String versionTag, KnowledgeCarrier queryCarrier, String xConfig) {
    Answer<KnowledgeCarrier> schema =
        kbManager.getKnowledgeBaseManifestation(kBaseId, versionTag);

    TypeDefinitionRegistry tdf = schema.flatMap(this::parse)
        .orElseGet(Assertions::fail);

    KnowledgeAsset schemaSurrogate = schema.flatMap(this::getSurrogate)
        .orElseGet(Assertions::fail);

    Map<String, KnowledgeCarrier> resolvers = findResolvers(schemaSurrogate);
    RuntimeWiring runtimeWiring = bindResolvers(resolvers);

    GraphQL engine = getEngine(tdf, runtimeWiring);

    return Answer.of(queryCarrier.asString())
        .map(engine::execute)
        .flatMap(this::processResult);
  }

  private GraphQL getEngine(TypeDefinitionRegistry tdf, RuntimeWiring runtimeWiring) {
    SchemaGenerator schemaGenerator = new SchemaGenerator();
    GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(tdf, runtimeWiring);
    return GraphQL.newGraphQL(graphQLSchema).build();
  }

  private RuntimeWiring bindResolvers(Map<String, KnowledgeCarrier> resolvers) {
    Builder wiring = newRuntimeWiring();
    resolvers.forEach((field, resolver) ->
        wiring.type("Query",
            builder -> builder.dataFetcher(
                field,
                dataFetchingEnvironment ->
                    sparqlBackend
                        .askQuery(DATABASE_ID.getUuid(), DATABASE_ID.getVersionTag(), resolver,
                            null)
                        .filter(l -> !l.isEmpty())
                        .map(l -> l.get(0))
                        .map(b -> (String) b.get(field))
                        .orElse(null))));
    return wiring.build();
  }

  private Map<String, KnowledgeCarrier> findResolvers(KnowledgeAsset schemaSurrogate) {
    return links(schemaSurrogate, Dependency.class, Imports)
        .map(rid -> repo
            .getKnowledgeAssetCanonicalCarrier(rid.getUuid(), rid.getVersionTag())
            .flatMap(kc -> sqlLifter
                .applyLift(kc, Abstract_Knowledge_Expression.getTag(), codedRep(SPARQL_1_1), null)))
        .flatMap(Answer::trimStream)
        .collect(Collectors.toMap(
            this::detectResolved,
            kc -> kc));
  }

  private String detectResolved(KnowledgeCarrier kc) {
    return kc.as(Query.class)
        .map(q -> q.getResultVars().get(0))
        .orElse("");
  }

  private Answer<KnowledgeAsset> getSurrogate(KnowledgeCarrier schema) {
    return Answer.of(schema)
        .map(KnowledgeCarrier::getAssetId)
        .flatMap(aId -> cat.getKnowledgeAsset(aId.getUuid(), aId.getVersionTag()));
  }

  private Answer<TypeDefinitionRegistry> parse(KnowledgeCarrier schema) {
    return gqlSchemaParser
        .applyLift(schema, Abstract_Knowledge_Expression.getTag(), codedRep(GraphQL_Schemas), null)
        .flatOpt(kc -> kc.as(TypeDefinitionRegistry.class));
  }

  private Answer<List<Bindings>> processResult(ExecutionResult res) {
    if (res.getErrors().isEmpty()) {
      Bindings binds = new Bindings();
      binds.putAll(res.getData());
      return Answer.of(Collections.singletonList(binds));
    } else {
      return Answer.failed(new ServerSideException(
          InternalServerError, res.getErrors().stream()
          .map(Object::toString)
          .collect(Collectors.joining())
      ));
    }
  }
}
