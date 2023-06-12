/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.mayo.kmdp.terms;

import static edu.mayo.kmdp.terms.components.TermsMessages.OFFLINE_MSG;
import static edu.mayo.kmdp.terms.components.TermsMessages.REINDEX_FAILED;
import static edu.mayo.kmdp.util.NameUtils.getTrailingPart;
import static edu.mayo.kmdp.util.Util.isEmpty;
import static edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries.ServiceUnavailable;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newKey;
import static org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder.decodeAll;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.API4KP_Datatypes;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;

import ca.uhn.fhir.context.FhirContext;
import edu.mayo.kmdp.language.translators.surrogate.v2.SurrogateV2ToHTML;
import edu.mayo.kmdp.terms.components.BasicCodeSystemToHTML;
import edu.mayo.kmdp.terms.components.TermsSearcher;
import edu.mayo.kmdp.util.DateTimeUtil;
import edu.mayo.kmdp.util.JSonUtil;
import edu.mayo.kmdp.util.URIUtil;
import edu.mayo.kmdp.util.Util;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.hl7.fhir.dstu3.model.CodeSystem;
import org.hl7.fhir.dstu3.model.CodeSystem.ConceptDefinitionComponent;
import org.hl7.fhir.dstu3.model.CodeSystem.ConceptDefinitionDesignationComponent;
import org.hl7.fhir.dstu3.model.Reference;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetCatalogApi;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetRepositoryApi;
import edu.mayo.kmdp.api.terminology.v4.server.TermsApiInternal;
import org.omg.spec.api4kp._20200801.id.IdentifierConstants;
import org.omg.spec.api4kp._20200801.id.KeyIdentifier;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.Term;
import org.omg.spec.api4kp._20200801.services.KPComponent;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries;
import org.omg.spec.api4kp._20200801.terms.model.ConceptDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@KPComponent(implementation = "fhir")
public class TermsFHIRFacade implements TermsApiInternal, CompositeTermsServer {

  private static final Logger logger = LoggerFactory.getLogger(TermsFHIRFacade.class);
  private static final FhirContext fhirContext = FhirContext.forDstu3();


  @Value("${edu.mayo.kmdp.terms.search.enabled:false}")
  protected boolean searchEnabled = false;

  @Value("${edu.mayo.kmdp.kasrs.repository.defaultRepoUrl:http://localhost:8080/kar}")
  protected String kasrURL;

  @Autowired(required = false)
  TermsContextAwareHrefBuilder hrefBuilder;

  protected KnowledgeAssetCatalogApi cat;
  protected KnowledgeAssetRepositoryApi repo;
  protected AtomicBoolean online = new AtomicBoolean(false);

  protected final Map<KeyIdentifier, CodeSystem> schemeIndex = new ConcurrentHashMap<>();
  protected final Map<UUID, ConceptDefinitionComponent> conceptIndex = new ConcurrentHashMap<>();

  protected TermsSearcher searchEngine;

  public TermsFHIRFacade() {
    // nothing to do - @PostConstruct will initialize the data structures
  }

  public TermsFHIRFacade(KnowledgeAssetCatalogApi cat, KnowledgeAssetRepositoryApi repo) {
    // test constructor
    this(cat, repo, false);
  }

  public TermsFHIRFacade(
      KnowledgeAssetCatalogApi cat, KnowledgeAssetRepositoryApi repo, boolean searchEnabledFlag) {
    // test constructor
    this.cat = cat;
    this.repo = repo;
    this.searchEnabled = searchEnabledFlag;

    init();
  }

  @PostConstruct
  void init() {
    if (cat == null && repo == null) {
      cat = KnowledgeAssetCatalogApi.newInstance(kasrURL);
      repo = KnowledgeAssetRepositoryApi.newInstance(kasrURL);
    }

    if (searchEnabled) {
      searchEngine = new TermsSearcher();
    }

    reindex();
  }

