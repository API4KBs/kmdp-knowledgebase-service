package edu.mayo.kmdp.terms;

import java.util.Optional;
import org.omg.spec.api4kp._20200801.api.terminology.v4.server.TermsApiInternal;

/**
 * Composite {@link TermsApiInternal} Supports components that implement {@link TermsApiInternal}
 * and whose implementation is based on FHIR resources as opposed to static, semantic Enumerations
 *
 * @see TermsFHIRFacade
 */
public interface CompositeTermsServer extends TermsApiInternal {

  enum TYPE {
    ENUM, FHIR, BROKER;
  }

  /**
   * @return the type of server - broker, enum based or fhir based
   */
  TYPE getType();

  /**
   * Returns the value of the configuration option that determines the origin of the terminology
   * information. Maps to a KARS base URL for FHIR-based providers, as opposed to a terminology
   * index file for Enum-based providers
   *
   * @return the source of information
   */
  String getSource();

  /**
   * @return the FHIR-based component of a broker, this for FHIR-based providers, empty othersise
   */
  Optional<TermsApiInternal> getFHIRBasedComponent();
}
