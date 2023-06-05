package edu.mayo.kmdp.language.translators.surrogate.v2;

import static edu.mayo.kmdp.util.StreamUtil.filterAs;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.In_Terms_Of;
import static org.hl7.fhir.dstu3.model.Identifier.IdentifierUse.OFFICIAL;
import static org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType.CITATION;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder.encode;
import static org.omg.spec.api4kp._20200801.taxonomy.relatedversiontype.RelatedVersionTypeSeries.Has_Predecessor_Version;
import static org.omg.spec.api4kp._20200801.taxonomy.relatedversiontype.RelatedVersionTypeSeries.Has_Previous_Version;

import edu.mayo.kmdp.id.helper.DatatypeHelper;
import edu.mayo.kmdp.registry.Registry;
import edu.mayo.kmdp.util.URIUtil;
import edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelType;
import edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DataRequirement;
import org.hl7.fhir.dstu3.model.DataRequirement.DataRequirementCodeFilterComponent;
import org.hl7.fhir.dstu3.model.Enumerations.PublicationStatus;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType;
import org.omg.spec.api4kp._20200801.id.ConceptIdentifier;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.surrogate.Annotation;
import org.omg.spec.api4kp._20200801.surrogate.Applicability;
import org.omg.spec.api4kp._20200801.surrogate.Component;
import org.omg.spec.api4kp._20200801.surrogate.Dependency;
import org.omg.spec.api4kp._20200801.surrogate.Derivative;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.Link;
import org.omg.spec.api4kp._20200801.surrogate.Publication;
import org.omg.spec.api4kp._20200801.surrogate.Version;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetType;
import org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormat;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries;
import org.omg.spec.api4kp._20200801.taxonomy.krprofile.KnowledgeRepresentationLanguageProfile;
import org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerialization;
import org.omg.spec.api4kp._20200801.taxonomy.lexicon.Lexicon;
import org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries;


public class SurrogateV2ToLibrary {

  public Library transform(KnowledgeAsset knowledgeAsset) {
    Library lib = new Library();

    mapSurrogateId(knowledgeAsset, lib);
    mapIdentifier(knowledgeAsset, lib);
    mapDescriptive(knowledgeAsset, lib);

    mapType(knowledgeAsset, lib);

    mapPublicationStatus(knowledgeAsset, lib);
    mapLifecycle(knowledgeAsset, lib);

    // FHIR and KMDP deal with applicability / domain semantics
    // in a similar but not directly compatible way.
    // Differences in granularity and pre/post-coordination make the mapping complex
    mapIntentAndApplicability(knowledgeAsset, lib);
    mapUseContext(knowledgeAsset, lib);

    mapPolicies(knowledgeAsset, lib);

    mapTopics(knowledgeAsset, lib);

    // Mapping of contributors is Not supported

    mapRelateds(knowledgeAsset, lib);
    mapBiblio(knowledgeAsset, lib);

    // in KMDP data dependencies are 'semantic', i.e. defined just in terms of a concept
    // The service "Concept Glossary Library' allows to map concepts to data definitions,
    // including FHIR profiles, etc..
    // This 'weaving' happens at a later stage, after the Library has been translated into
    mapDataRequirements(knowledgeAsset, lib);

    // For each artifact, map it..
    knowledgeAsset.getCarriers().stream()
        .flatMap(filterAs(KnowledgeArtifact.class))
        .forEach(cka -> lib.addContent(mapKnowledgeArtifact(cka)));

    return lib;
  }

  private Attachment mapKnowledgeArtifact(KnowledgeArtifact cka) {
    Attachment artifact = new Attachment();

    // (Version) URI are supposed to be dereferenceable
    artifact.setUrl(cka.getArtifactId().getVersionId().toString());

    if (!cka.getLocalization().isEmpty()) {
      artifact.setLanguage(cka.getLocalization().get(0).getTag());
    }

    artifact.setTitle(cka.getTitle());

    if (cka.getRepresentation() != null) {
      SyntacticRepresentation r = cka.getRepresentation();
      artifact.setContentType(encode(rep(
          mapLanguage(r.getLanguage()),
          mapProfile(r.getProfile()),
          mapSerialization(r.getSerialization()),
          mapFormat(r.getFormat()),
          null,
          null,
          mapLexicon(r.getLexicon()))
      ));
    }

    if (cka.getLifecycle() != null) {
      artifact.setCreation(cka.getLifecycle().getCreatedOn());
    }

    if (cka.getInlinedExpression() != null) {
      artifact.setData(cka.getInlinedExpression().getBytes());
    }

    return artifact;
  }

  private KnowledgeRepresentationLanguage mapLanguage(
      KnowledgeRepresentationLanguage language) {
    if (language == null) {
      return null;
    }
    return KnowledgeRepresentationLanguageSeries
        .resolveUUID(language.getUuid()).orElse(null);
  }

