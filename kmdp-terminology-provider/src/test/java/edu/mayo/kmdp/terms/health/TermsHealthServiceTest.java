package edu.mayo.kmdp.terms.health;

import static org.mockito.Mockito.when;

import edu.mayo.kmdp.health.datatype.ApplicationComponent;
import edu.mayo.kmdp.health.datatype.Status;
import edu.mayo.kmdp.health.service.HealthService;
import edu.mayo.kmdp.terms.CompositeTermsServer;
import edu.mayo.kmdp.terms.CompositeTermsServer.TYPE;
import java.util.List;
import java.util.Optional;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.junit.jupiter.api.Assertions;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.terminology.v4.server.TermsApiInternal;
import org.omg.spec.api4kp._20200801.id.Pointer;

class TermsHealthServiceTest {

  public static final String TAG_1 = "tag1";
  public static final String TAG_2 = "tag2";
  public static final String TAG_3 = "tag3";

  @Test
  void testNonCompositeSuccess() {

    TermsApiInternal termsApiInternal = Mockito.mock(TermsApiInternal.class);

    List<Pointer> pointerList = Lists.newArrayList();
    Pointer pointer1 = new Pointer().withTag(TAG_1);
    pointerList.add(pointer1);
    Answer<List<Pointer>> terminologiesList = Answer.of(pointerList);
    when(termsApiInternal.listTerminologies()).thenReturn(terminologiesList);

    TermsHealthService termsHealthService = new TermsHealthService(termsApiInternal);

    ApplicationComponent applicationComponent = termsHealthService.assessHealth();

    Assertions.assertEquals(Status.UP, applicationComponent.getStatus());
    Assertions.assertNotNull(
        applicationComponent.getDetails().get(HealthService.EXECUTION_TIME_MS));
    Assertions.assertEquals(TermsHealthService.ACTIVE_VOCABULARIES + TAG_1,
        applicationComponent.getStatusMessage());

  }

  @Test
  void testBrokerFailureImpaired() {

    CompositeTermsServer termsApiInternal = Mockito.mock(CompositeTermsServer.class);
    when(termsApiInternal.getType()).thenReturn(TYPE.BROKER);

    List<Pointer> pointerList = Lists.newArrayList();
    Pointer pointer1 = new Pointer().withTag(TAG_1);
    pointerList.add(pointer1);
    Answer<List<Pointer>> terminologiesList = Answer.of(pointerList);
    when(termsApiInternal.listTerminologies()).thenReturn(terminologiesList);

    TermsHealthService termsHealthService = new TermsHealthService(termsApiInternal);

    ApplicationComponent applicationComponent = termsHealthService.assessHealth();

    Assertions.assertEquals(Status.IMPAIRED, applicationComponent.getStatus());
    Assertions.assertNotNull(
        applicationComponent.getDetails().get(HealthService.EXECUTION_TIME_MS));
    Assertions.assertEquals(TermsHealthService.TERMINOLOGY_BROKER_HAS_NO_COMPONENTS,
        applicationComponent.getStatusMessage());

  }

  @Test
  void testBrokerComposite() {

    CompositeTermsServer termsApiInternal = Mockito.mock(CompositeTermsServer.class);
    when(termsApiInternal.getType()).thenReturn(TYPE.BROKER);

    List<Pointer> pointerList = Lists.newArrayList();
    Pointer pointer1 = new Pointer().withTag(TAG_1);
    pointerList.add(pointer1);
    Answer<List<Pointer>> terminologiesList = Answer.of(pointerList);
    when(termsApiInternal.listTerminologies()).thenReturn(terminologiesList);

    CompositeTermsServer fhirTermsApiInternal = Mockito.mock(CompositeTermsServer.class);
    when(fhirTermsApiInternal.getType()).thenReturn(TYPE.FHIR);
    Optional<TermsApiInternal> fhirTermsApiInternalOptional = Optional.of(fhirTermsApiInternal);
    when(termsApiInternal.getFHIRBasedComponent()).thenReturn(fhirTermsApiInternalOptional);
    List<Pointer> pointerList3 = Lists.newArrayList();
    Pointer pointer3 = new Pointer().withTag(TAG_3);
    pointerList3.add(pointer3);
    Answer<List<Pointer>> terminologiesList3 = Answer.of(pointerList3);
    when(fhirTermsApiInternal.listTerminologies()).thenReturn(terminologiesList3);

    CompositeTermsServer enumTermsApiInternal = Mockito.mock(CompositeTermsServer.class);
    when(enumTermsApiInternal.getType()).thenReturn(TYPE.ENUM);
    List<Pointer> pointerList2 = Lists.newArrayList();
    Pointer pointer2 = new Pointer().withTag(TAG_2);
    pointerList2.add(pointer2);
    Answer<List<Pointer>> terminologiesList2 = Answer.of(pointerList2);
    when(enumTermsApiInternal.listTerminologies()).thenReturn(terminologiesList2);

    TermsHealthService termsHealthService = new TermsHealthService(termsApiInternal);

    ApplicationComponent applicationComponent = termsHealthService.assessHealth();

    Assertions.assertEquals(Status.UP, applicationComponent.getStatus());
    Assertions.assertEquals(1, applicationComponent.getComponents().size());
    Assertions.assertNotNull(
        applicationComponent.getDetails().get(HealthService.EXECUTION_TIME_MS));
    Assertions.assertNull(applicationComponent.getStatusMessage());

  }

