package edu.mayo.kmdp.language;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.randomAssetId;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.CMMN_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.SPARQL_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries.CMMN_1_1_XML_Syntax;
import static org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries.DMN_1_1_XML_Syntax;
import static org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries.OWL_Manchester_Syntax;
import static org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries.RDF_XML_Syntax;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Concrete_Knowledge_Expression;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Encoded_Knowledge_Expression;

import edu.mayo.kmdp.language.parsers.cmmn.v1_1.CMMN11Parser;
import edu.mayo.kmdp.language.parsers.dmn.v1_1.DMN11Parser;
import edu.mayo.kmdp.language.parsers.dmn.v1_2.DMN12Parser;
import edu.mayo.kmdp.language.parsers.owl2.JenaOwlParser;
import edu.mayo.kmdp.language.parsers.owl2.OWLParser;
import edu.mayo.kmdp.language.parsers.sparql.SparqlLifter;
import java.nio.charset.Charset;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormat;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerialization;
import org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevel;
import org.semanticweb.owlapi.model.OWLOntology;

class ParserTest {

  static final String VER = "1.0.0";

  @Test
  void testParseDMN11() {
    testVerticalLift(
        new DMN11Parser(),
        "/dmn11example.dmn",
        DMN_1_1, DMN_1_1_XML_Syntax, XML_1_1,
        org.omg.spec.dmn._20151101.dmn.TDefinitions.class,
        Abstract_Knowledge_Expression);
  }

  @Test
  void testParseDMN12() {
    testVerticalLift(
        new DMN12Parser(),
        "/dmn12example.dmn",
        DMN_1_2, DMN_1_1_XML_Syntax, XML_1_1,
        org.omg.spec.dmn._20180521.model.TDefinitions.class,
        Abstract_Knowledge_Expression);
  }

  @Test
  void testParseCMMN() {
    testVerticalLift(
        new CMMN11Parser(),
        "/cmmn11Example.cmmn",
        CMMN_1_1, CMMN_1_1_XML_Syntax, XML_1_1,
        org.omg.spec.cmmn._20151109.model.TDefinitions.class,
        Abstract_Knowledge_Expression);
  }

  @Test
  void testParseOWL() {
    testVerticalLift(
        new OWLParser(),
        "/owlExample.owl",
        OWL_2, OWL_Manchester_Syntax, TXT,
        OWLOntology.class,
        Abstract_Knowledge_Expression);
  }

  @Test
  void testParseOWLWithJena() {
    testVerticalLift(
        new JenaOwlParser(),
        "/owlExample.rdf",
        OWL_2, RDF_XML_Syntax, XML_1_1,
        Model.class,
        Concrete_Knowledge_Expression);
  }

  @Test
  void testParseSparql() {
    testVerticalLift(
        new SparqlLifter(),
        "/sparqlTest.sparql",
        SPARQL_1_1, null, TXT,
        Query.class,
        Abstract_Knowledge_Expression);
  }

  private void testVerticalLift(
      DeserializeApiInternal parser,
      String sourcePath,
      KnowledgeRepresentationLanguage language,
      KnowledgeRepresentationLanguageSerialization ser,
      SerializationFormat fmt,
      Class<?> astRootClass,
      ParsingLevel level) {
    testVerticalLift(parser::applyLift,
        sourcePath,
        language,
        ser,
        fmt,
        astRootClass,
        level);
  }

  private void testVerticalLift(
      DeserializeApiInternal._applyLift parser,
      String sourcePath,
      KnowledgeRepresentationLanguage language,
      KnowledgeRepresentationLanguageSerialization ser,
      SerializationFormat fmt,
      Class<?> astRootClass,
      ParsingLevel level) {

    ResourceIdentifier assetId = randomAssetId();

    KnowledgeCarrier carrier =
        AbstractCarrier.of(ParserTest.class.getResourceAsStream(sourcePath))
            .withRepresentation(rep(language, ser, fmt, Charset.defaultCharset(), Encodings.DEFAULT))
            .withAssetId(assetId);

    assertNotNull(carrier.getAssetId());
    assertTrue(Encoded_Knowledge_Expression.sameAs(carrier.getLevel()));
    assertEquals(language, carrier.getRepresentation().getLanguage());

    Answer<KnowledgeCarrier> parsed =
        parser.applyLift(carrier, level.getTag(), null, null);

    assertTrue(parsed.isSuccess());
    KnowledgeCarrier knowledgeCarrier = parsed.get();
    assertEquals(carrier.getAssetId(), knowledgeCarrier.getAssetId());
    assertTrue(level.sameAs(knowledgeCarrier.getLevel()));

    if (level.sameAs(Abstract_Knowledge_Expression)) {
      assertTrue(knowledgeCarrier.as(astRootClass).isPresent());
    } else if (level.sameAs(Encoded_Knowledge_Expression)) {
      assertTrue(astRootClass.isAssignableFrom(carrier.getExpression().getClass()));
    }
  }


}
