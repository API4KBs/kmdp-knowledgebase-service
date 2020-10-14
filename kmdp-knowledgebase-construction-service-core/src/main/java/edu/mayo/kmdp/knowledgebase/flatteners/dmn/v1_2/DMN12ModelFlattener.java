package edu.mayo.kmdp.knowledgebase.flatteners.dmn.v1_2;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.ofAst;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Knowledge_Resource_Flattening_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;

import edu.mayo.kmdp.knowledgebase.AbstractKnowledgeBaseOperator;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.URIUtil;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Named;
import javax.xml.bind.JAXBElement;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.CompositionalApiInternal;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.omg.spec.dmn._20180521.model.ObjectFactory;
import org.omg.spec.dmn._20180521.model.TDecision;
import org.omg.spec.dmn._20180521.model.TDefinitions;
import org.omg.spec.dmn._20180521.model.TInformationRequirement;
import org.omg.spec.dmn._20180521.model.TInputData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@KPOperation(Knowledge_Resource_Flattening_Task)
@KPSupport(DMN_1_2)
@Named
public class DMN12ModelFlattener
    extends AbstractKnowledgeBaseOperator
    implements CompositionalApiInternal._flattenArtifact {

  private static Logger logger = LoggerFactory.getLogger(DMN12ModelFlattener.class);

  public static final UUID id = UUID.fromString("df214307-c5a2-4700-983d-73ccf9dbf7f3");
  public static final String version = "1.0.0";

  private ObjectFactory factory = new ObjectFactory();

  public DMN12ModelFlattener() {
    super(SemanticIdentifier.newId(id,version));
  }

  public DMN12ModelFlattener(KnowledgeBaseApiInternal kbManager) {
    this();
    this.kbManager = kbManager;
  }

  public Answer<KnowledgeCarrier> flattenArtifact(KnowledgeCarrier carrier, UUID rootAssetId) {
    if (carrier instanceof CompositeKnowledgeCarrier) {
      CompositeKnowledgeCarrier ckc = (CompositeKnowledgeCarrier) carrier;
      KnowledgeCarrier rootCarrier = ckc.mainComponent();

      TDefinitions flatRoot = (TDefinitions) rootCarrier.as(TDefinitions.class)
          .orElseThrow().clone();

      Map<String,TDefinitions> comps = ckc.componentsAs(TDefinitions.class)
          .collect(Collectors.toMap(
              TDefinitions::getNamespace,
              dmn -> dmn
          ));

      mergeModels(flatRoot,comps);

      KnowledgeCarrier flat = ofAst(flatRoot,ckc.getRepresentation())
          .withAssetId(ckc.getAssetId())
          .withLabel(rootCarrier.getLabel());
      return Answer.of(flat);
    } else {
      return Answer.of(carrier);
    }
  }

  private void mergeModels(TDefinitions flatRoot, Map<String, TDefinitions> comps) {
    List<TDecision> rootDecisions = streamDecisions(flatRoot).collect(Collectors.toList());
    // copy in a list to prevent concurrent modification exceptions
    rootDecisions.forEach(decision -> ensureResolved(decision,flatRoot,comps));
  }


  private void ensureResolved(TDecision decision, TDefinitions flatRoot,
      Map<String, TDefinitions> comps) {
    decision.getInformationRequirement().forEach(
        infoReq -> ensureResolved(infoReq,flatRoot,comps));
  }

  private void ensureResolved(TInformationRequirement infoReq, TDefinitions flatRoot,
      Map<String, TDefinitions> comps) {
    if (infoReq.getRequiredDecision() != null) {
      URI ref = URI.create(infoReq.getRequiredDecision().getHref());
      if (! isInternal(ref)) {
        URI externalModelId = URIUtil.normalizeURI(ref);
        TDefinitions tgtModel = comps.get(externalModelId.toString());

        TDecision subDecision = resolveDecision(tgtModel,ref);

        flatRoot.getDrgElement()
            .add(factory.createDecision(subDecision));
        infoReq.getRequiredDecision().setHref("#_" + ref.getFragment());

        ensureResolved(subDecision, flatRoot, comps);
      }
    } else if (infoReq.getRequiredInput() != null) {
      URI ref = URI.create(infoReq.getRequiredInput().getHref());
      if (! isInternal(ref)) {
        URI externalModelId = URIUtil.normalizeURI(ref);
        TDefinitions tgtModel = comps.get(externalModelId.toString());

        TInputData input = resolveInput(tgtModel,ref);

        flatRoot.getDrgElement()
            .add(factory.createInputData(input));
        infoReq.getRequiredInput().setHref("#_" + ref.getFragment());
      }
    }
  }

  private TDecision resolveDecision(TDefinitions tgtModel, URI ref) {
    TDecision externalDec = streamDecisions(tgtModel)
        .filter(dec -> dec.getId().contains(ref.getFragment()))
        .findFirst()
        .orElseThrow();
    for (TInformationRequirement infoReq : externalDec.getInformationRequirement()) {
      if (infoReq.getRequiredInput() != null) {
        String href = infoReq.getRequiredInput().getHref();
        if (isInternal(href)) {
          infoReq.getRequiredInput().setHref(tgtModel.getNamespace() + href);
        }
      }
      if (infoReq.getRequiredDecision() != null) {
        String href = infoReq.getRequiredDecision().getHref();
        if (isInternal(href)) {
          infoReq.getRequiredDecision().setHref(tgtModel.getNamespace() + href);
        }
      }
    }
    return externalDec;
  }

  private TInputData resolveInput(TDefinitions tgtModel, URI ref) {
    return streamInputs(tgtModel)
        // ignore '#' and '_'
        .filter(input -> input.getId().contains(ref.getFragment()))
        .findFirst()
        .orElseThrow();
  }

  private boolean isInternal(URI ref) {
    // check scheme and path?
    return ref != null && isInternal(ref.toString());
  }
  private boolean isInternal(String ref) {
    return ref != null && ref.startsWith("#");
  }

  private Stream<TDecision> streamDecisions(TDefinitions dmn) {
    return dmn.getDrgElement().stream()
        .map(JAXBElement::getValue)
        .flatMap(StreamUtil.filterAs(TDecision.class));
  }

  private Stream<TInputData> streamInputs(TDefinitions dmn) {
    return dmn.getDrgElement().stream()
        .map(JAXBElement::getValue)
        .flatMap(StreamUtil.filterAs(TInputData.class));
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return DMN_1_2;
  }

}