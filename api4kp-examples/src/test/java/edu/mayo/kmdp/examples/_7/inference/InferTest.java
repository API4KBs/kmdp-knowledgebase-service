package edu.mayo.kmdp.examples._7.inference;

import static edu.mayo.kmdp.registry.Registry.BASE_UUID_URN_URI;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.of;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.artifactId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.assetId;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_1;

import edu.mayo.kmdp.examples._3.publish.PublicationTest;
import edu.mayo.kmdp.knowledgebase.KnowledgeBaseProvider;
import edu.mayo.kmdp.knowledgebase.introspectors.dmn.v1_1.DMN11MetadataIntrospector;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryService;
import edu.mayo.kmdp.util.FileUtil;
import java.nio.charset.Charset;
import java.util.UUID;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.IntegerType;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.Type;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.api.inference.v4.server.ReasoningApiInternal._evaluate;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.datatypes.Bindings;
import org.omg.spec.api4kp._20200801.id.IdentifierConstants;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;

public class InferTest {

  UUID modelId = UUID.nameUUIDFromBytes("mockPredictor".getBytes());
  String versionTag = "0.0.0";

  DMN11MetadataIntrospector introspector = new DMN11MetadataIntrospector();

  KnowledgeAssetRepositoryService assetRepo = KnowledgeAssetRepositoryService
      .selfContainedRepository();
  KnowledgeBaseApiInternal kbaseManager = new KnowledgeBaseProvider(assetRepo)
      .withNamedIntrospector(introspector);

  private void publish() {
    byte[] modelData = FileUtil
        .readBytes(PublicationTest.class.getResourceAsStream("/mock/MockPredictor.dmn"))
        .orElseGet(Assertions::fail);
    KnowledgeCarrier artifactCarrier = of(modelData)
        .withAssetId(assetId(BASE_UUID_URN_URI, modelId, IdentifierConstants.VERSION_ZERO))
        .withArtifactId(artifactId(BASE_UUID_URN_URI, UUID.randomUUID().toString(), versionTag))
        .withRepresentation(rep(DMN_1_1, XML_1_1, Charset.defaultCharset(), Encodings.DEFAULT));

    // introspect
    KnowledgeAsset surrogate =
        introspector.applyNamedIntrospectDirect(DMN11MetadataIntrospector.id, artifactCarrier, null)
            .flatOpt(kc -> kc.as(KnowledgeAsset.class))
            .orElseGet(Assertions::fail);

    assetRepo.publish(surrogate, artifactCarrier);
  }

  private _evaluate initInference() {
    publish();
    return assetRepo
        // get Metadata
        .getKnowledgeAssetVersion(modelId, versionTag)
        // Use metadata to instantiate the appropriate engine
        // and deploy the KB constructed around the asset
        .flatOpt(asset -> new DMNEngineProvider(kbaseManager).apply(asset))
        .orElseGet(Assertions::fail);
  }

  @Test
  public void testInference() {
    _evaluate infService = initInference();

    Bindings<String, Type> map = new Bindings<>();
    map.put(MockVocab.Current_Caffeine_User.getTag(),
        new BooleanType().setValue(true));
    map.put(MockVocab.Current_Chronological_Age.getTag(),
        new IntegerType().setValue(37));

    java.util.Map<?, ?> out = infService.evaluate(modelId, versionTag, map, null)
        .orElseGet(Bindings::new);

    System.out.println(out);
    Quantity qty = (Quantity) out.get(MockVocab.Survival_Rate.getTag());
    System.out.println("Inferred risk >> " + qty.getValue());
  }


}
