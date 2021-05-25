package edu.mayo.kmdp.knowledgebase.flatteners.dmn.v1_2;

import static edu.mayo.kmdp.language.common.dmn.v1_2.DMN12Utils.joins;
import static edu.mayo.kmdp.language.common.dmn.v1_2.DMN12Utils.streamBKM;
import static edu.mayo.kmdp.language.common.dmn.v1_2.DMN12Utils.streamDecisionServices;
import static edu.mayo.kmdp.language.common.dmn.v1_2.DMN12Utils.streamDecisions;
import static edu.mayo.kmdp.language.common.dmn.v1_2.DMN12Utils.streamInputs;
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
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBElement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.dmn._20180521.model.TBusinessKnowledgeModel;
import org.omg.spec.dmn._20180521.model.TDecision;
import org.omg.spec.dmn._20180521.model.TDecisionService;
import org.omg.spec.dmn._20180521.model.TDefinitions;
import org.omg.spec.dmn._20180521.model.TInputData;

class DMN12FlattenerTest {

  protected KnowledgeBaseProvider kbManager = new KnowledgeBaseProvider(null)
      .withNamedFlattener(new DMN12ModelFlattener());

  @Test
  void testBasicFlatten() {
    String base = "/flatteners/dmn/v1_2/basic/";
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
  void testFlattenDecisionService() {
    String base = "/flatteners/dmn/v1_2/basic/";
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
            .allMatch(drg -> drg.getId() != null));

    assertTrue(
        streamDecisions(flat)
            .map(d -> d.getInformationRequirement().stream()
                .filter(drg -> drg.getRequiredInput() != null)
                .allMatch(drg -> drg.getRequiredInput().getHref().startsWith("#")))
            .reduce(Boolean::logicalAnd)
            .orElse(false));
    assertTrue(
        streamDecisions(flat)
            .map(d -> d.getInformationRequirement().stream()
                .filter(drg -> drg.getRequiredDecision() != null)
                .allMatch(drg -> drg.getRequiredDecision().getHref().startsWith("#")))
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


  @Test
  void testFlattenDecisionServiceSubService() {
    String base = "/flatteners/dmn/v1_2/nestedEncapsulated/";
    List<String> files = Arrays.asList(
        base + "Client.dmn.xml",
        base + "Service.dmn.xml",
        base + "SubService.dmn.xml"
    );
    List<KnowledgeCarrier> kcs = readDMNModels(files);
    assertEquals(3, kcs.size());
    TDefinitions flat = flatten(kcs.get(0).getAssetId(), kcs);

    TDecision client = streamDecisions(flat)
        .filter(d -> "Client".equalsIgnoreCase(d.getName().trim()))
        .findFirst().orElseGet(Assertions::fail);
    String clidRef = client.getKnowledgeRequirement().get(0).getRequiredKnowledge().getHref();
    assertTrue(clidRef.startsWith("#"));

    TBusinessKnowledgeModel clientBKM = streamBKM(flat)
        .filter(tbkm -> clidRef.contains(tbkm.getId()))
        .findFirst().orElseGet(Assertions::fail);

    TDecisionService service = streamDecisionServices(flat)
        .filter(ds -> "Service".equals(ds.getName().trim()))
        .findFirst().orElseGet(Assertions::fail);

    TDecision serviceOut = streamDecisions(flat)
        .filter(d -> "Service Decision".equalsIgnoreCase(d.getName().trim()))
        .findFirst().orElseGet(Assertions::fail);

    assertTrue(joins(serviceOut.getId(), service.getOutputDecision().get(0).getHref()));

    TDecision subServiceOut = streamDecisions(flat)
        .filter(d -> "SubService Decision".equalsIgnoreCase(d.getName().trim()))
        .findFirst().orElseGet(Assertions::fail);

    String subClIdRef = serviceOut.getKnowledgeRequirement().get(0)
        .getRequiredKnowledge().getHref();
    assertTrue(subClIdRef.startsWith("#"));
    TBusinessKnowledgeModel subBKM = streamBKM(flat)
        .filter(tbkm -> joins(tbkm.getId(), subClIdRef))
        .findFirst().orElseGet(Assertions::fail);

    TInputData subInput = streamInputs(flat)
        .filter(d -> "SubInput".equalsIgnoreCase(d.getName().trim()))
        .findFirst().orElseGet(Assertions::fail);

    TDecisionService subService = streamDecisionServices(flat)
        .filter(d -> "SubService".equalsIgnoreCase(d.getName().trim()))
        .findFirst().orElseGet(Assertions::fail);
    assertTrue(joins(subServiceOut.getId(), subService.getOutputDecision().get(0).getHref()));

    assertTrue(joins(service.getId(),
        clientBKM.getKnowledgeRequirement().get(0).getRequiredKnowledge().getHref()));
    assertTrue(joins(subService.getId(),
        subBKM.getKnowledgeRequirement().get(0).getRequiredKnowledge().getHref()));
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

}
