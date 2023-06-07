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
package edu.mayo.kmdp.repository.artifact;

import static edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries.NoContent;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.omg.spec.api4kp._20200801.Answer.unsupported;

import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerProperties.KnowledgeArtifactRepositoryOptions;
import edu.mayo.kmdp.repository.artifact.dao.Artifact;
import edu.mayo.kmdp.repository.artifact.dao.ArtifactDAO;
import edu.mayo.kmdp.repository.artifact.dao.ArtifactVersion;
import edu.mayo.kmdp.repository.artifact.dao.DaoResult;
import edu.mayo.kmdp.repository.artifact.exceptions.DaoRuntimeException;
import edu.mayo.kmdp.repository.artifact.exceptions.RepositoryNotFoundException;
import edu.mayo.kmdp.repository.artifact.exceptions.ResourceNotFoundException;
import edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.PlatformComponentHelper;
import org.omg.spec.api4kp._20200801.aspects.Failsafe;
import org.omg.spec.api4kp._20200801.aspects.LogLevel;
import org.omg.spec.api4kp._20200801.aspects.Loggable;
import org.omg.spec.api4kp._20200801.aspects.Track;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;


public abstract class KnowledgeArtifactRepositoryCore implements DisposableBean,
    ClearableKnowledgeArtifactRepositoryService {

  private static final Logger logger =
      LoggerFactory.getLogger(KnowledgeArtifactRepositoryCore.class);

  protected KnowledgeArtifactRepositoryServerProperties cfg;
  protected HrefBuilder hrefBuilder;

  protected String defaultRepositoryId;
  protected String defaultRepositoryName;

  protected ArtifactDAO dao;

  //*********************************************************************************************/
  //* Constructors */
  //*********************************************************************************************/


  protected KnowledgeArtifactRepositoryCore(ArtifactDAO dao,
      KnowledgeArtifactRepositoryServerProperties cfg) {
    init(dao, cfg);
  }

  @Loggable(beforeCode = "KART-000.A", level = LogLevel.INFO)
  protected void init(ArtifactDAO dao,
      KnowledgeArtifactRepositoryServerProperties cfg) {
    this.cfg = cfg;
    hrefBuilder = new HrefBuilder(this.cfg);
    this.dao = dao;
    this.defaultRepositoryId = this.cfg
        .getTyped(KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_ID);
    this.defaultRepositoryName = this.cfg
        .getTyped(KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_NAME);
  }

  private Optional<org.omg.spec.api4kp._20200801.services.repository.KnowledgeArtifactRepository> createRepositoryDescriptor(
      String repositoryId, String repositoryName) {
    return PlatformComponentHelper.repositoryDescr(
        cfg.getTyped(KnowledgeArtifactRepositoryOptions.BASE_NAMESPACE),
        repositoryId,
        repositoryName,
        null
    ).map(descr ->
        descr.withDefaultRepository(defaultRepositoryId.equals(repositoryId)));
  }

  @Failsafe
  public void shutdown() throws DaoRuntimeException {
    dao.shutdown();
  }

  //*********************************************************************************************/
  //* Knowledge Artifact Repository - Management APIs */
  //*********************************************************************************************/

  @Override
  @Loggable(beforeCode = "KART-012.A")
  public Answer<List<org.omg.spec.api4kp._20200801.services.repository.KnowledgeArtifactRepository>> listKnowledgeArtifactRepositories() {
    return createRepositoryDescriptor(defaultRepositoryId, defaultRepositoryName)
        .map(descr -> Answer.of(singletonList(descr)))
        .orElseGet(Answer::notFound);
  }

  @Override
  @Loggable(beforeCode = "KART-014.A", level = LogLevel.INFO)
  public Answer<org.omg.spec.api4kp._20200801.services.repository.KnowledgeArtifactRepository> initKnowledgeArtifactRepository() {
    return unsupported();
  }

  @Override
  @Loggable(beforeCode = "KART-023.A", level = LogLevel.WARN)
  public Answer<org.omg.spec.api4kp._20200801.services.repository.KnowledgeArtifactRepository> setKnowledgeArtifactRepository(
      String repositoryId,
      org.omg.spec.api4kp._20200801.services.repository.KnowledgeArtifactRepository repositoryDescr) {
    return unsupported();
  }

  @Override
  @Loggable(beforeCode = "KART-021.A")
  public Answer<Void> isKnowledgeArtifactRepository(String repositoryId) {
    return unsupported();
  }

  @Override
  @Failsafe(traces = @Track(throwable = RepositoryNotFoundException.class, value = LogLevel.DEBUG))
  @Loggable(beforeCode = "KART-022.A")
  public Answer<org.omg.spec.api4kp._20200801.services.repository.KnowledgeArtifactRepository>
  getKnowledgeArtifactRepository(String repositoryId) {
    if (defaultRepositoryId.equals(repositoryId)) {
      return Answer.of(createRepositoryDescriptor(repositoryId, defaultRepositoryName));
    } else {
      throw new RepositoryNotFoundException(repositoryId);
    }
  }

  @Override
  @Loggable(beforeCode = "KART-025.A", level = LogLevel.WARN)
  public Answer<Void> disableKnowledgeArtifactRepository(String repositoryId) {
    return unsupported();
  }

  //*********************************************************************************************/
  //* Knowledge Artifact Series - Management APIs */
  //*********************************************************************************************/

  @Override
  @Failsafe(traces = {@Track(value = LogLevel.WARN, throwable = RepositoryNotFoundException.class)})
  @Loggable(beforeCode = "KART-032.A", level = LogLevel.INFO)
  public Answer<List<Pointer>> listKnowledgeArtifacts(String repositoryId, Integer offset,
      Integer limit, Boolean deleted) {
    try (DaoResult<List<Artifact>> result = dao
        .listResources(repositoryId, deleted)) {
      List<Artifact> nodes = result.getValue();

      List<Pointer> pointers = nodes.stream()
          .map(node -> artifactToPointer(node, repositoryId))
          .collect(Collectors.toList());

      return Answer.of(pointers);
    }
  }

  @Override
  @Failsafe
  @Loggable(beforeCode = "KART-034.A")
  public Answer<UUID> initKnowledgeArtifact(String repositoryId) {
    var artifactId = UUID.randomUUID();

    try (DaoResult<Artifact> ignored = dao
        .saveResource(repositoryId, artifactId)) {
      return Answer.of(ResponseCodeSeries.Created, artifactId);
    }
  }

  @Override
  @Loggable(beforeCode = "KART-035.A", level = LogLevel.WARN)
  public Answer<Void> clearKnowledgeRepository(String repositoryId,
      Boolean deleted) {
    return unsupported();
  }

  @Override
  @Failsafe(traces = @Track(throwable = ResourceNotFoundException.class, value = LogLevel.DEBUG))
  @Loggable(beforeCode = "KART-042.A")
  public Answer<byte[]> getLatestKnowledgeArtifact(String repositoryId, UUID artifactId,
      Boolean deleted) {
    try (DaoResult<ArtifactVersion> result = dao
        .getLatestResourceVersion(repositoryId, artifactId, deleted)) {
      ArtifactVersion version = result.getValue();
      return Answer.of(getData(repositoryId, version));
    }
  }

  @Override
  @Failsafe
  @Loggable(beforeCode = "KART-041.A")
  public Answer<Void> isKnowledgeArtifactSeries(String repositoryId, UUID artifactId,
      Boolean deleted) {
    boolean seriesExist = dao.hasResourceSeries(repositoryId, artifactId).getValue();
    if (seriesExist) {
      boolean contentExist = dao.hasResourceVersions(repositoryId, artifactId, deleted)
          .getValue();
      if (contentExist) {
        return Answer.succeed();
      } else {
        return Answer.of(NoContent);
      }
    } else {
      return Answer.notFound();
    }
  }

  @Override
  @Failsafe(traces = @Track(throwable = ResourceNotFoundException.class, value = LogLevel.WARN))
  @Loggable(beforeCode = "KART-044.A")
  public Answer<Void> enableKnowledgeArtifact(String repositoryId,
      UUID artifactId) {
    dao.enableResourceSeries(repositoryId, artifactId);
    return Answer.of(ResponseCodeSeries.Created);
  }

  @Override
  @Failsafe(traces = @Track(throwable = ResourceNotFoundException.class, value = LogLevel.DEBUG))
  @Loggable(beforeCode = "KART-045.A", level = LogLevel.INFO)
  public Answer<Void> deleteKnowledgeArtifact(String repositoryId, UUID artifactId,
      Boolean deleted) {
    if ((Boolean.TRUE.equals(deleted))) {
      dao.removeResourceSeries(repositoryId, artifactId);
    } else {
      dao.deleteResourceSeries(repositoryId, artifactId);
    }
    return Answer.of(NoContent);
  }

  @Override
  @Failsafe(traces = @Track(throwable = ResourceNotFoundException.class, value = LogLevel.DEBUG))
  @Loggable(beforeCode = "KART-052.A")
  public Answer<List<Pointer>> getKnowledgeArtifactSeries(String repositoryId,
      UUID artifactId, Boolean deleted, Integer offset, Integer limit,
      String beforeTag, String afterTag, String sort) {
    try (DaoResult<List<ArtifactVersion>> result = dao
        .getResourceVersions(repositoryId, artifactId, deleted)) {
      List<ArtifactVersion> versions = result.getValue();

      return versions.isEmpty()
          ? Answer.of(Collections.emptyList())
          : Answer.of(versions.stream()
              .map(version -> versionToPointer(version, repositoryId))
              .collect(Collectors.toList()));
    }
  }

  @Override
  @Failsafe(traces = @Track(throwable = ResourceNotFoundException.class, value = LogLevel.WARN))
  @Loggable(beforeCode = "KART-054.A")
  public Answer<Void> addKnowledgeArtifactVersion(String repositoryId, UUID artifactId,
      byte[] document) {
    var versionId = UUID.randomUUID().toString();

    try (DaoResult<ArtifactVersion> result = dao
        .saveResource(repositoryId, artifactId, versionId, document, emptyMap())) {
      URI location = versionToPointer(result.getValue(), repositoryId).getHref();
      return Answer.referTo(location, true);
    }
  }

  //*********************************************************************************************/
  //* Knowledge Artifact - Management APIs */
  //*********************************************************************************************/

  @Override
  @Failsafe(traces = @Track(throwable = ResourceNotFoundException.class, value = LogLevel.TRACE))
  @Loggable(beforeCode = "KART-062.A")
  public Answer<byte[]> getKnowledgeArtifactVersion(String repositoryId, UUID artifactId,
      String versionTag, Boolean deleted) {
    try (DaoResult<ArtifactVersion> result = dao
        .getResourceVersion(repositoryId, artifactId, versionTag, deleted)) {
      ArtifactVersion version = result.getValue();

      return Answer.of(getData(repositoryId, version));
    }
  }

  @Override
  @Failsafe
  @Loggable(beforeCode = "KART-061.A")
  public Answer<Void> isKnowledgeArtifactVersion(String repositoryId, UUID artifactId,
      String versionTag, Boolean deleted) {
    try (DaoResult<ArtifactVersion> ignored = dao
        .getResourceVersion(repositoryId, artifactId, versionTag, deleted)) {
      return Answer.of(ResponseCodeSeries.OK);
    }
  }

  @Override
  @Failsafe(traces = @Track(throwable = ResourceNotFoundException.class, value = LogLevel.WARN))
  @Loggable(beforeCode = "KART-064.A")
  public Answer<Void> enableKnowledgeArtifactVersion(String repositoryId, UUID artifactId,
      String versionTag, Boolean deleted) {
    dao.enableResourceVersion(repositoryId, artifactId, versionTag);
    return Answer.of(NoContent);
  }

  @Override
  @Failsafe
  @Loggable(beforeCode = "KART-063.A")
  public Answer<Void> setKnowledgeArtifactVersion(String repositoryId, UUID artifactId,
      String versionTag, byte[] document) {
    try (DaoResult<ArtifactVersion> ignored = dao
        .saveResource(repositoryId, artifactId, versionTag, document, emptyMap())) {

      return Answer.of(NoContent);
    }
  }

  @Override
  @Failsafe(traces = @Track(throwable = ResourceNotFoundException.class, value = LogLevel.DEBUG))
  @Loggable(beforeCode = "KART-065.A")
  public Answer<Void> deleteKnowledgeArtifactVersion(String repositoryId, UUID artifactId,
      String versionTag, Boolean deleted) {
    if (Boolean.TRUE.equals(deleted)) {
      dao.removeResourceVersion(repositoryId, artifactId, versionTag);
    } else {
      dao.deleteResourceVersion(repositoryId, artifactId, versionTag);
    }
    return Answer.of(NoContent);
  }


  /**
   * Destructor Release all resources
   */
  @Override
  @Failsafe
  @Loggable(beforeCode = "KART-900.A")
  public void destroy() {
    dao.shutdown();
  }


  @Failsafe
  private Pointer versionToPointer(ArtifactVersion version, String repositoryId) {
    var resId = version.getResourceIdentifier();
    String artifactId = resId.getTag();
    String versionTag = resId.getVersionTag();

    return SemanticIdentifier.newIdAsPointer(
        URI.create(cfg.getTyped(KnowledgeArtifactRepositoryOptions.BASE_NAMESPACE)),
        artifactId, "",
        versionTag, hrefBuilder.getArtifactHref(artifactId, versionTag, repositoryId));
  }

  protected Pointer artifactToPointer(Artifact node, String repositoryId) {
    String artifactId = node.getArtifactTag();
    var pointer = SemanticIdentifier.newIdAsPointer(artifactId);
    pointer.withHref(hrefBuilder.getSeriesHref(artifactId, repositoryId));

    return pointer;
  }

  protected byte[] getData(String repositoryId, ArtifactVersion version) {
    return dao.getData(repositoryId, version);
  }

  @Override
  public void clear() {
    this.dao.clear();
  }
}
