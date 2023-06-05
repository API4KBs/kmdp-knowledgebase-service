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
package edu.mayo.kmdp.language.parsers.dmn.v1_1;

import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newVersionId;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Lifting_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Lowering_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_1;

import edu.mayo.kmdp.language.DeserializeApiOperator;
import edu.mayo.kmdp.language.parsers.XMLBasedLanguageParser;
import edu.mayo.kmdp.util.URIUtil;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import javax.inject.Named;
import org.omg.spec.api4kp._20200801.id.IdentifierConstants;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevel;
import org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries;
import org.omg.spec.dmn._20151101.dmn.ObjectFactory;
import org.omg.spec.dmn._20151101.dmn.TDefinitions;

@Named
@KPOperation(Lifting_Task)
@KPOperation(Lowering_Task)
@KPSupport(DMN_1_1)
public class DMN11Parser extends XMLBasedLanguageParser<TDefinitions>
    implements DeserializeApiOperator {

  public static final UUID id = UUID.fromString("ceff7f77-3b3d-4fb2-824d-cea3d2301efe");
  public static final String version = "1.0.0";

  public DMN11Parser() {
    this(Collections.emptyList());
  }

  public DMN11Parser(Collection<XMLBasedLanguageParser<?>> subParsers) {
    super(subParsers);
    setId(SemanticIdentifier.newId(id,version));
    configRootClass(TDefinitions.class);
    this.mapper = new ObjectFactory()::createDefinitions;
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return DMN_1_1;
  }

  @Override
  public KnowledgeCarrier newVerticalCarrier(
      KnowledgeCarrier source,
      ParsingLevel targetLevel,
      SyntacticRepresentation targetRepresentation,
      Object targetArtifact) {
    KnowledgeCarrier kc = super.newVerticalCarrier(
        source,
        targetLevel,
        targetRepresentation,
        targetArtifact);
    if (ParsingLevelSeries.Abstract_Knowledge_Expression.sameAs(targetLevel)) {
      TDefinitions model = (TDefinitions) targetArtifact;
      if (kc.getArtifactId() == null) {
        String ns = model.getNamespace();
        kc.withArtifactId(newVersionId(URIUtil.normalizeURI(URI.create(ns)), IdentifierConstants.VERSION_LATEST));
      }
      if (kc.getLabel() == null) {
        kc.withLabel(model.getName());
      }
    }
    return kc;
  }
}