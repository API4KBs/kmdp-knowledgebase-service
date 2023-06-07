package edu.mayo.kmdp.examples._5.carrier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.of;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.randomArtifactId;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;

import edu.mayo.kmdp.examples.PlatformConfig;
import edu.mayo.kmdp.language.detectors.html.HTMLDetector;
import edu.mayo.kmdp.language.parsers.dmn.v1_2.DMN12Parser;
import edu.mayo.kmdp.language.parsers.surrogate.v2.Surrogate2Parser;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryService;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = PlatformConfig.class)
public class CarrierTest {

  @KPServer
  @Inject
  DeserializeApiInternal parser;

  KnowledgeAssetRepositoryService assetRepo
      = KnowledgeAssetRepositoryService.selfContainedRepository(
      Arrays.asList(new Surrogate2Parser(), new DMN12Parser()),
      Collections.singletonList(new HTMLDetector()),
      Collections.emptyList(),
      Collections.emptyList()
  );

  @BeforeEach
  void setup() {
    InputStream modelIs = CarrierTest.class
        .getResourceAsStream("/mock/Basic Decision Model.dmn.xml");
    InputStream surrIs = CarrierTest.class
        .getResourceAsStream("/mock/Basic Decision Model.surrogate.xml");
    KnowledgeAsset surrogate = readSurrogate(surrIs);
    assetRepo.publish(surrogate, readArtifact(surrogate, modelIs));

    InputStream modelIs2 = CarrierTest.class.getResourceAsStream("/mock/Basic Decision Model.html");
    assetRepo.publish(surrogate,
        readArtifact(surrogate.getAssetId(), modelIs2));
  }

  private KnowledgeAsset readSurrogate(InputStream surrIs) {
    return parser.applyLift(
        of(surrIs)
            .withRepresentation(rep(Knowledge_Asset_Surrogate_2_0, XML_1_1)),
        Abstract_Knowledge_Expression.getTag())
        .flatOpt(kc -> kc.as(KnowledgeAsset.class))
        .orElseGet(Assertions::fail);
  }

  private KnowledgeCarrier readArtifact(KnowledgeAsset surrogate, InputStream modelIs) {
    return AbstractCarrier.of(modelIs)
        .withAssetId(surrogate.getAssetId())
        .withRepresentation(rep(
            surrogate.getCarriers().get(0).getRepresentation()))
        .withArtifactId(surrogate.getCarriers().get(0).getArtifactId());
  }

  private KnowledgeCarrier readArtifact(ResourceIdentifier assetId, InputStream modelIs) {
    return AbstractCarrier.of(modelIs)
        .withAssetId(assetId)
        .withRepresentation(rep(HTML, TXT))
        .withArtifactId(randomArtifactId());
  }

  @Test
  void testListCarriers() {
    List<Pointer> pointers = assetRepo.listKnowledgeAssets()
        .orElse(Collections.emptyList());
    assertTrue(pointers.size() > 0);

    ResourceIdentifier assetId = pointers.get(0);
    List<Pointer> artifacts
        = assetRepo
        .listKnowledgeAssetCarriers(assetId.getUuid(), assetId.getVersionTag())
        .orElseGet(Assertions::fail);

    System.out.println("Found Artifacts >> " + artifacts.size());
  }

  @Test
  void testGetCanonicalCarrier() {
    List<Pointer> pointers = assetRepo.listKnowledgeAssets()
        .orElse(Collections.emptyList());
    assertTrue(pointers.size() > 0);

    ResourceIdentifier assetId = pointers.get(0);
    KnowledgeCarrier kc = assetRepo
        .getKnowledgeAssetVersionCanonicalCarrier(assetId.getUuid(), assetId.getVersionTag())
        .orElseGet(Assertions::fail);

    System.out.println("Found Carrier w/ Lang >> " + kc.getRepresentation().getLanguage());
  }

  @Test
  void testGetCarrierWithNegotiation() {
    List<Pointer> pointers = assetRepo.listKnowledgeAssets()
        .orElse(Collections.emptyList());
    assertFalse(pointers.isEmpty());

    ResourceIdentifier assetId = pointers.get(0);
    KnowledgeCarrier kc = assetRepo
        .getKnowledgeAssetVersionCanonicalCarrier(assetId.getUuid(), assetId.getVersionTag(),
            "text/html")
        .orElseGet(Assertions::fail);

    System.out.println("Found Carrier w/ Lang >> " + kc.getRepresentation().getLanguage());
  }


}
