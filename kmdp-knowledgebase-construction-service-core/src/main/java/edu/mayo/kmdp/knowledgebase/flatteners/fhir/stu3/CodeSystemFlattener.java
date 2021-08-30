package edu.mayo.kmdp.knowledgebase.flatteners.fhir.stu3;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.randomId;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Knowledge_Resource_Flattening_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;

import edu.mayo.kmdp.knowledgebase.AbstractKnowledgeBaseOperator;
import edu.mayo.kmdp.language.parsers.fhir.stu3.FHIR3Deserializer;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.URIUtil;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Named;
import org.hl7.fhir.dstu3.model.CodeSystem;
import org.hl7.fhir.dstu3.model.CodeSystem.ConceptDefinitionComponent;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.CompositionalApiInternal;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@KPOperation(Knowledge_Resource_Flattening_Task)
@KPSupport(FHIR_STU3)
@Named
public class CodeSystemFlattener
    extends AbstractKnowledgeBaseOperator
    implements CompositionalApiInternal._flattenArtifact {

  private static final Logger logger = LoggerFactory.getLogger(CodeSystemFlattener.class);

  public static final UUID id = UUID.fromString("bee66c12-4c64-4a43-8fd4-5e70c297be9f");
  public static final String version = "1.0.0";

  public CodeSystemFlattener() {
    super(SemanticIdentifier.newId(id, version));
  }

  private static final FHIR3Deserializer parser = new FHIR3Deserializer();

  public Answer<KnowledgeCarrier> flattenArtifact(KnowledgeCarrier carrier, UUID rootAssetId,
      String params) {
    if (!(carrier instanceof CompositeKnowledgeCarrier)) {
      return Answer.of(carrier);
    }

    return Answer.of(carrier)
        .flatMap(
            ckc -> parser.applyLift(ckc, Abstract_Knowledge_Expression, codedRep(FHIR_STU3), null))
        .map(x -> x.components().collect(Collectors.toList()))
        .flatMap(this::flatten);
  }

  private Answer<KnowledgeCarrier> flatten(List<KnowledgeCarrier> codeSystems) {
    Optional<ResourceIdentifier> assetId = codeSystems.stream()
        .map(KnowledgeCarrier::getAssetId)
        .filter(Objects::nonNull)
        .reduce(SemanticIdentifier::hashIdentifiers);
    if (assetId.isEmpty()) {
      return codeSystems.isEmpty()
          ? Answer.of(wrap(randomId(), new CodeSystem()))
          : Answer.failed(new IllegalArgumentException("Unable to determine a combined Asset ID"));
    }

    return Answer.of(codeSystems.stream()
            .map(kc -> kc.as(CodeSystem.class))
            .flatMap(StreamUtil::trimStream)
            .reduce(this::merge))
        .map(flat -> wrap(assetId.orElse(randomId()), flat));
  }

  private KnowledgeCarrier wrap(ResourceIdentifier assetId, CodeSystem flat) {
    return AbstractCarrier.ofAst(flat)
        .withAssetId(assetId)
        .withArtifactId(randomId())
        .withRepresentation(rep(FHIR_STU3));
  }

  private CodeSystem merge(CodeSystem target, CodeSystem source) {
    String sourceUri = URIUtil.normalizeURIString(URI.create(source.getUrl()));
    String targetUri = URIUtil.normalizeURIString(URI.create(target.getUrl()));

    // expect both CodeSystems to share the same URL (namespace URI)
    if (sourceUri == null || ! sourceUri.equals(targetUri)) {
      logger.error("Unable to merge CodeSystem {} into CodeSystem {}",
          sourceUri, targetUri);
      if (targetUri == null) {
        logger.error("Unable to merge CodeSystem with no URI/URL");
        return new CodeSystem();
      } else {
        return target;
      }
    }

    // restate the (normalized) URI
    target.setUrl(targetUri);
    // merge concepts - no duplicates
    source.getConcept().forEach(cd -> {
      if (target.getConcept().stream().noneMatch(x -> matches(x, cd))) {
        target.getConcept().add(cd);
      }
    });

    return target;
  }

  private boolean matches(ConceptDefinitionComponent x, ConceptDefinitionComponent cd) {
    return x.getCode().equals(cd.getCode());
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return FHIR_STU3;
  }

}
