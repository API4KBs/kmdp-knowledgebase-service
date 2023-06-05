package edu.mayo.kmdp.language.translators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.SCG;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;

import edu.mayo.kmdp.language.parsers.scg.SCGParSerializer;
import edu.mayo.kmdp.language.translators.scg.owl2.SCGtoOWLTranslator;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.semanticweb.owlapi.formats.ManchesterSyntaxDocumentFormat;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.snomed.languages.scg.domain.model.SCGExpression;

class SCGtoOWLTranslatorTest {

  @Test
  void testParse() {
    String scg = "397956004 |prosthetic arthroplasty of the hip| : "
        + "363704007 |procedure site| = ( 24136001 |hip joint structure| : "
        + "272741003 |laterality| = 7771000 |left| ), "
        + "{ 363699004 |direct device| = 304120007 |total hip replacement prosthesis|, "
        + "260686004 |method| = 257867005 |insertion - action| }";

    var kc = AbstractCarrier.of(scg)
        .withRepresentation(rep(SCG, TXT, Charset.defaultCharset()));
    var ac = new SCGParSerializer().applyLift(kc, Abstract_Knowledge_Expression, codedRep(SCG),
            null)
        .orElseGet(Assertions::fail);
    assertTrue(ac.is(SCGExpression.class));
  }


  @Test
  void testTranslate() {
    String scg = "397956004 |prosthetic arthroplasty of the hip| : "
        + "363704007 |procedure site| = ( 24136001 |hip joint structure| : "
        + "272741003 |laterality| = 7771000 |left| ), "
        + "{ 363699004 |direct device| = 304120007 |total hip replacement prosthesis|, "
        + "260686004 |method| = 257867005 |insertion - action| }";

    var owl = Answer.of(AbstractCarrier.of(scg)
            .withRepresentation(rep(SCG, TXT, Charset.defaultCharset())))
        .flatMap(
            kc -> new SCGParSerializer().applyLift(kc, Abstract_Knowledge_Expression, codedRep(SCG),
                null))
        .flatMap(kc -> new SCGtoOWLTranslator().applyTransrepresent(kc, codedRep(OWL_2), null))
        .flatOpt(kc -> kc.as(OWLOntology.class))
        .orElseGet(Assertions::fail);

    var def = SCGtoOWLTranslator.tryGetPostCoordinated(owl)
        .orElseGet(Assertions::fail);
    assertTrue(def instanceof OWLObjectIntersectionOf);
    var and = (OWLObjectIntersectionOf) def;
    assertEquals(3, and.operands().count());

  }


  @Test
  void testTranslateSimple() throws OWLOntologyStorageException {
    String scg = "397956004 |prosthetic arthroplasty of the hip|";

    var owl = Answer.of(AbstractCarrier.of(scg)
            .withRepresentation(rep(SCG, TXT, Charset.defaultCharset())))
        .flatMap(
            kc -> new SCGParSerializer().applyLift(kc, Abstract_Knowledge_Expression, codedRep(SCG),
                null))
        .flatMap(kc -> new SCGtoOWLTranslator().applyTransrepresent(kc, codedRep(OWL_2), null))
        .flatOpt(kc -> kc.as(OWLOntology.class))
        .orElseGet(Assertions::fail);

    owl.saveOntology(new ManchesterSyntaxDocumentFormat(), System.out);

    SCGtoOWLTranslator.tryGetPreCoordinated(owl)
        .orElseGet(Assertions::fail);
  }
}
