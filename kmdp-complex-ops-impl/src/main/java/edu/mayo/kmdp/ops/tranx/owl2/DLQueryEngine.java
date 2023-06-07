package edu.mayo.kmdp.ops.tranx.owl2;

import static java.util.stream.Collectors.toSet;

import java.util.Collections;
import java.util.Set;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxEditorParser;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.BidirectionalShortFormProvider;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.ShortFormProvider;

public class DLQueryEngine {

  private final OWLReasoner reasoner;
  private final DLQueryParser parser;

  /**
   * Constructs a DLQueryEngine. This will answer "DL queries" using the specified reasoner. A short
   * form provider specifies how entities are rendered.
   *
   * @param reasoner          The reasoner to be used for answering the queries.
   * @param shortFormProvider A short form provider.
   */
  public DLQueryEngine(OWLReasoner reasoner, ShortFormProvider shortFormProvider) {
    this.reasoner = reasoner;
    OWLOntology rootOntology = reasoner.getRootOntology();
    parser = new DLQueryParser(rootOntology, shortFormProvider);
  }

  /**
   * Gets the superclasses of a class expression parsed from a string.
   *
   * @param classExpressionString The string from which the class expression will be parsed.
   * @param direct                Specifies whether direct superclasses should be returned or not.
   * @return The superclasses of the specified class expression
   */
  public Set<OWLClass> getSuperClasses(String classExpressionString, boolean direct) {
    if (classExpressionString.trim().length() == 0) {
      return Collections.emptySet();
    }
    OWLClassExpression classExpression = parser
        .parseClassExpression(classExpressionString);
    NodeSet<OWLClass> superClasses = reasoner
        .getSuperClasses(classExpression, direct);
    return superClasses.entities().collect(toSet());
  }

  /**
   * Gets the equivalent classes of a class expression parsed from a string.
   *
   * @param classExpressionString The string from which the class expression will be parsed.
   * @return The equivalent classes of the specified class expression
   */
  public Set<OWLClass> getEquivalentClasses(String classExpressionString) {
    if (classExpressionString.trim().length() == 0) {
      return Collections.emptySet();
    }
    OWLClassExpression classExpression = parser
        .parseClassExpression(classExpressionString);
    Node<OWLClass> equivalentClasses = reasoner.getEquivalentClasses(classExpression);
    Set<OWLClass> result;
    if (classExpression.isAnonymous()) {
      result = equivalentClasses.entities().collect(toSet());
    } else {
      result = equivalentClasses.getEntitiesMinus(classExpression.asOWLClass());
    }
    return result;
  }

  /**
   * Gets the subclasses of a class expression parsed from a string.
   *
   * @param classExpressionString The string from which the class expression will be parsed.
   * @param direct                Specifies whether direct subclasses should be returned or not.
   * @return The subclasses of the specified class expression
   */
  public Set<OWLClass> getSubClasses(String classExpressionString, boolean direct) {
    if (classExpressionString.trim().length() == 0) {
      return Collections.emptySet();
    }
    OWLClassExpression classExpression = parser
        .parseClassExpression(classExpressionString);
    NodeSet<OWLClass> subClasses = reasoner.getSubClasses(classExpression, direct);
    return subClasses.entities().collect(toSet());
  }

  /**
   * Gets the instances of a class expression parsed from a string.
   *
   * @param classExpressionString The string from which the class expression will be parsed.
   * @param direct                Specifies whether direct instances should be returned or not.
   * @return The instances of the specified class expression
   */
  public Set<OWLNamedIndividual> getInstances(
      String classExpressionString,
      boolean direct)  {
    if (classExpressionString.trim().length() == 0) {
      return Collections.emptySet();
    }
    OWLClassExpression classExpression = parser
        .parseClassExpression(classExpressionString);
    NodeSet<OWLNamedIndividual> individuals = reasoner.getInstances(classExpression,
        direct);
    return individuals.entities().collect(toSet());
  }


  static class DLQueryParser {

    private final OWLOntology rootOntology;
    private final BidirectionalShortFormProvider bidiShortFormProvider;

    /**
     * Constructs a DLQueryParser using the specified ontology and short form provider to map entity
     * IRIs to short names.
     *
     * @param rootOntology      The root ontology. This essentially provides the domain vocabulary
     *                          for the query.
     * @param shortFormProvider A short form provider to be used for mapping back and forth between
     *                          entities and their short names (renderings).
     */
    public DLQueryParser(OWLOntology rootOntology, ShortFormProvider shortFormProvider) {
      this.rootOntology = rootOntology;
      OWLOntologyManager manager = rootOntology.getOWLOntologyManager();
      Set<OWLOntology> importsClosure = rootOntology.importsClosure().collect(toSet());
      // Create a bidirectional short form provider to do the actual mapping.
      // It will generate names using the input
      // short form provider.
      bidiShortFormProvider = new BidirectionalShortFormProviderAdapter(manager,
          importsClosure, shortFormProvider);
    }

    /**
     * Parses a class expression string to obtain a class expression.
     *
     * @param classExpressionString The class expression string
     * @return The corresponding class expression
     */
    public OWLClassExpression parseClassExpression(String classExpressionString) {
      OWLDataFactory dataFactory = rootOntology.getOWLOntologyManager()
          .getOWLDataFactory();
      // Set up the real parser
      var parser = new ManchesterOWLSyntaxEditorParser(
          dataFactory, classExpressionString);
      parser.setDefaultOntology(rootOntology);
      OWLEntityChecker entityChecker = new ShortFormEntityChecker(bidiShortFormProvider);
      parser.setOWLEntityChecker(entityChecker);
      return parser.parseClassExpression();
    }
  }
}


