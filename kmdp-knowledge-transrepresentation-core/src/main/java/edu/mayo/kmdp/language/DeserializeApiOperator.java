package edu.mayo.kmdp.language;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.of;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.ofAst;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.ofTree;
import static org.omg.spec.api4kp._20200801.contrastors.SyntacticRepresentationContrastor.theRepContrastor;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.asEnum;

import edu.mayo.kmdp.util.Util;
import java.util.List;
import java.util.Optional;
import org.omg.spec.api4kp._20200801.KnowledgePlatformOperator;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.services.transrepresentation.DeserializationOperator;
import org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder;
import org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevel;

public interface DeserializeApiOperator
    extends DeserializeApiInternal.Operator, KnowledgePlatformOperator<DeserializationOperator> {

  List<SyntacticRepresentation> getFrom();

  List<SyntacticRepresentation> getInto();

  default DeserializationOperator getDescriptor() {
    return new DeserializationOperator()
        .withFrom(getFrom())
        .withInto(getInto())
        .withOperatorId(getOperatorId());
  }

  default ResourceIdentifier mapArtifactId(ResourceIdentifier artifactId) {
    return artifactId;
  }

  default ResourceIdentifier mapAssetId(ResourceIdentifier assetId) {
    return assetId;
  }

  default Optional<SyntacticRepresentation> inferRepresentationForLevel(ParsingLevel targetLevel) {
    return Optional.empty();
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

  default KnowledgeCarrier newVerticalCarrier(
      KnowledgeCarrier source,
      ParsingLevel targetLevel,
      SyntacticRepresentation targetRepresentation,
      Object targetArtifact) {

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

    return newCarrier
        .withAssetId(mapAssetId(source.getAssetId()))
        .withArtifactId(mapArtifactId(source.getArtifactId()))
        .withLevel(targetLevel)
        .withLabel(source.getLabel())
        .withRepresentation(targetRepresentation != null
            ? targetRepresentation
            : inferRepresentationForLevel(targetLevel).orElse(null));
    }



}
