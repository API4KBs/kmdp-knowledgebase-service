package edu.mayo.kmdp.language.validators.api4kp.v1_0;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Cognitive_Process_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.mayo.kmdp.util.JSonUtil;
import edu.mayo.kmdp.util.JaxbUtil;
import edu.mayo.kmdp.util.properties.jaxb.JaxbConfig;
import java.net.URI;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries;
import org.w3c.dom.Document;

public class SurrogateV2ValidatorTest {

  private KnowledgeCarrier KC_JSON_VALID = validJSONKnowledgeCarrier();
  private KnowledgeCarrier KC_XML_VALID = validXMLKnowledgeCarrier();
  private KnowledgeCarrier KC_JSON_INVALID = invalidJSONKnowledgeCarrier();
  private KnowledgeCarrier KC_XML_INVALID = invalidXMLKnowledgeCarrier();

  @Test
  void test_serialized_knowledge_expression_json_valid() {
    SurrogateV2Validator surrogateV2Validator = new SurrogateV2Validator();
    KnowledgeCarrier kc = KC_JSON_VALID;
    Answer<Void> result =
        surrogateV2Validator.applyNamedValidate(
            SurrogateV2Validator.id, kc, null);
    assertTrue(result.isSuccess());
  }

  @Test
  void test_serialized_knowledge_expression_json_invalid() {
    // Missing 'language' property on SyntacticRepresentation
    SurrogateV2Validator surrogateV2Validator = new SurrogateV2Validator();
    KnowledgeCarrier kc = KC_JSON_INVALID;

    Answer<Void> result =
        surrogateV2Validator.applyNamedValidate(
            SurrogateV2Validator.id, kc, null);
    assertFalse(result.isSuccess());
  }

  @Test
  void test_serialized_knowledge_expression_xml_valid() {
    SurrogateV2Validator surrogateV2Validator = new SurrogateV2Validator();
    KnowledgeCarrier kc = KC_XML_VALID;
    Answer<Void> result =
        surrogateV2Validator.applyNamedValidate(
            SurrogateV2Validator.id, kc, null);
    assertTrue(result.isSuccess());
  }

  @Test
  void test_serialized_knowledge_expression_xml_invalid() {
    // Missing 'language' property on SyntacticRepresentation
    SurrogateV2Validator surrogateV2Validator = new SurrogateV2Validator();
    KnowledgeCarrier kc = KC_XML_INVALID;
    Answer<Void> result =
        surrogateV2Validator.applyNamedValidate(
            SurrogateV2Validator.id, kc, null);
    assertFalse(result.isSuccess());
  }

  @Test
  void test_encoded_knowledge_expression_json_valid() {
    SurrogateV2Validator surrogateV2Validator = new SurrogateV2Validator();
    KnowledgeCarrier kc = KC_JSON_VALID;
    kc.setExpression(kc.getExpression().toString().getBytes());
    kc.setLevel(ParsingLevelSeries.Encoded_Knowledge_Expression);
    Answer<Void> result =
        surrogateV2Validator.applyNamedValidate(
            SurrogateV2Validator.id, kc, null);
    assertTrue(result.isSuccess());
  }

  @Test
  void test_encoded_knowledge_expression_json_invalid() {
    SurrogateV2Validator surrogateV2Validator = new SurrogateV2Validator();
    KnowledgeCarrier kc = KC_JSON_INVALID;
    Answer<Void> result =
        surrogateV2Validator.applyNamedValidate(
            SurrogateV2Validator.id, kc, null);
    assertFalse(result.isSuccess());
  }

  @Test
  void test_encoded_knowledge_expression_xml_valid() {
    SurrogateV2Validator surrogateV2Validator = new SurrogateV2Validator();
    KnowledgeCarrier kc = KC_XML_VALID;
    Answer<Void> result =
        surrogateV2Validator.applyNamedValidate(
            SurrogateV2Validator.id, kc, null);
    assertTrue(result.isSuccess());
  }

  @Test
  void test_encoded_knowledge_expression_xml_invalid() {
    SurrogateV2Validator surrogateV2Validator = new SurrogateV2Validator();
    KnowledgeCarrier kc = KC_XML_INVALID;
    Answer<Void> result =
        surrogateV2Validator.applyNamedValidate(
            SurrogateV2Validator.id, kc, null);
    assertFalse(result.isSuccess());
  }

  @Test
  void test_concrete_knowledge_expression_json_valid() {
    SurrogateV2Validator surrogateV2Validator = new SurrogateV2Validator();
    KnowledgeCarrier kc = KC_JSON_VALID;
    JsonNode jsonNode =
        JSonUtil.toJsonNode(
            JSonUtil.parseJson(kc.getExpression().toString(), KnowledgeAsset.class).get())
            .get();
    kc.setExpression(jsonNode);
    kc.setLevel(ParsingLevelSeries.Concrete_Knowledge_Expression);
    Answer<Void> result =
        surrogateV2Validator.applyNamedValidate(
            SurrogateV2Validator.id, kc, null);
    assertTrue(result.isSuccess());
  }

