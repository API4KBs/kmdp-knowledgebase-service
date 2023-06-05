package edu.mayo.kmdp.language.validators;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.randomId;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeresourceoutcome.KnowledgeResourceOutcomeSeries.Well_Formedness;

import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;

class ASCIIValidatorTest {

  @Test
  void testInvalidCharacters() {
    KnowledgeCarrier kc =
        AbstractCarrier.of("This contains an invalid   invisible space ")
            .withRepresentation(rep(null, null, UTF_8, Encodings.DEFAULT));

    Answer<Void> ans = new ASCIIValidator().applyValidate(kc, null);
    assertTrue(ans.isSuccess());
    assertEquals(Well_Formedness.getReferentId(), ans.getExplanationAsProblem().getType());
  }

  @Test
  void testValidCharacters() {
    KnowledgeCarrier kc =
        AbstractCarrier.of("All good \t\n\r    here")
            .withRepresentation(rep(null, null, UTF_8, Encodings.DEFAULT));

    Answer<Void> ans = new ASCIIValidator().applyValidate(kc, null);
    assertTrue(ans.isSuccess());
    assertEquals(Well_Formedness.getReferentId(), ans.getExplanationAsProblem().getType());
    System.out.println(ans.printExplanation());
  }

  @Test
  void testNothingToValidate() {
    KnowledgeCarrier kc = AbstractCarrier.of((String) null)
        .withAssetId(randomId());

    Answer<Void> ans = new ASCIIValidator().applyValidate(kc, null);
    assertTrue(ans.isSuccess());
    assertEquals(Well_Formedness.getReferentId(), ans.getExplanationAsProblem().getType());
  }

  @Test
  void testModeValidateAll() {
    String str = "\u00EF Mostly good \u00FF here";
    KnowledgeCarrier kc =
        AbstractCarrier.of(str)
            .withRepresentation(rep(null, null, UTF_8, Encodings.DEFAULT));

    Answer<Void> ans = new ASCIIValidator().applyValidate(kc, "Mode=ALL");
    assertTrue(ans.isSuccess());
    assertEquals(Well_Formedness.getReferentId(), ans.getExplanationAsProblem().getType());
  }

}