  @Test
  void testFhirComposite() {

    CompositeTermsServer termsApiInternal = Mockito.mock(CompositeTermsServer.class);
    when(termsApiInternal.getType()).thenReturn(TYPE.FHIR);

    List<Pointer> pointerList = Lists.newArrayList();
    Pointer pointer1 = new Pointer().withTag(TAG_1);
    pointerList.add(pointer1);
    Answer<List<Pointer>> terminologiesList = Answer.of(pointerList);
    when(termsApiInternal.listTerminologies()).thenReturn(terminologiesList);

    TermsHealthService termsHealthService = new TermsHealthService(termsApiInternal);

    ApplicationComponent applicationComponent = termsHealthService.assessHealth();

    Assertions.assertEquals(Status.UP, applicationComponent.getStatus());
    Assertions.assertNotNull(
        applicationComponent.getDetails().get(HealthService.EXECUTION_TIME_MS));
    Assertions.assertEquals(TermsHealthService.ACTIVE_VOCABULARIES + TAG_1,
        applicationComponent.getStatusMessage());

  }

  @Test
  void testEnumComposite() {

    CompositeTermsServer termsApiInternal = Mockito.mock(CompositeTermsServer.class);
    when(termsApiInternal.getType()).thenReturn(TYPE.ENUM);

    List<Pointer> pointerList = Lists.newArrayList();
    Pointer pointer1 = new Pointer().withTag(TAG_1);
    pointerList.add(pointer1);
    Answer<List<Pointer>> terminologiesList = Answer.of(pointerList);
    when(termsApiInternal.listTerminologies()).thenReturn(terminologiesList);

    TermsHealthService termsHealthService = new TermsHealthService(termsApiInternal);

    ApplicationComponent applicationComponent = termsHealthService.assessHealth();

    Assertions.assertEquals(Status.UP, applicationComponent.getStatus());
    Assertions.assertNotNull(
        applicationComponent.getDetails().get(HealthService.EXECUTION_TIME_MS));
    Assertions.assertEquals(TermsHealthService.ACTIVE_VOCABULARIES + TAG_1,
        applicationComponent.getStatusMessage());

  }

  @Test
  void testEnumCompositeImpaired() {

    CompositeTermsServer termsApiInternal = Mockito.mock(CompositeTermsServer.class);
    when(termsApiInternal.getType()).thenReturn(TYPE.ENUM);

    TermsHealthService termsHealthService = new TermsHealthService(termsApiInternal);

    ApplicationComponent applicationComponent = termsHealthService.assessHealth();

    Assertions.assertEquals(Status.IMPAIRED, applicationComponent.getStatus());
    Assertions.assertNotNull(
        applicationComponent.getDetails().get(HealthService.EXECUTION_TIME_MS));
    Assertions.assertEquals(TermsHealthService.UNABLE_TO_ACCESS_VOCABULARIES,
        applicationComponent.getStatusMessage());

  }

  @Test
  void testEnumCompositeImpairedTerminologiesEmpty() {

    CompositeTermsServer termsApiInternal = Mockito.mock(CompositeTermsServer.class);
    when(termsApiInternal.getType()).thenReturn(TYPE.ENUM);

    List<Pointer> pointerList = Lists.newArrayList();
    Answer<List<Pointer>> terminologiesList = Answer.of(pointerList);
    when(termsApiInternal.listTerminologies()).thenReturn(terminologiesList);

    TermsHealthService termsHealthService = new TermsHealthService(termsApiInternal);

    ApplicationComponent applicationComponent = termsHealthService.assessHealth();

    Assertions.assertEquals(Status.IMPAIRED, applicationComponent.getStatus());
    Assertions.assertNotNull(
        applicationComponent.getDetails().get(HealthService.EXECUTION_TIME_MS));
    Assertions.assertEquals(TermsHealthService.NO_TERMINOLOGIES_AVAILABLE,
        applicationComponent.getStatusMessage());

  }

  @Test
  void testException() {

    TermsApiInternal termsApiInternal = Mockito.mock(TermsApiInternal.class);
    when(termsApiInternal.listTerminologies()).thenThrow(new RuntimeException("Some exception occurred"));

    TermsHealthService termsHealthService = new TermsHealthService(termsApiInternal);

    ApplicationComponent applicationComponent = termsHealthService.assessHealth();

    Assertions.assertEquals(Status.DOWN, applicationComponent.getStatus());
    Assertions.assertNotNull(
        applicationComponent.getDetails().get(HealthService.EXECUTION_TIME_MS));
    Assertions.assertEquals("Unable to interrogate, exception: 'Some exception occurred'", applicationComponent.getStatusMessage());

  }

}