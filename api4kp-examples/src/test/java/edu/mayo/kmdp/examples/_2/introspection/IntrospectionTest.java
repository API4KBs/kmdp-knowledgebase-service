package edu.mayo.kmdp.examples._2.introspection;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.of;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.randomArtifactId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.randomAssetId;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Serialized_Knowledge_Expression;

import edu.mayo.kmdp.examples.PlatformConfig;
import edu.mayo.kmdp.knowledgebase.introspectors.dmn.v1_2.DMN12MetadataIntrospector;
import java.io.InputStream;
import java.nio.charset.Charset;
import javax.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = PlatformConfig.class)
public class IntrospectionTest {

  @KPServer
  @Inject
  KnowledgeBaseApiInternal introspector;

  @KPServer
  @Inject
  DeserializeApiInternal parser;


  @Test
  void testIntrospectDMN() {
    InputStream is = IntrospectionTest.class
        .getResourceAsStream("/mock/Basic Decision Model.dmn.xml");

    KnowledgeCarrier kc =
        introspector.initKnowledgeBase(
            of(is)
                .withAssetId(randomAssetId())
                .withArtifactId(randomArtifactId())
                .withRepresentation(
                    rep(DMN_1_2, XML_1_1, Charset.defaultCharset(), Encodings.DEFAULT)),
            null)
            .flatMap(kb -> introspector.namedIntrospect(
                kb.getUuid(),
                kb.getVersionTag(),
                DMN12MetadataIntrospector.id,
                null))
            .flatMap(
                kb -> introspector.getKnowledgeBaseManifestation(kb.getUuid(), kb.getVersionTag()))
            .orElseGet(Assertions::fail);

    System.out.println("Created >> " + kc.getRepresentation().getLanguage());
    KnowledgeAsset surrogate = kc.as(KnowledgeAsset.class)
        .orElseGet(Assertions::fail);

    parser.applyLower(kc, Serialized_Knowledge_Expression.getTag())
        .flatOpt(AbstractCarrier::asString)
        .ifPresent(System.out::print);

  }


}
