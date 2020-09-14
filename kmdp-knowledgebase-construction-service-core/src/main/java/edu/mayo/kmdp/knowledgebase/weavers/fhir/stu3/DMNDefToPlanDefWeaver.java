package edu.mayo.kmdp.knowledgebase.weavers.fhir.stu3;


import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.In_Terms_Of;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Weaving_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;

import edu.mayo.kmdp.knowledgebase.AbstractKnowledgeBaseOperator;
import edu.mayo.kmdp.knowledgebase.KnowledgeBaseProvider;
import edu.mayo.kmdp.util.StreamUtil;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.KnowledgeBaseApiInternal;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedWeave;
import org.omg.spec.api4kp._20200801.api.terminology.v4.server.TermsApiInternal;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.id.Term;
import org.omg.spec.api4kp._20200801.services.KPComponent;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.Annotation;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.omg.spec.dmn._20180521.model.TDRGElement;
import org.omg.spec.dmn._20180521.model.TDecision;
import org.omg.spec.dmn._20180521.model.TDecisionService;
import org.omg.spec.dmn._20180521.model.TDefinitions;
import org.omg.spec.dmn._20180521.model.TInputData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

@Named
@KPSupport({FHIR_STU3, DMN_1_2})
@KPComponent
@KPOperation(Weaving_Task)
public class DMNDefToPlanDefWeaver
    extends AbstractKnowledgeBaseOperator
    implements _applyNamedWeave {

  static Logger logger = LoggerFactory.getLogger(DMNDefToPlanDefWeaver.class);

  public static final UUID id = UUID.fromString("1a43a134-0cb9-4ac5-a468-241744f89fcb");
  public static final String version = "1.0.0";

  @Inject
  TermsApiInternal terminologyProvider;

  private KnowledgeBaseApiInternal kbManager;

  public DMNDefToPlanDefWeaver() {
    super(SemanticIdentifier.newId(id,version));
  }

  protected DMNDefToPlanDefWeaver(KnowledgeBaseApiInternal kbManager) {
    this();
    this.kbManager = kbManager;
  }

  public DMNDefToPlanDefWeaver(KnowledgeBaseProvider kbManager, TermsApiInternal terminologyProvider) {
    this();
    this.kbManager = kbManager;
    this.terminologyProvider = terminologyProvider;
    kbManager.withNamedWeaver(this);
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return FHIR_STU3;
  }

  @Override
  public Answer<KnowledgeCarrier> applyNamedWeave(UUID operatorId, KnowledgeCarrier aspects,
      UUID kbaseId, String versionTag, String xParams) {
    if (!getOperatorId().getUuid().equals(operatorId)) {
      return Answer.failed();
    }
    return kbManager.getKnowledgeBaseManifestation(kbaseId,versionTag)
        .flatMap(kb -> weave(kb, aspects));
  }

  private Answer<KnowledgeCarrier> weave(KnowledgeCarrier kc, KnowledgeCarrier dictionary) {
    TDefinitions dict = dictionary.as(TDefinitions.class)
        .orElseThrow(IllegalArgumentException::new);
    if (DMN_1_2.sameAs(kc.getRepresentation().getLanguage())) {
      TDefinitions srcDmn = kc.as(TDefinitions.class)
          .orElseThrow(IllegalStateException::new);
      // Should process all the places where the inputs are referred

      // decisions' inputs
      Collection<JAXBElement<? extends TDRGElement>> drgElements = new ArrayList<>(srcDmn.getDrgElement());
      drgElements.stream()
          .filter(elem -> elem.getValue() instanceof TDecision)
          .forEach(dec -> weaveDecision(dec,dict,srcDmn));

      // decision services' inputs
      drgElements = new ArrayList<>(srcDmn.getDrgElement());
      drgElements.stream()
          .filter(elem -> elem.getValue() instanceof TDecisionService)
          .forEach(decSvc -> weaveDecisionService(decSvc, dict, srcDmn));
    } else {
      //throw new UnsupportedOperationException(
      //    "Unable to handle " + kc.getRepresentation().getLanguage().getLabel());
    }
    return Answer.of(kc);
  }

  private void weaveDecision(JAXBElement<? extends TDRGElement> decElement,
      TDefinitions dictionary, TDefinitions sourceDecisionModel) {
    TDecision dec = (TDecision) decElement.getValue();
    logger.info("Checking for dictionary refs {}", dec.getName());
    dec.getInformationRequirement().stream()
        .filter(info -> info.getRequiredInput() != null)
        .filter(info -> info.getRequiredInput().getHref().startsWith(getDictionaryURI()))
        .forEach(info -> {
          logger.info("Found dictionary reference {}",  info.getRequiredInput().getHref());
          String refUUID = URI.create(info.getRequiredInput().getHref()).getFragment();
          info.getRequiredInput().setHref("#" + refUUID);
          JAXBElement<TInputData> resolvedInput = new JAXBElement<>(
              new QName("http://www.omg.org/spec/DMN/20180521/MODEL/","inputData"),
              TInputData.class,
              rewrite(lookup(dictionary,refUUID)));
          addResolvedInput(resolvedInput,sourceDecisionModel);
        });

  }

  private String getDictionaryURI() {
    // TODO Fixme rewrite once the API is clarified
    return terminologyProvider.listTerminologies()
        .flatOpt(vocabs -> vocabs.stream()
            //.filter() // TODO there should be a filter based on an operation parameter
            .findFirst()
            .map(p -> p.getHref())
            .map(URI::toString)
        )
        .orElseThrow(IllegalStateException::new);
  }

  private Optional<Term> resolveId(String uri) {
    return terminologyProvider.getTerm(null, null, uri)
        .map(Term.class::cast)
        .getOptionalValue();
  }


  private void weaveDecisionService(JAXBElement<? extends TDRGElement> decElement,
      TDefinitions dictionary, TDefinitions sourceDecisionModel) {
    TDecisionService dec = (TDecisionService) decElement.getValue();
    dec.getInputData().stream()
        .filter(info -> info.getHref().startsWith(getDictionaryURI()))
        .forEach(info -> {
          String refUUID = URI.create(info.getHref()).getFragment();
          info.setHref("#" + refUUID);
          JAXBElement<TInputData> resolvedInput = new JAXBElement<>(
              new QName("http://www.omg.org/spec/DMN/20180521/MODEL/","inputData"),
              TInputData.class,
              rewrite(lookup(dictionary,refUUID)));
          addResolvedInput(resolvedInput,sourceDecisionModel);
        });
  }

  private void addResolvedInput(JAXBElement<TInputData> resolvedInput,
      TDefinitions sourceDecisionModel) {
    logger.info("Trying to weave resolved input {}", resolvedInput.getValue().getName());
    boolean alreadyExisting = sourceDecisionModel.getDrgElement().stream()
        .map(JAXBElement::getValue)
        .flatMap(StreamUtil.filterAs(TInputData.class))
        .anyMatch(in -> in.getId().equals(resolvedInput.getValue().getId()));
    if (! alreadyExisting) {
      logger.info(("Adding " + resolvedInput.getValue().getName()));
      sourceDecisionModel.getDrgElement().add(resolvedInput);
    } else {
      logger.info("Already present - no need to add");
    }
  }

  private <X extends TDRGElement> X rewrite(TDRGElement lookup) {
    TInputData input = (TInputData) lookup;

    input.setId(input.getId().replace("_","#"));

    Collection<Object> annotations = input.getExtensionElements().getAny();
    Collection<Object> rewrittenAnnos = new ArrayList<>();

    annotations.stream()
        .flatMap(StreamUtil.filterAs(Element.class))
        .filter(el -> "semanticLink".equals(el.getLocalName()))
        .map(el -> el.getAttributeNode("uri"))
        .map(Attr::getValue)
        .map(uri -> resolveId(uri)
            .orElseThrow(() -> new IllegalArgumentException("Unable to resolve concept " + uri)))
        .map(cs -> new Annotation()
            .withRel(In_Terms_Of.asConceptIdentifier())
            .withRef(cs.asConceptIdentifier())
        ).findFirst()
        .ifPresent(rewrittenAnnos::add);

    Collection<Object> rawElems = annotations.stream()
        .flatMap(StreamUtil.filterAs(Element.class))
        .collect(Collectors.toList());

    input.getExtensionElements().getAny().removeAll(rawElems);
    input.getExtensionElements().getAny().addAll(rewrittenAnnos);
    return (X) input;
  }

  private <X extends TDRGElement> X lookup(TDefinitions dictionary, String refUUID) {
    TDRGElement element = dictionary.getDrgElement().stream()
        .map(JAXBElement::getValue)
        .flatMap(StreamUtil.filterAs(TInputData.class))
        .filter(input -> input.getId().contains(refUUID)) // ignore heading "#" or "_"
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Unable to find " + refUUID));
    return (X) element;
  }

}
