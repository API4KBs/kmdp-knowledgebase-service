package edu.mayo.kmdp.language.ccg.library;

import static java.nio.charset.Charset.defaultCharset;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;

import edu.mayo.kmdp.ccg.model.GlossaryEntry;
import edu.mayo.kmdp.language.ccg.library.mock.MockComplexDef;
import edu.mayo.kmdp.language.ccg.library.mock.MockOpDef;
import edu.mayo.kmdp.language.ccg.library.mock.MockOpDef2;
import edu.mayo.kmdp.language.parsers.surrogate.v2.Surrogate2Parser;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries;

public class CompositeEntryTest extends GlossaryLibraryTestBase {

  @Test
  void testComplexByDependency() {
    MockOpDef subDef = new MockOpDef();
    doPublish(subDef.buildSurrogate(), subDef.buildArtifact());
    MockOpDef2 entry2 = new MockOpDef2();
    doPublish(entry2.buildSurrogate(), entry2.buildArtifact());

    KnowledgeAsset complex = new MockComplexDef().buildSurrogate();
    semanticRepository.setKnowledgeAssetVersion(
        complex.getAssetId().getUuid(), complex.getAssetId().getVersionTag(), complex);

    Answer<List<Pointer>> publishedAssets = semanticRepository.listKnowledgeAssets();
    assertTrue(publishedAssets.isSuccess());
    assertEquals(
        5 // MockOpDef api, data x2, sp, fpath
            + 2 // MockOpDef data, sp. api does not count twice
            + 1 // Complex
        , publishedAssets.map(List::size).orElse(-1));

    Answer<GlossaryEntry> ansGLEntry = this.libraryApi
        .getGlossaryEntry(List.of("default"), MockComplexDef.definedConcept.getUuid());
    assertTrue(ansGLEntry.isSuccess());
    GlossaryEntry glEntry = ansGLEntry.get();

    assertEquals(1, glEntry.getDef().size());
    var def = glEntry.getDef().iterator().next();
    assertNotNull(def);


    assertEquals(2, def.getIncludes().size());
    assertEquals(2, def.getInTermsOf().size());
  }


  @Test
  void testInnerSerialization() {
    Surrogate2Parser parser = new Surrogate2Parser();

    MockOpDef subDef = new MockOpDef();
    KnowledgeCarrier surrogateCarrier = subDef.buildSurrogate();

    Answer<KnowledgeCarrier> asset1 = Answer.of(surrogateCarrier)
        .flatMap(kc ->
            parser.applyLower(
                kc,
                ParsingLevelSeries.Encoded_Knowledge_Expression,
                codedRep(Knowledge_Asset_Surrogate_2_0, JSON, defaultCharset(), Encodings.DEFAULT),
                null));
    assertTrue(asset1.isSuccess());

    doPublish(asset1.orElseGet(() ->
        fail(asset1.printExplanation())), subDef.buildArtifact());

    Answer<GlossaryEntry> ansGLEntry = this.libraryApi
        .getGlossaryEntry(List.of("default"), MockOpDef.definedConcept.getUuid());
    assertTrue(ansGLEntry.isSuccess());
    GlossaryEntry glEntry = ansGLEntry.get();

    assertEquals(1, glEntry.getDef().size());
    var def = glEntry.getDef().iterator().next();
    assertNotNull(def);
  }

}