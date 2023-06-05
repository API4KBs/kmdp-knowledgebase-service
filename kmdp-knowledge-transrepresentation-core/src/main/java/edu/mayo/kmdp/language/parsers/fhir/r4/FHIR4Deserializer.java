package edu.mayo.kmdp.language.parsers.fhir.r4;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Lifting_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Lowering_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.asEnum;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_R4;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Encoded_Knowledge_Expression;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Serialized_Knowledge_Expression;

import ca.uhn.fhir.context.FhirContext;
import edu.mayo.kmdp.language.parsers.AbstractDeSerializeOperator;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import javax.inject.Named;
import org.hl7.fhir.r4.model.Resource;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormat;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;

@Named
@KPOperation(Lifting_Task)
@KPOperation(Lowering_Task)
@KPSupport(FHIR_R4)
public class FHIR4Deserializer extends AbstractDeSerializeOperator {

  public static final UUID id = UUID.fromString("eaf1e350-0fbe-4078-8da2-246d2a780ec1");
  public static final String version = "1.0.0";

  static final FhirContext fhirContext = FhirContext.forR4();

  public FHIR4Deserializer() {
    setId(SemanticIdentifier.newId(id, version));
  }

  @Override
  protected List<SyntacticRepresentation> getSupportedRepresentations() {
    return Arrays.asList(
        new SyntacticRepresentation().withLanguage(FHIR_R4).withFormat(XML_1_1),
        new SyntacticRepresentation().withLanguage(FHIR_R4).withFormat(JSON)
    );
  }

  @Override
  protected SerializationFormat getDefaultFormat() {
    return JSON;
  }

  @Override
  public Optional<KnowledgeCarrier> innerDeserialize(KnowledgeCarrier carrier,
      Properties properties) {
    return Optional.empty();
  }

  @Override
  public Optional<KnowledgeCarrier> innerParse(KnowledgeCarrier carrier,
      Properties properties) {
    switch (asEnum(carrier.getRepresentation().getFormat())) {
      case JSON:
        return carrier.asString()
            .map(fhirContext.newJsonParser()::parseResource)
            .map(ast ->
                newVerticalCarrier(carrier, Abstract_Knowledge_Expression, rep(FHIR_R4), ast));
      case XML_1_1:
        return carrier.asString()
            .map(fhirContext.newXmlParser()::parseResource)
            .map(ast ->
                newVerticalCarrier(carrier, Abstract_Knowledge_Expression, rep(FHIR_R4), ast));
      default:
        return Optional.empty();
    }
  }

  @Override
  public Optional<KnowledgeCarrier> innerAbstract(KnowledgeCarrier carrier,
      Properties properties) {
    return Optional.empty();
  }


  @Override
  public Optional<KnowledgeCarrier> innerEncode(KnowledgeCarrier carrier,
      SyntacticRepresentation into, Properties properties) {
    return carrier.asBinary()
        .map(str -> newVerticalCarrier(carrier, Encoded_Knowledge_Expression, into, str));
  }

  @Override
  public Optional<KnowledgeCarrier> innerExternalize(KnowledgeCarrier carrier,
      SyntacticRepresentation into, Properties properties) {
    switch (asEnum(into.getFormat())) {
      case JSON:
        return carrier.as(Resource.class)
            .map(fhirContext.newJsonParser()::encodeResourceToString)
            .map(str -> newVerticalCarrier(carrier, Serialized_Knowledge_Expression, into, str));
      case XML_1_1:
        return carrier.as(Resource.class)
            .map(fhirContext.newXmlParser()::encodeResourceToString)
            .map(str -> newVerticalCarrier(carrier, Serialized_Knowledge_Expression, into, str));
      default:
        return Optional.empty();
    }
  }

  @Override
  public Optional<KnowledgeCarrier> innerSerialize(KnowledgeCarrier carrier,
      SyntacticRepresentation into, Properties properties) {
    return Optional.empty();
  }

  @Override
  public Optional<KnowledgeCarrier> innerConcretize(KnowledgeCarrier carrier,
      SyntacticRepresentation into, Properties properties) {
    return Optional.empty();
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return FHIR_R4;
  }
}
