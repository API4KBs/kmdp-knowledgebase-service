package edu.mayo.kmdp.knowledgebase.simplifiers.fhir.stu3;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Simplification_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;

import com.fasterxml.jackson.databind.JsonNode;
import edu.mayo.kmdp.knowledgebase.AbstractKnowledgeBaseOperator;
import edu.mayo.kmdp.knowledgebase.simplifiers.fhir.stu3.components.EmptyBundleTrimmer;
import edu.mayo.kmdp.knowledgebase.simplifiers.fhir.stu3.components.MetadataTrimmer;
import edu.mayo.kmdp.knowledgebase.simplifiers.fhir.stu3.components.ProvenanceTrimmer;
import edu.mayo.kmdp.knowledgebase.simplifiers.fhir.stu3.components.RxNormCodingOptimizer;
import edu.mayo.kmdp.language.common.fhir.stu3.FHIRVisitor;
import edu.mayo.kmdp.language.parsers.owl2.OWLParser;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Resource;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal._applyLift;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@KPOperation(Simplification_Task)
public class FHIRSimplifier extends AbstractKnowledgeBaseOperator
    //implements _applyRedact
{

  private static final Logger logger = LoggerFactory.getLogger(FHIRSimplifier.class);

  public static final UUID id
      = UUID.fromString("2b4b166f-033d-455f-92b3-fb212113c35c");
  public static final String version = "1.0.0";

  @Autowired
  @KPSupport(FHIR_STU3)
  private _applyLift parser;

  public FHIRSimplifier() {
    super(SemanticIdentifier.newId(id, version));
    this.parser = new OWLParser();
  }

  public FHIRSimplifier(KnowledgeBaseApiInternal kbManager) {
    this(kbManager, new OWLParser());
  }

  public FHIRSimplifier(KnowledgeBaseApiInternal kbManager, _applyLift parser) {
    this();
    this.kbManager = kbManager;
    this.parser = parser;
  }

  public Answer<KnowledgeCarrier> applyRedact(UUID kbaseId, String versionTag, String xParams) {
    CompositeKnowledgeCarrier ckc = (CompositeKnowledgeCarrier)
        kbManager.getKnowledgeBaseManifestation(kbaseId, versionTag).get();

    Parameters source = ckc.getComponent().get(0).as(Parameters.class).orElseThrow()
        .copy();
    JsonNode drugData = ckc.getComponent().get(1).as(JsonNode.class).orElseThrow();

    List<Consumer<Resource>> trimmers = Arrays.asList(
        new MetadataTrimmer(),
        new EmptyBundleTrimmer(),
        new RxNormCodingOptimizer(drugData),
        new ProvenanceTrimmer());

    FHIRVisitor.traverse(source,true)
        .forEach(res -> trimmers.forEach(t -> t.accept(res)));

    return Answer.of(AbstractCarrier.ofAst(source,rep(FHIR_STU3)));
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return FHIR_STU3;
  }
}
