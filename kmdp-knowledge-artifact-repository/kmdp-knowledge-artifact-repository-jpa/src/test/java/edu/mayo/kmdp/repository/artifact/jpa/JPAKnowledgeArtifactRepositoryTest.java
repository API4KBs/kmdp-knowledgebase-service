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
package edu.mayo.kmdp.repository.artifact.jpa;

import static edu.mayo.kmdp.registry.Registry.BASE_UUID_URN;
import static edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries.Created;
import static edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries.NoContent;
import static edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries.NotFound;
import static edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.omg.spec.api4kp._20200801.Answer.failed;

import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerProperties;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerProperties.KnowledgeArtifactRepositoryOptions;
import edu.mayo.kmdp.repository.artifact.dao.ArtifactVersion;
import edu.mayo.kmdp.repository.artifact.exceptions.RepositoryNotFoundException;
import edu.mayo.kmdp.repository.artifact.exceptions.ResourceNoContentException;
import edu.mayo.kmdp.repository.artifact.exceptions.ResourceNotFoundException;
import edu.mayo.kmdp.util.FileUtil;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@AutoConfigurationPackage
@ContextConfiguration(classes = TestJPAConfiguration.class)
class JPAKnowledgeArtifactRepositoryTest {

  @Autowired
  JPAArtifactDAO dao;

  @Autowired
  KnowledgeArtifactRepositoryServerProperties cfg;

  private JPAKnowledgeArtifactRepository repository;
  private String repoId;

  private UUID artifactID;
  private UUID artifactID2;

  @BeforeEach
  void repo() {
    repository = new JPAKnowledgeArtifactRepository(dao, cfg);
    repoId = cfg.getTyped(KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_ID);
    artifactID = UUID.randomUUID();
    artifactID2 = UUID.randomUUID();
  }

  @AfterEach
  void cleanup() {
    repository.shutdown();
  }

  @Test
  void testLoadTwice() {
    dao.saveResource(repoId, artifactID, "new", "hi!".getBytes());
    dao.saveResource(repoId, artifactID, "new", "hi!".getBytes());

    List<Pointer> result = repository.getKnowledgeArtifactSeries(repoId, artifactID)
        .orElse(Collections.emptyList());

    assertEquals(1, result.size());
  }

  //"List stored knowledge artifacts"

  @Test
  void testListArtifactsRepoUnknown() {
    dao.saveResource("unknown", artifactID, "LATEST",
        "hi!".getBytes());
    assertThrowsCaught(
        RepositoryNotFoundException.class,
        () -> repository
            .listKnowledgeArtifacts("none"));
  }

  @Test
  void testListArtifactsAllAvailableDeletedFalse() {
    dao.saveResource(repoId, artifactID, "LATEST",
        "hi!".getBytes());
    dao.saveResource(repoId, artifactID2, "LATEST",
        "hi!".getBytes());

    List<Pointer> pointers = repository.listKnowledgeArtifacts(repoId)
        .orElse(Collections.emptyList());

    assertEquals(2, pointers.size());

  }

  @Test
  void testListArtifactsOnlyGivenRepo() {
    dao.saveResource(repoId, artifactID, "LATEST",
        "hi!".getBytes());
    dao.saveResource("repository2", artifactID2, "LATEST",
        "hi!".getBytes());

    List<Pointer> pointers = repository.listKnowledgeArtifacts(repoId)
        .orElse(Collections.emptyList());

    assertEquals(1, pointers.size());
  }

  @Test
  void testListArtifactsOnlyAvailableDeletedFalse() {
    dao.saveResource(repoId, artifactID, "LATEST",
        "hi!".getBytes());
    dao.saveResource(repoId, artifactID2, "LATEST",
        "hi!".getBytes());

    dao.deleteResourceSeries(repoId, artifactID);

    List<Pointer> pointers = repository.listKnowledgeArtifacts(repoId)
        .orElse(Collections.emptyList());

    assertEquals(1, pointers.size());
  }

  @Test
  void testListArtifactsAllUnavailableDeletedFalse() {
    dao.saveResource(repoId, artifactID, "LATEST",
        "hi!".getBytes());
    dao.saveResource(repoId, artifactID2, "LATEST",
        "hi!".getBytes());

    dao.deleteResourceSeries(repoId, artifactID);
    dao.deleteResourceSeries(repoId, artifactID2);

    List<Pointer> pointers = repository.listKnowledgeArtifacts(repoId)
        .orElse(Collections.emptyList());

    assertEquals(0, pointers.size());
  }

  @Test
  void testListArtifactsAllReturnedDeletedTrue() {
    dao.saveResource(repoId, artifactID, "LATEST",
        "hi!".getBytes());
    dao.saveResource(repoId, artifactID2, "LATEST",
        "hi!".getBytes());

    assertEquals(2, dao.listResources(repoId, true).getValue().size());

    dao.deleteResourceSeries(repoId, artifactID);

    List<Pointer> pointers = repository.listKnowledgeArtifacts(repoId, null, null, true)
        .orElse(Collections.emptyList());

    assertEquals(2, pointers.size());
  }

  @Test
  void testListArtifactsEmptySeries() {
    dao.saveResource(repoId, artifactID);
    dao.saveResource(repoId, artifactID2, "LATEST",
        "hi!".getBytes());

    dao.deleteResourceSeries(repoId, artifactID);

    List<Pointer> pointers = repository.listKnowledgeArtifacts(repoId, null, null, true)
        .orElse(Collections.emptyList());

    assertEquals(2, pointers.size());
  }

