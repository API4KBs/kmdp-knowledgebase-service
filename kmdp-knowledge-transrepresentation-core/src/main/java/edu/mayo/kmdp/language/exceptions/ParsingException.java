package edu.mayo.kmdp.language.exceptions;

import edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries;
import org.omg.spec.api4kp._20200801.Explainer;
import org.omg.spec.api4kp._20200801.ServerSideException;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;

/**
 * Generic {@link ServerSideException} to handle de/serialization errors
 */
public class ParsingException extends ServerSideException {

  public ParsingException(
      KnowledgeCarrier carrier) {
    super(Explainer.GENERIC_ERROR_TYPE,
        "De/Serialization Error",
        ResponseCodeSeries.InternalServerError,
        "Unable to de/serialize artifact",
        carrier.getAssetId().getVersionId());
  }
}
