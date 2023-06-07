package edu.mayo.kmdp.ops.tranx.owl2;

import static java.util.Arrays.asList;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.AbstractCompositeCarrier.ofUniformAnonymousComposite;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.SPARQL_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;

import edu.mayo.kmdp.knowledgebase.KnowledgeBaseProvider;
import edu.mayo.kmdp.knowledgebase.constructors.JenaOwlImportConstructor;
import edu.mayo.kmdp.knowledgebase.extractors.rdf.SimplePivotExtractor;
import edu.mayo.kmdp.knowledgebase.flatteners.rdf.JenaModelFlattener;
import edu.mayo.kmdp.knowledgebase.selectors.skos.JenaSKOSSelector;
import edu.mayo.kmdp.knowledgebase.selectors.sparql.v1_1.SparqlSelector;
import edu.mayo.kmdp.language.parsers.owl2.JenaOwlParser;
import edu.mayo.kmdp.language.translators.owl2.OWLtoSKOSTranscreator;
import edu.mayo.kmdp.terms.mireot.MireotExtractor;
import edu.mayo.kmdp.terms.skosifier.Owl2SkosConfig.OWLtoSKOSTxParams;
import edu.mayo.kmdp.util.PropertiesUtil;
import java.util.Properties;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.AbstractCompositeCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal._getKnowledgeBaseStructure;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal._applyLift;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.TransxionApiInternal._applyTransrepresent;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.services.KnowledgeBase;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder;
import org.omg.spec.api4kp._20200801.taxonomy.lexicon._20210401.Lexicon;

public class ComplexOwl2SKOSTransrepresentator implements _applyTransrepresent {

  // parser
  _applyLift parser = new JenaOwlParser();

  // tranx
  _applyTransrepresent skosifier = new OWLtoSKOSTranscreator();

  JenaModelFlattener jenaFlattener = new JenaModelFlattener();

  // knowledgebase
  KnowledgeBaseApiInternal kbManager = new KnowledgeBaseProvider(null)
      .withNamedSelector(SparqlSelector::new)
      .withNamedSelector(JenaSKOSSelector::new)
      .withNamedFlattener(jenaFlattener)
      .withNamedExtractor(SimplePivotExtractor::new);
  _getKnowledgeBaseStructure constructor = new JenaOwlImportConstructor(kbManager);

  KnowledgeCarrier selectQuery = AbstractCarrier.ofTree(
      MireotExtractor.MIREOT,
      rep(SPARQL_1_1, TXT));


  @Override
  public Answer<KnowledgeCarrier> applyTransrepresent(KnowledgeCarrier sourceArtifact,
      String xAccept, String xParams) {

    final Properties allprops = PropertiesUtil.parseProperties(xParams);

    Pointer kbRef = newKB();

    sourceArtifact.components()
        .map(c -> parse(c, xParams))
        .collect(Answer.toList())
        .forEach(KnowledgeCarrier.class, owl -> addToKnowledgeBase(kbRef, owl));

    addStructureToKB(kbRef);

    return kbManager.getKnowledgeBaseComponents(kbRef.getUuid(), kbRef.getVersionTag())
        .flatList(Pointer.class, compPtr -> skosify(kbRef, compPtr, allprops))
        .map(AbstractCompositeCarrier::ofUniformAggregate);
  }


  public Answer<KnowledgeCarrier> skosify(Pointer kBaseRef, Pointer ontoPtr, Properties props) {
    Answer<Pointer> comp = extractOntologyComponent(kBaseRef, ontoPtr)
        .flatMap(this::flattenKB);

    // process Ontologies that contain SKOS A-box Concepts
    Answer<KnowledgeCarrier> nativeSkos = comp
        .flatMap(kbComp -> selectSKOS(kBaseRef, kbComp, props)
            .map(skosKB -> retainOntologyMetadata(skosKB, kbComp)));

    // process Ontologies that contain OWL T-box Concepts that need to be skos-ified
    Answer<KnowledgeCarrier> transformedSkos = comp
        .flatMap(o -> mireotOntology(o, props))
        .flatMap(ptr -> owlToSkos(ptr, getSkosifierProperties(props, ontoPtr)));

    if (transformedSkos.isSuccess() && nativeSkos.isFailure()) {
      // scenario: pure OWL (e.g. CSO ontologies)
      return transformedSkos;
    } else if (nativeSkos.isSuccess() && transformedSkos.isFailure()) {
      // scenario: pure SKOS (e.g. NLP concept ontology)
      return nativeSkos;
    } else {
      // scenario: hybrid - should be avoided if possible
      return transformedSkos.flatMap(x1 ->
          nativeSkos.flatMap(x2 -> jenaFlattener.flattenArtifact(
              ofUniformAnonymousComposite(x1.getAssetId(), asList(x1, x2)),
              x1.getAssetId().getUuid(),
              null)));
    }
  }

