package edu.mayo.kmdp.examples._8.graphql.components;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;

import edu.mayo.kmdp.kbase.query.sparql.v1_1.JenaQuery;
import java.util.List;
import java.util.UUID;
import org.apache.jena.rdf.model.ModelFactory;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.inference.v4.server.ReasoningApiInternal._askQuery;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.datatypes.Bindings;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;

public class MockSPARQLAdapter implements _askQuery {

  public static ResourceIdentifier DATABASE_ID = SemanticIdentifier.randomId();

  private final _askQuery jenaQuery;

  public MockSPARQLAdapter(KnowledgeBaseApiInternal kbManager) {
    this.jenaQuery = new JenaQuery(kbManager::getKnowledgeBase);

    // Mock (empty) DB
    kbManager.initKnowledgeBase(
        AbstractCarrier.ofAst(ModelFactory.createDefaultModel())
            .withRepresentation(rep(OWL_2))
            .withAssetId(DATABASE_ID),
        null);
  }

  @Override
  public Answer<List<Bindings>> askQuery(
      UUID kbID, String kbVersion, KnowledgeCarrier sparqlQuery, String xConfig) {
    return jenaQuery.askQuery(kbID, kbVersion, sparqlQuery, xConfig);
  }


}
