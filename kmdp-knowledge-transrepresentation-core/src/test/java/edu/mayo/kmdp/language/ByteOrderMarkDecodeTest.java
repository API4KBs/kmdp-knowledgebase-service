package edu.mayo.kmdp.language;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.of;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;

import edu.mayo.kmdp.language.parsers.html.HtmlDeserializer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Base64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;

public class ByteOrderMarkDecodeTest {

  @Test
  void testBOM() throws IOException {
    String html = "<html></html>";

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] bom = new byte[3];
    bom[0] = (byte) 0xEF;
    bom[1] = (byte) 0xBB;
    bom[2] = (byte) 0xBF;
    baos.write(bom);
    baos.write(html.getBytes());
    String enc = Base64.getEncoder().encodeToString(baos.toByteArray());

    KnowledgeCarrier kc = of(enc, rep(HTML, TXT, Charset.defaultCharset(), Encodings.DEFAULT));

    String decoded = new HtmlDeserializer().innerDecode(kc, null)
        .orElseGet(Assertions::fail)
        .asString()
        .orElseGet(Assertions::fail);

    assertEquals(html, decoded);
  }

}