  @Test
  void test_concrete_knowledge_expression_json_invalid() {
    SurrogateV2Validator surrogateV2Validator = new SurrogateV2Validator();
    KnowledgeCarrier kc = KC_JSON_INVALID;
    Answer<Void> result =
        surrogateV2Validator.applyNamedValidate(
            SurrogateV2Validator.id, kc, null);
    assertFalse(result.isSuccess());
  }

  @Test
  void test_concrete_knowledge_expression_xml_valid() {
    SurrogateV2Validator surrogateV2Validator = new SurrogateV2Validator();
    KnowledgeCarrier kc = KC_XML_VALID;
    Answer<Void> result =
        surrogateV2Validator.applyNamedValidate(
            SurrogateV2Validator.id, kc, null);
    assertTrue(result.isSuccess());
  }

  @Test
  void test_concrete_knowledge_expression_xml_invalid() {
    SurrogateV2Validator surrogateV2Validator = new SurrogateV2Validator();
    KnowledgeCarrier kc = KC_XML_INVALID;
    Answer<Void> result =
        surrogateV2Validator.applyNamedValidate(
            SurrogateV2Validator.id, kc, null);
    assertFalse(result.isSuccess());
  }

  @Test
  void test_abstract_knowledge_expression_valid() {
    SurrogateV2Validator surrogateV2Validator = new SurrogateV2Validator();
    KnowledgeCarrier kc = KC_JSON_VALID;
    KnowledgeAsset ka =
        JSonUtil.parseJson(kc.getExpression().toString(), KnowledgeAsset.class).get();
    kc.setExpression(ka);
    kc.setLevel(ParsingLevelSeries.Abstract_Knowledge_Expression);
    Answer<Void> result =
        surrogateV2Validator.applyNamedValidate(
            SurrogateV2Validator.id, kc, null);
    assertTrue(result.isSuccess());
  }

  @Test
  void test_abstract_knowledge_expression_invalid() {
    SurrogateV2Validator surrogateV2Validator = new SurrogateV2Validator();
    KnowledgeCarrier kc = KC_JSON_INVALID;
    Answer<Void> result =
        surrogateV2Validator.applyNamedValidate(
            SurrogateV2Validator.id, kc, null);
    assertFalse(result.isSuccess());
  }


  private KnowledgeCarrier validJSONKnowledgeCarrier() {
    ResourceIdentifier assetId = SemanticIdentifier.randomId();
    JsonNode node = JSonUtil.toJsonNode(mockKnowledgeAsset(assetId))
        .orElseGet(Assertions::fail);

    return AbstractCarrier.of(node)
        .withRepresentation(rep(Knowledge_Asset_Surrogate_2_0, JSON))
        .withAssetId(assetId);
  }

  private KnowledgeCarrier invalidJSONKnowledgeCarrier() {
    ResourceIdentifier assetId = SemanticIdentifier.randomId();
    JsonNode node = JSonUtil.toJsonNode(mockKnowledgeAsset(assetId))
        .orElseGet(Assertions::fail);
    ((ObjectNode) node).put("invalid", "invalid");

    return AbstractCarrier.of(node)
        .withRepresentation(rep(Knowledge_Asset_Surrogate_2_0, JSON))
        .withAssetId(assetId);
  }

  private KnowledgeCarrier validXMLKnowledgeCarrier() {
    ResourceIdentifier assetId = SemanticIdentifier.randomId();
    Document xml = JaxbUtil.marshallDox(
        Collections.singletonList(KnowledgeAsset.class),
        mockKnowledgeAsset(assetId), new JaxbConfig())
        .orElseGet(Assertions::fail);

    return AbstractCarrier.of(xml)
        .withRepresentation(rep(Knowledge_Asset_Surrogate_2_0, XML_1_1))
        .withAssetId(assetId);
  }

  private KnowledgeCarrier invalidXMLKnowledgeCarrier() {
    ResourceIdentifier assetId = SemanticIdentifier.randomId();
    Document xml = JaxbUtil.marshallDox(
        Collections.singletonList(KnowledgeAsset.class),
        mockKnowledgeAsset(assetId), new JaxbConfig())
        .orElseGet(Assertions::fail);
    xml.getDocumentElement().setAttribute("invalid", "invalid");

    return AbstractCarrier.of(xml)
        .withRepresentation(rep(Knowledge_Asset_Surrogate_2_0, XML_1_1))
        .withAssetId(assetId);
  }


  private KnowledgeAsset mockKnowledgeAsset(ResourceIdentifier assetId) {
    return new org.omg.spec.api4kp._20200801.surrogate.resources.KnowledgeAsset()
        .withAssetId(assetId)
        .withFormalType(Cognitive_Process_Model)
        .withName("Test")
        .withCarriers(
            new KnowledgeArtifact()
                .withArtifactId(SemanticIdentifier.randomId())
                .withRepresentation(rep(DMN_1_2, XML_1_1))
                .withLocator(URI.create("http://mock.org/foo"))
        );
  }

}
