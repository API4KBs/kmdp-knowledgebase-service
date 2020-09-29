package edu.mayo.kmdp.knowledgebase.selectors.dmn.v1_2;

import static edu.mayo.kmdp.util.JenaUtil.datA;
import static edu.mayo.kmdp.util.JenaUtil.objA;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.In_Terms_Of;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Selection_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.SPARQL_1_1;

import edu.mayo.kmdp.knowledgebase.AbstractKnowledgeBaseOperator;
import edu.mayo.kmdp.util.JenaUtil;
import edu.mayo.kmdp.util.StreamUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.xml.bind.JAXBElement;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedSelect;
import org.omg.spec.api4kp._20200801.id.ConceptIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPComponent;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.Annotation;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.omg.spec.dmn._20180521.model.TDMNElement;
import org.omg.spec.dmn._20180521.model.TDMNElement.ExtensionElements;
import org.omg.spec.dmn._20180521.model.TDefinitions;
import org.springframework.stereotype.Component;

@Component
@KPComponent
@KPSupport(DMN_1_2)
@KPOperation(Selection_Task)
public class DMN12ConceptSelector
    extends AbstractKnowledgeBaseOperator
    implements _applyNamedSelect {

  public static final UUID id = UUID.fromString("4019e740-6a3b-4b4c-90b3-e0cbc372b8f8");
  public static final String version = "1.0.0";

  private static final String SELECT_BY_NAMESPACE = ""
      + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
      + "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n"
      + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
      + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
      + "SELECT ?con ?label\n"
      + "\tWHERE { ?con a skos:Concept;\n"
      + "               skos:inScheme ?cs;\n"
      + "               rdfs:label ?label. }";

  public DMN12ConceptSelector() {
    super(SemanticIdentifier.newId(id, version));
  }

  public DMN12ConceptSelector(KnowledgeBaseApiInternal kbManager) {
    this();
    this.kbManager = kbManager;
  }

  public static KnowledgeCarrier getNamespaceFilter(String csUri) {
    ParameterizedSparqlString str = new ParameterizedSparqlString(SELECT_BY_NAMESPACE);
    str.setIri("?cs", csUri);
    return AbstractCarrier.ofAst(str.asQuery(), rep(SPARQL_1_1));
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return DMN_1_2;
  }


  @Override
  public Answer<KnowledgeCarrier> applyNamedSelect(UUID operatorId, KnowledgeCarrier definition,
      UUID kbaseId, String versionTag, String xParams) {
    return kbManager.getKnowledgeBaseManifestation(kbaseId,versionTag)
        .flatOpt(kc -> kc.as(TDefinitions.class))
        .map(q -> visit(q))
        .map(vs -> filter(vs,definition));
  }

  private KnowledgeCarrier filter(Model model, KnowledgeCarrier definition) {
    Model result = ModelFactory.createDefaultModel();
    Set<Resource> filteredConcepts = JenaUtil.askQuery(model, definition.as(Query.class).orElseThrow());
    model.listStatements()
        .filterKeep(st -> filteredConcepts.contains(st.getSubject()))
        .forEachRemaining(result::add);
    return AbstractCarrier.ofAst(result,rep(OWL_2));
  }

  private Model visit(TDefinitions decisionModel) {
    Model m = ModelFactory.createDefaultModel();
    decisionModel.getDrgElement().stream()
        .map(JAXBElement::getValue)
        .map(TDMNElement::getExtensionElements)
        .filter(Objects::nonNull)
        .map(ExtensionElements::getAny)
        .forEach(l -> l.stream()
            .flatMap(StreamUtil.filterAs(Annotation.class))
            .filter(ann -> ann.getRel() != null && In_Terms_Of.sameAs(ann.getRel()))
            .map(this::toStatements)
            .forEach(m::add));
    return m;
  }

  private List<Statement> toStatements(Annotation anno) {
    List<Statement> stats = new ArrayList<>();
    ConceptIdentifier term = anno.getRef();
    stats.add(objA(term.getResourceId().toString(), RDF.type.getURI(), SKOS.Concept.getURI()));
    stats.add(datA(term.getResourceId().toString(), RDFS.label.getURI(), term.getLabel()));
    stats.add(objA(term.getResourceId().toString(), SKOS.inScheme.getURI(), term.getNamespaceUri().toString()));
    return stats;
  }
}
