package edu.mayo.kmdp.language.validators.dmn.v1_2;

import static edu.mayo.kmdp.util.Util.isEmpty;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newVersionId;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Clinical_Decision_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Well_Formedness_Check_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;

import edu.mayo.kmdp.language.validators.cmmn.v1_1.CCPMComponentValidator;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.URIUtil;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Named;
import javax.xml.bind.JAXBElement;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.Severity;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.surrogate.Annotation;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.omg.spec.dmn._20180521.model.TAuthorityRequirement;
import org.omg.spec.dmn._20180521.model.TDMNElementReference;
import org.omg.spec.dmn._20180521.model.TDRGElement;
import org.omg.spec.dmn._20180521.model.TDecision;
import org.omg.spec.dmn._20180521.model.TDecisionService;
import org.omg.spec.dmn._20180521.model.TDefinitions;
import org.omg.spec.dmn._20180521.model.TImport;
import org.omg.spec.dmn._20180521.model.TInputData;
import org.omg.spec.dmn._20180521.model.TKnowledgeSource;

@Named
@KPOperation(Well_Formedness_Check_Task)
@KPSupport(DMN_1_2)
public class CCPMProfileDMNValidator extends CCPMComponentValidator {

  public static final UUID id = UUID.fromString("cdf5c894-c74a-4a46-892a-dd3ec63bf540");
  public static final String version = "1.0.0";

  private final ResourceIdentifier operatorId;

  public CCPMProfileDMNValidator() {
    this.operatorId = SemanticIdentifier.newId(id, version);
  }

  @Override
  public ResourceIdentifier getOperatorId() {
    return operatorId;
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return DMN_1_2;
  }

  @Override
  public List<SyntacticRepresentation> getFrom() {
    return Arrays.asList(rep(DMN_1_2), rep(Knowledge_Asset_Surrogate_2_0));
  }

  protected Answer<Void> validate(KnowledgeAsset knowledgeAsset, KnowledgeCarrier carrier) {
    return allOf(
        validateAssetId(knowledgeAsset, carrier),
        validateAssetVersion(knowledgeAsset, carrier),
        validateArtifactVersion(knowledgeAsset, carrier),
        validateAssetType(knowledgeAsset, carrier, Clinical_Decision_Model),
        validatePublicationStatus(knowledgeAsset, carrier)
    );
  }

  @Override
  protected Answer<Void> validate(org.omg.spec.dmn._20180521.model.TDefinitions decisionModel,
      KnowledgeCarrier carrier) {
    return allOf(
        validateNoImports(decisionModel, carrier),
        validateAnnotatedInputs(decisionModel, carrier),
        validateAnnotatedDecisionServices(decisionModel, carrier),
        validateDecisionServiceInputs(decisionModel, carrier),
        validateKnowledgeSources(decisionModel, carrier)
    );
  }

  /**
   * Checks for DMN to DMN imports other than Decision Service inputs At this point, any other
   * import should have been 'flattened'
   *
   * @param decisionModel
   * @param carrier
   * @return
   */
  private Answer<Void> validateNoImports(TDefinitions decisionModel, KnowledgeCarrier carrier) {
    Set<String> imports = decisionModel.getImport().stream()
        .map(TImport::getNamespace)
        .collect(Collectors.toSet());
    Set<String> bkmReqs = decisionModel.getDrgElement().stream()
        .map(JAXBElement::getValue)
        .flatMap(StreamUtil.filterAs(TDecision.class))
        .flatMap(dec -> dec.getKnowledgeRequirement().stream())
        .map(kr -> kr.getRequiredKnowledge().getHref())
        .map(URI::create)
        .map(URIUtil::normalizeURIString)
        .collect(Collectors.toSet());
    imports.removeAll(bkmReqs);

    return validationResponse(
        carrier,
        imports.isEmpty(),
        "DMN Includes",
        () -> bkmReqs.isEmpty() ? "none" : "imports DecSvc " + toString(bkmReqs, x -> x),
        () -> "NON-DecSvc IMPORTS " + toString(imports, x -> x)
    );
  }


  /**
   * Checks for CSO annotations on InputData elements
   *
   * @param decisionModel
   * @param carrier
   * @return
   */
  private Answer<Void> validateAnnotatedInputs(TDefinitions decisionModel,
      KnowledgeCarrier carrier) {
    Set<TInputData> inputs = decisionModel.getDrgElement().stream()
        .map(JAXBElement::getValue)
        .flatMap(StreamUtil.filterAs(TInputData.class))
        .collect(Collectors.toSet());
    Set<TInputData> annotated = inputs.stream()
        .filter(input -> hasCSOAnnotation(input))
        .collect(Collectors.toSet());
    Set<TInputData> notAnnotated = new HashSet<>(inputs);
    notAnnotated.removeAll(annotated);

    return validationResponse(
        carrier,
        notAnnotated.isEmpty(),
        "Input Annos",
        () -> "annotated inputs",
        () -> "MISSING INPUT ANNOS " + toString(notAnnotated, TInputData::getName)
    );
  }