  @Test
  void testLoadAndGetArtifactsRightPointerHref() {
    dao.saveResource(repoId, artifactID, "new", "hi!".getBytes());

    List<Pointer> result = repository.listKnowledgeArtifacts(repoId)
        .orElse(Collections.emptyList());

    assertEquals(1, result.size());

    assertEquals("http://repos/" + repoId + "/artifacts/" + artifactID,
        result.get(0).getHref().toString());
  }

  @Test
  void testLoadAndGetArtifactsRightPointerUri() {
    dao.saveResource(repoId, artifactID, "new", "hi!".getBytes());

    List<Pointer> result = repository.listKnowledgeArtifacts(repoId)
        .orElse(Collections.emptyList());

    assertEquals(1, result.size());

    assertEquals(BASE_UUID_URN + artifactID,
        result.get(0).getResourceId().toString());
  }

  @Test
  void testLoadAndGetArtifactsRightPointerHrefMultipleArtifacts() {
    dao.saveResource(repoId, artifactID, "new", "hi!".getBytes());
    dao.saveResource(repoId, artifactID, "new2", "hi!".getBytes());

    dao.saveResource(repoId, artifactID2, "new", "hi!".getBytes());
    dao.saveResource(repoId, artifactID2, "new2", "hi!".getBytes());

    List<Pointer> result = repository.listKnowledgeArtifacts(repoId)
        .orElse(Collections.emptyList());

    assertEquals(2, result.size());

    Set<String> resultSet = result.stream().map(it -> it.getHref().toString())
        .collect(Collectors.toSet());

    assertTrue(resultSet.contains("http://repos/" + repoId + "/artifacts/" + artifactID));
    assertTrue(resultSet.contains("http://repos/" + repoId + "/artifacts/" + artifactID2));
  }

  @Test
  void testLoadAndGetArtifactsRightPointerHrefMultiple() {
    String localRepoId = "nonStandardRepo";
    dao.saveResource(localRepoId, artifactID, "new", "hi!".getBytes());
    dao.saveResource(localRepoId, artifactID, "new2", "hi!".getBytes());

    List<Pointer> result = repository.listKnowledgeArtifacts(localRepoId)
        .orElse(Collections.emptyList());

    assertEquals(1, result.size());

    assertEquals("http://repos/" + localRepoId + "/artifacts/" + artifactID,
        result.get(0).getHref().toString());
  }

  //"Initialize new artifact series"
  @Test
  void testInitializeNewArtifactSeries() {
    dao.saveResource(repoId, artifactID, "LATEST",
        "hi!".getBytes());
    Answer<UUID> response = repository.initKnowledgeArtifact(repoId);
    UUID newArtifact = response.orElse(null);
    List<Pointer> artifacts = repository.listKnowledgeArtifacts(repoId)
        .orElse(Collections.emptyList());
    List<Pointer> versions = repository
        .getKnowledgeArtifactSeries(repoId, newArtifact)
        .orElse(Collections.emptyList());

    assertEquals(Created, response.getOutcomeType());
    assertEquals(2, artifacts.size());
    assertEquals(0, versions.size());
  }

  @Test
  void testInitializeNewArtifactSeriesNewRepo() {
    dao.saveResource(repoId, artifactID, "LATEST",
        "hi!".getBytes());
    Answer<UUID> response = repository.initKnowledgeArtifact("repository2");
    List<Pointer> artifacts = repository.listKnowledgeArtifacts("repository2")
        .orElse(Collections.emptyList());

    assertEquals(Created, response.getOutcomeType());
    assertEquals(1, artifacts.size());
  }

  //"Retrieves the LATEST version of a knowledge artifact"
  @Test
  void testGetLatestRepoUnknown() {
    dao.saveResource(repoId, artifactID, "LATEST",
        "hi!".getBytes());
    assertThrowsCaught(
        RepositoryNotFoundException.class,
        () -> repository
            .getLatestKnowledgeArtifact("none", artifactID));
  }

  @Test
  void testGetLatestArtifactUnknown() {
    dao.saveResource(repoId, artifactID, "LATEST",
        "hi!".getBytes());
    assertThrowsCaught(
        ResourceNotFoundException.class,
        () -> repository
            .getLatestKnowledgeArtifact(repoId, artifactID2));
  }

  @Test
  void testGetLatestArtifactUnavailableDeletedFalse() {
    dao.saveResource(repoId, artifactID, "LATEST",
        "hi!".getBytes());
    dao.deleteResourceSeries(repoId, artifactID);
    assertThrowsCaught(
        ResourceNoContentException.class,
        () -> repository
            .getLatestKnowledgeArtifact(repoId, artifactID));
  }

  @Test
  void testGetLatestArtifactUnavailableDeletedTrue() {
    dao.saveResource(repoId, artifactID, "new",
        "hi!".getBytes());
    dao.saveResource(repoId, artifactID, "LATEST",
        "newest".getBytes());
    dao.deleteResourceSeries(repoId, artifactID);
    Answer<byte[]> response = repository.getLatestKnowledgeArtifact(repoId, artifactID, true);

    assertEquals(OK, response.getOutcomeType());
    assertEquals("newest", new String(response.orElse(new byte[0])));
  }

  @Test
  void testGetLatestArtifactAvailableDeletedTrue() {
    dao.saveResource(repoId, artifactID, "new",
        "hi!".getBytes());
    dao.saveResource(repoId, artifactID, "LATEST",
        "newest".getBytes());
    Answer<byte[]> response = repository.getLatestKnowledgeArtifact(repoId, artifactID);

    assertEquals(OK, response.getOutcomeType());
    assertEquals("newest", new String(response.orElse(new byte[0])));
  }

