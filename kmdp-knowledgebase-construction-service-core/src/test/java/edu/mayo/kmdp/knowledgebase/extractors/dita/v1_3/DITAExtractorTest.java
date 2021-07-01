package edu.mayo.kmdp.knowledgebase.extractors.dita.v1_3;

import static java.nio.charset.Charset.defaultCharset;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DITA_1_3;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

    Workbook wb = roundtrip(ditaCarrier);
    assertNotNull(wb);
    Sheet sheet = wb.getSheetAt(0);
    assertEquals(3, sheet.getPhysicalNumberOfRows());
  }

  @Test
  void testDITAExtraction2() {
    KnowledgeCarrier ditaCarrier = loadDITA("/selectors/dita/v1_3/test2.dita.xml");
    Workbook wb = roundtrip(ditaCarrier);
    assertNotNull(wb);
    Sheet sheet = wb.getSheetAt(0);
    assertEquals(5, sheet.getPhysicalNumberOfRows());

    Set<String> params = IntStream.of(1,2,3,4)
        .mapToObj(sheet::getRow)
        .map(r -> r.getCell(0).getStringCellValue())
        .collect(Collectors.toSet());
    assertTrue(params.contains("chadsScore"));
    assertTrue(params.contains("female"));
    assertTrue(params.contains("strokeRisk"));
  }

  private Workbook roundtrip(KnowledgeCarrier ditaCarrier) {
    Answer<byte[]> data = selector
        .applyNamedExtractDirect(DITAConceptExtractor.id, ditaCarrier, null, null)
        .flatMap(selector::serialize)
        .flatOpt(AbstractCarrier::asBinary);
    assertTrue(data.isSuccess(), data.getExplanation().asString().orElse(""));

    try {
      return new XSSFWorkbook(new ByteArrayInputStream(data.get()));
    } catch (IOException e) {
      fail(e);
      return null;
    }
  }

  private KnowledgeCarrier loadDITA(String path) {
    return AbstractCarrier.of(
        DITAExtractorTest.class.getResourceAsStream(path))
        .withRepresentation(rep(DITA_1_3, XML_1_1, defaultCharset(), Encodings.DEFAULT))
        .withAssetId(SemanticIdentifier.randomId());
  }

}
