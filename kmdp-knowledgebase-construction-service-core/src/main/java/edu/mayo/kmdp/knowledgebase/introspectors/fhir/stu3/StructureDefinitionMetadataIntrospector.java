package edu.mayo.kmdp.knowledgebase.introspectors.fhir.stu3;

import static edu.mayo.kmdp.registry.Registry.MAYO_ASSETS_BASE_URI_URI;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.artifactId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.assetId;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Structured_Information_And_Data_Capture_Models;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Information_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Published;

import edu.mayo.kmdp.util.Util;
import java.net.URI;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.Publication;

/**
 * Introspector that generates {@link KnowledgeAsset} surrogates for FHIR STU3
 * {@link StructureDefinition} data shapes
 * <p>
 * Note: This is an initial, partial implementation. Supports base profiles for standard Resource
 * types, but not actual differential "profiles"
 *
 * @author Sottara.Davide@mayo.edu
 */
public class StructureDefinitionMetadataIntrospector extends AbstractFhirIntrospector<StructureDefinition> {

  public static final UUID OP_ID
      = UUID.fromString("be3f12cc-0d94-4e23-b224-475ce94ed5a3");
  public static final String OP_VERSION = "1.0.0";

  public static final String FHIR_TAG = "STU3";
  public static final String FHIR_VERSION = "3.0.2";

  public static final String FHIR_URL_DT_PATH = "Datatypes#";
  public static final String FHIR_URL_RES_PATH = "StructureDefinition/";
  public static final String FHIR_URL = "https://www.hl7.org/fhir/";

  /**
   * Extra configuration parameter - allows to set the base namespace for the Asset ID.
   */
  public static final String CFG_ASSET_NAMESPACE = "ASSET_NAMESPACE";


  public StructureDefinitionMetadataIntrospector() {
    super(SemanticIdentifier.newId(OP_ID, OP_VERSION));
  }

  /**
   * Builds a minimally populated {@link KnowledgeAsset} for a FHIR resource of a given type
   *
   * @param namespace    the Asset namespace URI
   * @param resourceType the FHIR resource type
   * @return the metadata record
   */
  public static KnowledgeAsset buildFhir3Resource(URI namespace, String resourceType) {
    var assetId = mintAssetID(namespace, resourceType, false);
    var rep1 = rep(FHIR_STU3, XML_1_1);
    var rep2 = rep(FHIR_STU3, JSON);
    return new KnowledgeAsset()
        .withAssetId(assetId)
        .withSecondaryId(mintFhirOfficialId(resourceType, false))
        .withFormalCategory(Structured_Information_And_Data_Capture_Models)
        .withFormalType(Information_Model)
        .withLifecycle(new Publication().withPublicationStatus(Published))
        .withName("FHIR STU3 " + resourceType)
        .withCarriers(
            new KnowledgeArtifact()
                .withArtifactId(
                    mintArtifactId(namespace, rep2, resourceType, FHIR_VERSION, false))
                .withRepresentation(rep2)
                .withLocator(URI.create(
                    FHIR_URL + FHIR_TAG + "/" + resourceType.toLowerCase()
                        + ".profile.json")));
  }

  /**
   * Builds a minimally populated {@link KnowledgeAsset} for a FHIR simple datatype
   *
   * @param namespace the Asset namespace URI
   * @param dataType  the FHIR data type
   * @return the metadata record
   */
  public static KnowledgeAsset buildFhir3Datatype(URI namespace, String dataType) {
    return new KnowledgeAsset()
        .withAssetId(mintAssetID(namespace, dataType, true))
        .withSecondaryId(mintFhirOfficialId(dataType, true))
        .withFormalCategory(Structured_Information_And_Data_Capture_Models)
        .withFormalType(Information_Model)
        .withLifecycle(new Publication().withPublicationStatus(Published))
        .withCarriers(new KnowledgeArtifact()
            .withRepresentation(rep(FHIR_STU3, JSON))
            .withName("FHIR STU3 " + dataType)
            .withLocator(
                URI.create(FHIR_URL + FHIR_TAG + "/datatypes.html#" + dataType)));
  }

  public static ResourceIdentifier mintFhirOfficialId(String fhirType, String versionTag,
      boolean isDatatype) {
    if (isDatatype) {
      return newId(URI.create(FHIR_URL + FHIR_URL_DT_PATH), fhirType, versionTag);
    } else {
      return newId(URI.create(FHIR_URL + FHIR_URL_RES_PATH), fhirType, versionTag);
    }
  }

  public static ResourceIdentifier mintAssetID(
      URI namespace,
      String fhirType, String version,
      boolean isDatatype) {
    if (isDatatype) {
      return assetId(
          namespace,
          Util.uuid(FHIR_URL + FHIR_URL_DT_PATH + fhirType),
          version);
    } else {
      return assetId(
          namespace,
          Util.uuid(FHIR_URL + FHIR_URL_RES_PATH + fhirType),
          version);
    }
  }

  public static ResourceIdentifier mintArtifactId(
      URI namespace,
      SyntacticRepresentation rep,
      String fhirType,
      String version,
      boolean isDatatype) {
    if (isDatatype) {
      return artifactId(
          namespace,
          Util.uuid(FHIR_URL + FHIR_URL_DT_PATH + fhirType + "/" + ModelMIMECoder.encode(rep)),
          version);
    } else {
      return artifactId(
          namespace,
          Util.uuid(FHIR_URL + FHIR_URL_RES_PATH + fhirType + "/" + ModelMIMECoder.encode(rep)),
          version);
    }
  }

  public static ResourceIdentifier mintFhirOfficialId(String fhirType, boolean isDatatype) {
    return mintFhirOfficialId(fhirType, FHIR_TAG, isDatatype);
  }

  public static ResourceIdentifier mintAssetID(URI namespace, String fhirType, boolean isDatatype) {
    return mintAssetID(namespace, fhirType, FHIR_VERSION, isDatatype);
  }

  @Override
  protected KnowledgeAsset innerIntrospect(StructureDefinition artifact,
      SyntacticRepresentation originalRepresentation, Properties props) {

    URI assetNamespace = Optional.ofNullable(props.get(CFG_ASSET_NAMESPACE))
        .map(s -> URI.create(s.toString()))
        .orElse(MAYO_ASSETS_BASE_URI_URI);
    return buildFhir3Resource(assetNamespace, artifact.getType());
  }

  @Override
  protected Class<StructureDefinition> getTypeClass() {
    return StructureDefinition.class;
  }

}