  @Test
  void testGetLatestIgnoreUnavailableDeletedFalse() {
    dao.saveResource(repoId, artifactID, "new",
        "first".getBytes());
    dao.saveResource(repoId, artifactID, "LATEST",
        "second".getBytes());
    dao.deleteResourceVersion(repoId, artifactID, "LATEST");
    Answer<byte[]> response = repository.getLatestKnowledgeArtifact(repoId, artifactID);

    assertEquals(OK, response.getOutcomeType());
    assertEquals("first", new String(response.orElse(new byte[0])));
  }

  @Test
  void testGetLatestIncludeUnavailableDeletedTrue() {
    dao.saveResource(repoId, artifactID, "new",
        "first".getBytes());
    dao.saveResource(repoId, artifactID, "LATEST",
        "second".getBytes());
    dao.deleteResourceVersion(repoId, artifactID, "LATEST");
    Answer<byte[]> response = repository.getLatestKnowledgeArtifact(repoId, artifactID, true);

    assertEquals(OK, response.getOutcomeType());
    assertEquals("second", new String(response.orElse(new byte[0])));
  }

  @Test
  void testGetLatestAllUnavailableDeletedFalse() {
    dao.saveResource(repoId, artifactID, "new",
        "first".getBytes());
    dao.saveResource(repoId, artifactID, "LATEST",
        "second".getBytes());
    dao.deleteResourceVersion(repoId, artifactID, "new");
    dao.deleteResourceVersion(repoId, artifactID, "LATEST");

    assertThrowsCaught(
        ResourceNoContentException.class,
        () -> repository
            .getLatestKnowledgeArtifact(repoId, artifactID));
  }

  @Test
  void testGetLatestEmptySeries() {
    dao.saveResource(repoId, artifactID);
    assertTrue(dao.hasRepository(repoId));
    assertTrue(dao.hasResourceSeries(repoId, artifactID, false));
    assertTrue(dao.hasResourceSeries(repoId, artifactID, true));

    assertThrowsCaught(
        ResourceNoContentException.class,
        () -> repository
            .getLatestKnowledgeArtifact(repoId, artifactID));
  }

  @Test
  void testGetLatestEmptySeriesDeletedTrue() {
    dao.saveResource(repoId, artifactID);

    assertThrowsCaught(
        ResourceNoContentException.class,
        () -> repository
            .getLatestKnowledgeArtifact(repoId, artifactID));
  }

  //    "Check Knowledge Artifact"
  @Test
  void checkSeriesAvailableUnknownRepo() {
    dao.saveResource(repoId, artifactID, "new", "document".getBytes());
    assertTrue(NotFound.sameAs(
        repository.isKnowledgeArtifactSeries("repository1", artifactID).getOutcomeType()));
  }

  @Test
  void checkSeriesAvailableUnknownArtifact() {
    dao.saveResource(repoId, artifactID, "new", "document".getBytes());
    assertTrue(NotFound.sameAs(
        repository.isKnowledgeArtifactSeries(repoId, artifactID2).getOutcomeType()));
  }

  @Test
  void checkSeriesAvailableWithAvailableVersions() {
    dao.saveResource(repoId, artifactID, "new", "document".getBytes());
    Answer<Void> responseEntity = repository
        .isKnowledgeArtifactSeries(repoId, artifactID);
    assertEquals(OK, responseEntity.getOutcomeType());
  }

  @Test
  void checkSeriesAvailableWithUnavailableVersions() {
    dao.saveResource(repoId, artifactID, "new", "document".getBytes());
    dao.deleteResourceVersion(repoId, artifactID, "new");
    Answer<Void> responseEntity = repository
        .isKnowledgeArtifactSeries(repoId, artifactID);
    assertEquals(NoContent, responseEntity.getOutcomeType());
  }

  @Test
  void checkSeriesAvailableWithNoVersions() {
    dao.saveResource(repoId, artifactID);
    Answer<Void> responseEntity = repository
        .isKnowledgeArtifactSeries(repoId, artifactID);
    assertEquals(NoContent, responseEntity.getOutcomeType());
  }

  @Test
  void checkSeriesUnavailableDeletedFalse() {
    dao.saveResource(repoId, artifactID);
    dao.deleteResourceSeries(repoId, artifactID);
    assertTrue(NoContent.sameAs(
        repository.isKnowledgeArtifactSeries(repoId, artifactID).getOutcomeType()));
  }

  @Test
  void checkSeriesUnavailableDeletedTrue() {
    dao.saveResource(repoId, artifactID);
    dao.saveResource(repoId, artifactID, "v1", "hello".getBytes());
    dao.deleteResourceSeries(repoId, artifactID);
    Answer<Void> responseEntity = repository
        .isKnowledgeArtifactSeries(repoId, artifactID, true);
    assertEquals(OK, responseEntity.getOutcomeType());
  }

  @Test
  void checkSeriesUnavailableDeletedTrueOnEmptySeries() {
    dao.saveResource(repoId, artifactID);
    dao.deleteResourceSeries(repoId, artifactID);
    assertTrue(NoContent.sameAs(
        repository.isKnowledgeArtifactSeries(repoId, artifactID, true).getOutcomeType()));
  }

  //"Enable Knowledge Artifact Series"

  @Test
  void testEnableSeriesRepoUnknown() {
    dao.saveResource(repoId, artifactID, "LATEST",
        "hi!".getBytes());
    assertThrowsCaught(
        RepositoryNotFoundException.class,
        () -> repository
            .enableKnowledgeArtifact("none", artifactID));
  }

