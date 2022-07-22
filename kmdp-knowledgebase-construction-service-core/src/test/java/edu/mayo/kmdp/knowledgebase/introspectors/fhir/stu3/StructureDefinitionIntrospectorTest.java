package edu.mayo.kmdp.knowledgebase.introspectors.fhir.stu3;

import static edu.mayo.kmdp.knowledgebase.introspectors.fhir.stu3.StructureDefinitionMetadataIntrospector.OP_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Structured_Information_And_Data_Capture_Models;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Information_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Published;

import org.hl7.fhir.dstu3.model.Enumerations.PublicationStatus;
import org.hl7.fhir.dstu3.model.Enumerations.ResourceType;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.hl7.fhir.dstu3.model.StructureDefinition.StructureDefinitionKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;

public class StructureDefinitionIntrospectorTest {

  StructureDefinitionMetadataIntrospector introspector =
      new StructureDefinitionMetadataIntrospector();

  @Test
  void testResource() {
    KnowledgeAsset metadata = introspect(ResourceType.OBSERVATION);

    assertTrue(Published.sameAs(metadata.getLifecycle().getPublicationStatus()));
    assertTrue(Structured_Information_And_Data_Capture_Models.isAnyOf(
        metadata.getFormalCategory()));
    assertTrue(Information_Model.isAnyOf(
        metadata.getFormalType()));

    var officialId = metadata.getSecondaryId().get(0);

    assertEquals("Observation", officialId.getTag());
    assertTrue(
        officialId.getResourceId().toString()
            .contains("StructureDefinition/Observation"));
    assertTrue(
        officialId.getVersionId().toString()
            .contains("versions/STU3"));

    KnowledgeArtifact carrier = metadata.getCarriers().get(0);

    assertTrue(
        "https://www.hl7.org/fhir/STU3/observation.profile.json"
            .equalsIgnoreCase(carrier.getLocator().toString()));
  }

  @Test
  void testKnownResources() {
    KnowledgeAsset meta1 = introspect(ResourceType.OBSERVATION);
    KnowledgeAsset meta2 = introspect(ResourceType.CLINICALIMPRESSION);

    assertEquals(
        "https://clinicalknowledgemanagement.mayo.edu/assets/cdd8645f-d2bc-3d95-bfa2-5e2d8e9b8f1e/versions/3.0.2",
        meta1.getAssetId().getVersionId().toString());

    assertEquals(
        "https://clinicalknowledgemanagement.mayo.edu/assets/3ab1ec21-3fa6-3393-89be-d482222dca4a/versions/3.0.2",
        meta2.getAssetId().getVersionId().toString());
  }

  private KnowledgeAsset introspect(ResourceType resourceTypeEnum) {
    String resourceType = resourceTypeEnum.toCode();
    var sd = new StructureDefinition()
        .setUrl("http://hl7.org/fhir/StructureDefinition/" + resourceType)
        .setName(resourceType)
        .setAbstract(false)
        .setStatus(PublicationStatus.ACTIVE)
        .setKind(StructureDefinitionKind.RESOURCE)
        .setType(resourceType);

    var kc = AbstractCarrier.ofAst(sd, rep(FHIR_STU3));

    return introspector.applyNamedIntrospectDirect(OP_ID, kc, null)
        .flatOpt(x -> x.as(KnowledgeAsset.class))
        .orElseGet(Assertions::fail);
  }
}