  public TermsFHIRFacade withHrefBuilder(TermsContextAwareHrefBuilder hrefBuilder) {
    this.hrefBuilder = hrefBuilder;
    return this;
  }

  @Override
  public Answer<List<Pointer>> listTerminologies() {
    if (!online.get()) {
      return Answer.<List<Pointer>>failed(ServiceUnavailable).withExplanation(OFFLINE_MSG);
    }

    var ptrs = schemeIndex.values().stream()
        .map(this::toPointer)
        .collect(Collectors.toList());
    return Answer.of(ptrs);
  }

  @Override
  public Answer<Void> clearTerminologies() {
    return reindex();
  }

  @Override
  public Answer<KnowledgeCarrier> getVocabulary(UUID vocabularyId, String versionTag,
      String xAccept) {
    if (!online.get()) {
      return Answer.<KnowledgeCarrier>failed(ServiceUnavailable).withExplanation(OFFLINE_MSG);
    }

    var vocId = newId(vocabularyId, versionTag);
    if (!schemeIndex.containsKey(vocId.asKey())) {
      return Answer.notFound();
    }
    var cs = schemeIndex.get(vocId.asKey());

    if (negotiateHTML(xAccept)) {
      var carrier = AbstractCarrier.ofAst(cs)
          .withAssetId(vocId)
          .withRepresentation(rep(FHIR_STU3))
          .withLabel(cs.getName());
      return new BasicCodeSystemToHTML().applyTransrepresent(carrier, xAccept,
          hrefBuilder != null ? hrefBuilder.getHost() : null);
    } else {
      var carrier = AbstractCarrier.of(
              fhirContext.newJsonParser().encodeResourceToString(cs))
          .withAssetId(vocId)
          .withRepresentation(rep(FHIR_STU3, JSON, Charset.defaultCharset()))
          .withLabel(cs.getName());
      return Answer.of(carrier);
    }
  }

  @Override
  public Answer<ConceptDescriptor> getTerm(UUID vocabularyId, String versionTag, String conceptId) {
    if (!online.get()) {
      return Answer.<ConceptDescriptor>failed(ServiceUnavailable).withExplanation(OFFLINE_MSG);
    }

    return schemeIndex.containsKey(newKey(vocabularyId, versionTag))
        ? lookupTerm(conceptId)
        : Answer.notFound();
  }

  @Override
  public Answer<List<ConceptDescriptor>> getTerms(
      UUID vocabularyId, String versionTag,
      String labelFilter) {
    if (!online.get()) {
      return Answer.<List<ConceptDescriptor>>failed(ServiceUnavailable).withExplanation(OFFLINE_MSG);
    }

    return Answer.ofNullable(schemeIndex.get(newKey(vocabularyId, versionTag)))
        .map(cs -> cs.getConcept().stream()
            .map(cd -> toConceptDescriptor(cd, cs))
            .collect(Collectors.toList()));
  }

  @Override
  public Answer<ConceptDescriptor> lookupTerm(String conceptId, String xAccept) {
    if (!online.get()) {
      return Answer.<ConceptDescriptor>failed(ServiceUnavailable).withExplanation(OFFLINE_MSG);
    }

    UUID uuid = Util.ensureUUID(conceptId)
        .or(() -> Util.ensureUUID(getTrailingPart(conceptId)))
        .orElseGet(() -> Util.uuid(conceptId));

    if (! conceptIndex.containsKey(uuid)) {
      return Answer.notFound();
    }

    var cd = toConceptDescriptor(conceptIndex.get(uuid));

    if (negotiateHTML(xAccept)) {
      return Answer.referTo(
          hrefBuilder.fromTerm(
              getTrailingPart(cd.getNamespaceUri().toString()), conceptId),
          false);
    } else {
      return Answer.of(cd);
    }
  }