  @Test
  void testEnableSeriesArtifactUnknown() {
    dao.saveResource(repoId, artifactID, "LATEST",
        "hi!".getBytes());
    Answer<Void> response = repository.enableKnowledgeArtifact(repoId, artifactID2);
    List<Pointer> artifacts = repository.listKnowledgeArtifacts(repoId)
        .orElse(Collections.emptyList());

    assertEquals(Created, response.getOutcomeType());
    assertEquals(2, artifacts.size());
  }

  @Test
  void testEnableSeriesSeriesUnavailable() {
    dao.saveResource(repoId, artifactID, "LATEST",
        "hi!".getBytes());
    dao.deleteResourceSeries(repoId, artifactID);
    Answer<Void> response = repository.enableKnowledgeArtifact(repoId, artifactID);
    List<Pointer> artifacts = repository.listKnowledgeArtifacts(repoId)
        .orElse(Collections.emptyList());

    assertEquals(Created, response.getOutcomeType());
    assertEquals(1, artifacts.size());
  }

  @Test
  void testEnableSeriesSeriesAvailable() {
    dao.saveResource(repoId, artifactID, "LATEST",
        "hi!".getBytes());
    Answer<Void> response = repository.enableKnowledgeArtifact(repoId, artifactID);
    List<Pointer> artifacts = repository.listKnowledgeArtifacts(repoId)
        .orElse(Collections.emptyList());

    assertEquals(Created, response.getOutcomeType());
    assertEquals(1, artifacts.size());
  }

  @Test
  void testEnableSeriesVersionsUnavailable() {
    dao.saveResource(repoId, artifactID, "LATEST",
        "hi!".getBytes());
    dao.saveResource(repoId, artifactID, "LATEST2",
        "hi!".getBytes());

    dao.deleteResourceVersion(repoId, artifactID, "LATEST");
    Answer<Void> response = repository.enableKnowledgeArtifact(repoId, artifactID);
    assertEquals(Created, response.getOutcomeType());

    List<Pointer> versions = repository
        .getKnowledgeArtifactSeries(repoId, artifactID)
        .orElse(Collections.emptyList());
    assertEquals(2, versions.size());
  }

  @Test
  void testEnableSeriesEmpty() {
    dao.saveResource(repoId, artifactID, "LATEST",
        "hi!".getBytes());
    dao.saveResource(repoId, artifactID2);
    Answer<Void> response = repository.enableKnowledgeArtifact(repoId, artifactID2);
    assertEquals(Created, response.getOutcomeType());
  }

  //"Removes a knowledge artifact from the repository"

  @Test
  void testRemoveSeriesRepoUnknown() {
    dao.saveResource(repoId, artifactID, "LATEST",
        "hi!".getBytes());
    assertThrowsCaught(
        ResourceNotFoundException.class,
        () -> repository
            .deleteKnowledgeArtifact("none", artifactID));
  }

  @Test
  void testRemoveSeriesArtifactUnknown() {
    dao.saveResource(repoId, artifactID2, "LATEST",
        "hi!".getBytes());
    assertThrowsCaught(
        ResourceNotFoundException.class,
        () -> repository
            .deleteKnowledgeArtifact(repoId, artifactID));
  }

  @Test
  void testRemoveSeriesAvailable() {
    dao.saveResource(repoId, artifactID2, "LATEST",
        "hi!".getBytes());
    dao.saveResource(repoId, artifactID2, "LATEST2",
        "hi!".getBytes());
    dao.deleteResourceVersion(repoId, artifactID2, "LATEST");

    Answer<Void> responseEntity = repository
        .deleteKnowledgeArtifact(repoId, artifactID2);

    assertThrowsCaught(
        ResourceNoContentException.class,
        () -> repository
            .getKnowledgeArtifactSeries(repoId, artifactID2));

    assertEquals(NoContent, responseEntity.getOutcomeType());
  }

  @Test
  void testRemoveSeriesAlreadyUnavailable() {
    dao.saveResource(repoId, artifactID2, "LATEST",
        "hi!".getBytes());
    dao.saveResource(repoId, artifactID2, "LATEST2",
        "hi!".getBytes());

    repository.deleteKnowledgeArtifact(repoId, artifactID2);

    Answer<Void> responseEntity = repository
        .deleteKnowledgeArtifact(repoId, artifactID2);

    assertEquals(NoContent, responseEntity.getOutcomeType());
  }

  @Test
  void testRemoveSeriesEmpty() {
    dao.saveResource(repoId, artifactID2);
    assertTrue(dao.hasResourceSeries(repoId, artifactID2, false));

    Answer<Void> responseEntity = repository
        .deleteKnowledgeArtifact(repoId, artifactID2);

    assertEquals(NoContent, responseEntity.getOutcomeType());
  }

  @Test
  void testRemoveSeriesDeleteParameter() {
    Answer<Void> response = repository.deleteKnowledgeArtifact(repoId, artifactID, true);
    assertEquals(NoContent, response.getOutcomeType());
  }

  //"List versions of a Knowledge Artifact"

  @Test
  void testListVersionsAllAvailable() {
    dao.saveResource(repoId, artifactID, "new", "hi!".getBytes());
    dao.saveResource(repoId, artifactID, "new2", "hi!".getBytes());

    List<Pointer> results = repository
        .getKnowledgeArtifactSeries(repoId, artifactID)
        .orElse(Collections.emptyList());

    assertEquals(2, results.size());
  }

