package edu.mayo.kmdp.examples._3.publish;


import static org.omg.spec.api4kp._20200801.AbstractCarrier.of;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;

import edu.mayo.kmdp.examples.MockAssetRepository;
import edu.mayo.kmdp.examples.PlatformConfig;
import edu.mayo.kmdp.util.Util;
import java.io.InputStream;
import java.util.Collections;
import javax.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = PlatformConfig.class)
public class PublicationTest {

  @KPServer
  @Inject
  DeserializeApiInternal parser;

  MockAssetRepository assetRepo = new MockAssetRepository();

  @Test
  void testIntrospectDMN() {
    InputStream modelIs = PublicationTest.class
        .getResourceAsStream("/mock/Basic Decision Model.dmn.xml");
    InputStream surrIs = PublicationTest.class
        .getResourceAsStream("/mock/Basic Decision Model.surrogate.xml");

    KnowledgeAsset surrogate = parser.applyLift(
        of(surrIs)
            .withRepresentation(rep(Knowledge_Asset_Surrogate_2_0, XML_1_1)),
        Abstract_Knowledge_Expression.getTag())
        .flatOpt(kc -> kc.as(KnowledgeAsset.class))
        .orElseGet(Assertions::fail);
    byte[] artifact = of(modelIs).asBinary()
        .orElseGet(Assertions::fail);

    ResourceIdentifier surrogateId = surrogate.getAssetId();
    ResourceIdentifier artifactId = surrogate.getCarriers().get(0).getArtifactId();

    // publish metadata
    assetRepo.setKnowledgeAssetVersion(
        Util.toUUID(surrogateId.getTag()),
        surrogateId.getVersionTag(),
        surrogate);

    // publish artifact
    assetRepo.setKnowledgeAssetCarrierVersion(
        Util.toUUID(surrogateId.getTag()),
        surrogateId.getVersionTag(),
        Util.toUUID(artifactId.getTag()),
        artifactId.getVersionTag(),
        artifact);

    int n = assetRepo.listKnowledgeAssets()
        .orElse(Collections.emptyList())
        .size();
    System.out.println("Number of published assets >> " + n);
  }


}
