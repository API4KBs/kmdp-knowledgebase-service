package edu.mayo.kmdp.ops.tranx.mvf.owl2.cso.components;

import static edu.mayo.kmdp.ops.tranx.mvf.owl2.cso.CSOFabricator.CSO_NS;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.SCG;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;

import edu.mayo.kmdp.language.parsers.scg.SCGParSerializer;
import edu.mayo.kmdp.language.translators.scg.owl2.SCGtoOWLTranslator;
import edu.mayo.kmdp.registry.Registry;
import edu.mayo.kmdp.util.DateTimeUtil;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.Util;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.id.Term;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AsOWLClass;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.util.OWLAnnotationPropertyTransformer;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConceptPrototypeOWLBinder {

  static final Logger LOGGER = LoggerFactory.getLogger(ConceptPrototypeOWLBinder.class);

  private ConceptPrototypeOWLBinder() {
    // nothing to do
  }

  public static OWLOntology bind(
      ConceptPrototype prototype,
      OWLOntology cso,
      OWLOntology onto) {
    LOGGER.trace("... Binding {}", prototype.getSelf().getLabel());

    var mgr = OWLManager.createOWLOntologyManager();
    var df = mgr.getOWLDataFactory();

    return bindConcept(prototype, cso, df, onto);
  }

  private static OWLOntology bindConcept(
      ConceptPrototype p,
      OWLOntology cso,
      OWLDataFactory df, OWLOntology onto) {
    var declared = addDeclaration(p, onto, df);
    declared.ifPresent(
        owlClass -> addDefinition(p, owlClass, onto, cso, df));

    return onto;
  }

  private static Optional<OWLClass> addDeclaration(
      ConceptPrototype proto,
      OWLOntology onto,
      OWLDataFactory df) {

    var klass = df.getOWLClass(toClass(proto.getSelf(), df));
    var parent = Optional.ofNullable(proto.getParentClass())
        .map(pc -> toClass(pc, df))
        .map(df::getOWLClass);

    var concrete = df.getOWLClass(
        CSO_NS + "d01c1f14-50fa-3116-84c1-1131c75f4aa8");

    onto.addAxiom(df.getOWLDeclarationAxiom(klass));
    parent.ifPresent(pc -> {
      onto.addAxiom(df.getOWLDeclarationAxiom(pc));
      onto.addAxiom(df.getOWLSubClassOfAxiom(klass, pc));
    });
    onto.addAxiom(df.getOWLSubClassOfAxiom(klass, concrete));

    onto.addAxiom(df.getOWLAnnotationAssertionAxiom(
        df.getOWLAnnotationProperty(IRI.create("http://www.w3.org/2000/01/rdf-schema#label")),
        klass.getIRI(),
        df.getOWLLiteral(proto.getLabel(), "en")));
    onto.addAxiom(df.getOWLAnnotationAssertionAxiom(
        df.getOWLAnnotationProperty(IRI.create("http://www.w3.org/2004/02/skos/core#prefLabel")),
        klass.getIRI(),
        df.getOWLLiteral(proto.getLabel(), "en")));

    Optional.ofNullable(proto.getDefinition())
        .filter(Util::isNotEmpty)
        .ifPresent(def ->
            onto.addAxiom(df.getOWLAnnotationAssertionAxiom(
                df.getOWLAnnotationProperty(
                    IRI.create("http://www.w3.org/2004/02/skos/core#definition")),
                klass.getIRI(),
                df.getOWLLiteral(def, "en"))));
    onto.addAxiom(df.getOWLAnnotationAssertionAxiom(
        df.getOWLAnnotationProperty(IRI.create("http://purl.org/dc/terms/created")),
        klass.getIRI(),
        df.getOWLLiteral(DateTimeUtil.serializeAsDateTime(
            Instant.now().truncatedTo(ChronoUnit.DAYS)), OWL2Datatype.XSD_DATE_TIME)));

    if (proto.getParentClass() != null) {
      onto.addAxiom(df.getOWLAnnotationAssertionAxiom(
          df.getOWLAnnotationProperty(
              IRI.create("http://www.w3.org/2000/01/rdf-schema#isDefinedBy")),
          klass.getIRI(),
          IRI.create(proto.getModuleUri())));
    }

    return Optional.of(klass);
  }


  private static void addDefinition(ConceptPrototype p,
      OWLClass newClass,
      OWLOntology localOnto,
      @Nullable OWLOntology cso,
      OWLDataFactory df) {
    if (Util.isEmpty(p.getFocusPostCoorDefinition())) {
      return;
    }

    OWLClass focusParent = Optional.ofNullable(p.getFocusParent())
        .map(Term::getConceptId)
        .map(k -> df.getOWLClass(k.toString()))
        .map(AsOWLClass::asOWLClass)
        .orElseThrow(() -> new IllegalStateException(
            "No parent SNOMED class for focal concept of " + p.getSelf().getLabel()));

    var defPattern = Optional.ofNullable(cso)
        .flatMap(core ->
            findClassExpressionPattern(p, p.getParentClass(), focusParent, core, df));

    Optional<OWLOntology> focusDef = parseSnomedConcept(p.getFocusPostCoorDefinition());
    OWLClass focus = focusDef
        .flatMap(SCGtoOWLTranslator::tryGetPreCoordinated)
        .orElseGet(() -> df.getOWLClass(Registry.BASE_UUID_URN + p.getFocusPostCoorDefinition()));

    focusDef.ifPresent(
        ax -> localOnto.addAxioms(ax.axioms()));

    localOnto.addAxiom(df.getOWLDeclarationAxiom(focus));
    localOnto.addAxiom(df.getOWLDeclarationAxiom(focusParent));
    localOnto.addAxiom(df.getOWLSubClassOfAxiom(focus, focusParent));

    defPattern.ifPresent(owlClassExpression ->
        localOnto.addAxiom(
            new OWLAnnotationPropertyTransformer(Map.of(focusParent, focus), df)
                .transformObject(df.getOWLEquivalentClassesAxiom(newClass, owlClassExpression))));
  }


  private static Optional<OWLClassExpression> findClassExpressionPattern(
      ConceptPrototype p,
      Term csoPattern,
      OWLClass anchor,
      OWLOntology cso,
      OWLDataFactory df) {
    var patts = cso.generalClassAxioms()
        .flatMap(StreamUtil.filterAs(OWLSubClassOfAxiom.class))
        .filter(x -> toClass(csoPattern, df).equals(x.getSuperClass()))
        .filter(x -> hasScopeAnno(x, anchor))
        .collect(Collectors.toList());

    if (patts.size() != 1) {
      LOGGER.error("Unable to find a pattern for {} \n as a {} \t\t using http://snomed.info/id/{}",
          p.getSelf().getLabel(), p.getParentClass().getLabel(), p.getFocusParent());
      return Optional.empty();
    }
    return Optional.of(patts.get(0).getSubClass());
  }

  private static boolean hasScopeAnno(OWLSubClassOfAxiom x, OWLClass focalParent) {
    return x.annotations()
        .anyMatch(ann -> ann.getValue().equals(focalParent.getIRI()));
  }

  private static OWLClass toClass(Term t, OWLDataFactory df) {
    return df.getOWLClass(t.getResourceId().toString());
  }

  private static Optional<OWLOntology> parseSnomedConcept(String source) {
    var scgStr = Answer.of(AbstractCarrier.of(source)
        .withRepresentation(rep(SCG, TXT, Charset.defaultCharset())));
    var scgExpr = scgStr.flatMap(
        kc -> new SCGParSerializer().applyLift(kc, Abstract_Knowledge_Expression, codedRep(SCG),
            null));
    var owl = scgExpr.flatMap(
        kc -> new SCGtoOWLTranslator().applyTransrepresent(kc, codedRep(OWL_2), null));

    return owl.flatOpt(kc -> kc.as(OWLOntology.class))
        .getOptionalValue();
  }

}
