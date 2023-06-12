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
package edu.mayo.kmdp.language.ccg;

import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetCatalogApiInternal;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetRepositoryApiInternal;
import edu.mayo.kmdp.api.terminology.v4.server.TermsApiInternal._lookupTerm;

import static edu.mayo.kmdp.util.PropertiesUtil.serializeProps;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.Defines;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.omg.spec.api4kp._20200801.id.Term.multiGroupByConcept;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper.getAnnotationValues;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetrole.KnowledgeAssetRoleSeries.Operational_Concept_Definition;

import edu.mayo.kmdp.api.ccgl.v3.server.GlossaryLibraryApiInternal;
import edu.mayo.kmdp.ccg.model.Glossary;
import edu.mayo.kmdp.ccg.model.GlossaryEntry;
import edu.mayo.kmdp.language.translators.surrogate.v2.SurrogateV2ToCcgEntry;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.Util;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import javax.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.TransxionApiInternal._applyTransrepresent;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.id.Term;
import org.omg.spec.api4kp._20200801.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeprocessingtechnique.KnowledgeProcessingTechnique;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeprocessingtechnique.KnowledgeProcessingTechniqueSeries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GlossaryLibraryService implements GlossaryLibraryApiInternal {

  private static final Logger logger = LoggerFactory.getLogger(GlossaryLibraryService.class);

  private static final String CCG_TYPE = Operational_Concept_Definition.getTag();

  private final KnowledgeAssetCatalogApiInternal assetCatalog;
  private final KnowledgeAssetRepositoryApiInternal assetRepo;
  private final _applyTransrepresent translator;
  private final _lookupTerm resolver;


  @Inject
  public GlossaryLibraryService(
      @KPServer KnowledgeAssetCatalogApiInternal catalogApi,
      @KPServer KnowledgeAssetRepositoryApiInternal assetRepoApi,
      _lookupTerm resolver) {
    super();

    this.assetCatalog = catalogApi;
    this.assetRepo = assetRepoApi;

    this.translator = new SurrogateV2ToCcgEntry();
    this.resolver = resolver;
  }

  @Override
  public Answer<List<Glossary>> listGlossaries() {
    var oneGlossary = defaultGlossary();
    return Answer.of(List.of(oneGlossary));
  }

  @Override
  public Answer<Glossary> getGlossary(String glossaryId) {
    if (!Util.isEmpty(glossaryId) && ! "default".equalsIgnoreCase(glossaryId)) {
      return Answer.notFound();
    }
    return Answer.of(defaultGlossary());
  }

  protected Glossary defaultGlossary() {
    return new Glossary()
        .glossaryId("urn:default")
        .name("Default Concept Glosssary")
        .description("A CCG implementation that gathers all available assets");
  }

  @Override
  public Answer<List<GlossaryEntry>> listGlossaryEntries(String glossaryId, UUID scope) {
    var g = getGlossary(glossaryId);
    if (g.isFailure()) {
      return Answer.failed(g);
    }
    var definingAssets = getDefiningAssets(null, scope, null);

    return Answer.of(definingAssets.entrySet().stream()
        .map(def ->
            new GlossaryEntry()
                .defines(def.getKey().getConceptId().toString())
                .id(getCCGEntryId(def.getValue()).getUuid()))
        .collect(toList()));
  }

  @Override
  public Answer<GlossaryEntry> getGlossaryEntry(
      String glossaryId,
      UUID definedConceptId,
      UUID applicabilityScope,
      String method) {

    // Future: we may also return assets that exactly define a concept X, where X isA 'pcID'...
    // conversely, if an Asset defines 2+ concepts, the Asset is indexed once per concept...
    var definingAssets = getDefiningAssets(definedConceptId, applicabilityScope, method);

    if (definingAssets.isEmpty()) {
      return Answer.<GlossaryEntry>notFound()
          .withAddedExplanationMessage(
              "Unable to find operational definition for concept Id " + definedConceptId);
    }

    // ... and retrieveEntry should attribute the Assets to that Concept
    var glossaryEntry = definingAssets.values().stream()
        .flatMap(List::stream)
        .distinct()
        .map(defAsset -> this.retrieveEntry(definedConceptId, defAsset.getAssetId()))
        .flatMap(StreamUtil::trimStream)
        .reduce((g1, g2) -> {
          g1.getDef().addAll(g2.getDef());
          return g1;
        });

    return Answer.ofTry(glossaryEntry);
  }

  private Map<Term, List<KnowledgeAsset>> getDefiningAssets(
      UUID pcId, UUID applicability, String methodTag) {
    var definedConcept = resolve(pcId);

    if (pcId != null && definedConcept.isEmpty()) {
      logger.warn(
          "PC ID `{}` was requested, but was unable to be resolved.", pcId);
      return emptyMap();
    }

    var applicabilitySituation = resolve(applicability);

    Optional<KnowledgeProcessingTechnique> method =
        KnowledgeProcessingTechniqueSeries.resolve(methodTag);

    if (StringUtils.isNotBlank(methodTag) && method.isEmpty()) {
      logger.warn(
          "Method Tag `{}` was requested, but was unable to be resolved.", methodTag);
    }

    List<Pointer> assetPointers = assetCatalog
        .listKnowledgeAssets(
            CCG_TYPE,
            definedConcept.isPresent() ? Defines.getTag() : null,
            definedConcept.map(Term::getEvokes).map(URI::toString).orElse(null),
            0, -1)
        .orElse(emptyList());

    return assetPointers.stream()
        .map(id -> assetCatalog.getKnowledgeAssetVersion(id.getUuid(), id.getVersionTag()))
        .flatMap(Answer::trimStream)
        .filter(ax -> isApplicable(ax, applicabilitySituation.orElse(null)))
        .filter(ax -> isApplicable(ax, method.orElse(null)))
        .collect(multiGroupByConcept(
            ax -> getAnnotationValues(ax, Defines)
        ));
  }

  protected Optional<Term> resolve(UUID conceptId) {
    if (conceptId == null) {
      return Optional.empty();
    }
    var resolved = resolver.lookupTerm(conceptId.toString(), null);
    if (resolved.isFailure()) {
      logger.warn(
          "Concept `{}` was requested, but not resolved.", conceptId);
    }
    return resolved.getOptionalValue()
        .map(Term.class::cast);
  }

  private ResourceIdentifier getCCGEntryId(List<KnowledgeAsset> knowledgeAssets) {
    if (knowledgeAssets == null || knowledgeAssets.isEmpty()) {
      throw new IllegalArgumentException("CCG entry IDs must be generated for entries"
          + " that include at least one defining asset");
    }
    return knowledgeAssets
        .stream()
        .map(KnowledgeAsset::getAssetId)
        .reduce(SemanticIdentifier::hashIdentifiers)
        .orElseThrow(() -> new IllegalArgumentException("Unable to create a CCG entry ID"
            + " from a list of defining assets IDs"));
  }


  private boolean isApplicable(KnowledgeAsset asset,
      Term applicabilitySituation) {
    if (applicabilitySituation == null) {
      return true;
    }
    if (asset.getApplicableIn() == null || asset.getApplicableIn().getSituation().isEmpty()) {
      return false;
    }
    var assetApplicability =
        resolve(asset.getApplicableIn().getSituation().get(0).getUuid());

    return assetApplicability
        .map(cs -> cs.getTag().equals(applicabilitySituation.getTag()))
        .orElse(false);
  }

  private boolean isApplicable(KnowledgeAsset asset,
      KnowledgeProcessingTechnique method) {
    if (method == null) {
      return true;
    }
    return asset.getProcessingMethod().stream()
        .anyMatch(actualMethod -> actualMethod.sameAs(method) || actualMethod.hasAncestor(method));
  }

  private Optional<GlossaryEntry> retrieveEntry(UUID pcId, ResourceIdentifier assetId) {
    Answer<CompositeKnowledgeCarrier> definitionMetadata = this.assetRepo
        .getAnonymousCompositeKnowledgeAssetSurrogate(
            assetId.getUuid(), assetId.getVersionTag());

    if (!definitionMetadata.isSuccess()) {
      return Optional.empty();
    }

    Properties cfg = new Properties();
    cfg.put(SurrogateV2ToCcgEntry.CFG_FOCAL_CONCEPT_UUID, pcId.toString());
    return translator.applyTransrepresent(
            definitionMetadata.get(), null, serializeProps(cfg))
        .map(kc -> kc.as(GlossaryEntry.class))
        .orElse(Optional.empty());
  }


}
