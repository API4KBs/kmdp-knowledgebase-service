package edu.mayo.kmdp.language.translators.skos;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultArtifactId;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Transcreation_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static org.omg.spec.api4kp._20200801.taxonomy.lexicon.LexiconSeries.SKOS;

import edu.mayo.kmdp.language.parsers.fhir.stu3.FHIR3Deserializer;
import edu.mayo.kmdp.language.parsers.owl2.JenaOwlParser;
import edu.mayo.kmdp.language.translators.AbstractSimpleTranslator;
import edu.mayo.kmdp.util.NameUtils;
import edu.mayo.kmdp.util.NameUtils.IdentifierType;
import edu.mayo.kmdp.util.Util;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import javax.inject.Named;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.hl7.fhir.dstu3.model.CodeSystem;
import org.hl7.fhir.dstu3.model.CodeSystem.ConceptDefinitionComponent;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Enumerations.FHIRAllTypes;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal._applyLift;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal._applyLower;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.taxonomy.iso639_2_languagecode._20190201.Language;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Named
@KPOperation(Transcreation_Task)
@KPSupport(OWL_2)
public class SKOStoCodeSystemTranscreator extends AbstractSimpleTranslator<Model, CodeSystem> {

  public static final UUID id = UUID.fromString("2fbb082f-9a17-4f75-98a1-24ca48d87dfb");
  public static final String version = "1.0.0";

  private static final Logger logger = LoggerFactory.getLogger(SKOStoCodeSystemTranscreator.class);
  public static final String CFG_INCLUDE_VALUESET = "includeValueset";

  JenaOwlParser parser = new JenaOwlParser();
  FHIR3Deserializer targetParser = new FHIR3Deserializer();

  public SKOStoCodeSystemTranscreator() {
    setId(SemanticIdentifier.newId(id, version));
  }

  @Override
  public List<SyntacticRepresentation> getFrom() {
    return List.of(
        rep(OWL_2).withLexicon(SKOS)
    );
  }

  @Override
  public List<SyntacticRepresentation> getInto() {
    return Arrays.asList(
        rep(FHIR_STU3),
        rep(FHIR_STU3, JSON),
        rep(FHIR_STU3, JSON, Charset.defaultCharset()),
        rep(FHIR_STU3, JSON, Charset.defaultCharset(), Encodings.DEFAULT));
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return OWL_2;
  }

  @Override
  public KnowledgeRepresentationLanguage getTargetLanguage() {
    return FHIR_STU3;
  }

  @Override
  protected Answer<_applyLift> getParser() {
    return Answer.of(parser);
  }

  @Override
  protected Answer<_applyLower> getTargetParser() {
    return Answer.of(targetParser);
  }

  protected Optional<CodeSystem> transformAst(
      ResourceIdentifier assetId, ResourceIdentifier srcArtifactId,
      Model model,
      SyntacticRepresentation srcRep,
      SyntacticRepresentation tgtRep,
      Properties config) {

    boolean withValueset = enableValueset(config);

    ResIterator schemes = model.listResourcesWithProperty(RDF.type,
        org.apache.jena.vocabulary.SKOS.ConceptScheme);
    if (!schemes.hasNext()) {
      return Optional.empty();
    }
    Resource conceptScheme = schemes.nextResource();
    String conceptSchemeLabel = getLabel(model, conceptScheme, RDFS.label)
        .orElseGet(() -> NameUtils.getTrailingPart(conceptScheme.getURI()));

    CodeSystem cs = new CodeSystem();
    cs.setName(NameUtils.nameToIdentifier(conceptSchemeLabel, IdentifierType.VARIABLE));
    cs.setTitle(conceptSchemeLabel);
    cs.setUrl(conceptScheme.getURI());
    cs.setVersion(assetId.getVersionTag());
    cs.setCompositional(true);
    cs.setIdentifier(new Identifier()
        .setValue(conceptScheme.getURI()));

    UUID mainUUID = defaultArtifactId(assetId, FHIR_STU3, assetId.getVersionTag()).getUuid();
    UUID vsUUID = Util.hashUUID(mainUUID, Util.uuid(FHIRAllTypes.VALUESET.toCode()));

    ValueSet vs = new ValueSet();
    vs.setId(vsUUID.toString());
    vs.setName(conceptSchemeLabel + " (Entire ValueSet)");

    if (withValueset) {
      cs.setValueSet("#" + vs.getId());
      cs.addExtension()
          .setUrl("http://hl7.org/fhir/StructureDefinition/codesystem-trusted-expansion")
          .setValue(new Reference().setReference(cs.getValueSet()));
      cs.addContained(vs);
    }

    // initially, index all Concepts
    Map<Resource, ConceptDefinitionComponent> allConcepts = new HashMap<>();
    model.listResourcesWithProperty(org.apache.jena.vocabulary.SKOS.inScheme, conceptScheme)
        .filterDrop(c -> isTopConcept(c, model))
        .forEachRemaining(c -> allConcepts.put(c, new ConceptDefinitionComponent()));

    //
    allConcepts.keySet().forEach(c -> populateConcept(c, cs, vs, model, allConcepts));

    return Optional.of(cs);
  }

