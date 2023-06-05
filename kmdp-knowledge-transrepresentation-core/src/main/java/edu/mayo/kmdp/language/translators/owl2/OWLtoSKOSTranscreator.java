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
package edu.mayo.kmdp.language.translators.owl2;

import static java.util.Collections.singletonList;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Transcreation_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries.RDF_XML_Syntax;
import static org.omg.spec.api4kp._20200801.taxonomy.lexicon.LexiconSeries.SKOS;

import edu.mayo.kmdp.language.translators.AbstractSimpleTranslator;
import edu.mayo.kmdp.terms.skosifier.Owl2SkosConfig;
import edu.mayo.kmdp.terms.skosifier.Owl2SkosConverter;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import javax.inject.Named;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;

@Named
@KPOperation(Transcreation_Task)
@KPSupport(OWL_2)
public class OWLtoSKOSTranscreator extends AbstractSimpleTranslator<Model,Model> {

  public static final UUID id = UUID.fromString("57869ee0-304c-40a4-8759-40ea667c328d");
  public static final String version = "1.0.0";

  public OWLtoSKOSTranscreator() {
    setId(SemanticIdentifier.newId(id,version));
  }

  @Override
  public List<SyntacticRepresentation> getFrom() {
    return Arrays.asList(
        rep(OWL_2),
        rep(OWL_2, RDF_XML_Syntax),
        rep(OWL_2, RDF_XML_Syntax, XML_1_1, Charset.defaultCharset()),
        rep(OWL_2, RDF_XML_Syntax, XML_1_1,Charset.defaultCharset(), Encodings.DEFAULT));
  }

  @Override
  public List<SyntacticRepresentation> getInto() {
    return singletonList(rep(OWL_2,
        RDF_XML_Syntax,
        XML_1_1,
        Charset.defaultCharset())
        .withLexicon(SKOS));
  }

  protected Optional<Model> transformAst(
      ResourceIdentifier assetId, ResourceIdentifier srcArtifactId,
      Model model,
      SyntacticRepresentation srcRep,
      SyntacticRepresentation tgtRep,
      Properties config) {
    return new Owl2SkosConverter()
        .apply(model, new Owl2SkosConfig().from(config));
  }

  protected Optional<Model> transformTree(
      ResourceIdentifier assetId, ResourceIdentifier srcArtifactId,
      Object expression, SyntacticRepresentation srcRep,
      SyntacticRepresentation tgtRep,
      Properties config) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected Optional<Model> transformString(ResourceIdentifier assetId,
      ResourceIdentifier srcArtifactId, String str,
      SyntacticRepresentation srcRep,
      SyntacticRepresentation tgtRep, Properties config) {
    return doTransform(new ByteArrayInputStream(str.getBytes()), config);
  }

  @Override
  protected Optional<Model> transformBinary(ResourceIdentifier assetId,
      ResourceIdentifier srcArtifactId, byte[] bytes,
      SyntacticRepresentation srcRep,
      SyntacticRepresentation tgtRep, Properties config) {
    return doTransform(new ByteArrayInputStream(bytes), config);
  }

  protected Optional<Model> doTransform(InputStream is, Properties params) {
    Model model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
    model = model.read(is, null);

    return new Owl2SkosConverter()
        .apply(model, (Owl2SkosConfig) params);
  }

  protected Owl2SkosConfig readProperties(String properties) {
    return new Owl2SkosConfig().from(super.readProperties(properties));
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return OWL_2;
  }

  @Override
  public KnowledgeRepresentationLanguage getTargetLanguage() {
    return OWL_2;
  }

}
