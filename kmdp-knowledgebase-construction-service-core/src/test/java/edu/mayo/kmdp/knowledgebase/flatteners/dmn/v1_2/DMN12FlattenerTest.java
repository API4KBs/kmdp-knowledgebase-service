package edu.mayo.kmdp.knowledgebase.flatteners.dmn.v1_2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.AbstractCompositeCarrier.ofUniformAnonymousComposite;
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
import org.omg.spec.api4kp._20200801.Composite;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.dmn._20180521.model.TDRGElement;
import org.omg.spec.dmn._20180521.model.TDecision;
import org.omg.spec.dmn._20180521.model.TDecisionService;
import org.omg.spec.dmn._20180521.model.TDefinitions;
import org.omg.spec.dmn._20180521.model.TInputData;

public class DMN12FlattenerTest {

  protected KnowledgeBaseProvider kbManager = new KnowledgeBaseProvider(null)
      .withNamedFlattener(new DMN12ModelFlattener());

  @Test
  public void testBasicFlatten() {
    String base = "/flatteners/dmn/v1_2/";
    List<String> files = Arrays.asList(
        base + "Decision Reuse.dmn.xml",
        base + "Basic Decision Model.dmn.xml"
    );
    List<KnowledgeCarrier> kcs = readDMNModels(files);
    assertEquals(2, kcs.size());

    TDefinitions reuse = kcs.get(0).as(TDefinitions.class).orElseGet(Assertions::fail);
    TDefinitions basic = kcs.get(1).as(TDefinitions.class).orElseGet(Assertions::fail);
    TDefinitions snap = (TDefinitions) basic.clone();

    TDefinitions flat = flatten(kcs.get(0).getAssetId(), kcs);
    assertEquals(reuse.getName(), flat.getName());
    assertNotSame(reuse, flat);

    assertEquals(2, streamInputs(flat).count());
    assertEquals(2, streamDecisions(flat).count());

    streamInputs(flat).forEach(clonedInput -> {
          boolean hasMatch = streamInputs(basic).anyMatch(input ->
              clonedInput.getId().equals(input.getId())
                  && clonedInput.getName().equals(input.getName())
                  && clonedInput != input);
          assertTrue(hasMatch);
        }
    );
    // Flattening did not impact the original models
    assertEquals(snap, basic);
  }


  @Test
  public void testFlattenDecisionService() {
    String base = "/flatteners/dmn/v1_2/";
    List<String> files = Arrays.asList(
        base + "Decision Client.dmn.xml",
        base + "Decision Server.dmn.xml"
    );
    List<KnowledgeCarrier> kcs = readDMNModels(files);
    assertEquals(2, kcs.size());
    TDefinitions flat = flatten(kcs.get(0).getAssetId(), kcs);

    assertEquals(3, streamInputs(flat).count());
    assertEquals(2, streamDecisions(flat).count());
    assertEquals(1, streamDecisionServices(flat).count());

    assertTrue(
        flat.getDrgElement().stream()
            .map(JAXBElement::getValue)
            .allMatch(drg -> drg.getId().startsWith("_")));

    assertTrue(
        streamDecisions(flat)
            .map(d -> d.getInformationRequirement().stream()
                .filter(drg -> drg.getRequiredInput() != null)
                .allMatch(drg -> drg.getRequiredInput().getHref().startsWith("#_")))
            .reduce(Boolean::logicalAnd)
            .orElse(false));
    assertTrue(
        streamDecisions(flat)
            .map(d -> d.getInformationRequirement().stream()
                .filter(drg -> drg.getRequiredDecision() != null)
                .allMatch(drg -> drg.getRequiredDecision().getHref().startsWith("#_")))
            .reduce(Boolean::logicalAnd)
            .orElse(false));

    TDecision client = streamDecisions(flat)
        .filter(dec -> dec.getName().contains("Sample Clinical"))
        .findFirst()
        .orElseGet(Assertions::fail);
    assertTrue(client.getInformationRequirement().isEmpty());
    assertEquals(1, client.getKnowledgeRequirement().size());

    TDecision server = streamDecisions(flat)
        .filter(dec -> dec.getName().contains("Score"))
        .findFirst()
        .orElseGet(Assertions::fail);
    assertEquals(3, server.getInformationRequirement().size());
    assertTrue(server.getKnowledgeRequirement().isEmpty());
  }

  private TDefinitions flatten(ResourceIdentifier assetId, List<KnowledgeCarrier> kcs) {
    CompositeKnowledgeCarrier ckc =
        ofUniformAnonymousComposite(assetId, kcs);

    Answer<KnowledgeCarrier> flatKC =
        kbManager.initKnowledgeBase(ckc, null)
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
    return streamDRG(dmn, TDecision.class);
  }

  private Stream<TInputData> streamInputs(TDefinitions dmn) {
    return streamDRG(dmn, TInputData.class);
  }

  private Stream<TDecisionService> streamDecisionServices(TDefinitions dmn) {
    return streamDRG(dmn, TDecisionService.class);
  }

  private <T extends TDRGElement> Stream<T> streamDRG(TDefinitions dmn, Class<T> drgType) {
    return dmn.getDrgElement().stream()
        .map(JAXBElement::getValue)
        .flatMap(StreamUtil.filterAs(drgType));
  }

}
