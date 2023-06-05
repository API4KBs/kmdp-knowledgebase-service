package edu.mayo.kmdp.language.translators;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.newRandomSurrogate;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;

import com.fasterxml.jackson.databind.JsonNode;
import edu.mayo.kmdp.language.translators.surrogate.v2.SurrogateV2Transcriptor;
import edu.mayo.kmdp.util.JSonUtil;
import edu.mayo.kmdp.util.JaxbUtil;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder;
import org.w3c.dom.Document;

class SurrogateTranscriptorTest {

  @Test
  void testXMLtoJSONTranscript() {
    KnowledgeAsset asset = newRandomSurrogate().get();

    KnowledgeCarrier jsonCarrier = AbstractCarrier.of(
        JSonUtil.printJson(asset).orElseGet(Assertions::fail))
        .withAssetId(asset.getAssetId())
        .withRepresentation(rep(Knowledge_Asset_Surrogate_2_0, JSON, Charset.defaultCharset()));

    KnowledgeCarrier xmlCarrier = new SurrogateV2Transcriptor()
        .applyTransrepresent(jsonCarrier,
            codedRep(rep(Knowledge_Asset_Surrogate_2_0, XML_1_1)),
            null)
        .orElseGet(Assertions::fail);

    assertTrue(xmlCarrier.getExpression() instanceof Document);
  }

  @Test
  void testJSONtoXMLTranscript() {
    KnowledgeAsset asset = newRandomSurrogate().get();

    KnowledgeCarrier xmlCarrier = AbstractCarrier.of(
        JaxbUtil.marshallToString(asset))
        .withAssetId(asset.getAssetId())
        .withRepresentation(rep(Knowledge_Asset_Surrogate_2_0, XML_1_1, Charset.defaultCharset()));

    KnowledgeCarrier jsonCarrier = new SurrogateV2Transcriptor()
        .applyTransrepresent(xmlCarrier,
            codedRep(rep(Knowledge_Asset_Surrogate_2_0, JSON)),
            null)
        .orElseGet(Assertions::fail);

    assertTrue(jsonCarrier.getExpression() instanceof JsonNode);
  }
}
