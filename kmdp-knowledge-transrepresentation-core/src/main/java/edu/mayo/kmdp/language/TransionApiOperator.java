package edu.mayo.kmdp.language;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.of;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.ofAst;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.ofTree;
import static org.omg.spec.api4kp._20200801.contrastors.SyntacticRepresentationContrastor.theRepContrastor;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultArtifactId;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.asEnum;

import edu.mayo.kmdp.util.Util;
import java.util.List;
import org.omg.spec.api4kp._20200801.KnowledgePlatformOperator;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.TransxionApiInternal;
import org.omg.spec.api4kp._20200801.contrastors.ParsingLevelContrastor;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder;
import org.omg.spec.api4kp._20200801.services.transrepresentation.TransrepresentationOperator;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevel;

;

public interface TransionApiOperator
    extends TransxionApiInternal.Operator, KnowledgePlatformOperator<TransrepresentationOperator> {

  List<SyntacticRepresentation> getFrom();

  List<SyntacticRepresentation> getInto();

  KnowledgeRepresentationLanguage getTargetLanguage();

  default ResourceIdentifier mapArtifactId(ResourceIdentifier assetId,
      ResourceIdentifier artifactId) {
    // in general, transformations preserve the asset, but create new artifacts
    return artifactId != null
        ? defaultArtifactId(assetId, getTargetLanguage(), artifactId.getVersionTag())
        : defaultArtifactId(assetId, getTargetLanguage());
  }

  default ResourceIdentifier mapAssetId(ResourceIdentifier assetId) {
    return assetId;
  }

  default boolean consumes(String from) {
    return Util.isEmpty(from) ||
        ModelMIMECoder.decode(from)
            .map(fromRep -> getFrom().stream()
                .anyMatch(supported -> theRepContrastor.isBroaderOrEqual(supported, fromRep)))
            .orElse(false);
  }

  default boolean produces(String into) {
    return Util.isEmpty(into) ||
        ModelMIMECoder.decode(into)
            .map(intoRep -> getInto().stream()
                .anyMatch(supported -> theRepContrastor.isNarrowerOrEqual(supported, intoRep)))
            .orElse(false);
  }

  @Override
  default TransrepresentationOperator getDescriptor() {
    return new TransrepresentationOperator()
        .withFrom(getFrom())
        .withInto(getInto())
        .withOperatorId(getOperatorId());
  }

  static KnowledgeCarrier newHorizontalCarrier(
      SyntacticRepresentation targetRepresentation,
      SyntacticRepresentation currentRepresentation,
      Object targetArtifact,
      ResourceIdentifier mappedAssetId,
      ResourceIdentifier mappedArtifactId, String label) {

    ParsingLevel targetLevel = ParsingLevelContrastor.detectLevel(currentRepresentation);

    KnowledgeCarrier newCarrier;
    switch (asEnum(targetLevel)) {
      case Abstract_Knowledge_Expression:
        newCarrier = ofAst(targetArtifact);
        break;
      case Concrete_Knowledge_Expression:
        newCarrier = ofTree(targetArtifact);
        break;
      case Serialized_Knowledge_Expression:
        newCarrier = of(targetArtifact.toString());
        break;
      case Encoded_Knowledge_Expression:
      default:
        newCarrier = of((byte[]) targetArtifact);
    }

    return newCarrier.withAssetId(mappedAssetId)
        .withArtifactId(mappedArtifactId)
        .withLevel(targetLevel)
        .withLabel(label)
        .withRepresentation(targetRepresentation);
  }
}
