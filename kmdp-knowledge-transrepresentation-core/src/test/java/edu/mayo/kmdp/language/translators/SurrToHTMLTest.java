package edu.mayo.kmdp.language.translators;

import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.Defines;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.Has_Primary_Subject;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.newRandomSurrogate;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Clinical_Rule;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Depends_On;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Imports;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Assessment_Predictive_And_Inferential_Models;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Rules_Policies_And_Guidelines;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;

import edu.mayo.kmdp.language.translators.surrogate.v2.SurrogateV2ToHTML;
import edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.UUID;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.id.Term;
import org.omg.spec.api4kp._20200801.surrogate.Annotation;
import org.omg.spec.api4kp._20200801.surrogate.Dependency;
import org.omg.spec.api4kp._20200801.surrogate.Derivative;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder;
import org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyType;
import org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.derivationreltype._20210401.DerivationType;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries;

public class SurrToHTMLTest {

  @Test
  void testTranslate() {
    String html = getHtml();
    assertNotNull(Jsoup.parse(html));
  }

  public static void main(String... args) {
    new TestHTMLViewer(getHtml());
  }

  static String getHtml() {
    ResourceIdentifier artId = SurrogateBuilder.randomArtifactId();
    KnowledgeAsset surr = newRandomSurrogate()
        .withName("Mock Score", "A hypothetical test score...")
        .withFormalType(Assessment_Predictive_And_Inferential_Models, KnowledgeAssetTypeSeries.Assessment_Model)
        .get()
        .withAnnotation(new Annotation()
            .withRel(Defines.asConceptIdentifier())
            .withRef(Term.mock("mock Risk of Condition", "123").asConceptIdentifier()))
        .withAnnotation(new Annotation()
            .withRel(Has_Primary_Subject.asConceptIdentifier())
            .withRef(Term.mock("mock Condition", "123").asConceptIdentifier()))
        .withLinks(new Dependency()
            .withRel(Depends_On)
            .withHref(
                newId(URI.create("http://www.mock.edu"), UUID.randomUUID(), "1.0.0")))
        .withLinks(new Derivative()
            .withRel(DerivationType.Is_Derived_From)
            .withHref(
                newId(URI.create("http://www.mock.edu"), UUID.randomUUID(), "1.0.0")))
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(artId)
            .withLocator(URI.create("http://www.mock.edu/repository/" + artId.getUuid()))
            .withRepresentation(rep(DMN_1_2, XML_1_1, Charset.forName("UTF-8"), Encodings.DEFAULT))
        );
    Document doh = new SurrogateV2ToHTML().transform(surr);
    return doh.outerHtml();
  }


}
