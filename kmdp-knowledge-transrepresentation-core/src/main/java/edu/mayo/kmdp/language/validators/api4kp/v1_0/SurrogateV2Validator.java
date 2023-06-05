package edu.mayo.kmdp.language.validators.api4kp.v1_0;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Well_Formedness_Check_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.asEnum;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.mayo.kmdp.language.validators.AbstractValidator;
import edu.mayo.kmdp.util.JSonUtil;
import edu.mayo.kmdp.util.JaxbUtil;
import edu.mayo.kmdp.util.Util;
import edu.mayo.kmdp.util.XMLUtil;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Named;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.ObjectFactory;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeresourceoutcome.KnowledgeResourceOutcome;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeresourceoutcome.KnowledgeResourceOutcomeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormat;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.w3c.dom.Document;

@Named
@KPOperation(Well_Formedness_Check_Task)
@KPSupport(Knowledge_Asset_Surrogate_2_0)
public class SurrogateV2Validator extends AbstractValidator {

  public static final UUID id = UUID.fromString("85ea1656-60e1-46a6-944f-942367de4ec5");
  public static final String version = "1.0.0";

  private final ResourceIdentifier operatorId;

  public SurrogateV2Validator() {
    this.operatorId = SemanticIdentifier.newId(id, version);
  }

  @Override
  public KnowledgeResourceOutcome getValidationType() {
    return KnowledgeResourceOutcomeSeries.Well_Formedness;
  }

  @Override
  public Answer<Void> applyValidate(KnowledgeCarrier sourceArtifact, String config) {
    try {
      return validateComponent(sourceArtifact, config);
    } catch (Exception e) {
      return Answer.failed(e);
    }
  }

  @Override
  protected Answer<Void> validateComponent(KnowledgeCarrier sourceArtifact, String xConfig) {
    Schema surrogateV2Schema = checkSchema(sourceArtifact);

    boolean outcome;
    switch (asEnum(sourceArtifact.getLevel())) {
      case Abstract_Knowledge_Expression:
        outcome = validateAbstractKnowledgeExpressions(sourceArtifact, surrogateV2Schema);
        break;
      case Concrete_Knowledge_Expression:
        outcome = validateConcreteKnowledgeExpression(sourceArtifact, surrogateV2Schema);
        break;
      case Serialized_Knowledge_Expression:
        outcome = validateString(sourceArtifact, surrogateV2Schema);
        break;
      case Encoded_Knowledge_Expression:
        outcome = validateBinary(sourceArtifact, surrogateV2Schema);
        break;
      default:
        outcome = false;
    }
    return outcome ? Answer.succeed() : Answer.failed();
  }

  private boolean validateString(KnowledgeCarrier sourceArtifact, Schema schema) {
    SerializationFormat serializationFormat = sourceArtifact.getRepresentation().getFormat();
    if (serializationFormat.sameAs(JSON)) {
      KnowledgeAsset knowledgeAsset =
          deserializeKnowledgeAssetJSON(sourceArtifact.getExpression().toString());
      return XMLUtil.validate(knowledgeAssetToXMLString(knowledgeAsset), schema);
    } else if (serializationFormat.sameAs(XML_1_1)) {
      return XMLUtil.validate(sourceArtifact.getExpression().toString(), schema);
    } else {
      throw new UnsupportedOperationException(
          "SurrogateV2 Validator unable to support representation format"
              + sourceArtifact.getRepresentation().getFormat().getName());
    }
  }

  private boolean validateBinary(KnowledgeCarrier sourceArtifact, Schema schema) {
    SerializationFormat serializationFormat = sourceArtifact.getRepresentation().getFormat();
    if (serializationFormat.sameAs(JSON)) {
      KnowledgeAsset knowledgeAsset =
          deserializeKnowledgeAssetBinary((byte[]) sourceArtifact.getExpression());
      return XMLUtil.validate(knowledgeAssetToXMLString(knowledgeAsset), schema);
    } else if (serializationFormat.sameAs(XML_1_1)) {
      return XMLUtil.validate(
          new StreamSource(new ByteArrayInputStream((byte[]) sourceArtifact.getExpression())),
          schema);
    } else {
      throw new UnsupportedOperationException(
          "SurrogateV2 Validator unable to support representation format"
              + sourceArtifact.getRepresentation().getFormat().getName());
    }
  }