  @Test
  void testListVersionsUnknownRepo() {
    dao.saveResource(repoId, artifactID, "new", "hi!".getBytes());
    assertThrowsCaught(
        ResourceNotFoundException.class,
        () -> repository.getKnowledgeArtifactSeries("2", artifactID));
  }

  @Test
  void testListVersionsUnknownArtifactId() {
    dao.saveResource(repoId, artifactID, "new", "hi!".getBytes());
    assertThrowsCaught(
        ResourceNotFoundException.class,
        () -> repository.getKnowledgeArtifactSeries(repoId, artifactID2));
  }

  @Test
  void testListVersionsSomeUnavailableDeletedFalse() {
    dao.saveResource(repoId, artifactID, "new", "hi!".getBytes());
    dao.saveResource(repoId, artifactID, "new2", "hi!".getBytes());
    dao.deleteResourceVersion(repoId, artifactID, "new2");

    List<Pointer> results = repository
        .getKnowledgeArtifactSeries(repoId, artifactID)
        .orElse(Collections.emptyList());

    assertEquals(1, results.size());
  }

  @Test
  void testListVersionsSomeUnavailableDeletedTrue() {
    dao.saveResource(repoId, artifactID, "new", "hi!".getBytes());
    dao.saveResource(repoId, artifactID, "new2", "hi!".getBytes());
    dao.deleteResourceVersion(repoId, artifactID, "new2");

    List<Pointer> results = repository
        .getKnowledgeArtifactSeries(repoId, artifactID, true, 0, -1, null, null, null)
        .orElse(Collections.emptyList());

    assertEquals(2, results.size());
  }

  @Test
  void testListVersionsAllUnavailableDeletedFalse() {
    dao.saveResource(repoId, artifactID, "new", "hi!".getBytes());
    dao.saveResource(repoId, artifactID, "new2", "hi!".getBytes());
    dao.deleteResourceVersion(repoId, artifactID, "new2");
    dao.deleteResourceVersion(repoId, artifactID, "new");

    List<Pointer> results = repository
        .getKnowledgeArtifactSeries(repoId, artifactID)
        .orElse(Collections.emptyList());

    assertEquals(0, results.size());
  }

  @Test
  void testListVersionsSeriesUnavailable() {
    dao.saveResource(repoId, artifactID, "new", "hi!".getBytes());
    dao.saveResource(repoId, artifactID, "new2", "hi!".getBytes());
    dao.deleteResourceVersion(repoId, artifactID, "new2");
    dao.deleteResourceVersion(repoId, artifactID, "new");

    List<Pointer> results = repository
        .getKnowledgeArtifactSeries(repoId, artifactID)
        .orElse(Collections.emptyList());

    assertEquals(0, results.size());
  }

  @Test
  void testListVersionsUnavailableSeries() {
    dao.saveResource(repoId, artifactID, "new", "hi!".getBytes());
    dao.deleteResourceSeries(repoId, artifactID);
    assertThrowsCaught(
        ResourceNoContentException.class,
        () -> repository.getKnowledgeArtifactSeries(repoId, artifactID));
  }

  @Test
  void testListVersionsUnavailableSeriesParameterTrue() {
    dao.saveResource(repoId, artifactID, "new", "hi!".getBytes());
    dao.deleteResourceSeries(repoId, artifactID);
    List<Pointer> results = repository
        .getKnowledgeArtifactSeries(repoId, artifactID, true, 0, -1, null, null, null)
        .orElse(Collections.emptyList());

    assertEquals(1, results.size());
  }

  @Test
  void testListVersionsEmptySeries() {
    dao.saveResource(repoId, artifactID);
    Answer<List<Pointer>> results = repository
        .getKnowledgeArtifactSeries(repoId, artifactID);

    assertEquals(0, results.orElse(null).size());
    assertEquals(OK, results.getOutcomeType());
  }

  @Test
  void testGetVersionsRightPointerHref() {
    dao.saveResource(repoId, artifactID, "new", "hi!".getBytes());

    List<Pointer> result = repository
        .getKnowledgeArtifactSeries(repoId, artifactID)
        .orElse(null);

    assertEquals(1, result.size());

    assertEquals("http://repos/" + repoId + "/artifacts/" + artifactID + "/versions/new",
        result.get(0).getHref().toString());
  }

  //“Add a (new version) of a Knowledge Artifact.”
  @Test
  void testAddArtifact() {
    Answer<Void> response = repository
        .addKnowledgeArtifactVersion(repoId, artifactID2, "hi!".getBytes());
    ArtifactVersion version = dao.getLatestResourceVersion(repoId, artifactID2, false).getValue();

    assertEquals(Created, response.getOutcomeType());
    assertEquals("hi!", getPayload(version));
    assertTrue(version.isAvailable());
  }

  @Test
  void testAddArtifactSeriesAvailable() {
    dao.saveResource(repoId, artifactID2, repoId, "hi!".getBytes());
    Answer<Void> response = repository
        .addKnowledgeArtifactVersion(repoId, artifactID2, "hi!".getBytes());
    ArtifactVersion version = dao.getLatestResourceVersion(repoId, artifactID2, false).getValue();

    assertEquals(Created, response.getOutcomeType());
    assertEquals("hi!", getPayload(version));
    assertTrue(version.isAvailable());
  }

  @Test
  void testAddArtifactSeriesUnavailable() {
    dao.saveResource(repoId, artifactID2, repoId, "hi!".getBytes());
    dao.deleteResourceSeries(repoId, artifactID2);
    Answer<Void> response = repository
        .addKnowledgeArtifactVersion(repoId, artifactID2, "hi!".getBytes());
    ArtifactVersion version = dao.getLatestResourceVersion(repoId, artifactID2, false).getValue();

    assertEquals(Created, response.getOutcomeType());
    assertEquals("hi!", getPayload(version));
    assertTrue(version.isAvailable());
  }

