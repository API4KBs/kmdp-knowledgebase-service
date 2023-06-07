package edu.mayo.kmdp.examples._4.discover;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.of;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;

import edu.mayo.kmdp.examples.PlatformConfig;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryService;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = PlatformConfig.class)
public class DiscoverTest {

  @KPServer
  @Inject
  DeserializeApiInternal parser;

  KnowledgeAssetRepositoryService assetRepo
      = KnowledgeAssetRepositoryService.selfContainedRepository();

  @BeforeEach
  void setup() {
    InputStream modelIs = DiscoverTest.class
        .getResourceAsStream("/mock/Basic Decision Model.dmn.xml");
    InputStream surrIs = DiscoverTest.class
        .getResourceAsStream("/mock/Basic Decision Model.surrogate.xml");
    publish(modelIs, surrIs);

    InputStream modelIs2 = DiscoverTest.class
        .getResourceAsStream("/mock/Basic Case Model.cmmn.xml");
    InputStream surrIs2 = DiscoverTest.class
        .getResourceAsStream("/mock/Basic Case Model.surrogate.xml");
    publish(modelIs2, surrIs2);
  }

  private void publish(InputStream modelIs, InputStream surrIs) {
    KnowledgeAsset surrogate = parser.applyLift(
        of(surrIs,rep(Knowledge_Asset_Surrogate_2_0, XML_1_1)),
        Abstract_Knowledge_Expression.getTag())
        .flatOpt(kc -> kc.as(KnowledgeAsset.class))
        .orElseGet(Assertions::fail);
    KnowledgeCarrier artifact = of(modelIs)
        .withAssetId(surrogate.getAssetId())
        .withArtifactId(surrogate.getCarriers().get(0).getArtifactId());

    assetRepo.publish(surrogate, artifact);
  }

  @Test
  void testListAssets() {
    List<Pointer> pointers = assetRepo.listKnowledgeAssets()
        .orElse(Collections.emptyList());
    assertFalse(pointers.isEmpty());

    System.out.println("Number of published assets >> " + pointers.size());
  }

  @Test
  void testGetAsset() {
    List<Pointer> pointers = assetRepo.listKnowledgeAssets()
        .orElse(Collections.emptyList());
    assertFalse(pointers.isEmpty());
    Pointer vid = pointers.get(0);
    KnowledgeAsset asset
        = assetRepo.getKnowledgeAsset(vid.getUuid(), vid.getVersionTag())
        .orElse(null);

    System.out.println("Asset name >> " + asset.getName());
  }


}
