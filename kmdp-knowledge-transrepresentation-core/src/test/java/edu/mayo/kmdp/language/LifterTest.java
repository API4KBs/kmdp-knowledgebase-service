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
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries.CMMN_1_1_XML_Syntax;
import static org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries.DMN_1_1_XML_Syntax;
import static org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries.OWL_Manchester_Syntax;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Encoded_Knowledge_Expression;

import edu.mayo.kmdp.language.parsers.cmmn.v1_1.CMMN11Parser;
import edu.mayo.kmdp.language.parsers.dmn.v1_1.DMN11Parser;
import edu.mayo.kmdp.language.parsers.dmn.v1_2.DMN12Parser;
import edu.mayo.kmdp.language.parsers.owl2.OWLParser;
import edu.mayo.kmdp.language.parsers.surrogate.v2.Surrogate2Parser;
import java.util.List;
import javax.xml.bind.JAXBElement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormat;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerialization;
import org.omg.spec.dmn._20180521.model.TContext;
import org.semanticweb.owlapi.model.OWLOntology;

class LifterTest {

  static final String VER = "1.0.0";

  @Test
  void testParseDMN11() {
    testVerticalLift(
        new DMN11Parser(),
        "/dmn11example.dmn",
        DMN_1_1, DMN_1_1_XML_Syntax, XML_1_1,
        org.omg.spec.dmn._20151101.dmn.TDefinitions.class);
  }

  @Test
  void testParseDMN12() {
    testVerticalLift(
        new DMN12Parser(),
        "/dmn12example.dmn",
        DMN_1_2, DMN_1_1_XML_Syntax, XML_1_1,
        org.omg.spec.dmn._20180521.model.TDefinitions.class);
  }

  @Test
  void testParseCMMN() {
    testVerticalLift(
        new CMMN11Parser(),
        "/cmmn11Example.cmmn",
        CMMN_1_1, CMMN_1_1_XML_Syntax, XML_1_1,
        org.omg.spec.cmmn._20151109.model.TDefinitions.class);
  }


  @Test
  void testParseMixedBPM() {
    var kc = testVerticalLift(
        new CMMN11Parser(List.of(new DMN12Parser())),
        "/cmmn11withDmn12Example.cmmn",
        CMMN_1_1, CMMN_1_1_XML_Syntax, XML_1_1,
        org.omg.spec.cmmn._20151109.model.TDefinitions.class);

    var cmmn = kc.as(org.omg.spec.cmmn._20151109.model.TDefinitions.class)
        .orElseGet(Assertions::fail);

    var ext1 = cmmn.getCase().get(0).getExtensionElements().getAny().get(0);
    assertTrue(ext1 instanceof JAXBElement);
    assertTrue(((JAXBElement<?>) ext1).getValue() instanceof TContext);
  }

  @Test
  void testParseOWL() {
    testVerticalLift(
        new OWLParser(),
        "/owlExample.owl",
        OWL_2, OWL_Manchester_Syntax, TXT,
        OWLOntology.class);
  }

  @Test
  void testParseSurr2() {
    testVerticalLift(
        new Surrogate2Parser(),
        "/surr2.xml",
        Knowledge_Asset_Surrogate_2_0, null, XML_1_1,
        KnowledgeAsset.class);
  }

  private KnowledgeCarrier testVerticalLift(
      DeserializeApiOperator parser,
      String sourcePath,
      KnowledgeRepresentationLanguage language,
      KnowledgeRepresentationLanguageSerialization ser,
      SerializationFormat fmt,
      Class<?> astRootClass) {

    ResourceIdentifier assetId = randomAssetId();

    KnowledgeCarrier carrier =
        AbstractCarrier.of(LifterTest.class.getResourceAsStream(sourcePath))
            .withRepresentation(rep(language, ser, fmt))
            .withAssetId(assetId);

    assertNotNull(carrier.getAssetId());
    assertTrue(Encoded_Knowledge_Expression.sameAs(carrier.getLevel()));
    assertEquals(language, carrier.getRepresentation().getLanguage());

    Answer<KnowledgeCarrier> parsed =
        parser.as_applyLift()
            .orElseGet(Assertions::fail)
            .applyLift(carrier, Abstract_Knowledge_Expression.getTag(), null, null);

    assertTrue(parsed.isSuccess());
    KnowledgeCarrier knowledgeCarrier = parsed.get();
    assertEquals(carrier.getAssetId(), knowledgeCarrier.getAssetId());
    assertTrue(Abstract_Knowledge_Expression.sameAs(knowledgeCarrier.getLevel()));

    assertTrue(knowledgeCarrier.is(astRootClass));

    return knowledgeCarrier;
  }


}
