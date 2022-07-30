package edu.mayo.kmdp.knowledgebase.flatteners.fhir.stu3;

import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Knowledge_Resource_Flattening_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;

import edu.mayo.kmdp.util.URIUtil;
import java.net.URI;
import java.util.UUID;
import javax.inject.Named;
import org.hl7.fhir.dstu3.model.CodeSystem;
import org.hl7.fhir.dstu3.model.CodeSystem.ConceptDefinitionComponent;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.CompositionalApiInternal;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@KPOperation(Knowledge_Resource_Flattening_Task)
@KPSupport(FHIR_STU3)
@Named
public class CodeSystemFlattener
    extends AbstractFhirFlattener<CodeSystem>
    implements CompositionalApiInternal._flattenArtifact {

  private static final Logger logger = LoggerFactory.getLogger(CodeSystemFlattener.class);

  public static final UUID id = UUID.fromString("bee66c12-4c64-4a43-8fd4-5e70c297be9f");
  public static final String version = "1.0.0";

  public CodeSystemFlattener() {
    super(SemanticIdentifier.newId(id, version));
  }

  protected CodeSystem merge(CodeSystem target, CodeSystem source) {
    String sourceUri = source.getUrl() != null
        ? URIUtil.normalizeURIString(URI.create(source.getUrl())) : null;
    String targetUri = target.getUrl() != null
        ? URIUtil.normalizeURIString(URI.create(target.getUrl())) : null;

    if (targetUri != null && sourceUri != null && !sourceUri.equals(targetUri)) {
      // expect both CodeSystems to share the same URL (namespace URI)
      logger.error("Unable to merge CodeSystem {} into CodeSystem {}",
          sourceUri, targetUri);
      return source;
    }

    // restate the (normalized) URI
    if (targetUri == null) {
      target.setUrl(sourceUri);
    } else {
      target.setUrl(targetUri);
    }

    // merge concepts - no duplicates
    source.getConcept().forEach(cd -> {
      if (target.getConcept().stream().noneMatch(x -> matches(x, cd))) {
        target.getConcept().add(cd);
      }
    });

    return target;
  }

  @Override
  protected Class<CodeSystem> getComponentType() {
    return CodeSystem.class;
  }

  @Override
  protected CodeSystem newInstance() {
    return new CodeSystem();
  }

  private boolean matches(ConceptDefinitionComponent x, ConceptDefinitionComponent cd) {
    return x.getCode().equals(cd.getCode());
  }

}
