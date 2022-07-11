package edu.mayo.kmdp.knowledgebase.introspectors.fhir.stu3;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Archived;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Draft;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Published;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Unpublished;

import edu.mayo.kmdp.knowledgebase.AbstractKnowledgeBaseOperator;
import edu.mayo.kmdp.language.parsers.fhir.stu3.FHIR3Deserializer;
import java.util.UUID;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.Resource;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedIntrospect;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedIntrospectDirect;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal._applyLift;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatus;

/**
 * Generic Introspector that extracts {@link KnowledgeAsset} metadata from selected types of FHIR
 * STU3 resources.
 * <p>
 * Factors common functionalities to wrap/unwrap the original artifact. Ensures that the resource is
 * parsed for the subclasses to focus on the actual processing logic, but will extract metadata
 * consistent with the original representation's ParsingLevel.
 *
 * @param <T> the FHIR resource type
 */
public abstract class AbstractFhirIntrospector<T extends Resource>
    extends AbstractKnowledgeBaseOperator
    implements _applyNamedIntrospect, _applyNamedIntrospectDirect {

  static final _applyLift parser = new FHIR3Deserializer();

  protected AbstractFhirIntrospector(ResourceIdentifier opId) {
    super(opId);
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return FHIR_STU3;
  }


  @Override
  public Answer<KnowledgeCarrier> applyNamedIntrospect(UUID operatorId, UUID kbaseId,
      String versionTag, String xParams) {

    return kbManager.getKnowledgeBaseManifestation(kbaseId, versionTag)
        .map(AbstractCarrier::mainComponent)
        .flatMap(kc -> this.doIntrospect(kc));
  }

  @Override
  public Answer<KnowledgeCarrier> applyNamedIntrospectDirect(UUID operatorId,
      KnowledgeCarrier artifact, String xParams) {
    return doIntrospect(artifact);
  }

  /**
   * Platform adapter method.
   * <p>
   * Unwraps the input {@link KnowledgeCarrier} to extract the FHIR resource, and returns an
   * Answer.of a KnowledgeCarrier that wraps the inferred {@link KnowledgeAsset}.
   * <p>
   * More specifically, this method unwraps and parses the original artifact, then delegates the
   * metadata extraction
   *
   * @param knowledgeCarrier the Carrier of the FHIR Resource artifact
   * @return a KnowledgeAsset surrogate for the FHIR resource, wrapped in a KnowledgeCarrier,
   * wrapped in an Answer.
   * @see #doIntrospect(Resource, SyntacticRepresentation)
   */
  protected Answer<KnowledgeCarrier> doIntrospect(KnowledgeCarrier knowledgeCarrier) {
    Answer<T> vsOpt =
        parser
            .applyLift(knowledgeCarrier, Abstract_Knowledge_Expression, codedRep(FHIR_STU3), null)
            .flatOpt(kc -> kc.as(getTypeClass()));
    if (vsOpt.isFailure()) {
      return Answer.failed(vsOpt);
    }
    return doIntrospect(vsOpt.get(), knowledgeCarrier.getRepresentation());
  }

  /**
   * Knowledge Platform adapter method.
   * <p>
   * Delegates the generation of the metadata from the FHIR resource, then wraps the resulting
   * Surrogate
   *
   * @param artifact               the Carrier of the FHIR Resource artifact, parsed
   * @param originalRepresentation the original {@link SyntacticRepresentation}
   * @return a KnowledgeAsset surrogate for the FHIR resource, wrapped in a KnowledgeCarrier,
   * wrapped in an Answer.
   * @see #innerIntrospect(Resource, SyntacticRepresentation)
   */
  protected Answer<KnowledgeCarrier> doIntrospect(
      T artifact,
      SyntacticRepresentation originalRepresentation) {
    // construct the Surrogate...
    var surrogate = innerIntrospect(artifact, originalRepresentation);
    // ...and wrap it
    return Answer.of(SurrogateHelper.carry(surrogate));
  }


  /**
   * Internal introspection function.
   * <p>
   * Maps a FHIR Resource of the concrete type to the {@link KnowledgeAsset} surrogate with
   * metadata for that resource.
   *
   * @param artifact the FHIR resource (object)
   * @param originalRepresentation the original {@link SyntacticRepresentation}
   * @return a KnowledgeAsset surrogate for the FHIR resource
   */
  protected abstract KnowledgeAsset innerIntrospect(
      T artifact, SyntacticRepresentation originalRepresentation);


  /**
   * Internal method used to reify the generic type T
   * @return Class<T>
   */
  protected abstract Class<T> getTypeClass();

  /**
   * Cross-vocabulary mapping for publication statuses
   *
   * @param status the FHIR publication status
   * @return the KMP publication status
   */
  protected PublicationStatus mapStatus(Enumerations.PublicationStatus status) {
    switch (status) {
      case DRAFT:
        return Draft;
      case ACTIVE:
        return Published;
      case RETIRED:
        return Archived;
      case NULL:
      case UNKNOWN:
      default:
        return Unpublished;
    }
  }

}
