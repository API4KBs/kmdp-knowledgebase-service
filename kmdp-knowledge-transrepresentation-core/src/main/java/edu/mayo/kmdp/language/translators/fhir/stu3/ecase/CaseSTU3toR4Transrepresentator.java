package edu.mayo.kmdp.language.translators.fhir.stu3.ecase;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_R4;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;

import edu.mayo.kmdp.language.translators.AbstractSimpleTranslator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.r4.model.Bundle;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;


public class CaseSTU3toR4Transrepresentator extends AbstractSimpleTranslator<Parameters, Bundle> {
  protected CaseConverter caseConverter = new CaseConverter();
  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return FHIR_STU3;
  }

  @Override
  public KnowledgeRepresentationLanguage getTargetLanguage() {
    return FHIR_R4;
  }

  @Override
  public List<SyntacticRepresentation> getFrom() {
    return List.of(rep(FHIR_STU3));
  }

  @Override
  public List<SyntacticRepresentation> getInto() {
    return List.of(rep(FHIR_R4));
  }

  @Override
  protected Optional<Bundle> transformAst(
      ResourceIdentifier assetId,
      ResourceIdentifier srcArtifactId,
      Parameters expression,
      SyntacticRepresentation srcRep,
      SyntacticRepresentation tgtRep,
      Properties config) {
    try {
      return Optional.ofNullable(caseConverter.convertCase(expression));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

}
