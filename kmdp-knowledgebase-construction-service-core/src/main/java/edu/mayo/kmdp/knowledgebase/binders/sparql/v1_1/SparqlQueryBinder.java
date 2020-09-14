/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.mayo.kmdp.knowledgebase.binders.sparql.v1_1;


import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.SPARQL_1_1;

import edu.mayo.kmdp.knowledgebase.AbstractKnowledgeBaseOperator;
import edu.mayo.kmdp.registry.Registry;
import java.net.URI;
import java.util.UUID;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.ResourceFactory;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedBind;
import org.omg.spec.api4kp._20200801.datatypes.Bindings;
import org.omg.spec.api4kp._20200801.id.IdentifierConstants;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPComponent;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.springframework.stereotype.Component;

@Component
@KPComponent
@KPSupport(SPARQL_1_1)
public class SparqlQueryBinder
    extends AbstractKnowledgeBaseOperator
    implements _applyNamedBind {

  public static final UUID id = UUID.fromString("73d9abfb-5192-45e8-8368-3ac7f5067e9b");
  public static final String version = "1.0.0";

  private KnowledgeBaseApiInternal kbManager;

  public SparqlQueryBinder() {
    super(SemanticIdentifier.newId(id,version));
  }

  protected SparqlQueryBinder(KnowledgeBaseApiInternal kbManager) {
    this();
    this.kbManager = kbManager;
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return SPARQL_1_1;
  }

  @Override
  public Answer<KnowledgeCarrier> applyNamedBind(UUID operatorId, Bindings bindings, UUID kbaseId,
      String versionTag, String xParams) {
    if (!getOperatorId().getUuid().equals(operatorId)) {
      return Answer.failed();
    }
    return kbManager.getKnowledgeBaseManifestation(kbaseId,versionTag)
        .flatMap(paramQuery -> bind(paramQuery, bindings));
  }

  public Answer<KnowledgeCarrier> bind(KnowledgeCarrier paramQuery, Bindings bindings) {
    ParameterizedSparqlString paramQ = paramQuery.as(ParameterizedSparqlString.class)
        .orElseThrow(IllegalArgumentException::new);
    bindings.forEach((key, value) -> {
      if (value instanceof URI) {
        paramQ.setParam(key, ResourceFactory.createResource(value.toString()));
      } else {
        paramQ.setLiteral(key, value.toString());
      }
    });
    return Answer.of(
        AbstractCarrier.ofAst(paramQ.asQuery())
            .withAssetId(SemanticIdentifier.newId(
                Registry.BASE_UUID_URN_URI,
                UUID.randomUUID(),
                IdentifierConstants.VERSION_ZERO))
            .withRepresentation(paramQuery.getRepresentation()));
  }

}
