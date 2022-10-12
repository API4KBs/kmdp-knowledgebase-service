package edu.mayo.kmdp.knowledgebase.flatteners.dmn.v1_2;

import static edu.mayo.kmdp.language.common.dmn.v1_2.DMN12Utils.findBKM;
import static edu.mayo.kmdp.language.common.dmn.v1_2.DMN12Utils.findDecision;
import static edu.mayo.kmdp.language.common.dmn.v1_2.DMN12Utils.findDecisionService;
import static edu.mayo.kmdp.language.common.dmn.v1_2.DMN12Utils.findInput;
import static edu.mayo.kmdp.language.common.dmn.v1_2.DMN12Utils.findKnowledgeSource;
import static edu.mayo.kmdp.language.common.dmn.v1_2.DMN12Utils.idToLocalRef;
import static edu.mayo.kmdp.language.common.dmn.v1_2.DMN12Utils.streamBKM;
import static edu.mayo.kmdp.language.common.dmn.v1_2.DMN12Utils.streamDecisions;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.ofAst;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Knowledge_Resource_Flattening_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;

import edu.mayo.kmdp.knowledgebase.AbstractKnowledgeBaseOperator;
import edu.mayo.kmdp.util.URIUtil;
import edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Named;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.Explainer;
import org.omg.spec.api4kp._20200801.ServerSideException;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.CompositionalApiInternal;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.omg.spec.dmn._20180521.model.ObjectFactory;
import org.omg.spec.dmn._20180521.model.TAuthorityRequirement;
import org.omg.spec.dmn._20180521.model.TBusinessKnowledgeModel;
import org.omg.spec.dmn._20180521.model.TDMNElementReference;
import org.omg.spec.dmn._20180521.model.TDecision;
import org.omg.spec.dmn._20180521.model.TDecisionService;
import org.omg.spec.dmn._20180521.model.TDefinitions;
import org.omg.spec.dmn._20180521.model.TInformationRequirement;
import org.omg.spec.dmn._20180521.model.TInputData;
import org.omg.spec.dmn._20180521.model.TKnowledgeRequirement;
import org.omg.spec.dmn._20180521.model.TKnowledgeSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zalando.problem.AbstractThrowableProblem;

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


  @Override
  public Answer<KnowledgeCarrier> flattenArtifact(KnowledgeCarrier carrier, UUID rootAssetId,
      String params) {
    if (carrier instanceof CompositeKnowledgeCarrier) {
      CompositeKnowledgeCarrier ckc = (CompositeKnowledgeCarrier) carrier;
      KnowledgeCarrier rootCarrier = ckc.mainComponent();

      TDefinitions flatRoot = (TDefinitions) rootCarrier.as(TDefinitions.class)
          .orElseThrow(() ->
              new IllegalStateException(
                  "FATAL - Expected DMN TDefinitions, found "
                      + rootCarrier.getExpression().getClass()))
          .clone();

      Map<String, TDefinitions> comps = ckc.componentsAs(TDefinitions.class)
          .collect(Collectors.toMap(
              TDefinitions::getNamespace,
              dmn -> dmn
          ));

      try {
        mergeModels(flatRoot, comps);
      } catch (AbstractThrowableProblem problem) {
        return Answer.failed(problem);
      }

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
    decision.getAuthorityRequirement().forEach(
        authReq -> ensureResolved(authReq, flatRoot, comps));
  }

  private void ensureResolved(TKnowledgeRequirement knowReq, TDefinitions flatRoot,
      Map<String, TDefinitions> comps) {
    URI ref = URI.create(knowReq.getRequiredKnowledge().getHref());
    if (!isInternal(ref)) {
      URI externalModelId = URIUtil.normalizeURI(ref);
      TDefinitions tgtModel = comps.get(externalModelId.toString());

      Optional<TDecisionService> decisionServiceOpt = resolveDecisionService(tgtModel, ref);
      decisionServiceOpt.ifPresent(
          tDecisionService -> resolveDecisionServiceDependencies(
              tDecisionService, tgtModel, knowReq, ref, flatRoot, comps));
    } else {
      TBusinessKnowledgeModel tbkm = streamBKM(flatRoot)
          .filter(bkm -> bkm.getId().contains(ref.getFragment()))
          .findFirst()
          .orElseThrow(() -> new IllegalStateException(
              String.format("Unable to resolve fragment %s as dmn:BKM within flattened model %s",
                  ref.getFragment(), flatRoot.getName())));
      tbkm.getKnowledgeRequirement().stream()
          .filter(kreq -> kreq.getRequiredKnowledge() != null)
          .forEach(kreq -> ensureResolved(kreq, flatRoot, comps));
      tbkm.getKnowledgeRequirement();
    }
  }

  private void resolveDecisionServiceDependencies(TDecisionService decisionService,
      TDefinitions tgtModel, TKnowledgeRequirement knowReq,
      URI ref, TDefinitions flatRoot,
      Map<String, TDefinitions> comps) {

    Set<TKnowledgeRequirement> encapsulatedKRs = new HashSet<>();
    decisionService.getEncapsulatedDecision().forEach(enc -> {
      TDecision innerDecision = resolveDecision(tgtModel, URI.create(enc.getHref()));
      encapsulatedKRs.addAll(innerDecision.getKnowledgeRequirement());
    });

    // do not carry over the hidden decisions
    decisionService.getEncapsulatedDecision().clear();
    addToFlat(flatRoot, decisionService);
    knowReq.getRequiredKnowledge().setHref(idToLocalRef(ref));

    List<String> inputRefs = new ArrayList<>();
    decisionService.getInputData().forEach(inputRef -> {
      if (!isInternal(inputRef.getHref())) {
        URI inputUriRef = URI.create(inputRef.getHref());
        String inputModelId = URIUtil.normalizeURIString(inputUriRef);
        TDefinitions extTgtModel = comps.get(inputModelId);
        TInputData input = resolveInput(extTgtModel, inputUriRef);

        addToFlat(flatRoot, input);
        inputRef.setHref(idToLocalRef(inputUriRef));
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

        addToFlat(flatRoot, input);
        inputRef.setHref(idToLocalRef(inputUriRef));
        inputDecRefs.add(inputRef.getHref());
      }
    });
    decisionService.getOutputDecision().forEach(outputRef -> {
      if (!isInternal(outputRef.getHref())) {
        remapDecisionServiceOutputs(
            outputRef, inputRefs, inputDecRefs, encapsulatedKRs, flatRoot, comps);
      }
    });
  }

  private void remapDecisionServiceOutputs(
      TDMNElementReference outputRef,
      List<String> inputRefs,
      List<String> inputDecRefs,
      Set<TKnowledgeRequirement> encapsulatedKRs,
      TDefinitions flatRoot,
      Map<String, TDefinitions> comps) {
    URI outputUriRef = URI.create(outputRef.getHref());
    String outputModelId = URIUtil.normalizeURIString(outputUriRef);
    TDefinitions extTgtModel = comps.get(outputModelId);

    TDecision output = resolveDecision(extTgtModel, outputUriRef);

    addToFlat(flatRoot, output);
    outputRef.setHref(idToLocalRef(outputUriRef));

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

    output.getAuthorityRequirement().forEach(
        authReq -> ensureResolved(authReq, flatRoot, comps));

    output.getKnowledgeRequirement().addAll(encapsulatedKRs);
    output.getKnowledgeRequirement().forEach(
        outKReq -> {
          URI outputSvcRef = URI.create(outKReq.getRequiredKnowledge().getHref());
          String extModelId = URIUtil.normalizeURIString(outputSvcRef);
          TDefinitions extModel = comps.get(extModelId);

          Optional<TBusinessKnowledgeModel> otbkm = resolveBKM(extModel, outputSvcRef);
          if (otbkm.isPresent()) {
            TBusinessKnowledgeModel tbkm = otbkm.get();
            addToFlat(flatRoot, tbkm);

            tbkm.getKnowledgeRequirement().stream()
                .filter(kreq -> kreq.getRequiredKnowledge() != null)
                .forEach(kreq -> ensureResolved(kreq, flatRoot, comps));

            outKReq.getRequiredKnowledge().setHref(idToLocalRef(outputSvcRef));
          } else {
            // points directly to the KS
            ensureResolved(outKReq, flatRoot, comps);
          }
        });

  }

  private void ensureResolved(TInformationRequirement infoReq, TDefinitions flatRoot,
      Map<String, TDefinitions> comps) {
    if (infoReq.getRequiredDecision() != null) {
      URI ref = URI.create(infoReq.getRequiredDecision().getHref());
      if (!isInternal(ref)) {
        URI externalModelId = URIUtil.normalizeURI(ref);
        TDefinitions tgtModel = comps.get(externalModelId.toString());
        if (tgtModel == null) {
          throw new ServerSideException(
              Explainer.GENERIC_ERROR_TYPE,
              "Broken reference",
              ResponseCodeSeries.UnprocessableEntity,
              "Unable to resolve external DMN fragment reference " + ref,
              URI.create(flatRoot.getNamespace()));
        }
        TDecision subDecision = resolveDecision(tgtModel, ref);

        addToFlat(flatRoot, subDecision);
        infoReq.getRequiredDecision().setHref(idToLocalRef(ref));

        ensureResolved(subDecision, flatRoot, comps);
      }
    } else if (infoReq.getRequiredInput() != null) {
      URI ref = URI.create(infoReq.getRequiredInput().getHref());
      if (!isInternal(ref)) {
        URI externalModelId = URIUtil.normalizeURI(ref);
        TDefinitions tgtModel = comps.get(externalModelId.toString());

        TInputData input = resolveInput(tgtModel, ref);

        addToFlat(flatRoot, input);
        infoReq.getRequiredInput().setHref(idToLocalRef(ref));
      }
    }
  }

  private void ensureResolved(TAuthorityRequirement authReq, TDefinitions flatRoot,
      Map<String, TDefinitions> comps) {
    if (authReq.getRequiredAuthority() != null) {
      URI ref = URI.create(authReq.getRequiredAuthority().getHref());
      if (!isInternal(ref)) {
        URI externalModelId = URIUtil.normalizeURI(ref);
        TDefinitions tgtModel = comps.get(externalModelId.toString());

        TKnowledgeSource knowledgeSource = resolveKnowledgeSource(tgtModel, ref);

        addToFlat(flatRoot, knowledgeSource);
        authReq.getRequiredAuthority().setHref(idToLocalRef(ref));
      }
    }
  }


  private TDecision resolveDecision(TDefinitions externalModel, URI ref) {
    TDecision externalDec = findDecision(ref, externalModel)
        .orElseThrow(() -> new IllegalStateException(
            String.format("Unable to resolve fragment %s as dmn:Decision in referenced model %s",
                ref.getFragment(), externalModel.getName())));

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
    for (TAuthorityRequirement authReq : externalDec.getAuthorityRequirement()) {
      if (authReq.getRequiredAuthority() != null) {
        String href = authReq.getRequiredAuthority().getHref();
        if (isInternal(href)) {
          authReq.getRequiredAuthority().setHref(externalModel.getNamespace() + href);
        }
      }
    }
    for (TKnowledgeRequirement knowReq : externalDec.getKnowledgeRequirement()) {
      if (knowReq.getRequiredKnowledge() != null) {
        String href = knowReq.getRequiredKnowledge().getHref();
        if (isInternal(href)) {
          knowReq.getRequiredKnowledge().setHref(externalModel.getNamespace() + href);
        }
      }
    }
    return externalDec;
  }


  private Optional<TDecisionService> resolveDecisionService(TDefinitions externalModel, URI ref) {
    Optional<TDecisionService> externalDecService =
        findDecisionService(ref, externalModel);
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
    TInputData externalInput = findInput(ref, externalModel)
        .orElseThrow(() -> new IllegalStateException(
            String.format("Unable to resolve fragment %s as dmn:Input in referenced model %s",
                ref.getFragment(), externalModel.getName())));
    return (TInputData) externalInput.clone();
  }

  private Optional<TBusinessKnowledgeModel> resolveBKM(TDefinitions externalModel, URI ref) {
    return findBKM(ref, externalModel);
  }

  private TKnowledgeSource resolveKnowledgeSource(TDefinitions externalModel, URI ref) {
    TKnowledgeSource externalKnowledge = findKnowledgeSource(ref, externalModel)
        .orElseThrow(() -> new IllegalStateException(
            String.format("Unable to resolve fragment %s as dmn:KSource in referenced model %s",
                ref.getFragment(), externalModel.getName())));
    return (TKnowledgeSource) externalKnowledge.clone();
  }

  private boolean isInternal(URI ref) {
    // check scheme and path?
    return ref != null && isInternal(ref.toString());
  }

  private boolean isInternal(String ref) {
    return ref != null && ref.startsWith("#");
  }

  private void addToFlat(TDefinitions flatRoot, TDecisionService decisionService) {
    flatRoot.getDrgElement()
        .add(factory.createDecisionService(decisionService));
  }

  private void addToFlat(TDefinitions flatRoot, TKnowledgeSource knowledgeSource) {
    flatRoot.getDrgElement()
        .add(factory.createKnowledgeSource(knowledgeSource));
  }

  private void addToFlat(TDefinitions flatRoot, TInputData inputData) {
    flatRoot.getDrgElement()
        .add(factory.createInputData(inputData));
  }

  private void addToFlat(TDefinitions flatRoot, TDecision decision) {
    flatRoot.getDrgElement()
        .add(factory.createDecision(decision));
  }

  private void addToFlat(TDefinitions flatRoot, TBusinessKnowledgeModel bkm) {
    flatRoot.getDrgElement()
        .add(factory.createBusinessKnowledgeModel(bkm));
  }

}
