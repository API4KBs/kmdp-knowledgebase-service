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
package edu.mayo.kmdp.language.translators.surrogate.v2;


import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.Defines;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.In_Terms_Of;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.Is_Applicable_To;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Effectuates;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Imports;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Includes_By_Reference;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetrole.KnowledgeAssetRoleSeries.Operational_Concept_Definition;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Syntactic_Translation_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Clinical_Concept_Glossary;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;

import edu.mayo.kmdp.ccg.model.GlossaryEntry;
import edu.mayo.kmdp.ccg.model.KnowledgeResourceRef;
import edu.mayo.kmdp.ccg.model.OperationalDefinition;
import edu.mayo.kmdp.language.TransionApiOperator;
import edu.mayo.kmdp.language.parsers.surrogate.v2.Surrogate2Parser;
import edu.mayo.kmdp.language.translators.AbstractSimpleTranslator;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.Util;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.id.ConceptIdentifier;
import org.omg.spec.api4kp._20200801.id.Identifier;
import org.omg.spec.api4kp._20200801.id.KeyIdentifier;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.id.Term;
import org.omg.spec.api4kp._20200801.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder;
import org.omg.spec.api4kp._20200801.services.transrepresentation.TransrepresentationOperator;
import org.omg.spec.api4kp._20200801.surrogate.Annotation;
import org.omg.spec.api4kp._20200801.surrogate.Dependency;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.omg.spec.api4kp._20200801.terms.ConceptTerm;