  /**
   * Checks for Annotations on Decision Services
   *
   * @param decisionModel
   * @param carrier
   * @return
   */
  private Answer<Void> validateDecisionServiceInputs(TDefinitions decisionModel,
      KnowledgeCarrier carrier) {
    Set<TDecisionService> decServices = decisionModel.getDrgElement().stream()
        .map(JAXBElement::getValue)
        .flatMap(StreamUtil.filterAs(TDecisionService.class))
        .collect(Collectors.toSet());

    Set<TDecisionService> withDecisionsAsInputs = decServices.stream()
        .filter(ds -> ! ds.getInputDecision().isEmpty())
        .collect(Collectors.toSet());

    return validationResponse(
        carrier,
        withDecisionsAsInputs.isEmpty() ? Severity.OK : Severity.FATAL,
        "DecSvc Inputs",
        () -> decServices.isEmpty() ? "none" : "all valid inputs",
        () -> "HAS INPUT DECISION " + toString(withDecisionsAsInputs, TDecisionService::getName)
    );
  }

  /**
   * Ensures that decision services inputs are only inputdata
   *
   * @param decisionModel
   * @param carrier
   * @return
   */
  private Answer<Void> validateAnnotatedDecisionServices(TDefinitions decisionModel,
      KnowledgeCarrier carrier) {
    Set<TDecisionService> decServices = decisionModel.getDrgElement().stream()
        .map(JAXBElement::getValue)
        .flatMap(StreamUtil.filterAs(TDecisionService.class))
        .collect(Collectors.toSet());
    Set<TDecisionService> semantic = decServices.stream()
        .filter(ds -> ! ds.getOutputDecision().isEmpty())
        .filter(ds -> ds.getOutputDecision().stream()
            .flatMap(ref -> lookup(ref, decisionModel, TDecision.class))
            .anyMatch(out -> hasCSOAnnotation(out)))
        .collect(Collectors.toSet());
    Set<TDecisionService> withDEK = decServices.stream()
        .filter(ds -> ! ds.getOutputDecision().isEmpty())
        .filter(ds -> ds.getOutputDecision().stream()
            .flatMap(ref -> lookup(ref, decisionModel, TDecision.class))
            .anyMatch(out -> hasDEK(out, decisionModel)))
        .collect(Collectors.toSet());
    Set<TDecisionService> incomplete = new HashSet<>(decServices);
    incomplete.removeAll(semantic);
    incomplete.removeAll(withDEK);

    return validationResponse(
        carrier,
        incomplete.isEmpty(),
        "DecSvc Output",
        () -> decServices.isEmpty() ? "none" : "has dek/concept",
        () -> "MISSING DEK/CONCEPT " + toString(incomplete, TDecisionService::getName)
    );
  }


  /**
   * Checks that KnowledgeSources have MIME type and location URI/URL
   *
   * Type attribute must be a valid mime type:
   * KCMS POC - text/html
   * DEK - application/dita+xml
   * IKR - application/fhir+json
   * External Link - <star>/</star>
   *
   * @param decisionModel
   * @param carrier
   * @return
   */
  private Answer<Void> validateKnowledgeSources(TDefinitions decisionModel,
      KnowledgeCarrier carrier) {
    Set<TKnowledgeSource> knowSources = decisionModel.getDrgElement().stream()
        .map(JAXBElement::getValue)
        .flatMap(StreamUtil.filterAs(TKnowledgeSource.class))
        .collect(Collectors.toSet());
    Set<TKnowledgeSource> incomplete = knowSources.stream()
        .filter(ks -> !isValidKSMime(ks.getType()) || !isValidAssetId(ks.getLocationURI(), ks.getType()))
        .collect(Collectors.toSet());

    return validationResponse(
        carrier,
        incomplete.isEmpty(),
        "Know Sources",
        () -> knowSources.isEmpty() ? "none" : "configured",
        () -> "INCOMPLETE KNOW SRCs " + toString(incomplete, TKnowledgeSource::getName)
    );
  }

  private boolean isValidAssetId(String locationURI, String type) {
    if (! type.startsWith("application")) {
      return ! isEmpty(locationURI);
    }
    try {
      return ! isEmpty(newVersionId(URI.create(locationURI)).getVersionTag());
    } catch (Exception e) {
      return false;
    }
  }


  private boolean hasDEK(TDecision out, TDefinitions decisionModel) {
    return out.getAuthorityRequirement().stream()
        .map(TAuthorityRequirement::getRequiredAuthority)
        .flatMap(ref -> lookup(ref, decisionModel, TKnowledgeSource.class))
        .anyMatch(ks -> "application/dita+xml".equalsIgnoreCase(ks.getType()));
  }

  private <T extends TDRGElement> Stream<T> lookup(
      TDMNElementReference ref, TDefinitions decisionModel, Class<T> type) {
    return decisionModel.getDrgElement().stream()
        .map(JAXBElement::getValue)
        .flatMap(StreamUtil.filterAs(type))
        .filter(d -> d.getId().equals(URI.create(ref.getHref()).getFragment()))
        .findFirst().stream();
  }


  private boolean hasCSOAnnotation(TDRGElement input) {
    return Optional.ofNullable(input.getExtensionElements()).stream()
        .flatMap(x -> x.getAny().stream())
        .flatMap(StreamUtil.filterAs(Annotation.class))
        .map(Annotation::getRef)
        .map(ResourceIdentifier::getNamespaceUri)
        .anyMatch(
            u -> "https://ontology.mayo.edu/taxonomies/clinicalsituations".equals(u.toString()));
  }

  private boolean isValidKSMime(String type) {
    return "application/dita+xml".equalsIgnoreCase(type)
        || "text/html".equalsIgnoreCase(type)
        || "application/fhir+json".equalsIgnoreCase(type)
        || "*/*".equalsIgnoreCase(type);
  }
}
