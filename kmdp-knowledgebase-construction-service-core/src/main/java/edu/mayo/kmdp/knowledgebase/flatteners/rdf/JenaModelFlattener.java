package edu.mayo.kmdp.knowledgebase.flatteners.rdf;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.ofAst;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Knowledge_Resource_Flattening_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;

import edu.mayo.kmdp.knowledgebase.AbstractKnowledgeBaseOperator;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.inject.Named;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.CompositionalApiInternal;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@KPOperation(Knowledge_Resource_Flattening_Task)
@KPSupport(OWL_2)
@Named
public class JenaModelFlattener
    extends AbstractKnowledgeBaseOperator
    implements CompositionalApiInternal._flattenArtifact {

  private static Logger logger = LoggerFactory
      .getLogger(JenaModelFlattener.class);

  public static final UUID id = UUID.fromString("4ae3e34b-b850-430c-b288-b892e95596bd");
  public static final String version = "1.0.0";

  public JenaModelFlattener() {
    super(SemanticIdentifier.newId(id,version));
  }

  //TO/DO remove rootAssetId from signature
  public Answer<KnowledgeCarrier> flattenArtifact(KnowledgeCarrier carrier, UUID rootAssetId) {
    if (carrier instanceof CompositeKnowledgeCarrier) {
      CompositeKnowledgeCarrier ckc = (CompositeKnowledgeCarrier) carrier;
      KnowledgeCarrier rootCarrier = ckc.mainComponent();

      Model root = ModelFactory.createDefaultModel();
      addOntologyAxioms(root, ckc.mainComponentAs(OntModel.class));

//      root.(onto.getURI());

//      System.out.println(">>> FLATTENING " + rootCarrier.getLabel());
//      ckc.components()
//          .filter(c -> c.as(Model.class).get() != root)
//          .forEach(c -> System.out.println("Adding " + c.getLabel()));
//      System.out.println(">>>>>> FLATTENING " + rootCarrier.getLabel());

      ckc.componentsAs(Model.class)
          .forEach(m -> mergeModels(m,root));

      if (rootCarrier.getLabel().contains("test")) {
        File f = new File("C:\\Users\\M123110\\Projects\\MEA\\knew-terminology-content\\src\\main\\resources\\owl\\edu\\mayo\\kmdp\\vocabs\\LATEST\\CSO\\test.flat.owl");
        try {
          FileOutputStream fos = new FileOutputStream(f);
          root.write(fos);
        } catch (FileNotFoundException e) {
          e.printStackTrace();
        }
      }

      KnowledgeCarrier flat = ofAst(root,ckc.getRepresentation())
          .withAssetId(ckc.getAssetId())
          .withLabel(rootCarrier.getLabel());
      return Answer.of(flat);
    } else {
      return Answer.of(carrier);
    }
  }

  private void addOntologyAxioms(Model root, OntModel ontologyModel) {
    Ontology onto = ontologyModel.listOntologies().next();
    List<Statement> ontos =
    ontologyModel.listStatements()
        .filterKeep(st -> st.getSubject().getURI() != null && st.getSubject().getURI().equals(onto.getURI())).toList();
    root.add(ontos);
  }

  private void mergeModels(Model m, Model root) {
    Set<Resource> bannedResources = new HashSet<>();
    if (m instanceof OntModel) {
      OntModel om = (OntModel) m;
      om.listOntologies().forEachRemaining(bannedResources::add);
    }

    m.listStatements()
        .filterDrop(s -> bannedResources.contains(s.getSubject()))
        .filterDrop(s -> s.getPredicate().equals(RDFS.subClassOf) && s.getSubject().equals(s.getObject()))
        .filterDrop(s -> s.getPredicate().equals(RDFS.subClassOf) && s.getObject().equals(OWL.Thing))
        .filterDrop(s -> s.getPredicate().equals(RDF.type) && s.getObject().equals(RDFS.Resource))
        .forEachRemaining(root::add);

   }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return OWL_2;
  }
}