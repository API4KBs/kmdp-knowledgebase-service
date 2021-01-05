package edu.mayo.kmdp.knowledgebase.flatteners.dmn.v1_2;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.ofAst;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Knowledge_Resource_Flattening_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;

import edu.mayo.kmdp.knowledgebase.AbstractKnowledgeBaseOperator;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.URIUtil;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.omg.spec.dmn._20180521.model.TBusinessKnowledgeModel;
import org.omg.spec.dmn._20180521.model.TDMNElementReference;
import org.omg.spec.dmn._20180521.model.TDRGElement;
import org.omg.spec.dmn._20180521.model.TDecision;
import org.omg.spec.dmn._20180521.model.TDecisionService;
import org.omg.spec.dmn._20180521.model.TDefinitions;
import org.omg.spec.dmn._20180521.model.TInformationRequirement;
import org.omg.spec.dmn._20180521.model.TInputData;
import org.omg.spec.dmn._20180521.model.TKnowledgeRequirement;
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
    super(SemanticIdentifier.newId(id, version));
  }

  public DMN12ModelFlattener(KnowledgeBaseApiInternal kbManager) {
    this();
    this.kbManager = kbManager;
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return DMN_1_2;
  }


  public Answer<KnowledgeCarrier> flattenArtifact(KnowledgeCarrier carrier, UUID rootAssetId) {
    if (carrier instanceof CompositeKnowledgeCarrier) {
      CompositeKnowledgeCarrier ckc = (CompositeKnowledgeCarrier) carrier;
      KnowledgeCarrier rootCarrier = ckc.mainComponent();

      TDefinitions flatRoot = (TDefinitions) rootCarrier.as(TDefinitions.class)
          .orElseThrow().clone();

      Map<String, TDefinitions> comps = ckc.componentsAs(TDefinitions.class)
          .collect(Collectors.toMap(
              TDefinitions::getNamespace,
              dmn -> dmn
          ));

      mergeModels(flatRoot, comps);

      KnowledgeCarrier flat = ofAst(flatRoot, ckc.getRepresentation())
          .withAssetId(rootCarrier.getAssetId())
          .withLabel(rootCarrier.getLabel());
      return Answer.of(flat);
    } else {
      return Answer.of(carrier);
    }
  }

  private void mergeModels(TDefinitions flatRoot, Map<String, TDefinitions> comps) {
    List<TDecision> rootDecisions = streamDecisions(flatRoot).collect(Collectors.toList());
    // copy in a list to prevent concurrent modification exceptions
    rootDecisions.forEach(decision -> ensureResolved(decision, flatRoot, comps));
  }


  private void ensureResolved(TDecision decision, TDefinitions flatRoot,
      Map<String, TDefinitions> comps) {
    decision.getInformationRequirement().forEach(
        infoReq -> ensureResolved(infoReq, flatRoot, comps));
    decision.getKnowledgeRequirement().forEach(
        knowReq -> ensureResolved(knowReq, flatRoot, comps));
  }

  private void ensureResolved(TKnowledgeRequirement knowReq, TDefinitions flatRoot,
      Map<String, TDefinitions> comps) {
    URI ref = URI.create(knowReq.getRequiredKnowledge().getHref());
    if (!isInternal(ref)) {
      URI externalModelId = URIUtil.normalizeURI(ref);
      TDefinitions tgtModel = comps.get(externalModelId.toString());

      Optional<TDecisionService> decisionServiceOpt = resolveDecisionService(tgtModel, ref);
      if (decisionServiceOpt.isPresent()) {
        TDecisionService decisionService = decisionServiceOpt.get();

        // do not carry over the hidden decisions
        decisionService.getEncapsulatedDecision().clear();

        flatRoot.getDrgElement()
            .add(factory.createDecisionService(decisionService));

        knowReq.getRequiredKnowledge().setHref(
            "#"
                + (ref.getFragment().startsWith("_") ? "" : "_")
                + ref.getFragment());

        List<String> inputRefs = new ArrayList<>();
        decisionService.getInputData().forEach(inputRef -> {
          if (!isInternal(inputRef.getHref())) {
            URI inputUriRef = URI.create(inputRef.getHref());
            String inputModelId = URIUtil.normalizeURIString(inputUriRef);
            TDefinitions extTgtModel = comps.get(inputModelId);
            TInputData input = resolveInput(extTgtModel, inputUriRef);

            flatRoot.getDrgElement()
                .add(factory.createInputData(input));
            inputRef.setHref("#"
                + (inputUriRef.getFragment().startsWith("_") ? "" : "_")
                + inputUriRef.getFragment());
            inputRefs.add(inputRef.getHref());
          }
        });
        List<String> inputDecRefs = new ArrayList<>();
        decisionService.getInputDecision().forEach(inputRef -> {
          if (!isInternal(inputRef.getHref())) {
            URI inputUriRef = URI.create(inputRef.getHref());
            String inputModelId = URIUtil.normalizeURIString(inputUriRef);
            TDefinitions extTgtModel = comps.get(inputModelId);
            TDecision input = resolveDecision(extTgtModel, inputUriRef);

            flatRoot.getDrgElement()
                .add(factory.createDecision(input));
            inputRef.setHref("#"
                + (inputUriRef.getFragment().startsWith("_") ? "" : "_")
                + inputUriRef.getFragment());
            inputDecRefs.add(inputRef.getHref());
          }
        });
        decisionService.getOutputDecision().forEach(outputRef -> {
          if (!isInternal(outputRef.getHref())) {
            URI outputUriRef = URI.create(outputRef.getHref());
            String outputModelId = URIUtil.normalizeURIString(outputUriRef);
            TDefinitions extTgtModel = comps.get(outputModelId);
            TDecision output = resolveDecision(extTgtModel, outputUriRef);

            flatRoot.getDrgElement()
                .add(factory.createDecision(output));
            outputRef.setHref("#"
                + (outputUriRef.getFragment().startsWith("_") ? "" : "_")
                + outputUriRef.getFragment());

            output.getInformationRequirement().clear();
            inputRefs.forEach(in ->
                output.getInformationRequirement().add(
                    new TInformationRequirement()
                    .withRequiredInput(new TDMNElementReference().withHref(in))
                ));
            inputDecRefs.forEach(in ->
                output.getInformationRequirement().add(
                    new TInformationRequirement()
                    .withRequiredDecision(new TDMNElementReference().withHref(in))
                ));
          }
        });
      }
    } else {
      TBusinessKnowledgeModel tbkm = streamBKM(flatRoot)
          .filter(bkm -> bkm.getId().contains(ref.getFragment()))
          .findFirst()
          .orElseThrow(() -> new IllegalStateException("Unable to resolve " + ref.getFragment()));
      tbkm.getKnowledgeRequirement().stream()
          .filter(kreq -> kreq.getRequiredKnowledge() != null)
          .forEach(kreq -> ensureResolved(kreq,flatRoot,comps));
    }
  }

  private void ensureResolved(TInformationRequirement infoReq, TDefinitions flatRoot,
      Map<String, TDefinitions> comps) {
    if (infoReq.getRequiredDecision() != null) {
      URI ref = URI.create(infoReq.getRequiredDecision().getHref());
      if (!isInternal(ref)) {
        URI externalModelId = URIUtil.normalizeURI(ref);
        TDefinitions tgtModel = comps.get(externalModelId.toString());

        TDecision subDecision = resolveDecision(tgtModel, ref);

        flatRoot.getDrgElement()
            .add(factory.createDecision(subDecision));
        infoReq.getRequiredDecision().setHref(
            "#"
                + (ref.getFragment().startsWith("_") ? "" : "_")
                + ref.getFragment());

        ensureResolved(subDecision, flatRoot, comps);
      }
    } else if (infoReq.getRequiredInput() != null) {
      URI ref = URI.create(infoReq.getRequiredInput().getHref());
      if (!isInternal(ref)) {
        URI externalModelId = URIUtil.normalizeURI(ref);
        TDefinitions tgtModel = comps.get(externalModelId.toString());

        TInputData input = resolveInput(tgtModel, ref);

        flatRoot.getDrgElement()
            .add(factory.createInputData(input));
        infoReq.getRequiredInput().setHref("#_" + ref.getFragment());
      }
    }
  }

  private TDecision resolveDecision(TDefinitions externalModel, URI ref) {
    TDecision externalDec = streamDecisions(externalModel)
        .filter(dec -> dec.getId().contains(ref.getFragment()))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Unable to resolve " + ref.getFragment()));

    externalDec = (TDecision) externalDec.clone();

    for (TInformationRequirement infoReq : externalDec.getInformationRequirement()) {
      if (infoReq.getRequiredInput() != null) {
        String href = infoReq.getRequiredInput().getHref();
        if (isInternal(href)) {
          infoReq.getRequiredInput().setHref(externalModel.getNamespace() + href);
        }
      }
      if (infoReq.getRequiredDecision() != null) {
        String href = infoReq.getRequiredDecision().getHref();
        if (isInternal(href)) {
          infoReq.getRequiredDecision().setHref(externalModel.getNamespace() + href);
        }
      }
    }
    return externalDec;
  }


  private Optional<TDecisionService> resolveDecisionService(TDefinitions externalModel, URI ref) {
    Optional<TDecisionService> externalDecService = streamDecisionServices(externalModel)
        .filter(dec -> dec.getId().contains(ref.getFragment()))
        .findFirst();
    if (externalDecService.isEmpty()) {
      return Optional.empty();
    }
    TDecisionService clonedService = (TDecisionService) externalDecService.get().clone();

    clonedService.getInputData().forEach(input -> {
      if (isInternal(input.getHref())) {
        input.setHref(externalModel.getNamespace() + input.getHref());
      }
    });
    clonedService.getInputDecision().forEach(input -> {
      if (isInternal(input.getHref())) {
        input.setHref(externalModel.getNamespace() + input.getHref());
      }
    });
    clonedService.getEncapsulatedDecision().forEach(hidden -> {
      if (isInternal(hidden.getHref())) {
        hidden.setHref(externalModel.getNamespace() + hidden.getHref());
      }
    });
    clonedService.getOutputDecision().forEach(output -> {
      if (isInternal(output.getHref())) {
        output.setHref(externalModel.getNamespace() + output.getHref());
      }
    });

    return Optional.of(clonedService);
  }


  private TInputData resolveInput(TDefinitions externalModel, URI ref) {
    TInputData externalInput = streamInputs(externalModel)
        // ignore '#' and '_'
        .filter(input -> input.getId().contains(ref.getFragment()))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Unable to resolve " + ref.getFragment()));

    return (TInputData) externalInput.clone();
  }


  private boolean isInternal(URI ref) {
    // check scheme and path?
    return ref != null && isInternal(ref.toString());
  }

  private boolean isInternal(String ref) {
    return ref != null && ref.startsWith("#");
  }

  private Stream<TBusinessKnowledgeModel> streamBKM(TDefinitions dmn) {
    return streamDRG(dmn, TBusinessKnowledgeModel.class);
  }

  private Stream<TDecision> streamDecisions(TDefinitions dmn) {
    return streamDRG(dmn, TDecision.class);
  }

  private Stream<TInputData> streamInputs(TDefinitions dmn) {
    return streamDRG(dmn, TInputData.class);
  }

  private Stream<TDecisionService> streamDecisionServices(TDefinitions dmn) {
    return streamDRG(dmn, TDecisionService.class);
  }

  private <T extends TDRGElement> Stream<T> streamDRG(TDefinitions dmn, Class<T> drgType) {
    return dmn.getDrgElement().stream()
        .map(JAXBElement::getValue)
        .flatMap(StreamUtil.filterAs(drgType));
  }


}
