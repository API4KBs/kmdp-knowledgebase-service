package edu.mayo.kmdp.language.validators.dmn.v1_2;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Well_Formedness_Check_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.asEnum;

import edu.mayo.kmdp.language.validators.AbstractValidator;
import edu.mayo.kmdp.util.XMLUtil;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
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
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeresourceoutcome.KnowledgeResourceOutcome;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeresourceoutcome.KnowledgeResourceOutcomeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.omg.spec.dmn._20180521.model.TDefinitions;
import org.w3c.dom.Document;

@Named
@KPOperation(Well_Formedness_Check_Task)
@KPSupport(DMN_1_2)
public class DMN12Validator extends AbstractValidator {

  public static final UUID id = UUID.fromString("1a43a134-0cb9-4ac5-a468-241744f89fcb");
  public static final String version = "1.0.0";

  private ResourceIdentifier operatorId;

  public DMN12Validator() {
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
    Schema dmn12Schema = checkSchema(sourceArtifact);

    boolean outcome = false;
    switch (asEnum(sourceArtifact.getLevel())) {
      case Abstract_Knowledge_Expression:
        outcome = validateAST(sourceArtifact.getExpression(), dmn12Schema);
        break;
      case Concrete_Knowledge_Expression:
        outcome = validateTree(sourceArtifact.getExpression(), dmn12Schema);
        break;
      case Serialized_Knowledge_Expression:
        outcome = sourceArtifact.asString()
            .map(str -> validateString(str, dmn12Schema))
            .orElse(false);
        break;
      case Encoded_Knowledge_Expression:
        outcome = sourceArtifact.asBinary()
            .map(bytes -> validateBinary(bytes, dmn12Schema))
            .orElse(false);
        break;
    }
    return outcome ? Answer.succeed() : Answer.failed();
  }

  private boolean validateBinary(byte[] bytes, Schema schema) {
    return XMLUtil.validate(
        new StreamSource(new ByteArrayInputStream(bytes)),
        schema);
  }

  private boolean validateString(String expr, Schema schema) {
    return XMLUtil.validate(expr, schema);
  }

  private boolean validateTree(Object tree, Schema schema) {
    return tree instanceof Document
        && XMLUtil.validate((Document) tree, schema);
  }

  private boolean validateAST(Object ast, Schema dmn12Schema) {
    return ast instanceof TDefinitions;
  }

  private Schema checkSchema(KnowledgeCarrier sourceArtifact) {
    if (!DMN_1_2.sameAs(sourceArtifact.getRepresentation().getLanguage())) {
      throw new UnsupportedOperationException("DMN12 Validator unable to validate "
          + sourceArtifact.getRepresentation().getLanguage());
    }
    return XMLUtil.getSchemas(DMN_1_2.getReferentId())
        .orElseThrow(
            () -> new UnsupportedOperationException("Unable to retrieve schema for DMN12"));
  }

  @Override
  public List<SyntacticRepresentation> getFrom() {
    return Arrays.asList(
        rep(DMN_1_2),
        rep(DMN_1_2, XML_1_1),
        rep(DMN_1_2, XML_1_1, Charset.defaultCharset()),
        rep(DMN_1_2, XML_1_1, Charset.defaultCharset(), Encodings.DEFAULT)
    );
  }

  @Override
  public ResourceIdentifier getOperatorId() {
    return operatorId;
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return DMN_1_2;
  }

}
