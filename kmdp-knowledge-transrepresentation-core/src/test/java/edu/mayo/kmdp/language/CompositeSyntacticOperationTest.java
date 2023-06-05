package edu.mayo.kmdp.language;

import static java.nio.charset.Charset.defaultCharset;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder.encode;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Depends_On;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Serialized_Knowledge_Expression;

import edu.mayo.kmdp.language.parsers.surrogate.v2.Surrogate2Parser;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.CompositeStructType;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.Dependency;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper;

class CompositeSyntacticOperationTest {

  @Test
  void testCompositeSerialization() {
    Surrogate2Parser parser = new Surrogate2Parser();
    Answer<KnowledgeCarrier> ckc = Answer.of(init());

    Answer<KnowledgeCarrier> serialized =
        ckc.flatMap(x -> parser
            .applyLower(x,
                Serialized_Knowledge_Expression,
                encode(rep(Knowledge_Asset_Surrogate_2_0, JSON, defaultCharset())),
                null));

    assertTrue(serialized.isSuccess());
    KnowledgeCarrier kc = serialized.get();
    assertTrue(kc instanceof CompositeKnowledgeCarrier);

    CompositeKnowledgeCarrier out = (CompositeKnowledgeCarrier) kc;

    assertSame(CompositeStructType.GRAPH, out.getStructType());
    assertNotNull(out.getStruct());
    assertNotNull(out.getRepresentation());
    assertNotNull(out.getRootId());
    assertNotNull(out.getLevel());

  }

  private CompositeKnowledgeCarrier init() {
    KnowledgeAsset ax2 = new KnowledgeAsset()
        .withAssetId(SurrogateBuilder.randomAssetId())
        .withName("Test2");
    KnowledgeAsset ax1 = new KnowledgeAsset()
        .withAssetId(SurrogateBuilder.randomAssetId())
        .withName("Test1")
        .withLinks(new Dependency()
            .withRel(Depends_On)
            .withHref(ax2.getAssetId()));

    return SurrogateHelper.toAnonymousCompositeAsset(
        ax1.getAssetId(),
        Arrays.asList(ax1,ax2));
  }
}
