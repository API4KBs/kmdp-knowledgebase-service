package edu.mayo.kmdp.language.ccg.library;

import edu.mayo.kmdp.language.LanguageDeSerializer;
import edu.mayo.kmdp.language.TransrepresentationExecutor;
import edu.mayo.kmdp.language.ccg.GlossaryLibraryService;
import edu.mayo.kmdp.language.ccg.library.mock.MockGLAssetRepo;
import edu.mayo.kmdp.language.ccg.library.mock.MockTermProvider;
import edu.mayo.kmdp.language.parsers.ccg.CCGEntryParser;
import edu.mayo.kmdp.language.parsers.surrogate.v2.Surrogate2Parser;
import edu.mayo.kmdp.language.translators.surrogate.v2.SurrogateV2ToCcgEntry;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.TransxionApiInternal;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import edu.mayo.kmdp.api.terminology.v4.server.TermsApiInternal._lookupTerm;
import edu.mayo.kmdp.api.terminology.v4.server.TermsApiInternal;

public class GlossaryLibraryTestBase {

  protected MockGLAssetRepo semanticRepository = new MockGLAssetRepo();

  protected DeserializeApiInternal parserApi = new LanguageDeSerializer(
      Arrays.asList(new CCGEntryParser(), new Surrogate2Parser()));
  protected TransxionApiInternal transxionApi = new TransrepresentationExecutor(
      Collections.singletonList(new SurrogateV2ToCcgEntry()));

  protected TermsApiInternal terminologyProvider = new MockTermProvider();

  protected GlossaryLibraryService libraryApi;

  @BeforeEach
  void reinit() {

    libraryApi = new GlossaryLibraryService(
        semanticRepository,
        semanticRepository,
        new MockTermProvider()::lookupTerm);
  }


  protected void doPublish(KnowledgeCarrier buildSurrogate, KnowledgeCarrier buildArtifact) {
    ResourceIdentifier rid = buildSurrogate.mainComponent().getAssetId();
    semanticRepository.addCanonicalKnowledgeAssetSurrogate(
        rid.getUuid(),
        rid.getVersionTag(),
        buildSurrogate);

    ResourceIdentifier aid = buildArtifact.getAssetId();
    ResourceIdentifier xid = buildArtifact.getArtifactId();
    semanticRepository.setKnowledgeAssetCarrierVersion(
        aid.getUuid(), aid.getVersionTag(),
        xid.getUuid(), xid.getVersionTag(),
        buildArtifact.asBinary()
            .orElseThrow(IllegalArgumentException::new));
  }
}
