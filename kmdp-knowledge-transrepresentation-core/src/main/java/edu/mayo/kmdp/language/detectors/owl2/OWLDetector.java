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
package edu.mayo.kmdp.language.detectors.owl2;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Language_Information_Detection_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krprofile.KnowledgeRepresentationLanguageProfileSeries.OWL2_DL;
import static org.omg.spec.api4kp._20200801.taxonomy.krprofile.KnowledgeRepresentationLanguageProfileSeries.OWL2_EL;
import static org.omg.spec.api4kp._20200801.taxonomy.krprofile.KnowledgeRepresentationLanguageProfileSeries.OWL2_Full;
import static org.omg.spec.api4kp._20200801.taxonomy.krprofile.KnowledgeRepresentationLanguageProfileSeries.OWL2_QL;
import static org.omg.spec.api4kp._20200801.taxonomy.krprofile.KnowledgeRepresentationLanguageProfileSeries.OWL2_RL;
import static org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries.OWL_Functional_Syntax;
import static org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries.OWL_Manchester_Syntax;
import static org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries.OWL_XML_Serialization;
import static org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries.RDF_XML_Syntax;
import static org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries.Turtle;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.asEnum;

import edu.mayo.kmdp.language.DetectApiOperator;
import edu.mayo.kmdp.language.common.owl2.CatalogBasedIRIMapper;
import edu.mayo.kmdp.language.detectors.AbstractLanguageDetector;
import edu.mayo.kmdp.language.detectors.owl2.OWLDetectorConfig.DetectorParams;
import edu.mayo.kmdp.util.Util;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.inject.Named;
import org.apache.jena.vocabulary.SKOS;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormat;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.omg.spec.api4kp._20200801.taxonomy.krprofile.KnowledgeRepresentationLanguageProfile;
import org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerialization;
import org.omg.spec.api4kp._20200801.taxonomy.lexicon.Lexicon;
import org.omg.spec.api4kp._20200801.taxonomy.lexicon.LexiconSeries;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.formats.ManchesterSyntaxDocumentFormat;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.profiles.OWL2DLProfile;
import org.semanticweb.owlapi.profiles.OWL2ELProfile;
import org.semanticweb.owlapi.profiles.OWL2QLProfile;
import org.semanticweb.owlapi.profiles.OWL2RLProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@KPOperation(Language_Information_Detection_Task)
@KPSupport(OWL_2)
public class OWLDetector
  extends AbstractLanguageDetector
    implements DetectApiOperator {

  static UUID id = UUID.randomUUID();
  static String version = "1.0.0";

  protected static final Logger logger = LoggerFactory.getLogger(OWLDetector.class);

  public OWLDetector() {
    setId(SemanticIdentifier.newId(id,version));
  }

  @Override
  public List<SyntacticRepresentation> getInto() {
    return getSupportedRepresentations();
  }

  @Override
  public List<SyntacticRepresentation> getSupportedRepresentations() {
    return Arrays.asList(
        // TODO need all permutations
        rep(OWL_2),
        rep(OWL_2,OWL_XML_Serialization,XML_1_1),
        rep(OWL_2,RDF_XML_Syntax,XML_1_1),
        rep(OWL_2,Turtle,TXT));
  }



  protected Optional<OWLOntology> asOWL(KnowledgeCarrier sourceArtifact) {
    switch (asEnum(sourceArtifact.getLevel())) {
      case Abstract_Knowledge_Expression:
        Object expr = sourceArtifact.getExpression();
        return (expr instanceof OWLOntology) ? Optional.of((OWLOntology) expr) : Optional.empty();
      case Encoded_Knowledge_Expression:
      case Concrete_Knowledge_Expression:
        return sourceArtifact.asBinary()
            .map(ByteArrayInputStream::new)
            .flatMap(this::loadOntology);
      default:
        return Optional.empty();
    }
  }


  @Override
  protected Optional<SyntacticRepresentation> detectBinary(byte[] bytes) {
    return loadOntology(new ByteArrayInputStream(bytes))
        .map(o -> rep(
            OWL_2,
            detectProfile(o),
            detectSerialization(o),
            detectFormat(o),
            Charset.defaultCharset(),
            Encodings.DEFAULT,
            detectLexicon(o)));
  }

  @Override
  protected Optional<SyntacticRepresentation> detectString(String string) {
    return detectBinary(string.getBytes());
  }

  @Override
  protected Optional<SyntacticRepresentation> detectTree(Object parseTree) {
    return detectAST(parseTree);
  }

  @Override
  protected Optional<SyntacticRepresentation> detectAST(Object ast) {
    return ast instanceof OWLOntology
        ? Optional.of(rep(OWL_2,detectProfile((OWLOntology) ast)))
        : Optional.empty();
  }


  private SerializationFormat detectFormat(OWLOntology o) {
    OWLDocumentFormat format = o.getFormat();
    if (format instanceof RDFXMLDocumentFormat) {
      return XML_1_1;
    } else if (format instanceof ManchesterSyntaxDocumentFormat) {
      return TXT;
    } else if (format instanceof OWLXMLDocumentFormat) {
      return XML_1_1;
    } else if (format instanceof TurtleDocumentFormat) {
      return TXT;
    } else if (format instanceof FunctionalSyntaxDocumentFormat) {
      return TXT;
    }
    return null;
  }

  private KnowledgeRepresentationLanguageSerialization detectSerialization(OWLOntology o) {
    OWLDocumentFormat format = o.getFormat();
    if (format instanceof RDFXMLDocumentFormat) {
      return RDF_XML_Syntax;
    } else if (format instanceof ManchesterSyntaxDocumentFormat) {
      return OWL_Manchester_Syntax;
    } else if (format instanceof OWLXMLDocumentFormat) {
      return OWL_XML_Serialization;
    } else if (format instanceof TurtleDocumentFormat) {
      return Turtle;
    } else if (format instanceof FunctionalSyntaxDocumentFormat) {
      return OWL_Functional_Syntax;
    }
    return null;
  }

  private Collection<Lexicon> detectLexicon(OWLOntology o) {
    Set<Lexicon> lexica = new HashSet<>();
    if (o.importsDeclarations().anyMatch(decl -> decl.getIRI().equals(IRI.create(SKOS.uri)))) {
      lexica.add(LexiconSeries.SKOS);
    }
    // eventually check for more...
    return lexica;
  }

  protected KnowledgeRepresentationLanguageProfile detectProfile(OWLOntology o) {
    if (new OWL2RLProfile().checkOntology(o).isInProfile()) {
      return OWL2_RL;
    }
    if (new OWL2QLProfile().checkOntology(o).isInProfile()) {
      return OWL2_QL;
    }
    if (new OWL2ELProfile().checkOntology(o).isInProfile()) {
      return OWL2_EL;
    }
    if (new OWL2DLProfile().checkOntology(o).isInProfile()) {
      return OWL2_DL;
    }
    return OWL2_Full;
  }

  protected Optional<OWLOntology> loadOntology(InputStream is) {
    return loadOntology(is, new OWLDetectorConfig());
  }

  protected Optional<OWLOntology> loadOntology(InputStream is, OWLDetectorConfig params) {
    try {
      OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
      OWLOntologyLoaderConfiguration loaderCfg = new OWLOntologyLoaderConfiguration() {
        @Override
        public boolean isIgnoredImport(IRI iri) {
          return true;
        }

        @Override
        public MissingImportHandlingStrategy getMissingImportHandlingStrategy() {
          return MissingImportHandlingStrategy.SILENT;
        }
      };

      manager.setOntologyLoaderConfiguration(loaderCfg);

      configureCatalog(manager, params);

      if (is.markSupported()) {
        is.mark(Integer.MAX_VALUE);
      }

      Optional<OWLOntology> onto = Optional
          .ofNullable(manager.loadOntologyFromOntologyDocument(is))
          .filter(o -> o.getOntologyID().getOntologyIRI().isPresent());

      if (is.markSupported()) {
        is.reset();
      }
      return onto;
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  private void configureCatalog(OWLOntologyManager manager, OWLDetectorConfig params) {
    String catalog = params.getTyped(DetectorParams.CATALOG);
    if (!Util.isEmpty(catalog)) {
      manager.setIRIMappers(Collections.singleton(
          new CatalogBasedIRIMapper(catalog)
      ));
    }

  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return OWL_2;
  }


}
