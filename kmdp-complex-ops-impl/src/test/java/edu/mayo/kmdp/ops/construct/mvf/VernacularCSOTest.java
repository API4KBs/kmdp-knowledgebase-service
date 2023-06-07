package edu.mayo.kmdp.ops.construct.mvf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.MVF_1_0;

import edu.mayo.kmdp.ops.tranx.mvf.owl2.cso.components.VernacularConceptGenerator;
import java.nio.charset.Charset;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLOntology;

class VernacularCSOTest {

  @Test
  void testOntologyCreation() {
    var ans = new VernacularConceptGenerator()
        .applyNamedTransformDirect(
            VernacularConceptGenerator.OPERATOR_ID,
            load("/mvf/mock.mvf.xml"),
            locateCSO());

    var kc = ans.orElseGet(Assertions::fail);
    var onto = kc.as(OWLOntology.class)
        .orElseGet(Assertions::fail);

    checkOnto(onto);
  }

  private String locateCSO() {
    return Optional.ofNullable(VernacularCSOTest.class.getResource("/mvf/testCSO.owl"))
        .orElseGet(Assertions::fail)
        .toString();
  }

  private void checkOnto(OWLOntology onto) {
    assertEquals(27, onto.classesInSignature().count());
    assertEquals(45, onto.nestedClassExpressions().count());
    assertEquals(4, onto.axioms()
        .filter(ax -> ax.getAxiomType() == AxiomType.EQUIVALENT_CLASSES)
        .count());
  }


  private static KnowledgeCarrier load(String file) {
    var is = VernacularCSOTest.class.getResourceAsStream(file);
    return AbstractCarrier.of(is)
        .withAssetId(SemanticIdentifier.randomId())
        .withArtifactId(SemanticIdentifier.randomId())
        .withRepresentation(rep(MVF_1_0, XML_1_1, Charset.defaultCharset(), Encodings.DEFAULT));
  }
}
