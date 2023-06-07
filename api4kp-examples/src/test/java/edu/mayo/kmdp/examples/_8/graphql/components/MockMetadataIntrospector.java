package edu.mayo.kmdp.examples._8.graphql.components;

import static org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper.carry;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Imports;

import edu.mayo.kmdp.util.Util;
import java.net.URI;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedIntrospectDirect;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.Dependency;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;

public class MockMetadataIntrospector implements _applyNamedIntrospectDirect {

  public static final UUID GQL_INTROSPECT =
      UUID.fromString("fa816efb-0612-4a7f-8302-10eed2093dbb");

  public static final UUID SQL_INTROSPECT =
      UUID.fromString("31e39489-ff3c-4e87-a6db-980751c34554");

  @Override
  public Answer<KnowledgeCarrier> applyNamedIntrospectDirect(UUID componentId,
      KnowledgeCarrier knowledgeCarrier, String config) {
    if (GQL_INTROSPECT.equals(componentId)) {
      return Answer.of(carry(doIntrospectGQL(knowledgeCarrier, config)));
    } else if (SQL_INTROSPECT.equals(componentId)) {
      return Answer.of(carry(doIntrospectSQL(knowledgeCarrier, config)));
    } else {
      return Answer.unsupported();
    }
  }

  private KnowledgeAsset doIntrospectGQL(KnowledgeCarrier kc, String config) {
    return doGenericIntrospect(kc, config);
  }

  private KnowledgeAsset doIntrospectSQL(KnowledgeCarrier kc, String config) {
    return doGenericIntrospect(kc, config);
  }

  private KnowledgeAsset doGenericIntrospect(KnowledgeCarrier kc, String config) {
    KnowledgeAsset ka = new KnowledgeAsset()
        .withAssetId(kc.getAssetId())
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(kc.getArtifactId())
            .withRepresentation(kc.getRepresentation()));
    decode(config).forEach(dep ->
        ka.withLinks(new Dependency()
            .withHref(dep)
            .withRel(Imports)));
    return ka;
  }

  private Stream<ResourceIdentifier> decode(String config) {
    return Arrays.stream(config.split(","))
        .filter(Util::isNotEmpty)
        .map(URI::create)
        .map(SemanticIdentifier::newVersionId);
  }

}
