/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.mayo.kmdp.repository.artifact.jpa;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerProperties;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerProperties.KnowledgeArtifactRepositoryOptions;
import edu.mayo.kmdp.repository.artifact.dao.Artifact;
import edu.mayo.kmdp.repository.artifact.dao.ArtifactVersion;
import edu.mayo.kmdp.repository.artifact.dao.DaoResult;
import edu.mayo.kmdp.repository.artifact.exceptions.ResourceNotFoundException;
import edu.mayo.kmdp.util.FileUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JPADaoNoSpringTest {

  private JPAArtifactDAO dao;

  KnowledgeArtifactRepositoryServerProperties cfg =
      new KnowledgeArtifactRepositoryServerProperties(
          JPADaoNoSpringTest.class.getResourceAsStream("/application.test.properties"));

  private UUID artifactUUID;
  private UUID artifactUUID2;
  private String repoId;

  @BeforeEach
  void repo() {
    artifactUUID = UUID.randomUUID();
    artifactUUID2 = UUID.randomUUID();
    repoId = cfg.getTyped(KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_ID);
    dao = new JPAArtifactDAO(JPAKnowledgeArtifactRepositoryService.inMemoryDataSource(),cfg);
  }

  @AfterEach
  void cleanup() {
    dao.clear();
    dao.shutdown();
  }

  @Test
  void testHasNonExistentArtifact() {
    UUID random = UUID.randomUUID();
    assertFalse(dao.hasResourceSeries(repoId, random,true));
  }

  @Test
  void testHasArtifact() {
    UUID random = UUID.randomUUID();
    Artifact entity = dao.saveResource(repoId,random).getValue();
    assertTrue(dao.hasResourceSeries(repoId, random,true));
  }


  @Test
  void testClear() {
    dao.saveResource(repoId, artifactUUID, "new", "hi!".getBytes());

    DaoResult<List<Artifact>> nodes = dao.listResources(repoId, false, new HashMap<>());

    assertEquals(1, nodes.getValue().size());

    DaoResult<ArtifactVersion> result = dao.getResourceVersion(repoId, artifactUUID, "new", false);

    assertEquals("hi!", d(result.getValue()));

    dao.clear();

    UUID randomUuid = UUID.randomUUID();

    dao.saveResource(repoId, randomUuid, "something", "new node".getBytes());

    nodes = dao.listResources(repoId, false, new HashMap<>());

    assertEquals(1, nodes.getValue().size());

    DaoResult<ArtifactVersion> result2 = dao.getResourceVersion(repoId, randomUuid, "something", false);
    assertEquals("new node", d(result2.getValue()));

  }

  @Test
  void testLoadAndGet() {
    dao.saveResource(repoId, artifactUUID, "new", "hi!".getBytes());

    ArtifactVersion result = dao.getResourceVersion(repoId, artifactUUID, "new", false).getValue();

    assertEquals("hi!", d(result));
  }

  @Test
  void testLoadAndGetHasAvailableStatus() throws Exception {
    dao.saveResource(repoId, artifactUUID, "new", "hi!".getBytes());

    ArtifactVersion result = dao.getResourceVersion(repoId, artifactUUID, "new", false).getValue();

    assertTrue(result.isAvailable());
  }

  @Test
  void testLoadAndGetVersion() {
    dao.saveResource(repoId, artifactUUID, "new1", "hi1".getBytes());
    dao.saveResource(repoId, artifactUUID, "new2", "hi2".getBytes());

    ArtifactVersion result1 = dao.getResourceVersion(repoId, artifactUUID, "new1", false).getValue();
    ArtifactVersion result2 = dao.getResourceVersion(repoId, artifactUUID, "new2", false).getValue();

    assertEquals("hi1", d(result1));
    assertEquals("hi2", d(result2));
  }

  @Test
  void testLoadAndGetVersions() {
    dao.saveResource(repoId, artifactUUID, "new1", "hi1".getBytes());
    dao.saveResource(repoId, artifactUUID, "new2", "hi2".getBytes());

    List<ArtifactVersion> versions = dao.getResourceVersions(repoId, artifactUUID, false).getValue();

    assertEquals(2, versions.size());
  }

  @Test
  void testLoadAndGetLatestVersion() throws InterruptedException {
    dao.saveResource(repoId, artifactUUID, "new1", "hi1".getBytes());
    TimeUnit.MILLISECONDS.sleep(5);
    dao.saveResource(repoId, artifactUUID, "new2", "hi2".getBytes());
    TimeUnit.MILLISECONDS.sleep(5);
    dao.saveResource(repoId, artifactUUID, "new3", "hi3".getBytes());
    TimeUnit.MILLISECONDS.sleep(5);
    dao.saveResource(repoId, artifactUUID, "new4", "hi4".getBytes());

    ArtifactVersion version = dao.getLatestResourceVersion(repoId, artifactUUID, false).getValue();

    assertEquals("hi4", d(version));
  }

  @Test
  void testLoadAndGetLatestVersionNone() {
    dao.saveResource(repoId, artifactUUID, "new1", "hi1".getBytes());
    dao.saveResource(repoId, artifactUUID, "new2", "hi2".getBytes());

    assertThrows(
        ResourceNotFoundException.class,
        () -> dao.getLatestResourceVersion(repoId, artifactUUID2, false));
  }

  @Test
  void testDeleteTagsVersionUnavailable() throws Exception {
    dao.saveResource(repoId, artifactUUID, "new1", "hi1".getBytes());
    dao.saveResource(repoId, artifactUUID, "new2", "hi2".getBytes());

    dao.deleteResourceVersion(repoId, artifactUUID, "new1");

    ArtifactVersion version = dao.getResourceVersion(repoId, artifactUUID, "new1", true).getValue();
    ArtifactVersion version2 = dao.getResourceVersion(repoId, artifactUUID, "new2", true).getValue();

    assertTrue(version.isUnavailable());
    assertTrue(version2.isAvailable());
  }

  @Test
  void testDeleteTagsArtifactsAndVersionsUnavailable() throws Exception {
    dao.saveResource(repoId, artifactUUID, "new1", "hi1".getBytes());
    dao.saveResource(repoId, artifactUUID, "new2", "hi2".getBytes());
    dao.saveResource(repoId, artifactUUID, "new3", "hi2".getBytes());
    dao.saveResource(repoId, artifactUUID, "new4", "hi2".getBytes());

    dao.deleteResourceSeries(repoId, artifactUUID);

    ArtifactVersion version = dao.getResourceVersion(repoId, artifactUUID, "new1", true).getValue();
    ArtifactVersion version2 = dao.getResourceVersion(repoId, artifactUUID, "new2", true).getValue();

    assertTrue(version.isUnavailable());
    assertTrue(version2.isUnavailable());
  }

  @Test
  void testQueryAll() {
    dao.saveResource(repoId, UUID.randomUUID(), "new1", "hi1".getBytes(), m("type", "foobar"));
    dao.saveResource(repoId, UUID.randomUUID(), "new1.1", "hi1.1".getBytes(), m("type", "foo"));

    dao.saveResource(repoId, UUID.randomUUID(), "new2", "hi2".getBytes(), m("type", "foobar"));
    dao.saveResource(repoId, UUID.randomUUID(), "new2.1", "hi2.1".getBytes(), m("type", "foo"));

    Map<String, String> query = emptyMap();

    List<Artifact> resources = dao.listResources(repoId, false, query).getValue();

    assertEquals(4, resources.size());
  }



  @Test
  void testQueryWithNumbers() {
    dao.saveResource(repoId, artifactUUID, "new1", "hi1".getBytes());
    dao.saveResource(repoId, artifactUUID, "new1.1", "hi1.1".getBytes());

    dao.saveResource(repoId, artifactUUID2, "new2", "hi2".getBytes());
    dao.saveResource(repoId, artifactUUID2, "new2.1", "hi2.1".getBytes());

    List<Artifact> resources = dao.listResources(repoId, false, emptyMap()).getValue();

    assertEquals(2, resources.size());
  }


  private Map<String, String> m(String k, String v) {
    Map<String, String> m = new HashMap<>();
    m.put(k, v);
    return m;
  }

  String d(ArtifactVersion v) {
    try {
      return FileUtil.read(v.getDataStream()).orElse("");
    } catch (Exception e) {
      fail(e.getMessage(),e);
      return "";
    }
  }


}
