package edu.mayo.kmdp.language.translators.surrogate.v2;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder.encode;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Syntactic_Transcription_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;

import edu.mayo.kmdp.language.TransionApiOperator;
import edu.mayo.kmdp.language.parsers.surrogate.v2.Surrogate2Parser;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.inject.Named;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.TransxionApiInternal._applyNamedTransrepresent;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.TransxionApiInternal._applyTransrepresent;
import org.omg.spec.api4kp._20200801.contrastors.ParsingLevelContrastor;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;

@Named
@KPOperation(Syntactic_Transcription_Task)
@KPSupport(Knowledge_Asset_Surrogate_2_0)
public class SurrogateV2Transcriptor implements TransionApiOperator,
    _applyTransrepresent,
    _applyNamedTransrepresent {

  public static final UUID id = UUID.fromString("d874d371-4e35-4c90-ac83-82a7a084cef2");
  public static final String version = "1.0.0";

  private Surrogate2Parser parser = new Surrogate2Parser();

  protected ResourceIdentifier operatorId;

  protected void setId(ResourceIdentifier id) {
    this.operatorId = id;
  }

  public SurrogateV2Transcriptor() {
    setId(SemanticIdentifier.newId(id, version));
  }

  @Override
  public List<SyntacticRepresentation> getFrom() {
    return Arrays.asList(
        rep(Knowledge_Asset_Surrogate_2_0),
        rep(Knowledge_Asset_Surrogate_2_0, XML_1_1),
        rep(Knowledge_Asset_Surrogate_2_0, XML_1_1, Charset.defaultCharset()),
        rep(Knowledge_Asset_Surrogate_2_0, XML_1_1, Charset.defaultCharset(), Encodings.DEFAULT),
        rep(Knowledge_Asset_Surrogate_2_0, JSON),
        rep(Knowledge_Asset_Surrogate_2_0, JSON, Charset.defaultCharset()),
        rep(Knowledge_Asset_Surrogate_2_0, JSON, Charset.defaultCharset(), Encodings.DEFAULT)
    );
  }

  @Override
  public List<SyntacticRepresentation> getInto() {
    return getFrom();
  }


  @Override
  public ResourceIdentifier getOperatorId() {
    return operatorId;
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return Knowledge_Asset_Surrogate_2_0;
  }

  @Override
  public KnowledgeRepresentationLanguage getTargetLanguage() {
    return Knowledge_Asset_Surrogate_2_0;
  }

  @Override
  public Answer<KnowledgeCarrier> applyTransrepresent(KnowledgeCarrier sourceArtifact,
      String xAccept, String xParams) {
    return Answer.of(ModelMIMECoder.decode(xAccept))
        .flatMap(tgtRep ->
            parser.applyLift(
                sourceArtifact, Abstract_Knowledge_Expression,
                encode(rep(Knowledge_Asset_Surrogate_2_0)), null)
                .flatMap(ast -> parser
                    .applyLower(ast, ParsingLevelContrastor.detectLevel(tgtRep), xAccept, null)
                ));
  }

  @Override
  public Answer<KnowledgeCarrier> applyNamedTransrepresent(UUID operatorId,
      KnowledgeCarrier sourceArtifact, String xAccept, String xParams) {
    if (! operatorId.equals(getOperatorId().getUuid())) {
      return Answer.unsupported();
    }
    return applyTransrepresent(sourceArtifact,xAccept,xParams);
  }
}
