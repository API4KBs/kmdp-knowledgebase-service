package edu.mayo.kmdp.terms;

import static edu.mayo.kmdp.terms.TermsTestUtil.prepopulateWithKnownKMDTaxonomy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Clinical_Rule;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Assessment_Model;

import edu.mayo.kmdp.terms.TermsTestUtil.MockRepo;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.metamodel.clazz.EntityDefinition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetCatalogApi;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetRepositoryApi;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.Term;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries;
import org.omg.spec.api4kp._20200801.terms.model.ConceptDescriptor;


class TermsFHIRBasedTest {

  static TermsFHIRFacade server;

  static Javers differ;

  @BeforeAll
  static void init() {
    MockRepo mock = new MockRepo();

    prepopulateWithKnownKMDTaxonomy(mock, mock);
    server = new TermsFHIRFacade(
        KnowledgeAssetCatalogApi.newInstance(mock),
        KnowledgeAssetRepositoryApi.newInstance(mock),
        true);

    // ignore typed labels map, which is not supported by the enum-driven server
    differ = JaversBuilder.javers()
        .registerEntity(new EntityDefinition(
            ConceptDescriptor.class, "uuid", Collections.singletonList("labels")))
        .build();
  }

  @Test
  void testListTerminologies() {
    List<Pointer> termSystems = server.listTerminologies().orElseGet(Assertions::fail);
    assertEquals(2, termSystems.size());
  }

  @Test
  void testGetTerms() {
    UUID uuid = UUID.fromString("243089c1-b6ab-318f-bec9-e1cfaf410992");
    String versionTag = "20210401";

    List<ConceptDescriptor> terms1 = server.getTerms(uuid, versionTag)
        .orElseGet(Assertions::fail);

    assertEquals(KnowledgeAssetTypeSeries.values().length, terms1.size());

    terms1.stream()
        .filter(cd -> cd.getUuid().equals(Assessment_Model.getUuid())).findFirst()
        .orElseGet(Assertions::fail);
  }

  @Test
  void testGetTerm() {
    UUID uuid = UUID.fromString("472ab418-8d62-3a72-9b4e-a7dc14530263");
    String versionTag = "20210401";
    Term t = Clinical_Rule;

    ConceptDescriptor cd1 = server.getTerm(uuid, versionTag, t.getUuid().toString())
        .orElseGet(Assertions::fail);
    ConceptDescriptor cd11 = server.getTerm(uuid, versionTag, t.getTag())
        .orElseGet(Assertions::fail);
    ConceptDescriptor cd12 = server.getTerm(uuid, versionTag, t.getResourceId().toString())
        .orElseGet(Assertions::fail);

    assertEquals(cd1, cd11);
    assertEquals(cd1, cd12);
  }


  @Test
  void testLookupTerm() {
    Term t = Clinical_Rule;

    ConceptDescriptor cd1 = server.lookupTerm(t.getUuid().toString())
        .orElseGet(Assertions::fail);
    ConceptDescriptor cd11 = server.lookupTerm(t.getTag())
        .orElseGet(Assertions::fail);
    ConceptDescriptor cd12 = server.lookupTerm(t.getResourceId().toString())
        .orElseGet(Assertions::fail);

    assertEquals(cd1, cd11);
    assertEquals(cd1, cd12);
  }

  @Test
  void testLookupFailure() {
    String random = UUID.randomUUID().toString();
    assertTrue(server.lookupTerm(random).isNotFound());
  }


  @Test
  void testSearch() {
    var res = server.searchTerms("rule").orElseGet(Assertions::fail);
    assertEquals(9, res.size());
    assertTrue(res.stream()
        .map(ResourceIdentifier::getName)
        .allMatch(l -> l.toLowerCase().contains("rule")));

    var res2 = server.searchTerms("ClinicalRule").orElseGet(Assertions::fail);
    assertEquals(1, res2.size());
  }
}
