package edu.mayo.kmdp.examples._8.graphql.components;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.Answer.of;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.GraphQL_Schemas;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;

import graphql.schema.idl.SchemaParser;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal._applyLift;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;

public class GraphQLDSLParser implements _applyLift {

  @Override
  public Answer<KnowledgeCarrier> applyLift(
      KnowledgeCarrier carrier,
      String parsingLevel,
      String codedRep,
      String config) {
    if (Abstract_Knowledge_Expression.getTag().equals(parsingLevel)) {
      SchemaParser schemaParser = new SchemaParser();
      return of(carrier.asString()
          .map(schemaParser::parse)
          .map(ast -> AbstractCarrier.ofAst(ast)
              .withAssetId(carrier.getAssetId())
              .withArtifactId(carrier.getArtifactId())
              .withRepresentation(rep(GraphQL_Schemas))));
    } else {
      return Answer.failed();
    }
  }
}
