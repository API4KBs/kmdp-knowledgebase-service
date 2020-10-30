package edu.mayo.kmdp.knowledgebase.selectors.sparql.v1_1;

import static edu.mayo.kmdp.terms.util.JenaUtil.addOntologyAxioms;
import static edu.mayo.kmdp.util.PropertiesUtil.parseProperties;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Selection_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.SPARQL_1_1;

import edu.mayo.kmdp.knowledgebase.AbstractKnowledgeBaseOperator;
import edu.mayo.kmdp.terms.mireot.MireotConfig;
import edu.mayo.kmdp.terms.mireot.MireotConfig.MireotParameters;
import edu.mayo.kmdp.util.JenaUtil;
import edu.mayo.kmdp.util.StreamUtil;
import java.net.URI;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedSelect;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPComponent;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.springframework.stereotype.Component;

@Component
@KPComponent
@KPSupport(SPARQL_1_1)
@KPOperation(Selection_Task)
public class SparqlSelector
    extends AbstractKnowledgeBaseOperator
    implements _applyNamedSelect {

  public static final UUID id = UUID.fromString("32b57b17-8c6e-45a0-90c0-df38cdd6aef2");
  public static final String version = "1.0.0";

  // TODO this should be in a resource ?
  private static final String EXTRACT_QUERY = "" 
      + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
      + "PREFIX owl: <http://www.w3.org/2002/07/owl#> "
      + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
      + " "
      + "SELECT ?X (count(?mid) as ?depth) "
      + "WHERE { "
      + " "
      + "    OPTIONAL { "
      + " "
      + "        ?X a owl:Class. "
      + "            FILTER isUri( ?X ). "
      + " "
      + "        ?X rdfs:subClassOf* ?mid. "
      + "        ?mid rdfs:subClassOf* ?focus. "
      + " "
      + "    } "
      + "} "
      + "GROUP BY ?X "
      + "HAVING ( (?depth >= ?n) && (?depth <= ?m) )";


  public SparqlSelector() {
    super(SemanticIdentifier.newId(id, version));
  }

  public SparqlSelector(KnowledgeBaseApiInternal kbManager) {
    this();
    this.kbManager = kbManager;
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return SPARQL_1_1;
  }

  @Override
  public Answer<KnowledgeCarrier> applyNamedSelect(UUID operatorId, KnowledgeCarrier query,
      UUID kbaseId, String versionTag, String xParams) {
    if (!getOperatorId().getUuid().equals(operatorId)) {
      return Answer.failed();
    }
    return kbManager.getKnowledgeBaseManifestation(kbaseId, versionTag)
        .flatMap(ontoModel -> select(ontoModel, query, parseProperties(xParams)));
  }

  private Answer<KnowledgeCarrier> select(KnowledgeCarrier ontoModel, KnowledgeCarrier query,
      Properties props) {
    MireotConfig cfg = new MireotConfig(props);
    final URI baseUri = cfg.getTyped(MireotParameters.BASE_URI);
    final URI rootEntityUri = cfg.getTyped(MireotParameters.TARGET_URI);
    final Model source = ontoModel.as(Model.class).orElseThrow(IllegalStateException::new);

    ModelFactory.createDefaultModel();
//    extract(source, rootEntityUri, baseUri, cfg)
//        .forEach(res -> System.out.println("    Adding class " + res));

    Model result = extract(source, rootEntityUri, baseUri, cfg).stream()
        .map(x -> fetchResource(source, URI.create(x.getURI()), baseUri, query))
        .flatMap(StreamUtil::trimStream)
        .reduce(ModelFactory.createDefaultModel(), Model::add);

    addOntologyAxioms(result, source);

    return Answer.of(AbstractCarrier.ofAst(result)
        .withRepresentation(ontoModel.getRepresentation())
        .withLabel(ontoModel.getLabel()));
  }

  Optional<Model> fetchResource(Model source, URI entityURI, URI baseUri,
      KnowledgeCarrier query) {
    ParameterizedSparqlString pss = query.as(ParameterizedSparqlString.class)
        .orElseThrow(IllegalStateException::new);

    pss.setParam("?X", NodeFactory.createURI(entityURI.toString()));
    pss.setParam("?baseUri", NodeFactory.createURI(baseUri.toString()));

    return Optional.of(JenaUtil.construct(source, pss.asQuery()));
  }


  Set<Resource> extract(Model source, URI rootEntityUri, URI baseUri, MireotConfig cfg) {
    Integer min = cfg.getTyped(MireotParameters.MIN_DEPTH);
    Integer max = cfg.getTyped(MireotParameters.MAX_DEPTH);

    ParameterizedSparqlString pss
        = new ParameterizedSparqlString(EXTRACT_QUERY, baseUri.toString());

    // count is 1-based, rather than 0-based
    pss.setParam("?focus", NodeFactory.createURI(rootEntityUri.toString()));
    pss.setLiteral("?n", min + 1);
    pss.setLiteral("?m", max < 0 ? Integer.MAX_VALUE : max + 1);

    return JenaUtil.askQuery(source, pss.asQuery());
  }

}