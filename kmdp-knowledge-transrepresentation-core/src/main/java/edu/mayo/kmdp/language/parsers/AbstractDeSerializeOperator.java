/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.mayo.kmdp.language.parsers;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.contrastors.ParsingLevelContrastor.detectLevel;
import static org.omg.spec.api4kp._20200801.contrastors.ParsingLevelContrastor.theLevelContrastor;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Concrete_Knowledge_Expression;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Encoded_Knowledge_Expression;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Serialized_Knowledge_Expression;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.asEnum;

import edu.mayo.kmdp.language.DeserializeApiOperator;
import edu.mayo.kmdp.util.PropertiesUtil;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.BiPredicate;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder;
import org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormat;
import org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevel;

/**
 * Abstract class that implements a generic parser/serializer for a Knowledge Representation
 * Language and its serializations
 */
public abstract class AbstractDeSerializeOperator
    implements DeserializeApiOperator, Lifter, Lowerer {

  private static final Charset CHARSET = Charset.defaultCharset();

  protected ResourceIdentifier operatorId;

  public ResourceIdentifier getOperatorId() {
    return operatorId;
  }

  protected void setId(ResourceIdentifier id) {
    this.operatorId = id;
  }

  @Override
  public List<SyntacticRepresentation> getFrom() {
    return getSupportedRepresentations();
  }

  @Override
  public List<SyntacticRepresentation> getInto() {
    return getSupportedRepresentations();
  }

  @Override
  public Answer<KnowledgeCarrier> applyLift(KnowledgeCarrier knowledgeCarrier,
      ParsingLevel parsingLevel, String into, String properties) {
    try {
      return Answer.ofTry(
          lift(knowledgeCarrier,
              parsingLevel,
              ModelMIMECoder.decode(into)
                  .orElse(inferRepresentationForLevel(getSupportedRepresentations(), parsingLevel)),
              PropertiesUtil.parseProperties(properties)
          ));
    } catch (UnsupportedOperationException e) {
      return Answer.failed(e);
    }
  }

  @Override
  public Answer<KnowledgeCarrier> applyLower(KnowledgeCarrier knowledgeCarrier,
      ParsingLevel parsingLevel, String into, String properties) {
    try {
      return Answer.ofTry(
          lower(knowledgeCarrier, parsingLevel,
              ModelMIMECoder.decode(into)
                  .orElse(inferRepresentationForLevel(getSupportedRepresentations(), parsingLevel)),
              PropertiesUtil.parseProperties(properties)));
    } catch (UnsupportedOperationException e) {
      return Answer.failed(e);
    }
  }


  public Optional<KnowledgeCarrier> lift(KnowledgeCarrier sourceArtifact,
      ParsingLevel into, SyntacticRepresentation targetRepresentation, Properties config) {
    checkLiftConsistency(sourceArtifact, into, targetRepresentation);

    switch (asEnum(sourceArtifact.getLevel())) {
      case Encoded_Knowledge_Expression:
        switch (asEnum(into)) {
          case Abstract_Knowledge_Expression:
            return this.innerDecode(sourceArtifact, config)
                .flatMap(str -> innerParse(str, config));
          case Concrete_Knowledge_Expression:
            return this.innerDecode(sourceArtifact, config)
                .flatMap(str -> innerDeserialize(str, config));
          case Serialized_Knowledge_Expression:
            return this.innerDecode(sourceArtifact, config);
          case Encoded_Knowledge_Expression:
            return Optional.of(sourceArtifact);
          default:
            return Optional.empty();
        }
      case Serialized_Knowledge_Expression:
        switch (asEnum(into)) {
          case Abstract_Knowledge_Expression:
            return this.innerParse(sourceArtifact, config);
          case Concrete_Knowledge_Expression:
            return this.innerDeserialize(sourceArtifact, config);
          case Serialized_Knowledge_Expression:
            return Optional.of(sourceArtifact);
          default:
            return Optional.empty();
        }
      case Concrete_Knowledge_Expression:
        switch (asEnum(into)) {
          case Abstract_Knowledge_Expression:
            return this.innerAbstract(sourceArtifact, config);
          case Concrete_Knowledge_Expression:
            return Optional.of(sourceArtifact);
          default:
            return Optional.empty();
        }
      case Abstract_Knowledge_Expression:
        return Optional.of(sourceArtifact);
      default:
        return Optional.empty();
    }
  }


  protected Optional<KnowledgeCarrier> lower(KnowledgeCarrier sourceArtifact,
      ParsingLevel toLevel, SyntacticRepresentation into, Properties config) {
    checkLowerConsistency(sourceArtifact, toLevel, into);

    switch (asEnum(sourceArtifact.getLevel())) {
      case Abstract_Knowledge_Expression:
        return serializeAST(sourceArtifact, toLevel, into, config);
      case Concrete_Knowledge_Expression:
        return serializeDoc(sourceArtifact, toLevel, into, config);
      case Serialized_Knowledge_Expression:
        return serializeExpression(sourceArtifact, toLevel, into, config);
      case Encoded_Knowledge_Expression:
      default:
        return Optional.of(sourceArtifact);
    }

  }


  protected void checkLiftConsistency(KnowledgeCarrier sourceArtifact, ParsingLevel into,
      SyntacticRepresentation targetRepresentation) {
    checkConsistency(sourceArtifact, into, targetRepresentation,
        theLevelContrastor::isNarrowerOrEqual);
  }

  protected void checkLowerConsistency(KnowledgeCarrier sourceArtifact, ParsingLevel into,
      SyntacticRepresentation targetRepresentation) {

    checkConsistency(sourceArtifact, into, targetRepresentation,
        theLevelContrastor::isBroaderOrEqual);

    if (targetRepresentation.getFormat() == null &&
        theLevelContrastor.isNarrowerOrEqual(into, Concrete_Knowledge_Expression)) {
      targetRepresentation.setFormat(getDefaultFormat());
    }
    if (targetRepresentation.getCharset() == null &&
        theLevelContrastor.isNarrowerOrEqual(into, Serialized_Knowledge_Expression)) {
      targetRepresentation.setCharset(Charset.defaultCharset().name());
    }
    if (targetRepresentation.getEncoding() == null && into.sameAs(Encoded_Knowledge_Expression)) {
      targetRepresentation.setEncoding(Encodings.DEFAULT.name());
    }
  }

  protected void checkConsistency(KnowledgeCarrier sourceArtifact, ParsingLevel into,
      SyntacticRepresentation targetRepresentation,
      BiPredicate<ParsingLevel, ParsingLevel> levelTest) {

    if (!getSupportedLanguage().sameAs(sourceArtifact.getRepresentation().getLanguage())) {
      throw new UnsupportedOperationException(
          "Parser Cannot handle language : " + sourceArtifact.getRepresentation().getLanguage());
    }

    if (targetRepresentation != null && !targetRepresentation.getLanguage()
        .sameAs(sourceArtifact.getRepresentation().getLanguage())) {
      throw new UnsupportedOperationException(
          "Parser Cannot handle artifact in: " + sourceArtifact.getRepresentation().getLanguage());
    }

    ParsingLevel sourceLevel = detectLevel(sourceArtifact);
    if (!levelTest.test(sourceLevel, into)) {
      // parsing must lift to a higher level <=> sourceLevel must be lower
      throw new UnsupportedOperationException(
          "Cannot lift/lower between : " + sourceArtifact.getLevel() + " and " + into);
    }

  }

  @Override
  public Optional<KnowledgeCarrier> innerDecode(KnowledgeCarrier carrier, Properties config) {
    // This probably needs to be generalized based on the actual encoding type
    Object expr = carrier.getExpression();
    String serializedExpr;
    if (expr instanceof String) {
      expr = Base64.getDecoder().decode((String) expr);
    }
    if (expr instanceof byte[]) {
      byte[] bytes = (byte[]) expr;
      if (bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
        bytes = Arrays.copyOfRange(bytes, 3, bytes.length);
      }
      serializedExpr = new String(bytes);
    } else {
      serializedExpr = null;
    }
    SyntacticRepresentation tgtRep =
        ((SyntacticRepresentation) carrier.getRepresentation().clone()).withEncoding(null);
    return Optional.ofNullable(serializedExpr)
        .map(s -> newVerticalCarrier(carrier, Serialized_Knowledge_Expression, tgtRep, s));
  }


  private Optional<KnowledgeCarrier> serializeExpression(KnowledgeCarrier expr,
      ParsingLevel toLevel,
      SyntacticRepresentation into,
      Properties config) {
    switch (asEnum(toLevel)) {
      case Serialized_Knowledge_Expression:
        return Optional.ofNullable(expr);
      case Encoded_Knowledge_Expression:
      default:
        return this.innerEncode(expr, into, config);
    }
  }

  private Optional<KnowledgeCarrier> serializeDoc(KnowledgeCarrier doc, ParsingLevel toLevel,
      SyntacticRepresentation into, Properties config) {
    switch (asEnum(toLevel)) {
      case Concrete_Knowledge_Expression:
        return Optional.ofNullable(doc);
      case Serialized_Knowledge_Expression:
        return this.innerSerialize(doc, into, config);
      case Encoded_Knowledge_Expression:
      default:
        return this.innerSerialize(doc, into, config)
            .flatMap(str -> innerEncode(str, config));
    }
  }

  protected Optional<KnowledgeCarrier> serializeAST(KnowledgeCarrier ast, ParsingLevel toLevel,
      SyntacticRepresentation into, Properties config) {
    switch (asEnum(toLevel)) {
      case Abstract_Knowledge_Expression:
        return Optional.ofNullable(ast);
      case Concrete_Knowledge_Expression:
        return this.innerConcretize(ast, into, config);
      case Serialized_Knowledge_Expression:
        return this.innerExternalize(ast, into, config);
      case Encoded_Knowledge_Expression:
      default:
        return this.innerExternalize(ast, into, config)
            .flatMap(str -> innerEncode(str, into, config));
    }
  }


  protected SyntacticRepresentation getParseResultRepresentation(
      KnowledgeCarrier sourceArtifact,
      ParsingLevel into) {
    SyntacticRepresentation rep = (SyntacticRepresentation) sourceArtifact.getRepresentation()
        .clone();
    switch (asEnum(into)) {
      case Abstract_Knowledge_Expression:
        rep.setFormat(null);
        rep.setSerialization(null);
        rep.setCharset(null);
        rep.setEncoding(null);
        break;
      case Concrete_Knowledge_Expression:
        rep.setCharset(null);
        rep.setEncoding(null);
        rep.setSerialization(null);
        break;
      case Serialized_Knowledge_Expression:
        rep.setEncoding(null);
        break;
      case Encoded_Knowledge_Expression:
      default:
        break;
    }
    return rep;
  }

  protected SyntacticRepresentation getSerializeResultRepresentation(
      KnowledgeCarrier sourceArtifact,
      ParsingLevel into) {
    SyntacticRepresentation rep = (SyntacticRepresentation) sourceArtifact.getRepresentation()
        .clone();
    switch (asEnum(into)) {
      case Serialized_Knowledge_Expression:
        rep.setEncoding(null);
        if (rep.getFormat() == null) {
          rep.setFormat(getDefaultFormat());
        }
        if (rep.getCharset() == null) {
          rep.setCharset(getDefaultCharset().name());
        }
        break;
      case Concrete_Knowledge_Expression:
        rep.setCharset(null);
        rep.setEncoding(null);
        rep.setSerialization(null);
        if (rep.getFormat() == null) {
          rep.setFormat(getDefaultFormat());
        }
        break;
      case Abstract_Knowledge_Expression:
        rep.setFormat(null);
        rep.setCharset(null);
        rep.setEncoding(null);
        rep.setSerialization(null);
        break;
      default:
        break;
    }
    return rep;
  }

  protected abstract List<SyntacticRepresentation> getSupportedRepresentations();


  protected Charset getDefaultCharset() {
    return CHARSET;
  }

  protected abstract SerializationFormat getDefaultFormat();

  @Override
  public Optional<SyntacticRepresentation> inferRepresentationForLevel(ParsingLevel targetLevel) {
    return Optional
        .ofNullable(inferRepresentationForLevel(getSupportedRepresentations(), targetLevel));
  }

  private SyntacticRepresentation inferRepresentationForLevel(
      List<SyntacticRepresentation> supportedRepresentations, ParsingLevel parsingLevel) {
    Optional<SyntacticRepresentation> tgtRep = supportedRepresentations.stream()
        .filter(rep -> detectLevel(rep).sameAs(parsingLevel))
        .findFirst();
    if (tgtRep.isPresent()) {
      return tgtRep.get();
    }
    switch (asEnum(parsingLevel)) {
      case Encoded_Knowledge_Expression:
        return rep(getSupportedLanguage(), getDefaultFormat(), Charset.defaultCharset(),
            Encodings.DEFAULT);
      case Serialized_Knowledge_Expression:
        return rep(getSupportedLanguage(), getDefaultFormat(), Charset.defaultCharset());
      case Concrete_Knowledge_Expression:
        return rep(getSupportedLanguage(), getDefaultFormat());
      case Abstract_Knowledge_Expression:
        return rep(getSupportedLanguage());
      default:
        throw new IllegalArgumentException("Unable to provide a SyntacticRepresentation "
            + "for level " + parsingLevel.getName());
    }
  }


  @Override
  public boolean can_applyLift() {
    return true;
  }

  @Override
  public boolean can_applyLower() {
    return true;
  }

  @Override
  public boolean can_applyNamedLift() {
    return true;
  }

  @Override
  public boolean can_applyNamedLower() {
    return true;
  }

}
