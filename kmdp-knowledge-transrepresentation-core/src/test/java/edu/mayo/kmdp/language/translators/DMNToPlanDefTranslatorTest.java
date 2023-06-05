package edu.mayo.kmdp.language.translators;

import static edu.mayo.kmdp.language.common.dmn.v1_2.DMN12Utils.idToRef;
import static edu.mayo.kmdp.language.common.fhir.stu3.FHIRPlanDefinitionUtils.getSubActions;
import static edu.mayo.kmdp.util.StreamUtil.filterAs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.of;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.randomId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultArtifactId;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Interactive_Documentation_Template;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Questionnaire;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;

import edu.mayo.kmdp.language.parsers.dmn.v1_2.DMN12Parser;
import edu.mayo.kmdp.language.translators.dmn.v1_2.fhir.stu3.DmnToPlanDef;
import edu.mayo.kmdp.registry.Registry;
import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.PlanDefinition.PlanDefinitionActionComponent;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.Annotation;
import org.omg.spec.dmn._20180521.model.ObjectFactory;
import org.omg.spec.dmn._20180521.model.TAuthorityRequirement;
import org.omg.spec.dmn._20180521.model.TDMNElement.ExtensionElements;
import org.omg.spec.dmn._20180521.model.TDMNElementReference;
import org.omg.spec.dmn._20180521.model.TDecision;
import org.omg.spec.dmn._20180521.model.TDefinitions;
import org.omg.spec.dmn._20180521.model.TKnowledgeSource;

class DMNToPlanDefTranslatorTest {

  @Test
  void testDecisionService() {
    DmnToPlanDef translator = new DmnToPlanDef();
    TDefinitions source = loadDMN("/fhir.stu3/DMNService.dmn.xml");
    ResourceIdentifier assetId = randomId();
    ResourceIdentifier srcArtifactId = randomId();
    ResourceIdentifier tgtArtifactId =
        defaultArtifactId(assetId, FHIR_STU3, srcArtifactId.getVersionTag());
    PlanDefinition planDef = translator.transform(assetId, srcArtifactId, tgtArtifactId, source);

    assertEquals(1, planDef.getAction().size());
    PlanDefinitionActionComponent clientAct = planDef.getActionFirstRep();
    assertEquals(1, clientAct.getAction().size());
    PlanDefinitionActionComponent serverAct = clientAct.getActionFirstRep();

    assertEquals(3, serverAct.getInput().size());
    assertEquals(0, serverAct.getOutput().size());
  }


  @Test
  void testKnowledgeSource() {
    DmnToPlanDef translator = new DmnToPlanDef();
    TDefinitions source = loadDMN("/fhir.stu3/decisionKnowSource.dmn.xml");
    ResourceIdentifier assetId = randomId();
    ResourceIdentifier srcArtifactId = randomId();
    ResourceIdentifier tgtArtifactId =
        defaultArtifactId(assetId, FHIR_STU3, srcArtifactId.getVersionTag());
    PlanDefinition planDef = translator.transform(assetId, srcArtifactId, tgtArtifactId, source);

    List<RelatedArtifact> relateds = getKnowledgeSources(planDef).collect(Collectors.toList());
    assertEquals(1, relateds.size());
    RelatedArtifact related = relateds.get(0);
    assertEquals("https://foo.bar/baz", related.getUrl());
    assertEquals("https://foo.bar/baz", related.getDocument().getUrl());
    assertEquals("Sample Source", related.getDisplay());
    assertEquals("Sample Source", related.getDocument().getTitle());
    assertEquals("*/*", related.getDocument().getContentType());

  }