  private org.omg.spec.api4kp._20200801.taxonomy.krprofile.KnowledgeRepresentationLanguageProfile mapProfile(
      KnowledgeRepresentationLanguageProfile profile) {
    if (profile == null) {
      return null;
    }
    return org.omg.spec.api4kp._20200801.taxonomy.krprofile.KnowledgeRepresentationLanguageProfileSeries
        .resolveUUID(profile.getUuid()).orElse(null);
  }

  private org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerialization mapSerialization(
      KnowledgeRepresentationLanguageSerialization serialization) {
    if (serialization == null) {
      return null;
    }
    return org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries
        .resolveUUID(serialization.getUuid()).orElse(null);
  }

  private org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormat mapFormat(
      SerializationFormat format) {
    if (format == null) {
      return null;
    }
    return org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries
        .resolveUUID(format.getUuid()).orElse(null);
  }

  private List<Lexicon> mapLexicon(
      Collection<Lexicon> lexicaList) {
    if (lexicaList == null || lexicaList.isEmpty()) {
      return Collections.emptyList();
    }
    return lexicaList.stream()
        .map(lexicon -> org.omg.spec.api4kp._20200801.taxonomy.lexicon.LexiconSeries
            .resolveUUID(lexicon.getUuid()).orElse(null))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }


  private void mapDataRequirements(KnowledgeAsset knowledgeAsset, Library lib) {
    knowledgeAsset.getAnnotation().stream()
        .flatMap(filterAs(Annotation.class))
        .forEach(ann -> mapInputAnnotations(ann, lib));
  }

  private void mapInputAnnotations(Annotation ann, Library lib) {
    Optional<SemanticAnnotationRelType> rel = SemanticAnnotationRelTypeSeries
        .resolveUUID(ann.getRel().getUuid());
    if (rel.isPresent() &&
        In_Terms_Of.sameAs(rel.get())) {
      lib.getDataRequirement().add(
          new DataRequirement()
              .setCodeFilter(Collections.singletonList(
                  new DataRequirementCodeFilterComponent()
                      .addValueCodeableConcept(toCode(ann.getRef()))
              ))
      );
    }
  }

  private void mapBiblio(KnowledgeAsset knowledgeAsset, Library lib) {
    knowledgeAsset.getCitations()
        .forEach(cit -> lib.getRelatedArtifact().add(
            new RelatedArtifact()
                .setType(CITATION)
                .setCitation(cit.getBibliography())
        ));
  }

  private void mapRelateds(KnowledgeAsset knowledgeAsset, Library lib) {
    knowledgeAsset.getLinks().forEach(assoc ->
        lib.getRelatedArtifact().add(
            new RelatedArtifact()
                .setType(mapRelatedType(assoc))
                .setDisplay(assoc.getHref().getName())
                .setUrl(assoc.getHref().getVersionId().toString())
        ));
  }

  private RelatedArtifactType mapRelatedType(Link assoc) {
    // KMDP Relationship will be simplified in v2
    if (assoc instanceof Dependency) {
      return RelatedArtifactType.DEPENDSON;
    }
    if (assoc instanceof Derivative) {
      return RelatedArtifactType.DERIVEDFROM;
    }
    if (assoc instanceof Component) {
      return RelatedArtifactType.COMPOSEDOF;
    }
    if (assoc instanceof Version) {
      Version v = (Version) assoc;
      if (Has_Previous_Version.sameAs(v.getRel())
          || Has_Predecessor_Version.sameAs(v.getRel())
      ) {
        return RelatedArtifactType.PREDECESSOR;
      } else {
        return RelatedArtifactType.SUCCESSOR;
      }
    }
    return RelatedArtifactType.DOCUMENTATION;
  }

  private void mapTopics(KnowledgeAsset knowledgeAsset, Library lib) {
    knowledgeAsset.getAnnotation()
        .forEach(ann -> mapSubjectAnnotation(ann, lib));
  }

  private void mapSubjectAnnotation(Annotation ann, Library lib) {
    Optional<SemanticAnnotationRelType> rel = SemanticAnnotationRelTypeSeries
        .resolveUUID(ann.getRel().getUuid());
    if (rel.isPresent()) {
      switch (SemanticAnnotationRelTypeSeries.asEnum(rel.get())) {
        case Has_Primary_Subject:
        case Secondary_Subject:
        case Has_Focus:
          lib.getTopic().add(toCode(ann.getRef()));
          break;
        default:
      }
    }
  }

  private void mapPolicies(KnowledgeAsset knowledgeAsset, Library lib) {
    // KMDP does not yet implement policies in general, nor jurisdictions in particular
    lib.setCopyright("TODO");
  }

  private void mapUseContext(KnowledgeAsset knowledgeAsset, Library lib) {
    knowledgeAsset.getAnnotation()
        .forEach(ann -> mapUsageAnnotation(ann, lib));
  }