  @Override
  public Answer<KnowledgeCarrier> lookupTermInVocabulary(String vocabularyTag, String conceptId,
      String xAccept) {
    if (!online.get()) {
      return Answer.<KnowledgeCarrier>failed(ServiceUnavailable).withExplanation(OFFLINE_MSG);
    }

    var ans = lookupTerm(conceptId);
    if (negotiateHTML(xAccept)) {
      return ans.map(cd -> AbstractCarrier.of(
              SurrogateV2ToHTML.MakeHTML.makeHTML(cd))
          .withRepresentation(rep(HTML, TXT, Charset.defaultCharset())));
    } else {
      return ans.map(cd ->
          AbstractCarrier.of(JSonUtil.writeJsonAsString(cd).orElse("{}"))
              .withRepresentation(rep(API4KP_Datatypes, JSON, Charset.defaultCharset())));
    }
  }

  @Override
  public Answer<List<Pointer>> searchTerms(String searchQuery) {
    if (!searchEnabled) {
      return Answer.unsupported();
    }
    if (!online.get()) {
      return Answer.<List<Pointer>>failed(ServiceUnavailable).withExplanation(OFFLINE_MSG);
    }
    return searchEngine.searchTerms(searchQuery, hrefBuilder);
  }


  private boolean negotiateHTML(String xAccept) {
    return decodeAll(xAccept).stream().anyMatch(wr -> HTML.sameAs(wr.getRep().getLanguage()));
  }

  Answer<Void> reindex() {
    var testOnline = cat.getKnowledgeAssetCatalog().isSuccess();
    if (!testOnline) {
      return Answer.<Void>failed(ServiceUnavailable).withExplanation(REINDEX_FAILED);
    }

    Index collector = new Index();
    Answer<Void> ans = cat
        .listKnowledgeAssets(KnowledgeAssetTypeSeries.Lexicon.getTag(), null, null, 0, -1)
        .forEach(Pointer.class, ptr -> indexCodeSystemAsset(ptr, collector));
    if (!ans.isSuccess()) {
      logger.error("TermsFHIRFacade reindex was not successful. Original content unchanged.");
      return ans;
    }

    transferContentToPrimary(collector);
    online.set(true);
    return ans;
  }

  private void indexCodeSystemAsset(Pointer karsPointer, Index collector) {
    Answer<KnowledgeAsset> ans =
        cat.getKnowledgeAssetVersion(karsPointer.getUuid(), karsPointer.getVersionTag());
    ans.ifPresent(asset -> {
      Answer<CodeSystem> artf = fetchCodeSystemArtifact(asset);
      artf.ifPresent(cs -> indexCodeSystem(karsPointer.asKey(), cs, collector));
    });
  }

  private void indexCodeSystem(KeyIdentifier key, CodeSystem cs, Index collector) {
    collector.tempSchemeIndex.put(key, cs);
    cs.getConcept()
        .forEach(cd -> {
          cd.addExtension().setValue(new Reference(cs));
          collector.tempConceptIndex.put(
              Util.ensureUUID(cd.getCode()).orElseGet(() -> Util.uuid(cd.getCode())),
              cd);
        });
  }

  private Answer<CodeSystem> fetchCodeSystemArtifact(KnowledgeAsset asset) {
    var fhirCarrier = asset.getCarriers().stream()
        .filter(ka -> ka.getRepresentation() != null
            && FHIR_STU3.sameAs(ka.getRepresentation().getLanguage()))
        .findFirst();
    return fhirCarrier.isPresent()
        ? fetchCodeSystemArtifact(asset.getAssetId(), fhirCarrier.get().getArtifactId())
        : Answer.notFound();
  }

  private Answer<CodeSystem> fetchCodeSystemArtifact(
      ResourceIdentifier assetId, ResourceIdentifier artifactId) {
    return repo.getKnowledgeAssetCarrierVersion(
            assetId.getUuid(), assetId.getVersionTag(),
            artifactId.getUuid(), artifactId.getVersionTag(),
            codedRep(FHIR_STU3))
        .flatOpt(AbstractCarrier::asBinary)
        .map(ByteArrayInputStream::new)
        .map(bais -> fhirContext.newJsonParser().parseResource(CodeSystem.class, bais));
  }


