package edu.mayo.kmdp.knowledgebase.flatteners.dmn.v1_2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;

import edu.mayo.kmdp.knowledgebase.KnowledgeBaseProvider;
import edu.mayo.kmdp.language.parsers.dmn.v1_2.DMN12Parser;
import edu.mayo.kmdp.util.StreamUtil;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.bind.JAXBElement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.dmn._20180521.model.TDecision;
import org.omg.spec.dmn._20180521.model.TDefinitions;
import org.omg.spec.dmn._20180521.model.TInputData;

public class DMN12FlattenerTest {

  protected KnowledgeBaseProvider kbManager = new KnowledgeBaseProvider(null)
      .withNamedFlattener(new DMN12ModelFlattener());

  @Test
  public void testBasicFlatten() {
    List<String> files = Arrays.asList(
        "/Decision Reuse.dmn.xml",
        "/Basic Decision Model.dmn.xml"
    );
    List<KnowledgeCarrier> kcs = readDMNModels(files);
    assertEquals(2, kcs.size());

    TDefinitions reuse = kcs.get(0).as(TDefinitions.class).orElseGet(Assertions::fail);
    TDefinitions basic = kcs.get(1).as(TDefinitions.class).orElseGet(Assertions::fail);
    TDefinitions snap = (TDefinitions) basic.clone();

    TDefinitions flat = flatten(kcs.get(0).getAssetId(),kcs);
    assertEquals(reuse.getName(),flat.getName());
    assertNotSame(reuse, flat);

    assertEquals(2, streamInputs(flat).count());
    assertEquals(2, streamDecisions(flat).count());

    streamInputs(flat).forEach(clonedInput ->
        streamInputs(basic).anyMatch(input ->
            clonedInput.getId().equals(input.getId())
                && clonedInput.getName().equals(input.getName())
                && clonedInput != input));

    // Flattening did not impact the original models
    assertEquals(snap, basic);
  }

  private TDefinitions flatten(ResourceIdentifier assetId, List<KnowledgeCarrier> kcs) {
    CompositeKnowledgeCarrier ckc = AbstractCarrier.ofHeterogeneousComposite(kcs)
        .withRootId(assetId);

    Answer<KnowledgeCarrier> flatKC =
        kbManager.initKnowledgeBase(ckc)
            .flatMap(ptr -> kbManager.flatten(ptr.getUuid(), ptr.getVersionTag()))
            .flatMap(
                ptr -> kbManager.getKnowledgeBaseManifestation(ptr.getUuid(), ptr.getVersionTag()))
            .map(KnowledgeCarrier::mainComponent);
    assertTrue(flatKC.isSuccess());

    return flatKC.get().as(TDefinitions.class).orElseGet(Assertions::fail);
  }

  private List<KnowledgeCarrier> readDMNModels(List<String> files) {
    DMN12Parser parser = new DMN12Parser();
    return files.stream()
        .map(DMN12FlattenerTest.class::getResourceAsStream)
        .map(is -> AbstractCarrier.of(is)
            .withRepresentation(rep(DMN_1_2,
                XML_1_1, Charset.defaultCharset(), Encodings.DEFAULT)))
        .map(kc -> parser.applyLift(kc, Abstract_Knowledge_Expression, codedRep(DMN_1_2), null))
        .map(Answer::get)
        .collect(Collectors.toList());
  }


  private Stream<TDecision> streamDecisions(TDefinitions dmn) {
    return dmn.getDrgElement().stream()
        .map(JAXBElement::getValue)
        .flatMap(StreamUtil.filterAs(TDecision.class));
  }

  private Stream<TInputData> streamInputs(TDefinitions dmn) {
    return dmn.getDrgElement().stream()
        .map(JAXBElement::getValue)
        .flatMap(StreamUtil.filterAs(TInputData.class));
  }

}
