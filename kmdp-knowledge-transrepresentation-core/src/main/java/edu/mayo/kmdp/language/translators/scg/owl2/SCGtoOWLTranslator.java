package edu.mayo.kmdp.language.translators.scg.owl2;

import static java.util.stream.Stream.concat;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.id.IdentifierConstants.SNOMED_BASE_URI;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.SCG;

import edu.mayo.kmdp.language.translators.AbstractSimpleTranslator;
import edu.mayo.kmdp.registry.Registry;
import edu.mayo.kmdp.util.Util;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AsOWLClass;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.snomed.languages.scg.domain.model.SCGAttribute;
import org.snomed.languages.scg.domain.model.SCGAttributeGroup;
import org.snomed.languages.scg.domain.model.SCGExpression;
import org.snomed.languages.scg.domain.model.SCGTerm;

/**
 * Translator based on the canonical mapping between the SNOMED Compositional Grammar (SCG) and the
 * Ontology Web Language (OWL2) EL profile.
 * <p>
 * Given a {@link SCGExpression}, creates an anonymous Ontology with an equivalentClass axiom, where
 * a randomly named class is associated to a {@link OWLClassExpression} derived from the original
 * SCG expression. Adds declarations for the involved {@link OWLClass} and {@link OWLObjectProperty}
 * including labels if detected.
 */
