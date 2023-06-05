/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.mayo.kmdp.language.parsers.owl2;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Lifting_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Lowering_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries.RDF_XML_Syntax;
import static org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries.Turtle;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;

import edu.mayo.kmdp.language.parsers.rdf.JenaRdfParser;
import edu.mayo.kmdp.terms.util.JenaUtil;
import edu.mayo.kmdp.util.NameUtils;
import edu.mayo.kmdp.util.Util;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import javax.inject.Named;
import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.ontology.ProfileRegistry;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPComponent;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormat;

@Named
@KPOperation(Lowering_Task)
@KPOperation(Lifting_Task)
@KPSupport(OWL_2)
@KPComponent(implementation = "jena-owl")
public class JenaOwlParser extends JenaRdfParser {

  public static final UUID id = UUID.fromString("1e912b84-f08f-4d9c-a2ae-30f6f09a27a4");
  public static final String version = "1.0.0";
  public static final Object CFG_PROCESS_IMPORTS = "processImports";

  public JenaOwlParser() {
    setId(SemanticIdentifier.newId(id,version));
  }

  @Override
  protected List<SyntacticRepresentation> getSupportedRepresentations() {
    return Arrays.asList(
        rep(OWL_2, Turtle, TXT),
        rep(OWL_2, Turtle, TXT, Charset.defaultCharset()),
        rep(OWL_2, RDF_XML_Syntax, XML_1_1),
        rep(OWL_2, RDF_XML_Syntax, XML_1_1, Charset.defaultCharset())
    );
  }

  @Override
  protected SerializationFormat getDefaultFormat() {
    return TXT;
  }

  /**
   * Lifts a concrete expression (String) into an abstract expression (abstract syntax tree)
   *
   * @param carrier A string carrier
   * @return An abstract syntax tree carrier
   */
  @Override
  public Optional<KnowledgeCarrier> innerParse(KnowledgeCarrier carrier, Properties properties) {
    SyntacticRepresentation tgtRep =
        getTargetLiftRepresentation(carrier.getRepresentation(), Abstract_Knowledge_Expression);
    Model model = readModel(
        carrier.asString().orElseThrow(UnsupportedOperationException::new),
        carrier.getRepresentation(),
        properties);
    String name = detectOntologyName(model);
    KnowledgeCarrier kc =
        newVerticalCarrier(carrier,
            Abstract_Knowledge_Expression,
            tgtRep,
            model)
        .withAssetId(
            Optional.ofNullable(carrier.getAssetId())
                .orElseGet(() -> detectOntologyID(model, name)))
        .withLabel(name);

    return Optional.of(kc);
  }

  private ResourceIdentifier detectOntologyID(Model model, String name) {
    URI uri = JenaUtil.detectOntologyIRI(model).map(URI::create).orElseThrow();
    URI vuri = JenaUtil.detectVersionIRI(model,uri.toString()).map(URI::create).orElseThrow();
    return SemanticIdentifier.newVersionId(uri,vuri, name);
  }

  private String detectOntologyName(Model model) {
    if (model instanceof OntModel) {
      OntModel om = (OntModel) model;
      List<Ontology> onts = om.listOntologies().toList();
      if (! onts.isEmpty()) {
        String name = onts.get(0).getLabel(null);
        if (!Util.isEmpty(name)) {
          return name;
        }
      }
    }
    String uri = JenaUtil.detectOntologyIRI(model).map(URI::create).map(URI::toString).orElseThrow();
    if (uri.endsWith("/")) {
      uri = uri.substring(0, uri.length() - 1);
    }
    return NameUtils.getTrailingPart(uri);
  }

  @Override
  protected Model readModel(String str, SyntacticRepresentation from, Properties properties) {
    OntDocumentManager odm = new OntDocumentManager();
    Optional.ofNullable(properties)
        .flatMap(p -> Optional.ofNullable(p.get(CFG_PROCESS_IMPORTS)))
        .map(v -> Boolean.valueOf(v.toString()))
        .ifPresent(odm::setProcessImports);
    OntModelSpec spec = new OntModelSpec( null, odm, null, ProfileRegistry.OWL_LANG );
    OntModel m = ModelFactory.createOntologyModel(spec, null);
    return m.read(new ByteArrayInputStream(str.getBytes()), null, toJenaLangCode(from));
  }

}
