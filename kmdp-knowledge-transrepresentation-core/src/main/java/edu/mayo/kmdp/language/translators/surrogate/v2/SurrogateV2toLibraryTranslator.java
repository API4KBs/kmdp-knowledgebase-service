package edu.mayo.kmdp.language.translators.surrogate.v2;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Syntactic_Translation_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;

import edu.mayo.kmdp.language.parsers.fhir.stu3.FHIR3Deserializer;
import edu.mayo.kmdp.language.parsers.surrogate.v2.Surrogate2Parser;
import edu.mayo.kmdp.language.translators.AbstractSimpleTranslator;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import javax.inject.Named;
import org.hl7.fhir.dstu3.model.Library;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal._applyLift;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal._applyLower;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;

@Named
@KPOperation(Syntactic_Translation_Task)
@KPSupport(Knowledge_Asset_Surrogate_2_0)
public class SurrogateV2toLibraryTranslator extends
    AbstractSimpleTranslator<org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset, Library> {

  public static final UUID id = UUID.fromString("e526d596-50d7-47b4-b435-879f6a31e622");
  public static final String version = "1.0.0";

  public SurrogateV2toLibraryTranslator() {
    setId(SemanticIdentifier.newId(id, version));
  }

  @Override
  public List<SyntacticRepresentation> getFrom() {
    return Arrays.asList(
        rep(Knowledge_Asset_Surrogate_2_0),
        rep(Knowledge_Asset_Surrogate_2_0, JSON),
        rep(Knowledge_Asset_Surrogate_2_0, JSON, Charset.defaultCharset()),
        rep(Knowledge_Asset_Surrogate_2_0, JSON, Charset.defaultCharset(), Encodings.DEFAULT));
  }

  @Override
  public List<SyntacticRepresentation> getInto() {
    return Arrays.asList(
        rep(FHIR_STU3),
        rep(FHIR_STU3, JSON),
        rep(FHIR_STU3, JSON, Charset.defaultCharset()),
        rep(FHIR_STU3, JSON, Charset.defaultCharset(), Encodings.DEFAULT));
  }

  @Override
  protected Answer<_applyLift> getParser() {
    return Answer.of(new Surrogate2Parser());
  }

  @Override
  protected Answer<_applyLower> getTargetParser() {
    return Answer.of(new FHIR3Deserializer());
  }



  @Override
  protected Optional<Library> transformAst(ResourceIdentifier assetId,
      ResourceIdentifier srcArtifactId, KnowledgeAsset expression,
      SyntacticRepresentation srcRep,
      SyntacticRepresentation tgtRep,
      Properties config) {
    return Optional.ofNullable(new SurrogateV2ToLibrary().transform(expression));
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return Knowledge_Asset_Surrogate_2_0;
  }

  @Override
  public KnowledgeRepresentationLanguage getTargetLanguage() {
    return FHIR_STU3;
  }
}