  @Test
  void testAddArtifactSeriesEmpty() {
    dao.saveResource(repoId, artifactID2);
    Answer<Void> response = repository
        .addKnowledgeArtifactVersion(repoId, artifactID2, "hi!".getBytes());
    ArtifactVersion version = dao.getLatestResourceVersion(repoId, artifactID2, false).getValue();

    assertEquals(Created, response.getOutcomeType());
    assertEquals("hi!", getPayload(version));
    assertTrue(version.isAvailable());
  }


  @Test
  void testAddArtifactSeriesReturnsLocation() {
    Answer<Void> response = repository
        .addKnowledgeArtifactVersion(repoId, artifactID2, "hi!".getBytes());
    String location = response.getMeta(HttpHeaders.LOCATION).orElse("").toString();
    String artifact = StringUtils.substringBetween(location, "artifacts/", "/versions");
    String repo = StringUtils.substringBetween(location, "repos/", "/artifact");

    assertEquals(artifactID2.toString(), artifact);
    assertEquals(repoId, repo);


  }


  //“Retrieve a specific version of a Knowledge Artifact”
  @Test
  void testGetVersionVersionAvailable() {
    dao.saveResource(repoId, artifactID, "new", "hi!".getBytes());
    Answer<byte[]> response = repository
        .getKnowledgeArtifactVersion(repoId, artifactID, "new", false);
    assertEquals("hi!", new String(response.orElse(new byte[0])));
    assertEquals(OK, response.getOutcomeType());
  }

  @Test
  void testGetVersionVersionUnavailable() {
    dao.saveResource(repoId, artifactID, "new", "hi!".getBytes());
    dao.deleteResourceVersion(repoId, artifactID, "new");

    assertTrue(dao.hasResourceSeries(repoId, artifactID, false));

    assertThrowsCaught(
        ResourceNotFoundException.class,
        () -> repository.getKnowledgeArtifactVersion(repoId, artifactID, "new", false));
  }

  @Test
  void testGetVersionDeletedParameterTrue() {
    dao.saveResource(repoId, artifactID, "new", "hi!".getBytes());
    dao.deleteResourceVersion(repoId, artifactID, "new");

    Answer<byte[]> response = repository
        .getKnowledgeArtifactVersion(repoId, artifactID, "new", true);
    assertEquals("hi!", new String(response.orElse(new byte[0])));
    assertEquals(OK, response.getOutcomeType());
  }

  @Test
  void testGetVersionRepoNotKnown() {
    dao.saveResource(repoId, artifactID, "someVersion", "hi!".getBytes());
    assertThrowsCaught(
        RepositoryNotFoundException.class,
        () -> repository.getKnowledgeArtifactVersion("differentRepo", artifactID, "new"));
  }

  @Test
  void testGetVersionSeriesNotKnown() {
    dao.saveResource(repoId, artifactID, "new", "hi!".getBytes());
    assertThrowsCaught(
        ResourceNotFoundException.class,
        () -> repository.getKnowledgeArtifactVersion(repoId, artifactID2, "new"));
  }

  @Test
  void testGetVersionVersionNotKnown() {
    dao.saveResource(repoId, artifactID, "differentVersion", "hi!".getBytes());
    assertThrowsCaught(
        ResourceNotFoundException.class,
        () -> repository.getKnowledgeArtifactVersion(repoId, artifactID, "new"));
  }

  @Test
  void testGetVersionSeriesEmpty() {
    dao.saveResource(repoId, artifactID);
    assertThrowsCaught(
        ResourceNotFoundException.class,
        () -> repository.getKnowledgeArtifactVersion(repoId, artifactID, "new"));
  }

  //"Check knowledge artifact version"
  @Test
  void testCheckVersionUnknownRepo() {
    dao.saveResource(repoId, artifactID, "version", "hello".getBytes());
    assertThrowsCaught(
        RepositoryNotFoundException.class,
        () -> repository.isKnowledgeArtifactVersion("repo2", artifactID, "new"));
  }

  @Test
  void testCheckVersionUnknownArtifact() {
    dao.saveResource(repoId, artifactID, "version", "hello".getBytes());
    assertThrowsCaught(
        ResourceNotFoundException.class,
        () -> repository.isKnowledgeArtifactVersion(repoId, artifactID2, "new"));
  }

  @Test
  void testCheckVersionUnknownVersion() {
    dao.saveResource(repoId, artifactID, "version", "hello".getBytes());
    assertThrowsCaught(
        ResourceNotFoundException.class,
        () -> repository.isKnowledgeArtifactVersion(repoId, artifactID, "new"));

  }

  @Test
  void testCheckVersionAvailable() {
    dao.saveResource(repoId, artifactID, "version", "hello".getBytes());
    Answer<Void> response = repository
        .isKnowledgeArtifactVersion(repoId, artifactID, "version");
    assertEquals(OK, response.getOutcomeType());
  }

  @Test
  void testCheckVersionUnavailableDeletedTrue() {
    dao.saveResource(repoId, artifactID, "version", "hello".getBytes());
    dao.deleteResourceVersion(repoId, artifactID, "version");
    Answer<Void> response = repository
        .isKnowledgeArtifactVersion(repoId, artifactID, "version", true);
    assertEquals(OK, response.getOutcomeType());
  }

