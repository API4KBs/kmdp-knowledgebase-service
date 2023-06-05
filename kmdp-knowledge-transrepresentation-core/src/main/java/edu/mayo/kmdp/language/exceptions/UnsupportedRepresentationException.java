package edu.mayo.kmdp.language.exceptions;

import edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries;
import org.omg.spec.api4kp._20200801.Explainer;
import org.omg.spec.api4kp._20200801.ServerSideException;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder;

/**
 * Generic {@link ServerSideException} to handle scenarios where an API implementation is unable to
 * process an Artifact in a specific representation
 */
public class UnsupportedRepresentationException extends ServerSideException {

  public UnsupportedRepresentationException(
      ResourceIdentifier operatorId,
      SyntacticRepresentation representation,
      KnowledgeCarrier carrier) {
    super(Explainer.GENERIC_ERROR_TYPE,
        "Unsupported representation",
        ResponseCodeSeries.NotAcceptable,
        "Operator "+ operatorId + " cannot process " + ModelMIMECoder.encode(representation),
        carrier.getAssetId().getVersionId());
  }
}
