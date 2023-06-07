package edu.mayo.kmdp.examples._8.graphql.components;

import static edu.mayo.kmdp.examples._8.graphql.components.MockMetadataIntrospector.GQL_INTROSPECT;
import static edu.mayo.kmdp.examples._8.graphql.components.MockMetadataIntrospector.SQL_INTROSPECT;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.of;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultArtifactId;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.GraphQL_Schemas;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.SPARQL_1_1;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedIntrospectDirect;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetCatalogApi;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetRepositoryApi;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;

public class PublicationHelper {

  KnowledgeAssetCatalogApi cat;
  KnowledgeAssetRepositoryApi repo;

  _applyNamedIntrospectDirect introspector;

  public PublicationHelper(
      KnowledgeAssetCatalogApi cat,
      KnowledgeAssetRepositoryApi repo,
      _applyNamedIntrospectDirect introspector) {
    this.cat = cat;
    this.repo = repo;
    this.introspector = introspector;
  }


  public Answer<KnowledgeCarrier> publishGraphQLSchema(
      ResourceIdentifier assetId, String gqls, ResourceIdentifier... resolvers) {
    Answer<KnowledgeCarrier> gql = Answer.of(gqls)
        .map(s -> of(s)
            .withRepresentation(rep(GraphQL_Schemas, TXT, Charset.defaultCharset()))
            .withAssetId(assetId)
            .withArtifactId(defaultArtifactId(assetId, GraphQL_Schemas)));
    gql.ifPresent(graphQL -> publish(graphQL, assetId, GQL_INTROSPECT, resolvers));
    return gql;
  }

  public Answer<KnowledgeCarrier> publishSPARQLResolver(
      ResourceIdentifier assetId, String sparql) {
    Answer<KnowledgeCarrier> sql = Answer.of(sparql)
        .map(s -> of(s)
            .withRepresentation(rep(SPARQL_1_1, TXT, Charset.defaultCharset()))
            .withAssetId(assetId)
            .withArtifactId(defaultArtifactId(assetId, SPARQL_1_1)));
    sql.ifPresent(graphQL -> publish(graphQL, assetId, SQL_INTROSPECT));
    return sql;
  }

  private Answer<Void> publish(KnowledgeCarrier carrier, ResourceIdentifier assetId, UUID introspectId,
      ResourceIdentifier... dependencies) {
    return introspector.applyNamedIntrospectDirect(introspectId, carrier, encode(dependencies))
        .flatOpt(kc -> kc.as(KnowledgeAsset.class))
        .flatMap(asset -> {
          Answer<Void> a1 = cat.setKnowledgeAssetVersion(assetId.getUuid(), assetId.getVersionTag(), asset);
          Answer<Void> a2 = repo.addKnowledgeAssetCarrier(assetId.getUuid(), assetId.getVersionTag(), carrier);
          return Answer.merge(a1,a2);
        });
  }

  private String encode(ResourceIdentifier[] dependencies) {
    return Arrays.stream(dependencies)
        .map(ResourceIdentifier::getVersionId)
        .map(URI::toString)
        .collect(Collectors.joining(","));
  }

}
