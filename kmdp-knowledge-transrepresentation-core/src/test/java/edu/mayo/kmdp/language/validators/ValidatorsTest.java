package edu.mayo.kmdp.language.validators;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.CMMN_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;

import edu.mayo.kmdp.language.LanguageValidator;
import edu.mayo.kmdp.language.validators.api4kp.v1_0.SurrogateV2Validator;
import edu.mayo.kmdp.language.validators.dmn.v1_2.DMN12Validator;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.dmn._20180521.model.TDefinitions;

class ValidatorsTest {

  @Test
  void testNoCanDo() {
    LanguageValidator validator = new LanguageValidator(Arrays.asList(
        new DMN12Validator(), new SurrogateV2Validator()
    ));

    KnowledgeCarrier kc = AbstractCarrier.of("stuff")
        .withAssetId(SemanticIdentifier.randomId())
        .withRepresentation(rep(CMMN_1_1))
        .withLabel("Mock CMMN");

    Answer<Void> ans = validator.applyValidate(kc, null);
    System.out.println(ans.printExplanationAsJson());
  }

  @Test
  void testOneCanDo() {
    LanguageValidator validator = new LanguageValidator(Arrays.asList(
        new DMN12Validator(), new SurrogateV2Validator()
    ));

    KnowledgeCarrier kc = AbstractCarrier.ofAst(new TDefinitions())
        .withAssetId(SemanticIdentifier.randomId())
        .withRepresentation(rep(DMN_1_2))
        .withLabel("Mock DMN");

    Answer<Void> ans = validator.applyValidate(kc, null);
    System.out.println(ans.printExplanationAsJson());
  }

}
