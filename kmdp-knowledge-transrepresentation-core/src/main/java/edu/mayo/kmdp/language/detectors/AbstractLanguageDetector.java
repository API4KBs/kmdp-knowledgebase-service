package edu.mayo.kmdp.language.detectors;

import static org.omg.spec.api4kp._20200801.contrastors.SyntacticRepresentationContrastor.theRepContrastor;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.asEnum;

import edu.mayo.kmdp.language.DetectApiOperator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DetectApiInternal._applyDetect;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DetectApiInternal._applyNamedDetect;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;

public abstract class AbstractLanguageDetector
    implements DetectApiOperator, _applyDetect, _applyNamedDetect {

  protected ResourceIdentifier operatorId;

  @Override
  public Answer<KnowledgeCarrier> applyNamedDetect(UUID uuid, KnowledgeCarrier knowledgeCarrier, String config) {
    return uuid.equals(getOperatorId().getUuid())
        ? applyDetect(knowledgeCarrier, config)
        : Answer.unsupported();
  }

  @Override
  public Answer<KnowledgeCarrier> applyDetect(KnowledgeCarrier sourceArtifact, String config) {
    try {
      return Answer.ofTry(
          detect(sourceArtifact)
              .map(rep -> applyDetectedRepresentation(rep, sourceArtifact)));
    } catch (Exception e) {
      return Answer.failed(e);
    }
  }

  @Override
  public List<SyntacticRepresentation> getInto() {
    return getSupportedRepresentations();
  }

  protected Optional<SyntacticRepresentation> detect(KnowledgeCarrier sourceArtifact) {
    switch (asEnum(sourceArtifact.getLevel())) {
      case Encoded_Knowledge_Expression:
        return sourceArtifact.asBinary().flatMap(this::detectBinary);
      case Serialized_Knowledge_Expression:
        return sourceArtifact.asString().flatMap(this::detectString);
      case Concrete_Knowledge_Expression:
        return detectTree(sourceArtifact.getExpression());
      case Abstract_Knowledge_Expression:
        return detectAST(sourceArtifact.getExpression());
      default:
        return Optional.empty();
    }
  }

  protected KnowledgeCarrier applyDetectedRepresentation(
      SyntacticRepresentation rep, KnowledgeCarrier sourceArtifact) {
    SyntacticRepresentation mergedRep = checkConsistency(rep,sourceArtifact.getRepresentation());
    KnowledgeCarrier copy = new KnowledgeCarrier();
    sourceArtifact.copyTo(copy);
    return sourceArtifact.withRepresentation(mergedRep);
  }

  protected SyntacticRepresentation checkConsistency(SyntacticRepresentation detected, SyntacticRepresentation original) {
    if (original != null) {
      switch (theRepContrastor.contrast(detected,original)) {
        case IDENTICAL:
        case EQUAL:
        case EQUIVALENT:
          return detected;
        case BROADER:
          return original;
        case NARROWER:
          return detected;
        default:
          throw new IllegalStateException("Provided and Detected representations are inconsistent");
      }
    } else {
      return detected;
    }
  }

  public abstract List<SyntacticRepresentation> getSupportedRepresentations();

  protected abstract Optional<SyntacticRepresentation> detectBinary(byte[] bytes);

  protected abstract Optional<SyntacticRepresentation> detectString(String string);

  protected abstract Optional<SyntacticRepresentation> detectTree(Object parseTree);

  protected abstract Optional<SyntacticRepresentation> detectAST(Object ast);


  public ResourceIdentifier getOperatorId() {
    return operatorId;
  }

  protected void setId(ResourceIdentifier id) {
    this.operatorId = id;
  }

  @Override
  public boolean can_applyDetect() {
    return true;
  }

  @Override
  public boolean can_applyNamedDetect() {
    return true;
  }
}
