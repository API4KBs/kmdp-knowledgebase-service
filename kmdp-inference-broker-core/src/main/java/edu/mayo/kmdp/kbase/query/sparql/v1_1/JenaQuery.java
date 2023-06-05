/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.mayo.kmdp.kbase.query.sparql.v1_1;

import static edu.mayo.kmdp.util.JenaUtil.askQueryResults;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.SPARQL_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.asEnum;

import edu.mayo.kmdp.knowledgebase.binders.sparql.v1_1.SparqlQueryBinder;
import edu.mayo.kmdp.util.Util;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Named;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.inference.v4.server.ReasoningApiInternal._askQuery;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.datatypes.Bindings;
import org.omg.spec.api4kp._20200801.services.KPComponent;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeBase;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.springframework.beans.factory.annotation.Autowired;

@Named
@KPComponent
@KPSupport(SPARQL_1_1)
public class JenaQuery implements _askQuery {

  KnowledgeBaseApiInternal._getKnowledgeBase kBaseSource;

  static SparqlQueryBinder binder = new SparqlQueryBinder();

  public static KnowledgeCarrier wholeGraph() {
    String query = "" +
        "select ?s ?p ?o where { ?s ?p ?o }";
    return AbstractCarrier.of(query)
        .withRepresentation(rep(SPARQL_1_1, TXT, Charset.defaultCharset()));
  }

  public static Answer<KnowledgeCarrier> bind(KnowledgeCarrier query, Bindings binds) {
    return binder.bind(query, binds);
  }

  public JenaQuery(@Autowired(required = false) KnowledgeBaseApiInternal._getKnowledgeBase kBaseSource) {
    this.kBaseSource = kBaseSource;
  }

  @Override
  public Answer<List<Bindings>> askQuery(UUID modelId, String versionTag, KnowledgeCarrier query, String params) {
    if (kBaseSource == null) {
      return Answer.unsupported();
    }
    return kBaseSource.getKnowledgeBase(modelId, versionTag, params)
        .flatMap(kBase -> {
          if (isLocal(kBase)) {
            return Answer.of(kBase)
                .map(KnowledgeBase::getManifestation)
                .flatOpt(m -> m.as(Model.class))
                .flatOpt(m -> applyQuery(query, m));
          } else {
            return askQueryRemote(kBase.getKbaseId().getHref().toString(), query);
          }
        });
  }

  private boolean isLocal(KnowledgeBase kBase) {
    return kBase.getKbaseId().getHref() == null
        || Util.isEmpty(kBase.getKbaseId().getHref().getScheme());
  }

  private Answer<List<Bindings>> askQueryRemote(String endpoint, KnowledgeCarrier query) {
    return Answer.of(
        query.as(Query.class)
            .map(q -> submitQuery(q, endpoint)));
  }

  private List<Bindings> submitQuery(Query q, String endpoint) {
    Function<RDFNode, String> mapper = (RDFNode::toString);
    QueryExecution queryExec = QueryExecutionFactory.sparqlService(endpoint, q);
    ResultSet results = queryExec.execSelect();
    List<Bindings> answers = new LinkedList<>();
    while (results.hasNext()) {
      QuerySolution sol = results.next();
      Bindings bindings = new Bindings();
      results.getResultVars()
          .forEach(var -> bindings.put(var, mapper.apply(sol.get(var))));
      answers.add(bindings);
    }
    return answers;
  }


  private Optional<List<Bindings>> applyQuery(KnowledgeCarrier query, Model m) {
    Optional<Query> qry = Optional.empty();
    switch (asEnum(query.getLevel())) {
      case Encoded_Knowledge_Expression:
      case Serialized_Knowledge_Expression:
        qry = query.asString()
            .map(ParameterizedSparqlString::new)
            .map(ParameterizedSparqlString::asQuery);
        break;
      case Concrete_Knowledge_Expression:
        qry = query.as(ParameterizedSparqlString.class)
            .map(ParameterizedSparqlString::asQuery);
        break;
      case Abstract_Knowledge_Expression:
        qry = query.as(Query.class);
        break;
      default:
    }
    return qry
        .map(q -> askQueryResults(m, q))
        .map(this::toBindings);
  }

  private List<Bindings> toBindings(Set<Map<String, String>> binds) {
    return binds.stream()
        .map(m -> {
          Bindings b = new Bindings();
          b.putAll(m);
          return b;
        }).collect(Collectors.toList());
  }

}