  private Pointer toPointer(CodeSystem codeSystem) {
    URI u = URI.create(codeSystem.getUrl());
    var p = newId(
        URI.create(u.getScheme() + u.getSchemeSpecificPart()),
        u.getFragment(),
        codeSystem.getVersion())
        .toPointer()
        .withName(codeSystem.getName());
    return hrefBuilder != null
        ? p.withHref(hrefBuilder.fromTerminologyPointer(p))
        : p;
  }

  private synchronized void transferContentToPrimary(Index collector) {
    schemeIndex.clear();
    schemeIndex.putAll(collector.tempSchemeIndex);

    conceptIndex.clear();
    conceptIndex.putAll(collector.tempConceptIndex);

    if (searchEnabled) {
      searchEngine.index(collector.tempConceptIndex);
    }

    collector.clear();
  }


  private ConceptDescriptor toConceptDescriptor(ConceptDefinitionComponent cscd) {
    var ref = (Reference)cscd.getExtensionFirstRep().getValue();
    var cs = (CodeSystem) ref.getResource();
    return toConceptDescriptor(cscd, cs);
  }

  private ConceptDescriptor toConceptDescriptor(ConceptDefinitionComponent cd, CodeSystem cs) {
    ConceptDescriptor descr = new ConceptDescriptor();
    descr.withLabels(mapDesignations(cd))
        .withTag(cd.getCode())
        .withResourceId(conceptId(cs.getUrl(), codeToUUID(cd.getCode()).toString()))
        .withUuid(codeToUUID(cd.getCode()))
        .withName(cd.getDisplay())
        .withVersionTag(cs.getVersion())
        .withVersionId(conceptId(cs.getUrl(), cs.getVersion(), codeToUUID(cd.getCode()).toString()))
        .withNamespaceUri(URIUtil.normalizeURI(URI.create(cs.getUrl())))
        .withEstablishedOn(DateTimeUtil.parseDate(cs.getVersion(), "yyyyMMdd"))
        .withReferentId(isEmpty(cd.getDefinition()) ? null : URI.create(cd.getDefinition()));
    descr.setAncestors(new Term[0]);
    descr.setClosure(new Term[0]);
    return descr;
  }

  private Map<String, String> mapDesignations(ConceptDefinitionComponent cd) {
    return cd.getDesignation().stream()
        .collect(Collectors.toMap(
            dx -> dx.getUse().getCode(),
            ConceptDefinitionDesignationComponent::getValue));
  }

  private UUID codeToUUID(String code) {
    return Util.ensureUUID(code).orElseGet(() -> Util.uuid(code));
  }

  private URI conceptId(String url, String version, String code) {
    String ns = url;
    int idx = url.lastIndexOf('#');
    if (idx > 0) {
      ns = ns.substring(0, idx);
    }
    if (Util.isNotEmpty(version)) {
      ns = ns + IdentifierConstants.VERSIONS + version;
    }
    if (Util.isNotEmpty(code)) {
      ns = ns + "#" + code;
    }
    return URI.create(ns);
  }

  private URI conceptId(String url, String code) {
    return conceptId(url, null, code);
  }

  @Override
  public TYPE getType() {
    return TYPE.FHIR;
  }

  @Override
  public String getSource() {
    return kasrURL;
  }

  @Override
  public Optional<TermsApiInternal> getFHIRBasedComponent() {
    return Optional.of(this);
  }


  private static class Index {
    final Map<KeyIdentifier, CodeSystem> tempSchemeIndex = new HashMap<>();
    final Map<UUID, ConceptDefinitionComponent> tempConceptIndex = new HashMap<>();

    public void clear() {
      tempConceptIndex.clear();
      tempSchemeIndex.clear();
    }
  }

}