  private boolean validateConcreteKnowledgeExpression(
      KnowledgeCarrier sourceArtifact, Schema schema) {
    SerializationFormat serializationFormat = sourceArtifact.getRepresentation().getFormat();
    if (serializationFormat.sameAs(JSON)) {
      KnowledgeAsset knowledgeAsset = jsonNodeToKnowledgeAsset(sourceArtifact.getExpression());
      return XMLUtil.validate(knowledgeAssetToXMLString(knowledgeAsset), schema);
    } else if (serializationFormat.sameAs(XML_1_1)) {
      return XMLUtil.validate(
          xmlDocumentToString((Document) sourceArtifact.getExpression()), schema);
    } else {
      throw new UnsupportedOperationException(
          "SurrogateV2 Validator unable to support representation format"
              + sourceArtifact.getRepresentation().getFormat().getName());
    }
  }

  private boolean validateAbstractKnowledgeExpressions(
      KnowledgeCarrier sourceArtifact, Schema schema) {
    KnowledgeAsset knowledgeAsset = (KnowledgeAsset) sourceArtifact.getExpression();
    return XMLUtil.validate(knowledgeAssetToXMLString(knowledgeAsset), schema);
  }

  private Schema checkSchema(KnowledgeCarrier sourceArtifact) {
    if (!Knowledge_Asset_Surrogate_2_0.sameAs(sourceArtifact.getRepresentation().getLanguage())) {
      throw new UnsupportedOperationException(
          "SurrogateV2 Validator unable to validate "
              + sourceArtifact.getRepresentation().getLanguage());
    }
    return XMLUtil.getSchemas(Knowledge_Asset_Surrogate_2_0.getReferentId())
        .orElseThrow(
            () -> new UnsupportedOperationException("Unable to retrieve schema for SurrogateV2"));
  }

  private static String knowledgeAssetToXMLString(KnowledgeAsset knowledgeAsset) {
    ObjectFactory of = new ObjectFactory();
    return JaxbUtil.marshall(
            Collections.singleton(of.getClass()),
            knowledgeAsset,
            of::createKnowledgeAsset,
            JaxbUtil.defaultProperties())
        .flatMap(Util::asString)
        .orElse("");
  }

  private static KnowledgeAsset deserializeKnowledgeAssetJSON(String sourceArtifact) {
    Optional<KnowledgeAsset> knowledgeAsset =
        JSonUtil.readJson(sourceArtifact, KnowledgeAsset.class);
    if (knowledgeAsset.isEmpty()) {
      throw new UnsupportedOperationException(
          "Unable to deserialize KnowledgeAsset from Serialized Knowledge Expression");
    }
    return knowledgeAsset.get();
  }

  private static KnowledgeAsset deserializeKnowledgeAssetBinary(byte[] sourceArtifact) {
    Optional<KnowledgeAsset> knowledgeAsset =
        JSonUtil.readJson(sourceArtifact, KnowledgeAsset.class);
    if (knowledgeAsset.isEmpty()) {
      throw new UnsupportedOperationException(
          "Unable to deserialize KnowledgeAsset from Encoded Knowledge Expression");
    }
    return knowledgeAsset.get();
  }

  private static KnowledgeAsset jsonNodeToKnowledgeAsset(Object expression) {
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode node = objectMapper.valueToTree(expression);
    Optional<KnowledgeAsset> knowledgeAsset = JSonUtil.parseJson(node, KnowledgeAsset.class);
    if (knowledgeAsset.isEmpty()) {
      throw new UnsupportedOperationException(
          "Unable to parse KnowledgeAsset from Concrete Knowledge Expression");
    }
    return knowledgeAsset.get();
  }

  private static String xmlDocumentToString(Document expression) {
    String xmlString = new String(XMLUtil.toByteArray(expression));
    if (xmlString.isEmpty()) {
      throw new UnsupportedOperationException("Unable to marshall Concrete Knowledge Expression");
    }
    return xmlString;
  }

  @Override
  public List<SyntacticRepresentation> getFrom() {
    return Arrays.asList(
        rep(Knowledge_Asset_Surrogate_2_0),
        rep(Knowledge_Asset_Surrogate_2_0, XML_1_1),
        rep(Knowledge_Asset_Surrogate_2_0, XML_1_1, Charset.defaultCharset()),
        rep(Knowledge_Asset_Surrogate_2_0, XML_1_1, Charset.defaultCharset(), Encodings.DEFAULT));
  }

  @Override
  public ResourceIdentifier getOperatorId() {
    return operatorId;
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return Knowledge_Asset_Surrogate_2_0;
  }

}
