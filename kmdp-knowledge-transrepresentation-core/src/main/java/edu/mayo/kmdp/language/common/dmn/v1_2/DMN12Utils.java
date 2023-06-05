package edu.mayo.kmdp.language.common.dmn.v1_2;

import edu.mayo.kmdp.util.StreamUtil;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import javax.xml.bind.JAXBElement;
import org.omg.spec.dmn._20180521.model.TBusinessKnowledgeModel;
import org.omg.spec.dmn._20180521.model.TDMNElementReference;
import org.omg.spec.dmn._20180521.model.TDRGElement;
import org.omg.spec.dmn._20180521.model.TDecision;
import org.omg.spec.dmn._20180521.model.TDecisionService;
import org.omg.spec.dmn._20180521.model.TDefinitions;
import org.omg.spec.dmn._20180521.model.TInformationRequirement;
import org.omg.spec.dmn._20180521.model.TInputData;
import org.omg.spec.dmn._20180521.model.TKnowledgeSource;

public class DMN12Utils {

  public static Stream<TDecision> getSubDecisionsClosure(
      TDecision dec, TDefinitions decisionModel) {
    return Stream.concat(
        Stream.of(dec),
        getSubDecisions(dec, decisionModel)
            .flatMap(d -> getSubDecisionsClosure(d, decisionModel)));
  }

  public static Stream<TDecision> getSubDecisions(
      TDecision dec, TDefinitions decisionModel) {
    return dec.getInformationRequirement().stream()
        .map(TInformationRequirement::getRequiredDecision) // exclude inputs
        .filter(Objects::nonNull)
        .map(ref -> streamDecisions(decisionModel)
            .filter(d -> joins(d.getId(), ref.getHref()))
            .findFirst().orElseThrow());
  }

  public static Optional<TInputData> findInput(
      TDMNElementReference requiredInput, TDefinitions decisionModel) {
    return findInput(requiredInput.getHref(), decisionModel);
  }

  public static Optional<TInputData> findInput(
      URI inputRef, TDefinitions decisionModel) {
    return findInput("#" + inputRef.getFragment(), decisionModel);
  }

  public static Optional<TInputData> findInput(
      String inputHref, TDefinitions decisionModel) {
    return streamInputs(decisionModel)
        .filter(in -> joins(in.getId(), inputHref))
        .findFirst();
  }

  public static Optional<TBusinessKnowledgeModel> findBKM(
      TDMNElementReference requiredKnowledge, TDefinitions decisionModel) {
    return findBKM(requiredKnowledge.getHref(), decisionModel);
  }

  public static Optional<TBusinessKnowledgeModel> findBKM(
      URI bkmRef, TDefinitions decisionModel) {
    return findBKM("#" + bkmRef.getFragment(), decisionModel);
  }

  public static Optional<TBusinessKnowledgeModel> findBKM(
      String knowledgeHref, TDefinitions decisionModel) {
    return streamBKM(decisionModel)
        .filter(in -> joins(in.getId(), knowledgeHref))
        .findFirst();
  }

  public static Optional<TDecision> findDecision(
      TDMNElementReference requiredDecision, TDefinitions decisionModel) {
    return findDecision(requiredDecision.getHref(), decisionModel);
  }

  public static Optional<TDecision> findDecision(
      URI decisionHref, TDefinitions decisionModel) {
    return findDecision("#" + decisionHref.getFragment(), decisionModel);
  }

  public static Optional<TDecision> findDecision(
      String decisionHref, TDefinitions decisionModel) {
    return streamDecisions(decisionModel)
        .filter(in -> joins(in.getId(), decisionHref))
        .findFirst();
  }

  public static Optional<TDecisionService> findDecisionService(
      TDMNElementReference requiredDecisionService, TDefinitions decisionModel) {
    return findDecisionService(requiredDecisionService.getHref(), decisionModel);
  }

  public static Optional<TDecisionService> findDecisionService(
      URI decisionServiceHref, TDefinitions decisionModel) {
    return findDecisionService("#" + decisionServiceHref.getFragment(), decisionModel);
  }

  public static Optional<TDecisionService> findDecisionService(
      String decisionServiceHref, TDefinitions decisionModel) {
    return streamDecisionServices(decisionModel)
        .filter(in -> joins(in.getId(), decisionServiceHref))
        .findFirst();
  }

  public static Optional<TKnowledgeSource> findKnowledgeSource(
      TDMNElementReference requiredSource, TDefinitions decisionModel) {
    return findKnowledgeSource(requiredSource.getHref(), decisionModel);
  }

  public static Optional<TKnowledgeSource> findKnowledgeSource(
      URI sourceHref, TDefinitions decisionModel) {
    return findKnowledgeSource("#" + sourceHref.getFragment(), decisionModel);
  }

  public static Optional<TKnowledgeSource> findKnowledgeSource(
      String sourceHref, TDefinitions decisionModel) {
    return streamKnowledgeSources(decisionModel)
        .filter(in -> joins(in.getId(), sourceHref))
        .findFirst();
  }

  public static Stream<TKnowledgeSource> streamKnowledgeSources(TDefinitions dmnModel) {
    return streamDRGElements(dmnModel, TKnowledgeSource.class);
  }

  public static Stream<TBusinessKnowledgeModel> streamBKM(TDefinitions dmnModel) {
    return streamDRGElements(dmnModel, TBusinessKnowledgeModel.class);
  }

  public static Stream<TDecisionService> streamDecisionServices(TDefinitions dmnModel) {
    return streamDRGElements(dmnModel, TDecisionService.class);
  }

  public static Stream<TDecision> streamDecisions(TDefinitions dmnModel) {
    return streamDRGElements(dmnModel, TDecision.class);
  }

  public static Stream<TInputData> streamInputs(TDefinitions dmnModel) {
    return streamDRGElements(dmnModel, TInputData.class);
  }

  private static <T extends TDRGElement> Stream<T> streamDRGElements(
      TDefinitions model, Class<T> type) {
    return model.getDrgElement().stream()
        .map(JAXBElement::getValue)
        .flatMap(StreamUtil.filterAs(type));
  }


  public static String refToId(String href) {
    if (!href.startsWith("#")) {
      throw new IllegalStateException(
          "Reference " + href + " expected to be a relative URI fragment");
    }
    return asId(href.substring(1));
  }

  public static String idToLocalRef(URI id) {
    return idToRef(id.getFragment());
  }

  public static String idToRef(String id) {
    return "#" + asId(id);
  }

  public static String asId(String id) {
    return id.trim().replace("_", "");
  }

  public static boolean joins(String pk, String fkHref) {
    if (pk == null || fkHref == null) {
      return false;
    }
    return asId(pk).equals(refToId(fkHref));
  }


  private DMN12Utils() {
    // functions only
  }
}
