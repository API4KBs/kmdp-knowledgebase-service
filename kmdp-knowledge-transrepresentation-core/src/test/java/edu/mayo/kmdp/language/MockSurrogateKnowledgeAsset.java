package edu.mayo.kmdp.language;

import static edu.mayo.kmdp.registry.Registry.DID_URN_URI;
import static edu.mayo.ontology.taxonomies.kmdo.citationreltype.BibliographicCitationTypeSeries.Cites_As_Authority;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.Has_Focus;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Clinical_Rule;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Depends_On;
import static org.omg.spec.api4kp._20200801.taxonomy.derivationreltype.DerivationTypeSeries.Is_Derived_From;
import static org.omg.spec.api4kp._20200801.taxonomy.iso639_2_languagecode.LanguageSeries.Italian;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeartifactcategory.KnowledgeArtifactCategorySeries.Software;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Rules_Policies_And_Guidelines;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetrole.KnowledgeAssetRoleSeries.Operational_Concept_Definition;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeprocessingtechnique.KnowledgeProcessingTechniqueSeries.Quantitative_Technique;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.krprofile.KnowledgeRepresentationLanguageProfileSeries.CQL_Essentials;
import static org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries.DMN_1_1_XML_Syntax;
import static org.omg.spec.api4kp._20200801.taxonomy.languagerole.KnowledgeRepresentationLanguageRoleSeries.Schema_Language;
import static org.omg.spec.api4kp._20200801.taxonomy.lexicon.LexiconSeries.SNOMED_CT;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Published;
import static org.omg.spec.api4kp._20200801.taxonomy.relatedversiontype.RelatedVersionTypeSeries.Has_Previous_Version;
import static org.omg.spec.api4kp._20200801.taxonomy.structuralreltype.StructuralPartTypeSeries.Has_Proper_Part;
import static org.omg.spec.api4kp._20200801.taxonomy.summaryreltype.SummarizationTypeSeries.Summarizes;
import static org.omg.spec.api4kp._20200801.taxonomy.variantreltype.VariantTypeSeries.Is_Translation_Of;

import java.net.URI;
import java.util.UUID;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.Term;
import org.omg.spec.api4kp._20200801.surrogate.Annotation;
import org.omg.spec.api4kp._20200801.surrogate.Citation;
import org.omg.spec.api4kp._20200801.surrogate.Component;
import org.omg.spec.api4kp._20200801.surrogate.Dependency;
import org.omg.spec.api4kp._20200801.surrogate.Derivative;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.Publication;
import org.omg.spec.api4kp._20200801.surrogate.Summary;
import org.omg.spec.api4kp._20200801.surrogate.Variant;
import org.omg.spec.api4kp._20200801.surrogate.Version;

public class MockSurrogateKnowledgeAsset {

  public KnowledgeAsset buildMetadata() {
    return new KnowledgeAsset()
        .withAssetId(
            newId(URI.create(DID_URN_URI + "0c36a4a3-7645-4276-baf5-be957112717b"), "142412"))
        .withFormalCategory(Rules_Policies_And_Guidelines)
        .withFormalType(Clinical_Rule)
        .withProcessingMethod(Quantitative_Technique)
        .withRole(Operational_Concept_Definition)
        .withName("Diabetes Treatment Plan")
        .withDescription("Description of Diabetes...")
        .withAnnotation(
            new Annotation()
                .withRef(
                    Term.mock("mock", "12345").asConceptIdentifier())
                .withRel(Has_Focus.asConceptIdentifier()))
        .withLinks(
            new Component()
                .withRel(Has_Proper_Part)
                .withHref(newId(UUID.randomUUID(), "1")))
        .withLinks(
            new Derivative()
                .withRel(Is_Derived_From)
                .withHref(newId(UUID.randomUUID(), "99").withName("Sub Assett Test")))
        .withLinks(new Derivative().withRel(Is_Derived_From).withHref(ref(UUID.randomUUID(), "2")))
        .withLinks(new Dependency().withRel(Depends_On).withHref(ref(UUID.randomUUID(), "3")))
        .withLinks(new Variant().withRel(Is_Translation_Of).withHref(ref(UUID.randomUUID(), "4")))
        .withLinks(
            new Version().withRel(Has_Previous_Version).withHref(ref(UUID.randomUUID(), "5")))
        .withCitations(
            new Citation()
                .withRel(Cites_As_Authority.asConceptIdentifier())
                .withBibliography("F.F. Very Important Paper."))
        .withLifecycle(
            new Publication()
                .withPublicationStatus(Published))
        .withCarriers(
            new KnowledgeArtifact()
                .withArtifactId(
                    newId(URI.create(DID_URN_URI + "f2b9828d-f84c-4d09-9c88-413c7f1439a4"),
                        "000"))
                .withLocalization(Italian)
                .withExpressionCategory(Software)
                .withTitle("A mock example")
                .withSummary(new Summary().withRel(Summarizes))
                .withRepresentation(
                    rep(DMN_1_1, CQL_Essentials, DMN_1_1_XML_Syntax, TXT, null, null, SNOMED_CT)
                        .withSubLanguage(rep(DMN_1_1, XML_1_1, SNOMED_CT)
                            .withRole(Schema_Language))))
        .withSurrogate(
            new KnowledgeArtifact()
                .withArtifactId(
                    newId(URI.create(DID_URN_URI + "a42e2cef-be7f-4aab-82ba-fe6f12495e3f"),
                        "65464"))
                .withRepresentation(rep(Knowledge_Asset_Surrogate_2_0)));
  }

  private ResourceIdentifier ref(UUID randomUUID, String version) {
    return newId(URI.create(DID_URN_URI + randomUUID.toString()), version);
  }
}
