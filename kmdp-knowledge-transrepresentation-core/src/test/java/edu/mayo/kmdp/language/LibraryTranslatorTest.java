package edu.mayo.kmdp.language;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder.encode;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Clinical_Rule;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate;

import edu.mayo.kmdp.language.translators.surrogate.v2.SurrogateV2toLibraryTranslator;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.Enumerations.PublicationStatus;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;

public class LibraryTranslatorTest {

  KnowledgeAsset meta = new MockSurrogateKnowledgeAsset().buildMetadata();
  TransionApiOperator translator = new SurrogateV2toLibraryTranslator();

  @Test
  @Disabled("Upgrade the Library Translator to v2")
  void testLibraryTranslation() {
    Answer<KnowledgeCarrier> fhir =
        Answer.of(AbstractCarrier.ofAst(meta)
            .withRepresentation(rep(Knowledge_Asset_Surrogate)))
            .flatMap(kc -> translator.try_applyTransrepresent(kc, encode(rep(FHIR_STU3)), null));

    assertTrue(fhir.isSuccess());
    Library lib = fhir
        .flatOpt(kc -> kc.as(Library.class))
        .orElse(new Library());

    assertEquals(Clinical_Rule.getTag(),
        lib.getType().getCodingFirstRep().getCode());
    assertEquals("0c36a4a3-7645-4276-baf5-be957112717b",
        lib.getIdentifierFirstRep().getValue());

    assertEquals(7, lib.getRelatedArtifact().size());
    assertTrue(lib.getRelatedArtifact().stream()
        .anyMatch(rel -> rel.getType().equals(RelatedArtifactType.DEPENDSON)));
    assertTrue(lib.getRelatedArtifact().stream()
        .anyMatch(rel -> rel.getType().equals(RelatedArtifactType.COMPOSEDOF)));
    assertTrue(lib.getRelatedArtifact().stream()
        .anyMatch(rel -> rel.getType().equals(RelatedArtifactType.DERIVEDFROM)));
    assertTrue(lib.getRelatedArtifact().stream()
        .anyMatch(rel -> rel.getType().equals(RelatedArtifactType.SUCCESSOR)));

    assertEquals(PublicationStatus.ACTIVE, lib.getStatus());

    assertEquals(1, lib.getContent().size());

    Attachment artifact = lib.getContentFirstRep();
    assertTrue(artifact.getUrl().contains("f2b9828d-f84c-4d09-9c88-413c7f1439a4"));
    assertEquals("it", artifact.getLanguage());
    assertEquals("model/dmn-v11[cql-exx]+xml;lex={sct}", artifact.getContentType());
    assertEquals("A mock example", artifact.getTitle());

  }
}