public class SCGtoOWLTranslator extends
    AbstractSimpleTranslator<SCGExpression, OWLOntology> {

  /**
   * Well known IRI for rdfs:label
   */
  protected static final IRI LABEL = IRI.create("http://www.w3.org/2000/01/rdf-schema#label");

  /**
   * Extracts the {@link OWLClassExpression} that derives from a {@link SCGExpression} from an
   * {@link OWLOntology} created by this translator
   *
   * @param onto the {@link OWLOntology} that contains the anonymous {@link OWLClassExpression}
   * @return the {@link OWLClassExpression}, optionally
   */
  public static Optional<OWLClassExpression> tryGetPostCoordinated(OWLOntology onto) {
    return onto.axioms(AxiomType.EQUIVALENT_CLASSES).findFirst()
        .flatMap(eq -> eq.classExpressions()
            .filter(x -> x.isAnonymous()
                || !x.asOWLClass().getIRI().toString().startsWith(Registry.UUID_URN))
            .findFirst());
  }

  public static Optional<OWLClass> tryGetPreCoordinated(OWLOntology onto) {
    return onto.axioms(AxiomType.EQUIVALENT_CLASSES).findFirst()
        .flatMap(eq -> eq.classExpressions()
            .filter(x -> !x.isAnonymous())
            .map(AsOWLClass::asOWLClass)
            .findFirst())
        .or(() -> onto.classesInSignature().findFirst());
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return SCG;
  }

  @Override
  public List<SyntacticRepresentation> getFrom() {
    return List.of(rep(SCG));
  }

  @Override
  public List<SyntacticRepresentation> getInto() {
    return List.of(rep(OWL_2));
  }

  @Override
  public KnowledgeRepresentationLanguage getTargetLanguage() {
    return OWL_2;
  }

  /**
   * @param assetId       The AssetId of the source expression - usually anonymous for this
   *                      Translator
   * @param srcArtifactId The ArtifactId of the source expression - usually anonymous for this
   *                      Translator
   * @param scgExpression The {@link SCGExpression} to translate
   * @param srcRep        The source representation, assumed to be SCG
   * @param tgtRep        The target representation, assumed to be OWL2
   * @param config        Additional parameters (ignored)
   * @return An {@link OWLOntology} that wraps the derived Class Expression
   */
  @Override
  protected Optional<OWLOntology> transformAst(
      ResourceIdentifier assetId, ResourceIdentifier srcArtifactId,
      SCGExpression scgExpression,
      SyntacticRepresentation srcRep,
      SyntacticRepresentation tgtRep,
      Properties config) {
    try {
      var om = OWLManager.createOWLOntologyManager();
      var df = om.getOWLDataFactory();
      var onto = om.createOntology();

      var klassXp = translate(scgExpression, onto, df);
      if (klassXp.isAnonymous()) {
        var anonKlass = getAnonymous(scgExpression, df);
        onto.addAxiom(df.getOWLEquivalentClassesAxiom(anonKlass, klassXp));
      } else {
        onto.addAxioms(df.getOWLDeclarationAxiom(klassXp.asOWLClass()));
      }

      return Optional.of(onto);
    } catch (Exception e) {
      e.printStackTrace();
      return Optional.empty();
    }
  }

  /**
   * Returns an anonymously named {@link OWLClass}, to be equated to the translated OWL class
   * expression.
   * <p>
   * In an {@link OWLOntology}, {@link OWLClassExpression} must be associated to another
   * {@link OWLEntity} We offer an {@link OWLClass} which represents the pre-coordinated version of
   * the anonymous concept defined by the original {@link SCGExpression} or, equivalently, by the
   * translated {@link OWLClassExpression}. The IRI of this pre-coordinated class is based off the
   * urn:uuid namespace, and uses a UUID derived from the hash of the SCGExpression String in normal
   * form (i.e. no labels included)
   *
   * @param scgExpression The source {@link SCGExpression}
   * @param df            the {@link OWLDataFactory}
   * @return the pre-coordinated {@link OWLClass}
   */
  private OWLClass getAnonymous(SCGExpression scgExpression, OWLDataFactory df) {
    return df.getOWLClass(IRI.create(Registry.UUID_URN + Util.uuid(scgExpression.toString())));
  }

  /**
   * Translates a {@link SCGExpression} into its equivalent {@link OWLClassExpression}.
   * <p>
   * The named focal concepts are mapped to the
   * {@link org.semanticweb.owlapi.model.OWLObjectIntersectionOf} of {@link OWLClass}. Attributes
   * and Attribute Groups are mapped to the intersection of
   * {@link org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom} Class Expressions. Finally, foci
   * and attribute (groups) are further combined as an
   * {@link org.semanticweb.owlapi.model.OWLObjectIntersectionOf}
   * <p>
   * Note that, in the process, it is necessary to declare the classes and properties used in the
   * post-coordinated expression. The {@link OWLOntology} 'onto' context is used as an accumulator
   * for this purpose
   *
   * @param xpr  the source {@link SCGExpression}
   * @param onto the {@link OWLOntology} collector, which accumulates the
   *             {@link org.semanticweb.owlapi.model.OWLDeclarationAxiom} of the primitive classes
   *             and properties
   * @param df   the {@link OWLDataFactory}
   * @return the translated {@link OWLClassExpression}
   */
  private OWLClassExpression translate(SCGExpression xpr, OWLOntology onto, OWLDataFactory df) {
    var s1 = xpr.getFocusConcepts().stream()
        .map(t -> toClass(t, onto, df));
    var s2 = xpr.getAttributes().stream()
        .map(a -> translate(a, onto, df));
    var s3 = xpr.getAttributeGroups().stream()
        .map(ag -> translate(ag, onto, df));
    var operands = concat(s1, concat(s2, s3));
    var and = df.getOWLObjectIntersectionOf(operands);
    if (and.operands().count() == 1) {
      return and.operands().findFirst().orElseThrow();
    } else {
      return and;
    }
  }

  /**
   * Translates a {@link SCGAttribute} into its equivalent
   * {@link org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom} component.
   *
   * @param att  the source {@link SCGAttribute}
   * @param onto the {@link OWLOntology} collector, which accumulates the
   *             {@link org.semanticweb.owlapi.model.OWLDeclarationAxiom} of the primitive classes
   *             and properties
   * @param df   the {@link OWLDataFactory}
   * @return the translated {@link OWLClassExpression}
   */
  private OWLClassExpression translate(SCGAttribute att, OWLOntology onto, OWLDataFactory df) {
    var attr = att.getProperty();
    var prop = toProp(attr, onto, df);

    OWLClassExpression range;
    if (att.getAttributeValue().isNested()) {
      range = translate(att.getAttributeValue().getConceptExpression(), onto, df);
    } else {
      var value = att.getAttributeValue().getConcept();
      range = toClass(value, onto, df);
    }

    return df.getOWLObjectSomeValuesFrom(prop, range);
  }


  /**
   * Translates a {@link SCGAttributeGroup} into its equivalent
   * {@link org.semanticweb.owlapi.model.OWLObjectIntersectionOf} component. Translates each
   * {@link SCGAttribute} in the group recursively
   *
   * @param ag   the source {@link SCGAttributeGroup}
   * @param onto the {@link OWLOntology} collector, which accumulates the
   *             {@link org.semanticweb.owlapi.model.OWLDeclarationAxiom} of the primitive classes
   *             and properties
   * @param df   the {@link OWLDataFactory}
   * @return the translated {@link OWLClassExpression}
   */
  private OWLClassExpression translate(SCGAttributeGroup ag, OWLOntology onto, OWLDataFactory df) {
    var s = ag.getAttributes().stream()
        .map(att -> translate(att, onto, df));
    return df.getOWLObjectIntersectionOf(s);
  }

  /**
   * Translates a Concept's {@link SCGTerm} into its equivalent {@link OWLClass}
   *
   * @param t    the source {@link SCGTerm}
   * @param onto the {@link OWLOntology} collector, which accumulates the
   *             {@link org.semanticweb.owlapi.model.OWLDeclarationAxiom} of the primitive classes
   *             and properties
   * @param df   the {@link OWLDataFactory}
   * @return the translated {@link OWLClass}
   */
  protected OWLClass toClass(SCGTerm t, OWLOntology onto, OWLDataFactory df) {
    var klass = df.getOWLClass(SNOMED_BASE_URI + t.getConceptId());
    onto.add(df.getOWLDeclarationAxiom(klass));
    addLabel(klass, t.getLabel(), onto, df);
    return klass;
  }

  /**
   * Translates an Attribute's {@link SCGTerm} into its equivalent {@link OWLObjectProperty}
   *
   * @param t    the source {@link SCGTerm}
   * @param onto the {@link OWLOntology} collector, which accumulates the
   *             {@link org.semanticweb.owlapi.model.OWLDeclarationAxiom} of the primitive classes
   *             and properties
   * @param df   the {@link OWLDataFactory}
   * @return the translated {@link OWLObjectProperty}
   */
  protected OWLObjectProperty toProp(SCGTerm t, OWLOntology onto, OWLDataFactory df) {
    var prop = df.getOWLObjectProperty(SNOMED_BASE_URI + t.getConceptId());
    onto.add(df.getOWLDeclarationAxiom(prop));
    addLabel(prop, t.getLabel(), onto, df);
    return prop;
  }

  /**
   * Adds the rdfs:label {@link org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom} to an
   * {@link OWLClass} or {@link OWLObjectProperty}
   *
   * @param subject the {@link OWLEntity} to be annotated
   * @param label   the label (note that this is not guaranteed to be a SNOMED preferred label)
   * @param onto    the {@link OWLOntology} collector, which accumulates the
   *                {@link org.semanticweb.owlapi.model.OWLDeclarationAxiom} of the primitive
   *                classes and properties
   * @param df      the {@link OWLDataFactory}
   */
  private void addLabel(OWLEntity subject, String label, OWLOntology onto, OWLDataFactory df) {
    if (Util.isNotEmpty(label)) {
      onto.addAxiom(df.getOWLAnnotationAssertionAxiom(
          df.getOWLAnnotationProperty(LABEL),
          subject.getIRI(),
          df.getOWLLiteral(label)
      ));
    }
  }

}
