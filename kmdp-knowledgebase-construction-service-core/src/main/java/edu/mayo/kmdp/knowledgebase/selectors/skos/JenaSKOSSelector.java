package edu.mayo.kmdp.knowledgebase.selectors.skos;

import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Selection_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL2_DL;

import edu.mayo.kmdp.knowledgebase.AbstractKnowledgeBaseOperator;
import java.util.Objects;
import java.util.UUID;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedSelect;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedSelectDirect;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPComponent;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.springframework.stereotype.Component;

@Component
@KPComponent
@KPSupport(OWL2_DL)
@KPOperation(Selection_Task)
public class JenaSKOSSelector
    extends AbstractKnowledgeBaseOperator
    implements _applyNamedSelect , _applyNamedSelectDirect {

  public static final UUID id = UUID.fromString("7d2697bb-9eac-4bfe-836b-f35ee01be26a");
  public static final String version = "1.0.0";

  public JenaSKOSSelector() {
    super(SemanticIdentifier.newId(id, version));
  }

  public JenaSKOSSelector(KnowledgeBaseApiInternal kbManager) {
    this();
    this.kbManager = kbManager;
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return OWL2_DL;
  }

  @Override
  public Answer<KnowledgeCarrier> applyNamedSelect(UUID operatorId, KnowledgeCarrier query,
      UUID kbaseId, String versionTag, String xParams) {
    if (!getOperatorId().getUuid().equals(operatorId)) {
      return Answer.failed();
    }
    return kbManager.getKnowledgeBaseManifestation(kbaseId, versionTag)
        .flatMap(model -> applyNamedSelectDirect(operatorId, model, null, xParams));
  }


  @Override
  public Answer<KnowledgeCarrier> applyNamedSelectDirect(UUID operatorId, KnowledgeCarrier artifact,
      KnowledgeCarrier definition, String xParams) {
    if (!getOperatorId().getUuid().equals(operatorId)) {
      return Answer.failed();
    }

    final Model source = artifact.as(Model.class).orElseThrow(IllegalStateException::new);
    Model result = ModelFactory.createDefaultModel();

    source.listStatements()
        .filterKeep(st ->
            is(st.getSubject(), SKOS.Concept, source) || is(st.getSubject(), SKOS.ConceptScheme, source))
        .forEachRemaining(ind ->
            source.listStatements(new SimpleSelector() {
              @Override
              public boolean test(Statement s) {
                return s.getSubject() != null &&
                    ind.getSubject() != null
                    && Objects.equals(s.getSubject().getURI(), ind.getSubject().getURI());
              }
            }).forEachRemaining(result::add));

    return Answer.of(AbstractCarrier.ofAst(result)
        .withRepresentation(artifact.getRepresentation())
        .withLabel(artifact.getLabel()));
  }

  private boolean is(Resource ind, Resource concept, Model source) {
    return source.contains(
        ResourceFactory.createProperty(ind.getURI()),
        RDF.type,
        concept);
  }

}