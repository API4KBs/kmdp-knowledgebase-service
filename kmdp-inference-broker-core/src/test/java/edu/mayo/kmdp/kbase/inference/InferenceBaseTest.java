package edu.mayo.kmdp.kbase.inference;

import static edu.mayo.kmdp.registry.Registry.DID_URN_URI;
import static org.junit.jupiter.api.Assertions.fail;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.artifactId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.assetId;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HL7_CQL_1_3;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.asEnum;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Encoded_Knowledge_Expression;

import edu.mayo.kmdp.kbase.inference.dmn.KieDMNHelper;
import edu.mayo.kmdp.kbase.inference.mockRepo.MockSingletonAssetRepository;
import edu.mayo.kmdp.knowledgebase.KnowledgeBaseProvider;
import edu.mayo.kmdp.knowledgebase.introspectors.cql.v1_3.CQLMetadataIntrospector;
import edu.mayo.kmdp.knowledgebase.introspectors.dmn.DMNMetadataIntrospector;
import edu.mayo.kmdp.knowledgebase.introspectors.dmn.v1_1.DMN11MetadataIntrospector;
import java.io.InputStream;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.kie.dmn.api.core.DMNRuntime;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetRepositoryApiInternal;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.services.KPComponent;
import org.omg.spec.api4kp._20200801.services.KnowledgeBase;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;

public abstract class InferenceBaseTest {

  public static final String VTAG = "0.0.0-LATEST";

  @KPComponent
  KnowledgeBaseApiInternal kbManager = new KnowledgeBaseProvider(null);

  @KPComponent
  DMNMetadataIntrospector dmnMetadataExtractor = new DMNMetadataIntrospector(kbManager);

  @KPComponent
  CQLMetadataIntrospector cqlMetadataExtractor = new CQLMetadataIntrospector(kbManager);


  protected MockSingletonAssetRepository initMockRepo(UUID modelId, String version,
      String path) {
    return initMockRepo(modelId, version, path, getRepresentation(path));
  }

  protected SyntacticRepresentation getRepresentation(String modelPath) {
    SyntacticRepresentation rep;
    if (modelPath.endsWith(".dmn")) {
      rep = rep(DMN_1_1, XML_1_1);
    } else if (modelPath.endsWith("cql")) {
      rep = rep(HL7_CQL_1_3, TXT);
    } else {
      throw new IllegalArgumentException("Unable to detect model type from path " + modelPath);
    }
    return rep;
  }

  protected MockSingletonAssetRepository initMockRepo(UUID modelId, String version,
      String path,
      SyntacticRepresentation rep) {

    KnowledgeCarrier carrier = AbstractCarrier
        .of(getBytes(path))
        .withAssetId(assetId(DID_URN_URI, modelId.toString(), version))
        .withArtifactId(artifactId(DID_URN_URI, UUID.randomUUID().toString(), VTAG))
        .withLevel(Encoded_Knowledge_Expression)
        .withRepresentation(rep);

    return new MockSingletonAssetRepository(modelId, version, carrier, getSurrogate(carrier));
  }

  private KnowledgeAsset getSurrogate(KnowledgeCarrier carrier) {

    Pointer ptr = kbManager.initKnowledgeBase(carrier, null).orElseGet(Assertions::fail);

    switch (asEnum(carrier.getRepresentation().getLanguage())) {
      case DMN_1_1:
        return dmnMetadataExtractor
            .applyNamedIntrospect(DMN11MetadataIntrospector.id, ptr.getUuid(), ptr.getVersionTag(),
                null)
            .flatOpt(kc -> kc.as(KnowledgeAsset.class))
            .orElseThrow(UnsupportedOperationException::new);

      case HL7_CQL_1_3:
        return cqlMetadataExtractor
            .applyNamedIntrospect(CQLMetadataIntrospector.id, ptr.getUuid(), ptr.getVersionTag(),
                null)
            .flatOpt(kc -> kc.as(KnowledgeAsset.class))
            .orElseThrow(UnsupportedOperationException::new);
      default:
        fail("Not supported");
        throw new UnsupportedOperationException();
    }
  }

  private InputStream getBytes(String path) {
    return InferenceBaseTest.class.getResourceAsStream(path);
  }


  protected DMNRuntime initRuntime(String modelPath) {
    UUID id = UUID.randomUUID();

    SyntacticRepresentation rep = getRepresentation(modelPath);
    KnowledgeAssetRepositoryApiInternal semRepo =
        initMockRepo(id, VTAG, modelPath, rep);

    return KieDMNHelper
        .initRuntime(new KnowledgeBase().withManifestation(
            semRepo.getKnowledgeAssetVersionCanonicalCarrier(id, VTAG).get()));
  }

}