  private boolean isTopConcept(Resource concept, Model model) {
    return model.contains(null, org.apache.jena.vocabulary.SKOS.hasTopConcept, concept);
  }

  private void populateConcept(
      Resource concept,
      CodeSystem cs,
      ValueSet vs,
      Model model,
      Map<Resource, ConceptDefinitionComponent> index) {
    ConceptDefinitionComponent cd = index.get(concept);

    String notation = getNotation(model, concept);
    String identifier = getUUID(model, concept);

    cd.setCode(notation);

    if (!cd.getCode().equals(notation)) {
      cd.addProperty()
          .setCode(org.apache.jena.vocabulary.SKOS.notation.getLocalName())
          .setValue(new StringType(notation));
    }
    if (!cd.getCode().equals(identifier)) {
      cd.addProperty()
          .setCode(DCTerms.identifier.getLocalName())
          .setValue(new StringType(identifier));
    }

    Optional<String> referent = getReferent(model, concept);
    referent.ifPresent(cd::setDefinition);

    populateLabels(cd, concept, model);
    vs.getExpansion()
        .addContains()
        .setCode(notation)
        .setDisplay(cd.getDisplay())
        .setSystem(cs.getUrl());

    List<Resource> parents = model.listObjectsOfProperty(concept,
            org.apache.jena.vocabulary.SKOS.broader)
        .filterKeep(n -> n instanceof Resource)
        .mapWith(Resource.class::cast)
        .toList();

    // avoid reflexivity  of skos:broader
    parents.remove(concept);

    cs.addConcept(cd);
    parents.stream()
        .filter(c -> !isTopConcept(c, model))
        .forEach(parent -> index.get(parent).addConcept(cd));

  }

  private String getNotation(Model model, Resource concept) {
    return Optional.ofNullable(model.getProperty(concept, org.apache.jena.vocabulary.SKOS.notation))
        .map(Statement::getObject)
        .map(RDFNode::asLiteral)
        .map(Objects::toString)
        .orElseGet(() -> URI.create(concept.getURI()).getFragment());
  }

  private String getUUID(Model model, Resource concept) {
    // The OWL - SKOS - FHIR mapping prefers identifier (always guid) over notation for consistency
    return Optional.ofNullable(model.getProperty(concept, DCTerms.identifier))
        .map(Statement::getObject)
        .map(RDFNode::asLiteral)
        .map(Objects::toString)
        .orElseGet(() -> URI.create(concept.getURI()).getFragment());
  }


  private void populateLabels(ConceptDefinitionComponent cd, Resource concept, Model model) {
    // Display label
    Optional<String> prefLabel =
        getLabel(model, concept, org.apache.jena.vocabulary.SKOS.prefLabel);
    // Explicit label
    Optional<String> altLabel =
        getLabel(model, concept, org.apache.jena.vocabulary.SKOS.altLabel);
    // Technical label
    Optional<String> hiddenLabel =
        getLabel(model, concept, org.apache.jena.vocabulary.SKOS.hiddenLabel);
    // Fallback Option
    Optional<String> genericLabel =
        getLabel(model, concept, RDFS.label);

    prefLabel.or(() -> altLabel).or(() -> genericLabel)
        .ifPresent(cd::setDisplay);
    prefLabel.ifPresent(l -> addDesignation(l, cd, org.apache.jena.vocabulary.SKOS.prefLabel));
    hiddenLabel.ifPresent(l -> addDesignation(l, cd, org.apache.jena.vocabulary.SKOS.hiddenLabel));
    altLabel.ifPresent(l -> addDesignation(l, cd, org.apache.jena.vocabulary.SKOS.altLabel));
  }

  private void addDesignation(
      String label,
      ConceptDefinitionComponent cd,
      Property labelType) {
    cd.addDesignation()
        .setValue(label)
        .setLanguage(Language.English.getTag())
        .setUse(toCode(labelType));
  }

  private Coding toCode(Property labelType) {
    return new Coding()
        .setCode(labelType.getLocalName())
        .setSystem(org.apache.jena.vocabulary.SKOS.getURI());
  }

  private Optional<String> getLabel(Model model, Resource resource, Property labelType) {
    return Optional.ofNullable(
            model.getProperty(resource, labelType))
        .map(Statement::getObject)
        .map(RDFNode::asLiteral)
        .map(Literal::getString);
  }

  private Optional<String> getReferent(Model model, Resource resource) {
    Optional<String> str = Optional.ofNullable(model.getProperty(resource, RDFS.isDefinedBy))
        .map(Statement::getObject)
        .map(RDFNode::asLiteral)
        .map(Objects::toString);
    if (str.isEmpty() && !resource.getURI().contains("nlpService")) {
      // NLP concepts are known to NOT have a referent - no need to provide a warning
      if (logger.isWarnEnabled()) {
        logger.warn("Warning: no referent for {} : {}",
            resource.getURI(),
            getLabel(model, resource, RDFS.label));
      }
    }
    return str;
  }


  private boolean enableValueset(Properties config) {
    var flag = (String) config.getOrDefault(CFG_INCLUDE_VALUESET, Boolean.FALSE.toString());
    return Boolean.parseBoolean(flag);
  }
}