  @Test
  void testCheckVersionUnavailableDeletedFalse() {
    dao.saveResource(repoId, artifactID, "version", "hello".getBytes());
    dao.deleteResourceVersion(repoId, artifactID, "version");
    assertThrowsCaught(
        ResourceNotFoundException.class,
        () -> repository.isKnowledgeArtifactVersion(repoId, artifactID, "version"));
  }

  @Test
  void testCheckVersionArtifactUnavailableDeletedFalse() {
    dao.saveResource(repoId, artifactID, "version", "hello".getBytes());
    dao.deleteResourceSeries(repoId, artifactID);
    assertThrowsCaught(
        ResourceNotFoundException.class,
        () -> repository.isKnowledgeArtifactVersion(repoId, artifactID, "version"));
  }

  @Test
  void testCheckVersionArtifactUnavailableDeletedTrue() {
    dao.saveResource(repoId, artifactID, "version", "hello".getBytes());
    dao.deleteResourceSeries(repoId, artifactID);
    Answer<Void> response = repository
        .isKnowledgeArtifactVersion(repoId, artifactID, "version", true);
    assertEquals(OK, response.getOutcomeType());

  }

  //"Ensure a specific version of a Knowledge Artifact is available"

  @Test
  void testEnsureRepositoryUnknown() {
    dao.saveResource(repoId, artifactID, "version", "thisExists".getBytes());
    assertThrowsCaught(
        RepositoryNotFoundException.class,
        () -> repository.enableKnowledgeArtifactVersion("repository2", artifactID, "version"));

  }

  @Test
  void testEnsureArtifactUnknown() {
    dao.saveResource(repoId, artifactID, "version", "thisExists".getBytes());
    assertThrowsCaught(
        ResourceNotFoundException.class,
        () -> repository.enableKnowledgeArtifactVersion(repoId, artifactID2, "version"));

  }

  @Test
  void testEnsureVersionUnknown() {
    dao.saveResource(repoId, artifactID, "version", "thisExists".getBytes());
    assertThrowsCaught(
        ResourceNotFoundException.class,
        () -> repository.enableKnowledgeArtifactVersion(repoId, artifactID, "version2"));

  }

  @Test
  void testEnsureVersionUnavailable() {
    dao.saveResource(repoId, artifactID, "version", "thisExists".getBytes());
    dao.deleteResourceVersion(repoId, artifactID, "version");
    Answer<Void> response = repository
        .enableKnowledgeArtifactVersion(repoId, artifactID, "version");
    Answer<List<Pointer>> availableVersions = repository
        .getKnowledgeArtifactSeries(repoId, artifactID);
    assertEquals(NoContent, response.getOutcomeType());
    assertEquals(1, availableVersions.orElse(null).size());
  }

  @Test
  void testEnsureSeriesUnavailable() {
    dao.saveResource(repoId, artifactID, "version", "thisExists".getBytes());
    dao.deleteResourceSeries(repoId, artifactID);
    Answer<Void> response = repository
        .enableKnowledgeArtifactVersion(repoId, artifactID, "version", false);
    Answer<List<Pointer>> availableVersions = repository
        .getKnowledgeArtifactSeries(repoId, artifactID);
    assertEquals(NoContent, response.getOutcomeType());
    assertEquals(1, availableVersions.orElse(null).size());
  }

  @Test
  void testEnsureVersionIsAlreadyAvailable() {
    dao.saveResource(repoId, artifactID, "version", "thisExists".getBytes());
    Answer<Void> response = repository
        .enableKnowledgeArtifactVersion(repoId, artifactID, "version", false);
    assertEquals(NoContent, response.getOutcomeType());

  }

  //"Sets a version of a specific knowledge artifact"
  @Test
  void testPutOnExistingVersion() {
    dao.saveResource(repoId, artifactID, "new", "hi!".getBytes());
    Answer<Void> response = repository
        .setKnowledgeArtifactVersion(repoId, artifactID, "new", "replaced".getBytes());
    byte[] replacedArtifact = repository
        .getKnowledgeArtifactVersion(repoId, artifactID, "new")
        .orElse(new byte[0]);

    assertEquals("replaced", new String(replacedArtifact));
    assertEquals(NoContent, response.getOutcomeType());
  }

  @Test
  void testPutOnVersionDoesNotExist() {
    dao.saveResource(repoId, artifactID, "first", "hi!".getBytes());
    Answer<Void> response = repository
        .setKnowledgeArtifactVersion(repoId, artifactID, "new", "replaced".getBytes());
    byte[] replacedArtifact = repository
        .getKnowledgeArtifactVersion(repoId, artifactID, "new", false)
        .orElse(new byte[0]);

    assertEquals("replaced", new String(replacedArtifact));
    assertEquals(NoContent, response.getOutcomeType());
  }

  @Test
  void testPutOnVersionArtifactDoesNotExist() {
    dao.saveResource(repoId, artifactID, "first", "hi!".getBytes());
    Answer<Void> response = repository
        .setKnowledgeArtifactVersion(repoId, artifactID2, "new", "replaced".getBytes());
    byte[] replacedArtifact = repository
        .getKnowledgeArtifactVersion(repoId, artifactID2, "new", false)
        .orElse(new byte[0]);

    assertEquals("replaced", new String(replacedArtifact));
    assertEquals(NoContent, response.getOutcomeType());
  }

  @Test
  void testPutOnVersionRepoDoesNotExist() {
    dao.saveResource(repoId, artifactID, "first", "hi!".getBytes());
    Answer<Void> response = repository
        .setKnowledgeArtifactVersion("repositoryId2", artifactID2, "new", "replaced".getBytes());
    byte[] replacedArtifact = repository
        .getKnowledgeArtifactVersion("repositoryId2", artifactID2, "new", false)
        .orElse(new byte[0]);

    assertEquals("replaced", new String(replacedArtifact));
    assertEquals(NoContent, response.getOutcomeType());
  }

