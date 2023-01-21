package edu.mayo.kmdp.knowledgebase.introspectors.owl2.internal;

import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultArtifactId;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;

import edu.mayo.kmdp.util.DateTimeUtil;
import java.util.Date;
import java.util.Optional;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDFS;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Descriptive metadata for OWL ontology documents (artifacts)
 * <p>
 * Derives an artifact ID from the ontology asset ID, itself derived from the ontology IRI.
 * <p>
 * Supports optional annotations to control name ({@link RDFS#label}) versioning
 * ({@link OWL2#versionInfo}), creation ({@link DCTerms#created}) and last modification dates
 * ({@link DCTerms#modified})
 */
public class OntoArtfactIdentifiers {


  private final ResourceIdentifier owlArtifactId;
  private final Date owlCreationDate;
  private final Date owlLastModifiedDate;
  private final String owlVersionTag;

  private final String ontologyName;


  public OntoArtfactIdentifiers(OntoIdentifiers ontoIdentifiers, OWLOntology owl) {
    this.owlVersionTag = readAnnotation(owl, OWL2.versionInfo.getURI())
        .orElse(ontoIdentifiers.getVersionTag());
    this.ontologyName = readAnnotation(owl, RDFS.label.getURI())
        .orElse(null);
    this.owlCreationDate = readAnnotation(owl, DCTerms.created.getURI())
        .map(DateTimeUtil::parseDate).orElse(null);
    this.owlLastModifiedDate = readAnnotation(owl, DCTerms.modified.getURI())
        .map(DateTimeUtil::parseDate).orElse(null);

    owlArtifactId =
        defaultArtifactId(
            ontoIdentifiers.getAssetId(),
            OWL_2,
            Optional.ofNullable(getOwlVersionTag())
                .orElse(ontoIdentifiers.getVersionTag()));
  }


  private Optional<String> readAnnotation(OWLOntology owl, String annotationProp) {
    return owl.annotations()
        .filter(ann -> annotationProp.equals(ann.getProperty().getIRI().getIRIString()))
        .findFirst()
        .flatMap(ann -> ann.getValue().asLiteral())
        .map(OWLLiteral::getLiteral);
  }

  public ResourceIdentifier getOwlArtifactId() {
    return owlArtifactId;
  }

  public Date getOwlCreationDate() {
    return owlCreationDate;
  }

  public Date getOwlLastModifiedDate() {
    return owlLastModifiedDate;
  }

  public String getOwlVersionTag() {
    return owlVersionTag;
  }

  public String getOntologyName() {
    return ontologyName;
  }
}