  private void mapUsageAnnotation(Annotation ann, Library lib) {
    Optional<SemanticAnnotationRelType> rel = SemanticAnnotationRelTypeSeries
        .resolveUUID(ann.getRel().getUuid());
    if (rel.isPresent()) {
      // Mayo has some annotations for 'Usage Context', but they are not yet implemented in KMDP
    }
    lib.setUseContext(Collections.emptyList());
  }

  private void mapIntentAndApplicability(
      KnowledgeAsset knowledgeAsset, Library lib) {
    // applicability is less formal in FHIR
    if (knowledgeAsset.getApplicableIn() != null) {
      Applicability app = knowledgeAsset.getApplicableIn();
      if (app.getSituation() != null) {
        lib.setUsage(app.getSituation().stream().map(ResourceIdentifier::getTag)
            .collect(Collectors.joining(",")));
      }
    }

    // but purpose is not modelled yet
    lib.setPurpose("TODO");
  }

  private void mapLifecycle(KnowledgeAsset knowledgeAsset, Library lib) {
    Publication pub = knowledgeAsset.getLifecycle();
    // Assuming Knowledge Resources are immutable, and that the 'createdon' refers to the latest version
    lib.setDate(pub.getCreatedOn());

    // Are these the right mapping?
    if (pub.getIssuedOn() != null) {
      lib.setApprovalDate(pub.getIssuedOn());
    }
    if (pub.getLastReviewedOn() != null) {
      lib.setLastReviewDate(pub.getLastReviewedOn());
    }
  }

  private void mapType(KnowledgeAsset knowledgeAsset, Library lib) {
    if (knowledgeAsset.getFormalType().isEmpty()) {
      return;
    }
    // No more than one type is supported..
    KnowledgeAssetType type0 = knowledgeAsset.getFormalType().get(0);
    lib.setType(toCode(type0.asConceptIdentifier()));
  }


  private void mapPublicationStatus(KnowledgeAsset knowledgeAsset, Library lib) {
    lib.setExperimental(false);

    // Need a more precise mapping of publication statuses
    if (knowledgeAsset.getLifecycle() == null
        || knowledgeAsset.getLifecycle().getPublicationStatus() == null) {
      lib.setStatus(PublicationStatus.NULL);
      return;
    }

    switch (PublicationStatusSeries.asEnum(knowledgeAsset.getLifecycle().getPublicationStatus())) {
      case Published:
        lib.setStatus(PublicationStatus.ACTIVE);
        break;
      case Draft:
        lib.setStatus(PublicationStatus.DRAFT);
        break;
      case Archived:
        lib.setStatus(PublicationStatus.RETIRED);
        break;
      default:
        lib.setStatus(PublicationStatus.UNKNOWN);
    }
  }

  private void mapDescriptive(KnowledgeAsset knowledgeAsset, Library lib) {
    lib.setName(knowledgeAsset.getName());
    lib.setDescription(knowledgeAsset.getDescription());
    lib.setTitle(knowledgeAsset.getName());
  }

  private void mapIdentifier(KnowledgeAsset knowledgeAsset, Library lib) {
    if (knowledgeAsset.getAssetId() == null) {
      throw new IllegalArgumentException("Surrogates MUST have an asset ID - none found");
    }
    ResourceIdentifier vid = knowledgeAsset.getAssetId();
    Identifier assetId = new Identifier()
        .setValue(getTag(vid))
        .setSystem(Registry.MAYO_ASSETS_BASE_URI)
        .setPeriod(new Period().setStart(knowledgeAsset.getAssetId().getEstablishedOn()))
        .setUse(OFFICIAL);

    lib.setIdentifier(Collections.singletonList(assetId));
    lib.setVersion(getVersionTag(vid));
  }

  private void mapSurrogateId(KnowledgeAsset knowledgeAsset, Library lib) {
    if (knowledgeAsset.getSurrogate().isEmpty()) {
      return;
    }
    KnowledgeArtifact surrogate = knowledgeAsset.getSurrogate().get(0);
    if (surrogate != null) {
      lib.setId(getTag(surrogate.getArtifactId()) + ":" + getVersionTag(surrogate.getArtifactId()));
      lib.setUrl(surrogate.getArtifactId().getVersionId().toString());
    }
  }


  private CodeableConcept toCode(ConceptIdentifier cid) {
    return new CodeableConcept()
        .setCoding(Collections.singletonList(
            new Coding()
                .setCode(cid.getTag())
                .setDisplay(cid.getLabel())
                .setSystem(cid.getNamespaceUri() != null
                    ? cid.getNamespaceUri().toString()
                    : null)
                .setVersion(cid.getVersionTag())));
  }


  private static String getTag(ResourceIdentifier id) {
    return URIUtil.detectLocalName(id.getResourceId());
  }

  private static String getVersionTag(ResourceIdentifier id) {
    return DatatypeHelper.toVersionIdentifier(id.getVersionId()).getVersionTag();
  }

}