@KPOperation(Syntactic_Translation_Task)
public class SurrogateV2ToCcgEntry extends AbstractSimpleTranslator<KnowledgeAsset, GlossaryEntry>
    implements TransionApiOperator {

  public static final UUID OP_ID = UUID.fromString("366409fc-71cf-44b3-a56f-d6ef5bd7fb92");
  public static final String VERSION = "1.0.0";

  /**
   * When an Knowledge Asset defines multiple concepts at the same time (e.g. C1, C2), passing a
   * specific concept (e.g. C1) ensures that the Glossary Entry is returned as an OpDef of that
   * concept specifically.
   */
  public static final String CFG_FOCAL_CONCEPT_UUID = "FOCAL_CONCEPT";

  protected Surrogate2Parser lifter = new Surrogate2Parser();

  protected TransrepresentationOperator op = new TransrepresentationOperator()
      .withOperatorId(getOperatorId())
      .withFrom(getFrom())
      .withInto(getInto());

  public SurrogateV2ToCcgEntry() {
    setId(SemanticIdentifier.newId(OP_ID, VERSION));
  }

  @Override
  public List<SyntacticRepresentation> getFrom() {
    return lifter.getFrom();
  }

  @Override
  public List<SyntacticRepresentation> getInto() {
    return Collections.singletonList(rep(Clinical_Concept_Glossary));
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return Knowledge_Asset_Surrogate_2_0;
  }

  @Override
  public KnowledgeRepresentationLanguage getTargetLanguage() {
    return Clinical_Concept_Glossary;
  }


  @Override
  protected Optional<KnowledgeCarrier> applyTransrepresentation(
      KnowledgeCarrier src,
      SyntacticRepresentation tgtRep,
      Properties config) {

    UUID focusConceptId = Optional.ofNullable(config.get(CFG_FOCAL_CONCEPT_UUID))
        .map(Object::toString)
        .map(UUID::fromString)
        .orElse(null);

    if (src instanceof CompositeKnowledgeCarrier) {
      CompositeKnowledgeCarrier ckc = (CompositeKnowledgeCarrier) src;
      KnowledgeCarrier main = ckc.mainComponent();
      List<KnowledgeCarrier> subs = ckc.getComponent().stream()
          .filter(kc -> kc != main)
          .collect(Collectors.toList());
      return Optional.of(translate(main, subs, focusConceptId));
    } else {
      return Optional.ofNullable(translate(src, focusConceptId));
    }

  }


  protected KnowledgeCarrier translate(KnowledgeCarrier main, UUID focusConceptId) {
    return translate(main, Collections.emptyList(), focusConceptId);
  }

  protected KnowledgeCarrier translate(
      KnowledgeCarrier main, List<KnowledgeCarrier> subs, UUID focusConceptId) {

    if (main.getRepresentation() != null) {
      String xAccept = ModelMIMECoder.encode(rep(Knowledge_Asset_Surrogate_2_0));
      main = lifter.applyLift(main, Abstract_Knowledge_Expression, xAccept, null)
          .orElseThrow(() -> new RuntimeException("'applyLift' failed"));
      subs = subs.stream()
          .map(sub -> lifter.applyLift(sub, Abstract_Knowledge_Expression, xAccept, null))
          .collect(Answer.toList())
          .orElseThrow(() -> new RuntimeException("'applyLift' failed"));
    }

    KnowledgeAsset mainAsset = main.as(KnowledgeAsset.class)
        .orElseThrow(IllegalArgumentException::new);

    Set<KnowledgeAsset> includeds =
        subs.stream()
            .map(kc -> kc.as(KnowledgeAsset.class))
            .flatMap(StreamUtil::trimStream)
            .collect(Collectors.toSet());

    return AbstractCarrier.ofAst(toCCGEntry(mainAsset, includeds, focusConceptId))
        .withRepresentation(getInto().get(0));
  }

  private GlossaryEntry toCCGEntry(
      KnowledgeAsset mainAsset, Set<KnowledgeAsset> includeds, UUID focusConceptId) {
    var glossaryEntry = new GlossaryEntry();

    var definedConcept = getDefines(mainAsset, focusConceptId)
        .orElseThrow(
            () -> new IllegalStateException("CCG ENTRIES MUST HAVE A 'defines PC' ANNOTATION "));
    glossaryEntry.id(
        mintGlossaryEntryId(mainAsset.getAssetId(), definedConcept.getConceptId().toString()));

    var od = toOpDef(mainAsset,definedConcept.getUuid(), includeds);

    return glossaryEntry
        .focus(focusConceptId != null ? focusConceptId.toString() : null)
        .defines(definedConcept.getConceptId().toString())
        .addDefItem(od);
  }

  public static UUID mintGlossaryEntryId(ResourceIdentifier assetId, String definedConceptId) {
    var key = assetId.getVersionId().toString() + Defines.getTag() + definedConceptId;
    return Util.uuid(key);
  }


  OperationalDefinition toOpDef(
      KnowledgeAsset knowledgeAsset,
      UUID definedConcept,
      Set<KnowledgeAsset> includedKnowledgeAssets) {
    OperationalDefinition opDef = new OperationalDefinition()
        .id(knowledgeAsset.getAssetId().getVersionId().toString())
        .description(knowledgeAsset.getDescription())
        .name(knowledgeAsset.getName())
        .processingMethod(allConcepts(knowledgeAsset.getProcessingMethod()))
        .defines(definedConcept)
        .applicabilityScope(getAnnotation(Is_Applicable_To, knowledgeAsset))
        .inTermsOf(getAnnotation(In_Terms_Of, knowledgeAsset))
        .effectuates(getEffectuatesReference(knowledgeAsset, includedKnowledgeAssets)
            .orElse(null));

    if (!knowledgeAsset.getCarriers().isEmpty()) {
      KnowledgeArtifact expression =
          knowledgeAsset.getCarriers().get(0);
      KnowledgeResourceRef ref = new KnowledgeResourceRef()
          .assetId(knowledgeAsset.getAssetId().getVersionId().toString())
          .artifactId(
              expression.getArtifactId().getVersionId().toString())
          .mimeCode(expression.getRepresentation() != null
              ? codedRep(expression.getRepresentation()) : null)
          .href(knowledgeAsset.getAssetId().getVersionId().toString())
          .assetType(allTypes(knowledgeAsset.getFormalType()))
          .inlinedExpr(expression.getInlinedExpression())
          .publicationStatus(getStatus(expression, knowledgeAsset));

      opDef.computableSpec(ref);
    }

    List<KeyIdentifier> includes = getIncludes(knowledgeAsset);
    includedKnowledgeAssets.stream()
        .filter(asset -> includes.contains(asset.getAssetId().asKey()))
        .filter(
            asset -> asset.getRole().stream().anyMatch(Operational_Concept_Definition::sameAs))
        .forEach(subDef -> {
          var sd = this.toOpDef(subDef, definedConcept, includedKnowledgeAssets);
          opDef.addIncludesItem(sd);
        });

    return opDef;
  }

  private String getStatus(KnowledgeArtifact expression, KnowledgeAsset knowledgeAsset) {
    return Optional.ofNullable(expression.getLifecycle())
        .flatMap(l -> Optional.ofNullable(l.getPublicationStatus()))
        .or(() ->Optional.ofNullable(knowledgeAsset.getLifecycle())
            .flatMap(l -> Optional.ofNullable(l.getPublicationStatus())))
        .map(ConceptTerm::getTag)
        .orElse(null);
  }


  private List<String> allTypes(List<? extends Term> terms) {
    return terms.stream()
        .map(Identifier::getTag).collect(Collectors.toList());
  }


  private List<String> allConcepts(List<? extends Term> terms) {
    return terms.stream()
        .map(Identifier::getTag).collect(Collectors.toList());
  }

  private Optional<ConceptIdentifier> getDefines(KnowledgeAsset knowledgeAsset, UUID pcId) {
    return knowledgeAsset.getAnnotation().stream()
        .filter(anno -> Defines.sameTermAs(anno.getRel()))
        .map(Annotation::getRef)
        .filter(cid -> pcId == null || pcId.equals(cid.getUuid()))
        .findAny();
  }

  private List<String> getAnnotation(Term rel, KnowledgeAsset knowledgeAsset) {
    return knowledgeAsset.getAnnotation().stream()
        .filter(anno -> rel.sameTermAs(anno.getRel()))
        .map(Annotation::getRef)
        .map(c -> c.getConceptId().toString())
        .collect(Collectors.toList());
  }


  private List<KeyIdentifier> getIncludes(KnowledgeAsset knowledgeAsset) {
    return knowledgeAsset.getLinks().stream()
        .flatMap(StreamUtil.filterAs(Dependency.class))
        .filter(x -> Includes_By_Reference.sameAs(x.getRel())
            || Imports.sameAs(x.getRel()))
        .map(Dependency::getHref)
        .map(SemanticIdentifier::asKey)
        .collect(Collectors.toList());
  }

  private Optional<String> getEffectuatesReference(
      KnowledgeAsset knowledgeAsset,
      Set<KnowledgeAsset> includes) {
    if (knowledgeAsset.getRole().stream()
        .noneMatch(Operational_Concept_Definition::sameAs)) {
      return Optional.empty();
    }

    return this.getEffectuates(knowledgeAsset)
        .flatMap(assetId -> includes.stream()
            .filter(asset -> asset.getAssetId().asKey().equals(assetId.asKey()))
            .findFirst())
        .map(dataTypeAsset -> {
          List<KnowledgeArtifact> carriers = dataTypeAsset.getCarriers();

          if (carriers.isEmpty()) {
            return null;
          }

          if (carriers.size() > 1) {
            throw new UnsupportedOperationException("More than one 'Effectuates' carrier found.");
          }

          KnowledgeArtifact dataShapeCarrier = carriers.get(0);
          if (dataShapeCarrier.getLocator() != null) {
            return dataShapeCarrier.getLocator().toString();
          } else if (!dataShapeCarrier.getSecondaryId().isEmpty()) {
            return
                dataShapeCarrier.getSecondaryId().get(0).getResourceId().toString();
          } else {
            return dataShapeCarrier.getArtifactId().getResourceId().toString();
          }

        });

  }

  private Optional<ResourceIdentifier> getEffectuates(KnowledgeAsset knowledgeAsset) {
    return knowledgeAsset.getLinks().stream()
        .flatMap(StreamUtil.filterAs(Dependency.class))
        .filter(x -> Effectuates.sameAs(x.getRel()))
        .map(Dependency::getHref)
        .findFirst();
  }

  public static GlossaryEntry merge(GlossaryEntry g1, GlossaryEntry g2) {
    g1.setId(Util.hashUUID(g1.getId(), g2.getId()));
    g1.getDef().addAll(g2.getDef());

    return g1;
  }


}
