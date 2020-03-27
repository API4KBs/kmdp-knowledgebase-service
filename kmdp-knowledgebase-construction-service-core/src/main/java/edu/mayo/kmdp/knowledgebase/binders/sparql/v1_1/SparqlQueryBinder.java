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

import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.SPARQL_1_1;

import edu.mayo.kmdp.id.helper.DatatypeHelper;
import edu.mayo.kmdp.knowledgebase.v4.server.BindingApiInternal._bind;
import edu.mayo.kmdp.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.registry.Registry;
import java.net.URI;
import java.util.UUID;
import javax.inject.Inject;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.ResourceFactory;
import org.omg.spec.api4kp._1_0.Answer;
import org.omg.spec.api4kp._1_0.datatypes.Bindings;
import org.omg.spec.api4kp._1_0.id.IdentifierConstants;
import org.omg.spec.api4kp._1_0.id.Pointer;
import org.omg.spec.api4kp._1_0.id.SemanticIdentifier;
import org.omg.spec.api4kp._1_0.services.DocumentCarrier;
import org.omg.spec.api4kp._1_0.services.KPSupport;
import org.omg.spec.api4kp._1_0.services.KnowledgeBase;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;
import org.springframework.stereotype.Component;

@Component
@KPSupport(SPARQL_1_1)
public class SparqlQueryBinder implements _bind {

  public static final UUID OPERATOR_ID = UUID.fromString("73d9abfb-5192-45e8-8368-3ac7f5067e9b");

  @Inject
  KnowledgeBaseApiInternal kbManager;

  @Override
  public Answer<Pointer> bind(UUID kbaseId, String versionTag, Bindings bindings) {
    return kbManager.getKnowledgeBase(kbaseId, versionTag)
        .map(KnowledgeBase::getManifestation)
        .flatMap(paramQuery -> bind(paramQuery, bindings))
        .flatMap(boundCarrier ->
            kbManager.initKnowledgeBase(new KnowledgeAsset()
                .withAssetId(DatatypeHelper.toURIIdentifier(boundCarrier.getAssetId())))
                .flatMap(boundKbId ->
                    kbManager.populateKnowledgeBase(boundKbId.getUuid(), boundKbId.getVersionTag(), boundCarrier))
        );
  }

  public Answer<KnowledgeCarrier> bind(KnowledgeCarrier paramQuery, Bindings bindings) {
    ParameterizedSparqlString paramQ = (ParameterizedSparqlString) ((DocumentCarrier) paramQuery)
        .getStructuredExpression();
    bindings.forEach((key, value) -> {
      if (value instanceof URI) {
        paramQ.setParam(key, ResourceFactory.createResource(value.toString()));
      } else {
        paramQ.setLiteral(key, value.toString());
      }
    });
    return Answer.of(new DocumentCarrier()
        .withAssetId(SemanticIdentifier.newId(
            Registry.BASE_UUID_URN_URI,
            UUID.randomUUID(),
            IdentifierConstants.VERSION_ZERO))
        .withStructuredExpression(paramQ.asQuery())
        .withRepresentation(paramQuery.getRepresentation()));
  }

}
