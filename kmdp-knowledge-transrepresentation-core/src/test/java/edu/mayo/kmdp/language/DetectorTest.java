package edu.mayo.kmdp.language;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.of;

import edu.mayo.kmdp.language.detectors.html.HTMLDetector;
import edu.mayo.kmdp.language.detectors.surrogate.v2.Surrogate2Detector;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;

public class DetectorTest {

  @Test
  void testHtmlDetector() {
    String html = "<html>\n"
        + "<body>\n"
        + "\n"
        + "<h1>Test Heading</h1>\n"
        + "<p>Some Paragraph.</p>\n"
        + "\n"
        + "</body>\n"
        + "</html>";

    HTMLDetector htmlDetector = new HTMLDetector();
    Answer<SyntacticRepresentation> rep = htmlDetector.applyDetect(of(html), null)
        .map(KnowledgeCarrier::getRepresentation);
    assertTrue(rep.isSuccess());
  }

  @Test
  void testHtmlDetectorWithXML() {
    String html = "<foo></foo>";

    HTMLDetector htmlDetector = new HTMLDetector();
    Answer<SyntacticRepresentation> rep = htmlDetector.applyDetect(of(html), null)
        .map(KnowledgeCarrier::getRepresentation);
    assertTrue(rep.isFailure());
  }

  @Test
  void testHtmlDetectorWithText() {
    String html = "blah blah blah";

    HTMLDetector htmlDetector = new HTMLDetector();
    Answer<SyntacticRepresentation> rep = htmlDetector.applyDetect(of(html), null)
        .map(KnowledgeCarrier::getRepresentation);
    assertTrue(rep.isFailure());
  }

  @Test
  void testSurr2Detector() {
    String xml = "<surr:knowledgeAsset "
        + "  xmlns:surr=\"https://www.omg.org/spec/API4KP/20200801/surrogate\"\n>"
        + "</surr:knowledgeAsset>";

    Surrogate2Detector surrDetector = new Surrogate2Detector();
    Answer<SyntacticRepresentation> rep = surrDetector.applyDetect(of(xml), null)
        .map(KnowledgeCarrier::getRepresentation);
    assertTrue(rep.isSuccess());
  }

}
