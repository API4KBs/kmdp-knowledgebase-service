package edu.mayo.kmdp.knowledgebase.extractors.dita.v1_3;

import static java.nio.charset.Charset.defaultCharset;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DITA_1_3;

import edu.mayo.kmdp.knowledgebase.extractors.dita.v1_3.DITAConceptExtractor.Entry;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;

class DITAExtractorTest {

  DITAConceptExtractor selector = new DITAConceptExtractor();

  @Test
  void testDITAExtraction() {
    KnowledgeCarrier ditaCarrier = loadDITA("/selectors/dita/v1_3/test.dita.xml");

    List<Entry> entries = roundtrip(ditaCarrier);
    assertNotNull(entries);
    assertEquals(2, entries.size());
    Entry e = entries.stream()
        .filter(x -> x.getTerm().equals("XYZClass"))
        .findFirst().orElseGet(Assertions::fail);
    assertEquals(5, e.getValues().split(" ").length);
  }

  @Test
  void testDITAExtraction2() {
    KnowledgeCarrier ditaCarrier = loadDITA("/selectors/dita/v1_3/test2.dita.xml");
    List<Entry> entries = roundtrip(ditaCarrier);
    assertEquals(4, entries.size());

    assertTrue(entries.stream().anyMatch(e -> "chadsScore".equals(e.getTerm())));
    assertTrue(entries.stream().anyMatch(e -> "female".equals(e.getTerm())));
    assertTrue(entries.stream().anyMatch(e -> "strokeRisk".equals(e.getTerm())));
  }

  @Test
  void testDITAExtraction3() {
    KnowledgeCarrier ditaCarrier = loadDITA("/selectors/dita/v1_3/test3.dita.xml");
    List<Entry> entries = roundtrip(ditaCarrier);
    assertEquals(10, entries.size());
  }

  private List<Entry> roundtrip(KnowledgeCarrier ditaCarrier) {
    Answer<byte[]> data = selector
        .applyNamedExtractDirect(DITAConceptExtractor.id, ditaCarrier, null, null)
        .flatMap(selector::serialize)
        .flatOpt(AbstractCarrier::asBinary);
    assertTrue(data.isSuccess(), data.getExplanation().asString().orElse(""));

    String src = new String(data.get());
    return Arrays.stream(src.split("\n"))
        .map(Entry::parse)
        .collect(Collectors.toList());
  }

  private KnowledgeCarrier loadDITA(String path) {
    return AbstractCarrier.of(
            DITAExtractorTest.class.getResourceAsStream(path))
        .withRepresentation(rep(DITA_1_3, XML_1_1, defaultCharset(), Encodings.DEFAULT))
        .withAssetId(SemanticIdentifier.randomId());
  }

}
