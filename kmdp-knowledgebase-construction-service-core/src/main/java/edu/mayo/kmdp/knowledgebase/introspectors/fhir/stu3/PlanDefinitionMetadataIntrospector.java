package edu.mayo.kmdp.knowledgebase.introspectors.fhir.stu3;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.id.VersionIdentifier.toSemVer;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultSurrogateId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.newSurrogate;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Care_Process_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Plans_Processes_Pathways_And_Protocol_Definitions;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Description_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;

import java.net.URI;
import java.util.UUID;
import javax.inject.Named;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPComponent;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;

/**
 * Introspection class for fhir:PlanDefinition.
 * <p>
 * Generates the surrogate (KnowledgeAsset) for a KnowledgeCarrier-wrapped PlanDefinition.
 */
@Named
@KPOperation(Description_Task)
@KPSupport(FHIR_STU3)
@KPComponent
public class PlanDefinitionMetadataIntrospector
    extends AbstractFhirIntrospector<PlanDefinition> {

  public static final UUID id
      = UUID.fromString("ae1302ac-fafa-46e5-8ceb-b59b6959aa9d");
  public static final String OP_VERSION = "1.0.0";

  public PlanDefinitionMetadataIntrospector() {
    super(SemanticIdentifier.newId(id, OP_VERSION));
  }

  public PlanDefinitionMetadataIntrospector(KnowledgeBaseApiInternal kbManager) {
    this();
    this.kbManager = kbManager;
  }

  protected KnowledgeAsset innerIntrospect(PlanDefinition planDef,
      SyntacticRepresentation original) {

    // ignore 's' for now
    ResourceIdentifier assetId = SemanticIdentifier.newVersionId(URI.create(planDef.getUrl()));
    ResourceIdentifier artifactId =
        defaultSurrogateId(assetId, FHIR_STU3, toSemVer(planDef.getVersion()));

    return newSurrogate(assetId).get()
        .withName(planDef.getName())
        .withFormalCategory(Plans_Processes_Pathways_And_Protocol_Definitions)
        // TODO this should be inferred from the annotation on the PlanDefinition itself
        // or derived from the standard 'type'
        .withFormalType(Care_Process_Model)
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(artifactId)
            .withRepresentation(rep(FHIR_STU3)));
  }

  @Override
  protected Class<PlanDefinition> getTypeClass() {
    return PlanDefinition.class;
  }


}
