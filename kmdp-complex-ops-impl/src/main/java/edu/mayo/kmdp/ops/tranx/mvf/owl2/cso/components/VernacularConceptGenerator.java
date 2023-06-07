package edu.mayo.kmdp.ops.tranx.mvf.owl2.cso.components;

import static edu.mayo.kmdp.ops.tranx.mvf.owl2.cso.CSOFabricator.CSV_NS;
import static edu.mayo.kmdp.ops.tranx.mvf.owl2.cso.components.CSOOntologyHelper.initOntology;
import static edu.mayo.kmdp.ops.tranx.mvf.owl2.cso.components.CSOOntologyHelper.loadCSO;
import static edu.mayo.kmdp.ops.tranx.mvf.owl2.cso.components.CSOOntologyHelper.newCSOOntologyManager;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultArtifactId;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.MVF_1_0;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;

import edu.mayo.kmdp.language.parsers.mvf.v1_0.MVFParser;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedTransformDirect;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal._applyLift;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.mvf._20220702.mvf.MVFDictionary;
import org.semanticweb.owlapi.model.OWLOntology;
import org.snomed.SCTHelper;

/**
 * Tool that generates CSO classes from a 'template'
 */

public class VernacularConceptGenerator implements _applyNamedTransformDirect {

  public static final UUID OPERATOR_ID = UUID.fromString("7091b355-078e-4fa2-a70c-348485b70353");

  private final _applyLift parser = new MVFParser();

  public VernacularConceptGenerator() {
    //
  }

  @Override
  public Answer<KnowledgeCarrier> applyNamedTransformDirect(
      UUID operatorId,
      KnowledgeCarrier artifact,
      String xParams) {
    var models = Answer.of(artifact)
        .flatMap(kc ->
            parser.applyLift(kc, Abstract_Knowledge_Expression.getTag(), codedRep(MVF_1_0), null));
    var concepts = models
        .map(ckc -> ckc.components()
            .flatMap(this::extractCSOConcepts)
            .collect(Collectors.toSet()));
    var onto = concepts
        .flatMap(protos -> toOntology(protos, xParams));
    return onto.map(o -> carry(o, artifact.getAssetId()));
  }

  private Stream<ConceptPrototype> extractCSOConcepts(KnowledgeCarrier kc) {
    var as = kc.as(MVFDictionary.class);
    if (as.isEmpty()) {
      return Stream.empty();
    }
    var dict = as.get();
    var sctVocab = dict.getVocabulary().stream()
        .filter(voc -> voc.getUri().equals(SCTHelper.SNOMED))
        .findFirst();
    var csvVocab = dict.getVocabulary().stream()
        .filter(voc -> voc.getUri().equals(CSV_NS.toString()))
        .findFirst();
    if (sctVocab.isEmpty() || csvVocab.isEmpty()) {
      return Stream.empty();
    }
    return new MVFtoPrototype()
        .getDefinitions(dict, csvVocab.get(), sctVocab.get())
        .stream();
  }

  private Answer<OWLOntology> toOntology(Set<ConceptPrototype> protos, String xParams) {
    try {
      var om = newCSOOntologyManager();
      var cso = loadCSO(xParams, om);
      var ontology = initOntology(om);

      protos.forEach(p ->
          ConceptPrototypeOWLBinder.bind(p, cso.orElse(null), ontology));
      return Answer.of(ontology);
    } catch (Exception e) {
      return Answer.failed(e);
    }
  }

  private KnowledgeCarrier carry(OWLOntology onto, ResourceIdentifier assetId) {
    return AbstractCarrier.ofAst(onto)
        .withRepresentation(rep(OWL_2))
        .withAssetId(assetId)
        .withArtifactId(defaultArtifactId(assetId, OWL_2));
  }


}