package edu.mayo.kmdp.language.validators;

import static edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries.BadRequest;
import static org.omg.spec.api4kp._20200801.contrastors.SyntacticRepresentationContrastor.theRepContrastor;

import edu.mayo.kmdp.language.exceptions.UnsupportedRepresentationException;
import edu.mayo.kmdp.language.ValidateApiOperator;
import java.net.URI;
import java.util.UUID;
import java.util.stream.Stream;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.Explainer;
import org.omg.spec.api4kp._20200801.ServerSideException;
import org.omg.spec.api4kp._20200801.Severity;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.ValidateApiInternal._applyNamedValidate;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.ValidateApiInternal._applyValidate;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.zalando.problem.Problem;

public abstract class AbstractValidator
    implements ValidateApiOperator,
    _applyValidate,
    _applyNamedValidate {


  public Answer<Void> applyNamedValidate(UUID uuid, KnowledgeCarrier knowledgeCarrier,
      String config) {
    return uuid.equals(getOperatorId().getUuid())
        ? applyValidate(knowledgeCarrier, config)
        : Answer.unsupported();
  }

  @Override
  public Answer<Void> applyValidate(KnowledgeCarrier knowledgeCarrier, String xConfig) {
    Answer<Void> response = knowledgeCarrier.componentList().isEmpty()
        ? Answer.failed(new ServerSideException(BadRequest, "Missing Artifact to Validate"))
        : Answer.succeed().withExplanationDetail(introExplainValidation(knowledgeCarrier));
    if (response.isFailure()) {
      return response;
    }
    return Stream.concat(
            Stream.of(response),
            knowledgeCarrier.components().map(kc -> checkAndValidateComponent(kc, xConfig)))
        .reduce(Answer::merge)
        .orElseGet(Answer::failed);
  }

  protected Problem introExplainValidation(KnowledgeCarrier knowledgeCarrier) {
    return Explainer.newOutcomeProblem(getValidationType(), Severity.INF)
        .withTitle("Validation")
        .withDetail("Validating " + knowledgeCarrier.getLabel())
        .withInstance(knowledgeCarrier.getAssetId().getVersionId())
        .build();
  }

  protected Answer<Void> checkAndValidateComponent(KnowledgeCarrier carrier, String xConfig) {
    if (! isSupported(carrier.getRepresentation())) {
      return Answer.failed(
          new UnsupportedRepresentationException(
              getOperatorId(), carrier.getRepresentation(), carrier));
    }
    return validateComponent(carrier, xConfig);
  }

  protected boolean isSupported(SyntacticRepresentation representation) {
    return getFrom().stream()
        .anyMatch(fromRep -> theRepContrastor.isEqual(fromRep, representation));
  }

  protected abstract Answer<Void> validateComponent(KnowledgeCarrier carrier, String xConfig);

  protected Problem format(String key, Severity status, String ruleName, String message) {
    return format(URI.create(key), status, ruleName, message);
  }

  protected Problem format(URI instance, Severity severity, String ruleName, String message) {
    return Explainer.newOutcomeProblem(getValidationType(), severity)
        .withTitle(ruleName)
        .withDetail(message)
        .withInstance(instance)
        .build();
  }


  protected String mapAssetId(KnowledgeCarrier carrier) {
    return mapResourceId(carrier.getAssetId(), 4);
  }

  protected String mapResourceId(ResourceIdentifier rid, int len) {
    return rid.getVersionId() != null
        ? rid.getVersionId().toString()
        : rid.getResourceId().toString();
  }

  @Override
  public boolean can_applyNamedValidate() {
    return true;
  }

  @Override
  public boolean can_applyValidate() {
    return true;
  }
}