  @Test
  void testPutOnVersionUnavailableVersion() {
    dao.saveResource(repoId, artifactID, "first", "hi!".getBytes());
    dao.deleteResourceVersion(repoId, artifactID, "first");
    Answer<Void> response = repository
        .setKnowledgeArtifactVersion(repoId, artifactID, "first", "replaced".getBytes());
    byte[] replacedArtifact = repository
        .getKnowledgeArtifactVersion(repoId, artifactID, "first", false)
        .orElse(new byte[0]);

    assertEquals("replaced", new String(replacedArtifact));
    assertEquals(NoContent, response.getOutcomeType());
  }

  @Test
  void testPutOnVersionUnavailableArtifact() {
    dao.saveResource(repoId, artifactID, "first", "payload".getBytes());
    dao.deleteResourceSeries(repoId, artifactID);
    Answer<Void> response = repository
        .setKnowledgeArtifactVersion(repoId, artifactID, "first", "payload".getBytes());
    byte[] replacedArtifact = repository
        .getKnowledgeArtifactVersion(repoId, artifactID, "first", false)
        .orElse(new byte[0]);

    assertEquals("payload", new String(replacedArtifact));
    assertEquals(NoContent, response.getOutcomeType());
  }

  @Test
  void testPutOnVersionEmptyArtifact() {
    repository.initKnowledgeArtifact(repoId);
    Answer<Void> response = repository
        .setKnowledgeArtifactVersion(repoId, artifactID, "first", "payload".getBytes());
    byte[] replacedArtifact = repository
        .getKnowledgeArtifactVersion(repoId, artifactID, "first", false)
        .orElse(new byte[0]);

    assertEquals("payload", new String(replacedArtifact));
    assertEquals(NoContent, response.getOutcomeType());
  }

  // “Remove a specific version of a Knowledge Artifact”

  @Test
  void testDeleteVersionAvailable() throws Exception {
    dao.saveResource(repoId, artifactID, "new", "hi!".getBytes());
    Answer<Void> response = repository
        .deleteKnowledgeArtifactVersion(repoId, artifactID, "new", false);

    ArtifactVersion deletedVersion = dao.getResourceVersion(repoId, artifactID, "new", true).getValue();
    assertTrue(deletedVersion.isUnavailable());
    assertEquals(NoContent, response.getOutcomeType());
  }

  @Test
  void testDeleteVersionNoArtifact() {
    dao.saveResource(repoId, artifactID, "new", "hi!".getBytes());
    assertThrowsCaught(
        ResourceNotFoundException.class,
        () -> repository.deleteKnowledgeArtifactVersion(repoId, artifactID2, "new", false));
  }

  @Test
  void testDeleteVersionNoVersion() {
    dao.saveResource(repoId, artifactID, "new", "hi!".getBytes());
    assertThrowsCaught(
        ResourceNotFoundException.class,
        () -> repository.deleteKnowledgeArtifactVersion(repoId, artifactID, "none"));
  }

  @Test
  void testDeleteVersionNoRepo() {
    dao.saveResource(repoId, artifactID, "new", "hi!".getBytes());
    assertThrowsCaught(
        ResourceNotFoundException.class,
        () -> repository.deleteKnowledgeArtifactVersion(repoId, artifactID, "none"));
  }

  @Test
  void testDeleteVersionAlreadyUnavailable() {
    dao.saveResource(repoId, artifactID, "new", "hi!".getBytes());
    dao.deleteResourceVersion(repoId, artifactID, "new");
    Answer<Void> response = repository
        .deleteKnowledgeArtifactVersion(repoId, artifactID, "new");
    assertEquals(NoContent, response.getOutcomeType());
  }

  @Test
  void testDeleteVersionSeriesUnavailable() {
    dao.saveResource(repoId, artifactID, "new", "hi!".getBytes());
    dao.deleteResourceSeries(repoId, artifactID);
    Answer<Void> response = repository
        .deleteKnowledgeArtifactVersion(repoId, artifactID, "new");
    assertEquals(NoContent, response.getOutcomeType());
  }

  @Test
  void testDeleteParameterNotImplemented() {
    Answer<Void> response = repository
        .deleteKnowledgeArtifactVersion(repoId, artifactID, "new", true);
    assertEquals(NoContent, response.getOutcomeType());
  }

  @Test
  void testHardDeleteArtifactVersion() {
    dao.saveResource(repoId, artifactID, "new", "hi!".getBytes());
    Answer<Void> response = repository
        .deleteKnowledgeArtifactVersion(repoId, artifactID, "new", true);
    assertEquals(NoContent, response.getOutcomeType());
    assertTrue(dao.tryFetchArtifactVersion(
        repoId, artifactID, "new", true).isEmpty());
  }

  String getPayload(ArtifactVersion v) {
    return FileUtil.readBytes(v.getDataStream())
        .map(String::new)
        .orElseGet(Assertions::fail);
  }


  private <T> void assertThrowsCaught(
      Class<? extends Exception> exceptionType,
      Supplier<Answer<T>> method) {
    try {
      Answer<T> ans = method.get();
      assertTrue(
          ans.getOutcomeType().sameAs(
              failed(exceptionType.getConstructor().newInstance()).getOutcomeType()));
    } catch (Exception e) {
      fail(e.getMessage(), e);
    }
  }
}