  @Test
  void testKnowledgeSourceDependencies() {
    ObjectFactory of = new ObjectFactory();
    TDecision dec = new TDecision()
        .withLabel("DecX")
        .withId(UUID.randomUUID().toString());
    TKnowledgeSource ks1 = new TKnowledgeSource()
        .withLabel("KS1")
        .withExtensionElements(new ExtensionElements().withAny(new Annotation()
            .withRef(Questionnaire.asConceptIdentifier())))
        .withLocationURI("http://ckm.m.e/assets/" + UUID.randomUUID() + "/versions/0.0.0")
        .withId(UUID.randomUUID().toString());
    TKnowledgeSource ks2 = new TKnowledgeSource()
        .withLabel("KS2")
        .withExtensionElements(new ExtensionElements().withAny(new Annotation()
            .withRef(Interactive_Documentation_Template.asConceptIdentifier())))
        .withLocationURI("http://ckm.m.e/assets/" + UUID.randomUUID() + "/versions/0.0.0")
        .withId(UUID.randomUUID().toString());
    dec.withAuthorityRequirement(new TAuthorityRequirement()
        .withRequiredAuthority(new TDMNElementReference().withHref(idToRef(ks1.getId()))));
    dec.withAuthorityRequirement(new TAuthorityRequirement()
        .withRequiredAuthority(new TDMNElementReference().withHref(idToRef(ks2.getId()))));
    ks2.withAuthorityRequirement(new TAuthorityRequirement()
        .withRequiredAuthority(new TDMNElementReference().withHref(idToRef(ks1.getId()))));
    TDefinitions dmn = new TDefinitions()
        .withDrgElement(of.createDecision(dec))
        .withDrgElement(of.createKnowledgeSource(ks1))
        .withDrgElement(of.createKnowledgeSource(ks2));

    DmnToPlanDef translator = new DmnToPlanDef();
    ResourceIdentifier assetId = randomId();
    ResourceIdentifier srcArtifactId = randomId();
    ResourceIdentifier tgtArtifactId =
        defaultArtifactId(assetId, FHIR_STU3, srcArtifactId.getVersionTag());
    PlanDefinition planDef = translator.transform(assetId, srcArtifactId, tgtArtifactId, dmn);

    PlanDefinitionActionComponent decAct = planDef.getActionFirstRep();
    assertEquals(2, decAct.getDocumentation().size());
    assertEquals(2, planDef.getContained().size());

    Library lib = planDef.getContained().stream()
        .flatMap(filterAs(Library.class))
        .filter(l -> !l.getRelatedArtifact().isEmpty())
        .findFirst().orElseGet(Assertions::fail);

    assertEquals(lib.getType().getCodingFirstRep().getCode(),
        Interactive_Documentation_Template.getTag());
    assertFalse(lib.getRelatedArtifact().isEmpty());

    RelatedArtifact ra = lib.getRelatedArtifactFirstRep();
    assertEquals(ks1.getLocationURI(), ra.getUrl());
    assertEquals(RelatedArtifactType.COMPOSEDOF, ra.getType());
  }

  @Test
  void testIdentityMapping() {
    TDefinitions dmn = new TDefinitions();
    DmnToPlanDef translator = new DmnToPlanDef();
    ResourceIdentifier assetId = randomId();
    ResourceIdentifier srcArtifactId =
        newId(Registry.BASE_UUID_URN_URI, UUID.randomUUID(), "42.0.0");
    ResourceIdentifier tgtArtifactId =
        defaultArtifactId(assetId, FHIR_STU3, srcArtifactId.getVersionTag());
    PlanDefinition planDef = translator.transform(assetId, srcArtifactId, tgtArtifactId, dmn);

    assertEquals(2, planDef.getIdentifier().size());
    assertTrue(planDef.getIdentifier().stream().anyMatch(
        id -> id.getValue().contains(assetId.getTag())));
    assertTrue(planDef.getIdentifier().stream().anyMatch(
        id -> id.getValue().contains(tgtArtifactId.getTag())));

    assertEquals(tgtArtifactId.getUuid().toString(), planDef.getId());
    assertEquals(srcArtifactId.getVersionTag(), planDef.getVersion());

    assertEquals(srcArtifactId.toString(), planDef.getRelatedArtifactFirstRep().getUrl());
    assertEquals(RelatedArtifactType.DERIVEDFROM, planDef.getRelatedArtifactFirstRep().getType());
  }

  private Stream<RelatedArtifact> getKnowledgeSources(PlanDefinition x) {
    return getSubActions(x)
        .flatMap(act -> act.getDocumentation().stream());
  }

  private TDefinitions loadDMN(String path) {
    KnowledgeCarrier kc = of(DMNToPlanDefTranslatorTest.class.getResourceAsStream(path))
        .withRepresentation(rep(DMN_1_2, XML_1_1, Charset.defaultCharset(), Encodings.DEFAULT));
    return new DMN12Parser()
        .applyLift(kc, Abstract_Knowledge_Expression, codedRep(DMN_1_2), null)
        .flatOpt(x -> x.as(TDefinitions.class))
        .orElseGet(Assertions::fail);
  }
}
