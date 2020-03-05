package edu.mayo.kmdp.knowledgebase.weavers.fhir.stu3;

import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;

import edu.mayo.kmdp.id.Term;
import edu.mayo.kmdp.id.helper.DatatypeHelper;
import edu.mayo.kmdp.knowledgebase.v3.server.BindingApiInternal;
import edu.mayo.kmdp.knowledgebase.v3.server.KnowledgeBaseApiInternal;
import edu.mayo.kmdp.metadata.annotations.SimpleAnnotation;
import edu.mayo.kmdp.terms.v4.server.TermsApiInternal;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.ontology.taxonomies.api4kp.knowledgeoperations.KnowledgeProcessingOperationSeries;
import edu.mayo.ontology.taxonomies.kmdo.annotationreltype.AnnotationRelTypeSeries;
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
import org.omg.spec.api4kp._1_0.Answer;
import org.omg.spec.api4kp._1_0.identifiers.Pointer;
import org.omg.spec.api4kp._1_0.services.KPOperation;
import org.omg.spec.api4kp._1_0.services.KPSupport;
import org.omg.spec.api4kp._1_0.services.KnowledgeBase;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;
import org.omg.spec.dmn._20180521.model.TDRGElement;
import org.omg.spec.dmn._20180521.model.TDecision;
import org.omg.spec.dmn._20180521.model.TDecisionService;
import org.omg.spec.dmn._20180521.model.TDefinitions;
import org.omg.spec.dmn._20180521.model.TInputData;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

@Named
@KPSupport({FHIR_STU3,DMN_1_2})
@KPOperation(KnowledgeProcessingOperationSeries.Injection_Task) //TODO FIXME add 'weaving' to the ontology
public class DMNDefToPlanDefWeaver implements BindingApiInternal._weave {

  @Inject
  KnowledgeBaseApiInternal kbaseManager;

  @Inject
  TermsApiInternal terminologyProvider;

  public static BindingApiInternal._weave newInstance(KnowledgeBaseApiInternal kbaseManager,
      TermsApiInternal terminologyProvider) {
    DMNDefToPlanDefWeaver weaver = new DMNDefToPlanDefWeaver();
    weaver.kbaseManager = kbaseManager;
    weaver.terminologyProvider = terminologyProvider;
    return weaver;
  }

  @Override
  public Answer<Pointer> weave(UUID kbaseId, String versionTag, KnowledgeCarrier aspects) {
    Answer<KnowledgeCarrier> woven = kbaseManager.getKnowledgeBase(kbaseId,versionTag)
        .map(KnowledgeBase::getManifestation)
        .flatMap(kc -> weave(kc, aspects));

    // TODO FIXME neeed a way to set the new version of the KB
    return kbaseManager.initKnowledgeBase()
        .map(DatatypeHelper::deRef)
        .flatMap(vid -> kbaseManager.populateKnowledgeBase(
            UUID.fromString(vid.getTag()),
            vid.getVersion(),
            woven.get()
        ))
    ;

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
    System.out.println("Checking for dictionary refs " + dec.getName());
    dec.getInformationRequirement().stream()
        .filter(info -> info.getRequiredInput() != null)
        .filter(info -> info.getRequiredInput().getHref().startsWith(getDictionaryURI()))
        .forEach(info -> {
          System.out.println("Found dictionary reference " + info.getRequiredInput().getHref());
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
            .map(Pointer::getHref)
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
    System.out.println("Trying to weave resolved input " + resolvedInput.getValue().getName());
    boolean alreadyExisting = sourceDecisionModel.getDrgElement().stream()
        .map(JAXBElement::getValue)
        .flatMap(StreamUtil.filterAs(TInputData.class))
        .anyMatch(in -> in.getId().equals(resolvedInput.getValue().getId()));
    if (! alreadyExisting) {
      System.out.println("Adding " + resolvedInput.getValue().getName());
      sourceDecisionModel.getDrgElement().add(resolvedInput);
    } else {
      System.out.println("Already present - no need to add");
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
        .map(cs -> new SimpleAnnotation()
            .withRel(AnnotationRelTypeSeries.In_Terms_Of.asConcept())
            .withExpr(cs.asConcept())
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