package edu.mayo.kmdp.language.validators.dmn.v1_2;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.Severity.ERR;
import static org.omg.spec.api4kp._20200801.Severity.FATAL;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeresourceoutcome.KnowledgeResourceOutcomeSeries.Style_Conformance;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;

import edu.mayo.kmdp.language.parsers.dmn.v1_2.DMN12Parser;
import edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.Severity;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries;
import org.zalando.problem.Problem;

class CPMProfileDMNValidatorTest {

  @Test
  void testComputableDecisionServiceWithDecisionInputs() {
    KnowledgeCarrier model =
        AbstractCarrier.of(loadModel("/compDecServiceWithDecisionInputs.dmn.xml"))
            .withLabel("Mock DMN model with Decision Services")
            .withAssetId(SemanticIdentifier.randomId())
            .withRepresentation(rep(DMN_1_2, XML_1_1, UTF_8, Encodings.DEFAULT));

    Answer<Void> ans = new DMN12Parser()
        .applyLift(model, ParsingLevelSeries.Abstract_Knowledge_Expression, codedRep(DMN_1_2), null)
        .flatMap(kc -> new CCPMProfileDMNValidator().applyValidate(kc, null));

    assertTrue(ans.isSuccess());

    Problem exp = ans.getExplanationAsProblem();
    assertTrue(Style_Conformance.refersTo(exp.getType()));
    assertEquals(FATAL, Severity.severityOf(exp));
  }

  @Test
  void testComputableDecisionServiceWithoutDecisionInputs() {
    KnowledgeCarrier model =
        AbstractCarrier.of(loadModel("/compDecServiceNoDecisionInputs.dmn.xml"))
            .withLabel("Mock DMN model with Decision Services")
            .withAssetId(SemanticIdentifier.randomId())
            .withRepresentation(rep(DMN_1_2, XML_1_1, UTF_8, Encodings.DEFAULT));

    Answer<Void> ans = new DMN12Parser()
        .applyLift(model, ParsingLevelSeries.Abstract_Knowledge_Expression, codedRep(DMN_1_2), null)
        .flatMap(kc -> new CCPMProfileDMNValidator().applyValidate(kc, null));

    assertTrue(ans.isSuccess());

    Problem exp = ans.getExplanationAsProblem();
    assertTrue(Style_Conformance.refersTo(exp.getType()));
    // Still ERRORs because a lot of other criteria are not met...
    assertEquals(ERR, Severity.severityOf(exp));
  }

  @Test
  void testCannotValidateBinaries() {
    KnowledgeCarrier kc =
        AbstractCarrier.of("<definitions/>")
            .withLabel("Not even a DMN model but does not matter")
            .withAssetId(SemanticIdentifier.randomId())
            .withRepresentation(rep(DMN_1_2, XML_1_1, UTF_8, Encodings.DEFAULT));

    Answer<Void> ans = new CCPMProfileDMNValidator().applyValidate(kc, null);
    assertFalse(ans.isSuccess());
    assertTrue(ResponseCodeSeries.NotAcceptable.sameAs(ans.getOutcomeType()));
  }

  private InputStream loadModel(String model) {
    return CPMProfileDMNValidatorTest.class.getResourceAsStream("/dmn/v1_2" + model);
  }

}
