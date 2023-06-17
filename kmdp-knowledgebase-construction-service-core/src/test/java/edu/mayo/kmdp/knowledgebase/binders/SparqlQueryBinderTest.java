package edu.mayo.kmdp.knowledgebase.binders;

import static edu.mayo.kmdp.id.adapter.CopyableHashMap.toBinds;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.SPARQL_1_1;

import edu.mayo.kmdp.knowledgebase.binders.sparql.v1_1.SparqlQueryBinder;
import java.nio.charset.Charset;
import org.apache.jena.query.ParameterizedSparqlString;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;

class SparqlQueryBinderTest {

  final static String query = "select ?p ?o where { ?s ?p ?o }";

  SparqlQueryBinder binder = new SparqlQueryBinder();

  @Test
  void testBindStringQueryStr() {
    KnowledgeCarrier kc = AbstractCarrier.of(query, rep(SPARQL_1_1,TXT, Charset.defaultCharset()));
    testBind(kc);
  }

  @Test
  void testBindStringQueryParsed() {
    KnowledgeCarrier kc = AbstractCarrier.ofTree(new ParameterizedSparqlString(query), rep(SPARQL_1_1,TXT));
    testBind(kc);
  }

  private void testBind(KnowledgeCarrier kc) {
    Answer<KnowledgeCarrier> ans = binder.bind(kc, toBinds("s", "urn:a", "o", "urn:b"));
    assertTrue(ans.isSuccess());

    var q = ans.flatOpt(x -> x.as(ParameterizedSparqlString.class))
        .orElseGet(Assertions::fail);
    var qs = q.toString();
    assertFalse(qs.contains("?o"));
    assertFalse(qs.contains("?s"));
    assertTrue(qs.contains("?p"));

    assertTrue(qs.contains("<urn:a>"));
    assertTrue(qs.contains("<urn:b>"));
  }


}
