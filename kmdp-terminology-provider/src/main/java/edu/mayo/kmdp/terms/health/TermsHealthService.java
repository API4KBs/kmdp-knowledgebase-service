package edu.mayo.kmdp.terms.health;

import edu.mayo.kmdp.health.datatype.ApplicationComponent;
import edu.mayo.kmdp.health.datatype.MiscProperties;
import edu.mayo.kmdp.health.datatype.Status;
import edu.mayo.kmdp.health.service.HealthService;
import edu.mayo.kmdp.health.utils.MonitorUtil;
import edu.mayo.kmdp.terms.CompositeTermsServer;
import edu.mayo.kmdp.terms.CompositeTermsServer.TYPE;
import java.util.List;
import java.util.stream.Collectors;
import org.omg.spec.api4kp._20200801.Answer;
import edu.mayo.kmdp.api.terminology.v4.server.TermsApiInternal;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StopWatch;

/**
 * A {@link edu.mayo.kmdp.health.service.HealthService} that interrogates a TermsApiInternal
 * implementation for the purpose of determining if it (and possibly its subcomponents) are
 * available and functioning as expected (ie: UP, DOWN, or IMPAIRED).
 */
public class TermsHealthService implements HealthService {

  public static final String DELIMITER = ",";
  public static final String NAME = "Terminology Provider";
  public static final String TERMS_TYPE = "edu.mayo.kmdp.terms.type";
  public static final String TERMS_SOURCE = "edu.mayo.kmdp.terms.source";
  public static final String ACTIVE_VOCABULARIES = "Active vocabularies: ";
  public static final String NO_TERMINOLOGIES_AVAILABLE = "No Terminologies available";
  public static final String COMPOSITE_TERMINOLOGY_PROVIDER = "Composite Terminology Provider";
  public static final String UNABLE_TO_ACCESS_VOCABULARIES = "Unable to access vocabularies";
  public static final String ENUM_BASED_TERMINOLOGY_PROVIDER = "Enum Based Terminology Provider";
  public static final String FHIR_BASED_TERMINOLOGY_PROVIDER = "FHIR Based Terminology Provider";
  public static final String TERMINOLOGY_BROKER_HAS_NO_COMPONENTS = "Terminology Broker has NO components";

  protected TermsApiInternal termsApiInternal;

  public TermsHealthService(@Autowired TermsApiInternal termsApiInternal) {
    this.termsApiInternal = termsApiInternal;
  }

  @Override
  public ApplicationComponent assessHealth() {

    ApplicationComponent applicationComponent = initializeApplicationComponent(NAME);

    StopWatch stopWatch = new StopWatch();
    stopWatch.start();

    try {

      applicationComponent = diagnoseTermsServer();

    } catch (Exception exception) {

      handleException(applicationComponent, exception);

    } finally {

      stopAndAddToDetails(stopWatch, applicationComponent);

    }

    return applicationComponent;

  }

  protected ApplicationComponent diagnoseTermsServer() {

    if (termsApiInternal instanceof CompositeTermsServer) {

      CompositeTermsServer compositeTermsServer = (CompositeTermsServer) termsApiInternal;
      TYPE type = compositeTermsServer.getType();

      if (TYPE.BROKER.equals(type)) {

        return diagnoseComposite(compositeTermsServer);

      } else if (TYPE.ENUM.equals(type)) {

        return diagnoseEnumBased(compositeTermsServer);

      } else if (TYPE.FHIR.equals(type)) {

        return diagnoseFhirBased(compositeTermsServer);

      }

    }

    ApplicationComponent applicationComponent = initializeApplicationComponent(NAME);

    assessStatus(termsApiInternal, applicationComponent);

    return applicationComponent;

  }

  protected ApplicationComponent diagnoseComposite(CompositeTermsServer compositeTermsServer) {

    ApplicationComponent applicationComponent = initializeApplicationComponent(
        COMPOSITE_TERMINOLOGY_PROVIDER);
    applicationComponent.getDetails().put(TERMS_TYPE, compositeTermsServer.getType().name());

    compositeTermsServer.getFHIRBasedComponent()
        .ifPresent(
            fhirTerms -> applicationComponent.addComponentsItem(diagnoseFhirBased(fhirTerms)));

    if (CollectionUtils.isEmpty(applicationComponent.getComponents())) {

      applicationComponent.setStatus(Status.IMPAIRED);
      applicationComponent.setStatusMessage(TERMINOLOGY_BROKER_HAS_NO_COMPONENTS);

    } else {

      applicationComponent.setStatusMessage(null);
      applicationComponent.setStatus(
          MonitorUtil.defaultAggregateStatus(applicationComponent.getComponents()));

    }

    return applicationComponent;

  }

  protected ApplicationComponent diagnoseEnumBased(TermsApiInternal terms) {
    return diagnoseGeneric(terms, ENUM_BASED_TERMINOLOGY_PROVIDER);
  }

  protected ApplicationComponent diagnoseFhirBased(TermsApiInternal terms) {
    return diagnoseGeneric(terms, FHIR_BASED_TERMINOLOGY_PROVIDER);
  }

  protected ApplicationComponent diagnoseGeneric(TermsApiInternal termsApiInternal, String name) {

    ApplicationComponent applicationComponent = initializeApplicationComponent(name);

    if (termsApiInternal instanceof CompositeTermsServer) {

      CompositeTermsServer compositeTermsServer = (CompositeTermsServer) termsApiInternal;

      MiscProperties miscProperties = applicationComponent.getDetails();
      miscProperties.put(TERMS_SOURCE, compositeTermsServer.getSource());
      miscProperties.put(TERMS_TYPE, compositeTermsServer.getType().name());

    }

    assessStatus(termsApiInternal, applicationComponent);

    return applicationComponent;

  }

  protected void assessStatus(TermsApiInternal termsApiInternal,
      ApplicationComponent applicationComponent) {

    Answer<List<Pointer>> terminologies = termsApiInternal.listTerminologies();

    if (terminologies == null || terminologies.isFailure()) {

      applicationComponent.setStatus(Status.IMPAIRED);

      String message = UNABLE_TO_ACCESS_VOCABULARIES;
      if (terminologies != null) {
        message = message + ": " + terminologies.printExplanation();
      }

      applicationComponent.setStatusMessage(message);

    } else if (terminologies.get().isEmpty()) {

      applicationComponent.setStatus(Status.IMPAIRED);
      applicationComponent.setStatusMessage(NO_TERMINOLOGIES_AVAILABLE);

    } else {

      String statusMessage = ACTIVE_VOCABULARIES + terminologies.get().stream()
          .map(ResourceIdentifier::getTag)
          .collect(Collectors.joining(DELIMITER));

      applicationComponent.setStatusMessage(statusMessage);

      applicationComponent.setStatus(Status.UP);

    }

  }

}
