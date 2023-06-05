package edu.mayo.kmdp.language.translators.surrogate.v2;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.contrastors.ParsingLevelContrastor.detectLevel;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Syntactic_Translation_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;

import edu.mayo.kmdp.language.parsers.surrogate.v2.Surrogate2Parser;
import edu.mayo.kmdp.language.translators.AbstractSimpleTranslator;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import javax.inject.Named;
import org.jsoup.nodes.Document;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
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
public class SurrogateV2toHTMLTranslator extends
    AbstractSimpleTranslator<org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset, Document> {

  public static final UUID id = UUID.fromString("fa5eae55-3548-4de6-97d2-8aedac6bfe72");
  public static final String version = "1.0.0";

  private Surrogate2Parser parser = new Surrogate2Parser();

  public SurrogateV2toHTMLTranslator() {
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
    return Arrays.asList(
        rep(HTML),
        rep(HTML, TXT),
        rep(HTML, TXT, Charset.defaultCharset()),
        rep(HTML, TXT, Charset.defaultCharset(), Encodings.DEFAULT)
    );
  }

  @Override
  protected Optional<Document> transformAst(ResourceIdentifier assetId,
      ResourceIdentifier srcArtifactId, KnowledgeAsset expression,
      SyntacticRepresentation srcRep,
      SyntacticRepresentation tgtRep,
      Properties config) {
    return Optional.ofNullable(new SurrogateV2ToHTML().transform(expression, config));
  }

  @Override
  protected Answer<_applyLift> getParser() {
    return Answer.of(parser);
  }

  @Override
  protected Answer<_applyLower> getTargetParser() {
    return Answer.of(
        (sourceArtifact, levelTag, xAccept, xParams) ->
            sourceArtifact
                .as(Document.class)
                .map(Document::outerHtml)
                .map(str -> AbstractCarrier.of(str, rep(HTML, TXT)))
                .map(Answer::of)
                .orElse(Answer.failed()));
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return Knowledge_Asset_Surrogate_2_0;
  }

  @Override
  public KnowledgeRepresentationLanguage getTargetLanguage() {
    return HTML;
  }
}
