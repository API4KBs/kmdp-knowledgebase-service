package edu.mayo.kmdp.language.parsers.scg;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.SCG;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Concrete_Knowledge_Expression;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Encoded_Knowledge_Expression;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Serialized_Knowledge_Expression;

import edu.mayo.kmdp.language.parsers.AbstractDeSerializeOperator;
import grammar.SCGParser.ExpressionContext;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormat;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.snomed.languages.scg.SCGExpressionParser;
import org.snomed.languages.scg.domain.model.SCGExpression;

public class SCGParSerializer extends AbstractDeSerializeOperator {

  public static final UUID id = UUID.fromString("eb8664ee-e809-4910-8c61-afb415386b86");
  public static final String version = "1.0.0";

  public SCGParSerializer() {
    setId(SemanticIdentifier.newId(id, version));
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return SCG;
  }

  @Override
  protected List<SyntacticRepresentation> getSupportedRepresentations() {
    return Arrays.asList(
        rep(SCG),
        rep(SCG, TXT),
        rep(SCG, TXT, Charset.defaultCharset()),
        rep(SCG, TXT, Charset.defaultCharset(), Encodings.DEFAULT)
    );
  }

  @Override
  protected SerializationFormat getDefaultFormat() {
    return TXT;
  }

  @Override
  public Optional<KnowledgeCarrier> innerDeserialize(KnowledgeCarrier carrier, Properties config) {
    return carrier.asString()
        .map(str -> new SCGExpressionParser().parseExpression(str))
        .map(xpr -> newVerticalCarrier(carrier, Concrete_Knowledge_Expression, rep(SCG, TXT), xpr));
  }

  @Override
  public Optional<KnowledgeCarrier> innerParse(KnowledgeCarrier carrier, Properties config) {
    return carrier.asString()
        .map(str -> new SCGExpressionParser().abstractExpression(str))
        .map(xpr -> newVerticalCarrier(carrier, Abstract_Knowledge_Expression, rep(SCG), xpr));
  }

  @Override
  public Optional<KnowledgeCarrier> innerAbstract(KnowledgeCarrier carrier, Properties config) {
    return carrier.as(ExpressionContext.class)
        .map(str -> new SCGExpressionParser().abstractExpression(str))
        .map(xpr -> newVerticalCarrier(carrier, Abstract_Knowledge_Expression, rep(SCG), xpr));
  }


  @Override
  public Optional<KnowledgeCarrier> innerEncode(KnowledgeCarrier carrier,
      SyntacticRepresentation into, Properties config) {
    return carrier.asBinary()
        .map(xpr -> newVerticalCarrier(carrier, Encoded_Knowledge_Expression,
            rep(SCG, TXT, Charset.defaultCharset(), Encodings.DEFAULT), xpr));
  }

  @Override
  public Optional<KnowledgeCarrier> innerExternalize(KnowledgeCarrier carrier,
      SyntacticRepresentation into, Properties config) {
    return carrier.as(SCGExpression.class)
        .map(SCGExpression::toFullString)
        .map(xpr -> newVerticalCarrier(carrier, Serialized_Knowledge_Expression,
            rep(SCG, TXT, Charset.defaultCharset()), xpr));
  }

  @Override
  public Optional<KnowledgeCarrier> innerSerialize(KnowledgeCarrier carrier,
      SyntacticRepresentation into, Properties config) {
    return innerAbstract(carrier, config)
        .flatMap(kc -> innerExternalize(kc, rep(SCG, TXT, Charset.defaultCharset()), config));
  }

  @Override
  public Optional<KnowledgeCarrier> innerConcretize(KnowledgeCarrier carrier,
      SyntacticRepresentation into, Properties config) {
    return innerSerialize(carrier, rep(SCG, TXT, Charset.defaultCharset()), config)
        .flatMap(kc -> innerDeserialize(kc, config));
  }
}