  private KnowledgeCarrier retainOntologyMetadata(KnowledgeCarrier skosKB, Pointer kbComp) {
    var kb = kbManager.getKnowledgeBase(kbComp.getUuid(), kbComp.getVersionTag())
        .map(KnowledgeBase::getManifestation)
        .flatOpt(kc -> kc.as(Model.class))
        .orElseThrow();
    var m = skosKB.as(Model.class).orElseThrow();

    kb.listSubjectsWithProperty(RDF.type, OWL2.Ontology)
        .forEachRemaining(o -> {
          kb.listStatements(o, RDF.type, (RDFNode) null).forEachRemaining(m::add);
          kb.listStatements(o, OWL2.versionIRI, (RDFNode) null).forEachRemaining(m::add);
          kb.listStatements(o, RDFS.label, (String) null).forEachRemaining(m::add);
          m.add(o, OWL2.imports, ResourceFactory.createResource(SKOS.getURI()));
        });

    return skosKB;
  }

  private Answer<KnowledgeCarrier> selectSKOS(
      Pointer kBaseRef, Pointer onto, Properties props) {
    return kbManager
        .namedSelect(onto.getUuid(), onto.getVersionTag(),
            JenaSKOSSelector.id, null, PropertiesUtil.serializeProps(props))
        .flatMap(ptr -> kbManager
            .getKnowledgeBaseManifestation(ptr.getUuid(), ptr.getVersionTag()))
        .filter(kc -> kc.as(Model.class).map(m -> ! m.isEmpty()).orElse(false));
  }


  protected Answer<KnowledgeCarrier> owlToSkos(Pointer ptr, Properties cfg) {
    return kbManager
        .getKnowledgeBaseManifestation(ptr.getUuid(), ptr.getVersionTag())
        .filter(kc -> kc.as(Model.class).map(m -> ! m.isEmpty()).orElse(false))
        .flatMap(kc -> skosifier.applyTransrepresent(
            kc,
            ModelMIMECoder.encode(rep(OWL_2, Lexicon.SKOS)),
            PropertiesUtil.serializeProps(cfg))
        );
  }

  protected Answer<Pointer> mireotOntology(Pointer flatPtr, Properties props) {
    return kbManager
        .namedSelect(flatPtr.getUuid(), flatPtr.getVersionTag(),
            SparqlSelector.id, selectQuery, PropertiesUtil.serializeProps(props));
  }

  protected Answer<Pointer> flattenKB(Pointer pivotKB) {
    return kbManager.flatten(pivotKB.getUuid(), pivotKB.getVersionTag(), null);
  }

  protected Answer<Pointer> extractOntologyComponent(Pointer kBaseRef, Pointer ontoPtr) {
    return kbManager.extract(kBaseRef.getUuid(), kBaseRef.getVersionTag(), ontoPtr.getUuid(), null);
  }


  protected void addStructureToKB(Pointer kbRef) {
    constructor.getKnowledgeBaseStructure(kbRef.getUuid(), kbRef.getVersionTag(), null)
        .flatMap(struct -> kbManager
            .setKnowledgeBaseStructure(kbRef.getUuid(), kbRef.getVersionTag(), struct));
  }

  protected Pointer newKB() {
    return kbManager.initKnowledgeBase().orElseThrow(IllegalStateException::new);
  }

  protected Answer<KnowledgeCarrier> parse(KnowledgeCarrier binaryOntology, String cfg) {
    return parser.applyLift(binaryOntology,
        Abstract_Knowledge_Expression.getTag(),
        ModelMIMECoder.encode(rep(OWL_2)),
        cfg);
  }

  protected void addToKnowledgeBase(Pointer kbRef, KnowledgeCarrier parsedOntology) {
    kbManager.populateKnowledgeBase(kbRef.getUuid(), kbRef.getVersionTag(), parsedOntology);
  }


  protected Properties getSkosifierProperties(Properties props, Pointer ptr) {
    String name = ptr.getName();
    props.put(OWLtoSKOSTxParams.SCHEME_NAME.getName(), name);
    props.put(OWLtoSKOSTxParams.TOP_CONCEPT_NAME.getName(), name);
    return props;
  }


}
