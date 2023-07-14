package edu.mayo.kmdp.language.translators.misc;

import static edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries.OK;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;

import edu.mayo.kmdp.language.TransionApiOperator;
import edu.mayo.kmdp.util.JSonUtil;
import edu.mayo.kmdp.util.XMLUtil;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.TransxionApiInternal._applyTransrepresent;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder;
import org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;


public class FormatBeauitifier implements TransionApiOperator, _applyTransrepresent {

  public static final UUID OPERATOR_ID = UUID.fromString("03e354d4-3d48-454b-8a5a-b1d27695d9e4");
  public static final String VERSION = "1.0.0";
  private static final String CONTENT_TYPE = "Content-Type";

  private static final ResourceIdentifier operatorId = SemanticIdentifier.newId(OPERATOR_ID,
      VERSION);

  @Override
  public ResourceIdentifier getOperatorId() {
    return operatorId;
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return null;
  }

  @Override
  public List<SyntacticRepresentation> getFrom() {
    return List.of(
        rep(null, null, XML_1_1),
        rep(null, XML_1_1, StandardCharsets.UTF_8),
        rep(null, XML_1_1, StandardCharsets.UTF_8, Encodings.DEFAULT),
        rep(null, null, JSON),
        rep(null, JSON, StandardCharsets.UTF_8),
        rep(null, JSON, StandardCharsets.UTF_8, Encodings.DEFAULT)
    );
  }

  @Override
  public List<SyntacticRepresentation> getInto() {
    return List.of(
        rep(null, XML_1_1, StandardCharsets.UTF_8, Encodings.DEFAULT),
        rep(null, JSON, StandardCharsets.UTF_8, Encodings.DEFAULT)
    );
  }

  @Override
  public KnowledgeRepresentationLanguage getTargetLanguage() {
    return null;
  }

  @Override
  public Answer<KnowledgeCarrier> applyTransrepresent(KnowledgeCarrier source, String xAccept,
      String xParams) {
    if (canBeautify(source, ModelMIMECoder.decode(xAccept).orElse(null))) {
      switch (SerializationFormatSeries.asEnum(source.getRepresentation().getFormat())) {
        case JSON:
          return beautifyJson(source);
        case XML_1_1:
          return beautifyXml(source);
        case TXT:
          return beautifyTxt(source);
        default:
      }
    }
    return Answer.of(source);
  }

  private Answer<KnowledgeCarrier> beautifyTxt(KnowledgeCarrier source) {
    var isHtml = HTML.sameAs(source.getRepresentation().getLanguage());
    return Answer.ofTry(source.asString())
        .flatMap(x -> Answer.of(OK, x,
            Map.of(CONTENT_TYPE,
                isHtml ? List.of("text/html") : List.of("text/plain"))))
        .map(s -> s.getBytes(StandardCharsets.UTF_8))
        .map(b -> rewrap(b, source));
  }

  private Answer<KnowledgeCarrier> beautifyXml(KnowledgeCarrier source) {
    return Answer.ofTry(source.asString())
        .flatOpt(XMLUtil::loadXMLDocument)
        .map(XMLUtil::toString)
        .flatMap(x -> Answer.of(OK, x, Map.of(CONTENT_TYPE, List.of("application/xml"))))
        .map(s -> s.getBytes(StandardCharsets.UTF_8))
        .map(b -> rewrap(b, source));
  }

  private Answer<KnowledgeCarrier> beautifyJson(KnowledgeCarrier source) {
    return Answer.ofTry(source.asString())
        .flatOpt(JSonUtil::readJson)
        .map(JSonUtil::prettyPrintJsonString)
        .flatMap(x -> Answer.of(OK, x, Map.of(CONTENT_TYPE, List.of("application/json"))))
        .map(s -> s.getBytes(StandardCharsets.UTF_8))
        .map(b -> rewrap(b, source));
  }

  private KnowledgeCarrier rewrap(byte[] str, KnowledgeCarrier source) {
    return AbstractCarrier.of(str)
        .withRepresentation(source.getRepresentation()
            .withCharset(StandardCharsets.UTF_8.name())
            .withEncoding(Encodings.DEFAULT.name()))
        .withAssetId(source.getAssetId())
        .withArtifactId(source.getArtifactId())
        .withLabel(source.getLabel())
        .withHref(source.getHref());
  }

  private boolean canBeautify(KnowledgeCarrier source, SyntacticRepresentation tgtRep) {
    if (source.getRepresentation() == null
        || source.getRepresentation().getFormat() == null) {
      return false;
    }
    return tgtRep == null || tgtRep.getFormat() == null
        || source.getRepresentation().getFormat().sameAs(tgtRep.getFormat());
  }
}
