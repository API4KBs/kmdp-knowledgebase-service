package edu.mayo.kmdp.language.translators.mvf.fhir.stu3;

import static edu.mayo.kmdp.language.common.fhir.stu3.FHIRPlanDefinitionUtils.toFHIRIdentifier;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.id.Term.newTerm;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Transcreation_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.MVF_1_0;

import edu.mayo.kmdp.language.parsers.fhir.stu3.FHIR3Deserializer;
import edu.mayo.kmdp.language.parsers.mvf.v1_0.MVFParser;
import edu.mayo.kmdp.language.translators.AbstractSimpleTranslator;
import edu.mayo.kmdp.util.Util;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import javax.inject.Named;
import org.hl7.fhir.dstu3.model.CodeSystem;
import org.hl7.fhir.dstu3.model.CodeSystem.CodeSystemContentMode;
import org.hl7.fhir.dstu3.model.CodeSystem.CodeSystemHierarchyMeaning;
import org.hl7.fhir.dstu3.model.CodeSystem.ConceptDefinitionComponent;
import org.hl7.fhir.dstu3.model.ConceptMap;
import org.hl7.fhir.dstu3.model.ConceptMap.TargetElementComponent;
import org.hl7.fhir.dstu3.model.Enumerations.ConceptMapEquivalence;
import org.hl7.fhir.dstu3.model.Reference;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal._applyLift;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal._applyLower;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.omg.spec.mvf._20220702.mvf.MVFDictionary;
import org.omg.spec.mvf._20220702.mvf.MVFEntry;
import org.omg.spec.mvf._20220702.mvf.Vocabulary;
import org.omg.spec.mvf._20220702.mvf.VocabularyEntry;

@Named
@KPOperation(Transcreation_Task)
@KPSupport(MVF_1_0)
public class MVFToFHIRTermsTranslator extends
    AbstractSimpleTranslator<MVFDictionary, CodeSystem> {

  public static final UUID OPERATOR_ID = UUID.fromString("aec127d2-8c78-462f-8472-eb440bcd33e3");
  public static final String OPERATOR_VERSION = "1.0.0";

  private final _applyLift mvfParser = new MVFParser();
  private final _applyLower fhirParser = new FHIR3Deserializer();


  public MVFToFHIRTermsTranslator() {
    setId(newId(OPERATOR_ID, OPERATOR_VERSION));
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return MVF_1_0;
  }

  @Override
  public List<SyntacticRepresentation> getFrom() {
    return List.of(
        rep(MVF_1_0),
        rep(MVF_1_0, XML_1_1),
        rep(MVF_1_0, XML_1_1, Charset.defaultCharset()),
        rep(MVF_1_0, XML_1_1, Charset.defaultCharset(), Encodings.DEFAULT));
  }

  @Override
  public List<SyntacticRepresentation> getInto() {
    return List.of(
        rep(FHIR_STU3),
        rep(FHIR_STU3, JSON),
        rep(FHIR_STU3, JSON, Charset.defaultCharset()),
        rep(FHIR_STU3, JSON, Charset.defaultCharset(), Encodings.DEFAULT));
  }

  @Override
  public KnowledgeRepresentationLanguage getTargetLanguage() {
    return FHIR_STU3;
  }

  @Override
  protected Answer<_applyLift> getParser() {
    return Answer.of(mvfParser);
  }

  @Override
  protected Answer<_applyLower> getTargetParser() {
    return Answer.of(fhirParser);
  }

  @Override
  protected Optional<CodeSystem> transformAst(
      ResourceIdentifier assetId,
      ResourceIdentifier srcArtifactId,
      MVFDictionary dict,
      SyntacticRepresentation srcRep,
      SyntacticRepresentation tgtRep,
      Properties config) {

    var cs = new CodeSystem()
        .setName(dict.getName())
        .setDescription(dict.getDescription())
        .setCompositional(true)
        .setContent(CodeSystemContentMode.FRAGMENT)
        .setCount(dict.getEntry().size())
        .setHierarchyMeaning(CodeSystemHierarchyMeaning.ISA);
    setKnowledgeIdentifier(cs, dict.getUri());

    mapConcepts(dict, cs);

    addOntologyMappings(dict, cs);

    return Optional.of(cs);
  }

  private void addOntologyMappings(MVFDictionary dict, CodeSystem cs) {
    var cm = new ConceptMap();
    var mapId = Util.uuid(dict.getUri()).toString();
    cm.setId(mapId);

    dict.getEntry().stream()
        .filter(e -> e.getExternalReference() != null)
        .forEach(e -> addMapping(e, dict, cm));

    cs.addContained(cm);
    cs.addExtension()
        .setUrl(DependencyTypeSeries.Is_Supplemented_By.getReferentId().toString())
        .setValue(new Reference()
            .setReference("#" + mapId));
  }

  private void addMapping(MVFEntry entry, MVFDictionary dict, ConceptMap cm) {
    var srcCS = dict.getUri();
    var tgtCS = lookupVocabulary(entry, dict).getUri();
    var srcCode = getMVFCode(entry);
    var tgtCode = lookupVocabularyEntry(entry, dict).getTerm();
    var label = entry.getName();

    var map = cm.getGroup().stream()
        .filter(g -> g.getTarget().equals(tgtCS))
        .findFirst()
        .orElseGet(() -> cm.addGroup()
            .setSource(srcCS)
            .setTarget(tgtCS));
    map.addElement()
        .setCode(srcCode)
        .setDisplay(label)
        .addTarget(new TargetElementComponent()
            .setCode(tgtCode)
            .setEquivalence(ConceptMapEquivalence.EQUIVALENT));
  }

  private VocabularyEntry lookupVocabularyEntry(MVFEntry entry, MVFDictionary dict) {
    return dict.getVocabulary().stream()
        .flatMap(v -> v.getEntry().stream())
        .filter(e -> Objects.equals(
            entry.getUri(),
            e.getMVFEntry().getUri()))
        .findFirst()
        .orElseThrow();
  }

  private Vocabulary lookupVocabulary(MVFEntry entry, MVFDictionary dict) {
    return dict.getVocabulary().stream()
        .filter(v -> v.getEntry().stream()
            .anyMatch(e -> Objects.equals(
                entry.getUri(),
                e.getMVFEntry().getUri())))
        .findFirst()
        .orElseThrow();
  }

  private void mapConcepts(MVFDictionary dict, CodeSystem cs) {
    dict.getEntry().stream()
        .filter(x -> x.getExternalReference() != null)
        .map(this::toCode)
        .forEach(cs::addConcept);

  }

  private ConceptDefinitionComponent toCode(MVFEntry entry) {
    return new ConceptDefinitionComponent()
        .setCode(getMVFCode(entry))
        .setDisplay(entry.getName())
        .setDefinition(entry.getDescription());
  }

  private String getMVFCode(MVFEntry entry) {
    return newId(URI.create(entry.getUri())).getTag();
  }


  protected void setKnowledgeIdentifier(
      CodeSystem cs,
      String artifactId) {
    var rid = newId(URI.create(artifactId));
    cs.setIdentifier(
        toFHIRIdentifier(rid,
            newTerm(URI.create("https://www.omg.org/spec/API4KP/"), "KnowledgeArtifact")));
    cs.setUrl(artifactId);
  }
}
