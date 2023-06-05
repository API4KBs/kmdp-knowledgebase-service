package edu.mayo.kmdp.language;

import java.util.List;
import java.util.UUID;
import javax.inject.Named;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DetectApiInternal;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DiscoveryApiInternal;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.ValidateApiInternal;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.omg.spec.api4kp._20200801.services.transrepresentation.Deserializer;
import org.omg.spec.api4kp._20200801.services.transrepresentation.Detector;
import org.omg.spec.api4kp._20200801.services.transrepresentation.Transrepresentator;
import org.omg.spec.api4kp._20200801.services.transrepresentation.Validator;
import org.springframework.beans.factory.annotation.Autowired;

@KPServer
@Named
public class LanguageServicesCatalog implements DiscoveryApiInternal {

  DetectApiInternal detector;
  ValidateApiInternal validator;
  DeserializeApiInternal parser;
  TransionApiOperator translator;

  public LanguageServicesCatalog(
    @Autowired(required = false) @KPServer DetectApiInternal detector,
    @Autowired(required = false) @KPServer ValidateApiInternal validator,
    @Autowired(required = false) @KPServer DeserializeApiInternal parser,
    @Autowired(required = false) @KPServer TransionApiOperator translator
  ) {
    this.detector = detector;
    this.validator = validator;
    this.parser = parser;
    this.translator = translator;
  }

  @Override
  public Answer<Deserializer> getDeserializationComponent(UUID componentId) {
    return parser instanceof _getDeserializationComponent
        ? ((_getDeserializationComponent) parser).getDeserializationComponent(componentId)
        : Answer.unsupported();
  }

  @Override
  public Answer<List<Deserializer>> listDeserializationComponents(String from, String into, String methodTag) {
    return parser instanceof _listDeserializationComponents
        ? ((_listDeserializationComponents) parser).listDeserializationComponents(from, into, methodTag)
        : Answer.unsupported();
  }

  @Override
  public Answer<Detector> getDetectComponent(UUID componentId) {
    return detector instanceof _getDetectComponent
        ? ((_getDetectComponent) detector).getDetectComponent(componentId)
        : Answer.unsupported();
  }

  @Override
  public Answer<List<Detector>> listDetectComponents(String into, String methodTag) {
    return detector instanceof _listDeserializationComponents
        ? ((_listDetectComponents) detector).listDetectComponents(into, methodTag)
        : Answer.unsupported();
  }

  @Override
  public Answer<Validator> getValidationComponent(UUID componentId) {
    return validator instanceof _getValidationComponent
        ? ((_getValidationComponent) detector).getValidationComponent(componentId)
        : Answer.unsupported();
  }

  @Override
  public Answer<List<Validator>> listValidationComponents(String from, String methodTag) {
    return validator instanceof _listValidationComponents
        ? ((_listValidationComponents) validator).listValidationComponents(from, methodTag)
        : Answer.unsupported();
  }

  @Override
  public Answer<Transrepresentator> getTxComponent(UUID componentId) {
    return translator instanceof _getTxComponent
        ? ((_getTxComponent) translator).getTxComponent(componentId)
        : Answer.unsupported();
  }

  @Override
  public Answer<List<Transrepresentator>> listTxComponents(String from, String into,
      String methodTag) {
    return translator instanceof _listTxComponents
        ? ((_listTxComponents) translator).listTxComponents(from, into, methodTag)
        : Answer.unsupported();
  }


}
