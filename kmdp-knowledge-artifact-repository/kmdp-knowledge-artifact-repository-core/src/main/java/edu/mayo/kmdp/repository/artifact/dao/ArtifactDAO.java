package edu.mayo.kmdp.repository.artifact.dao;

import edu.mayo.kmdp.repository.artifact.exceptions.DaoRuntimeException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ArtifactDAO {

  void shutdown() throws DaoRuntimeException;

  DaoResult<List<Artifact>> listResources(String repositoryId, Boolean deleted,
      Map<String, String> config);

  default DaoResult<List<Artifact>> listResources(String repositoryId, Boolean deleted) {
    return listResources(repositoryId, deleted, Collections.emptyMap());
  }

  DaoResult<ArtifactVersion> getResourceVersion(String repositoryId, UUID artifactId,
      String versionTag, Boolean deleted);

  DaoResult<Artifact> getResourceSeries(String repositoryId, UUID artifactId);

  /**
   * Returns true if the Artifact Series exist (regardless of versions and their deletion status)
   *
   * @param repositoryId
   * @param artifactId
   * @return
   */
  DaoResult<Boolean> hasResourceSeries(String repositoryId, UUID artifactId);

  /**
   * Returns true if there is at least one version that is available or (if deleted=true)
   * soft-deleted
   *
   * @param repositoryId
   * @param artifactId
   * @param deleted
   * @return
   */
  DaoResult<Boolean> hasResourceVersions(String repositoryId, UUID artifactId, Boolean deleted);

  DaoResult<List<ArtifactVersion>> getResourceVersions(String repositoryId, UUID artifactId,
      Boolean deleted);

  DaoResult<ArtifactVersion> getLatestResourceVersion(String repositoryId, UUID artifactId,
      Boolean deleted);

  void clear();

  /**
   * Soft-delete
   *
   * @param repositoryId
   * @param artifactId
   * @param versionTag
   */
  void deleteResourceVersion(String repositoryId, UUID artifactId, String versionTag);

  /**
   * Hard-delete
   *
   * @param repositoryId
   * @param artifactId
   * @param versionTag
   */
  default void removeResourceVersion(String repositoryId, UUID artifactId, String versionTag) {
    deleteResourceVersion(repositoryId, artifactId, versionTag);
  }

  void deleteResourceSeries(String repositoryId, UUID artifactId);

  default void removeResourceSeries(String repositoryId, UUID artifactId) {
    deleteResourceSeries(repositoryId, artifactId);
  }

  void enableResourceVersion(String repositoryId, UUID artifactId, String versionTag);

  void enableResourceSeries(String repositoryId, UUID artifactId);

  DaoResult<ArtifactVersion> saveResource(
      String repositoryId,
      UUID artifactId, String versionTag,
      byte[] document,
      Map<String, String> config);

  default DaoResult<ArtifactVersion> saveResource(
      String repositoryId,
      UUID artifactId, String versionTag,
      byte[] document) {
    return saveResource(repositoryId, artifactId, versionTag, document, Collections.emptyMap());
  }

  DaoResult<Artifact> saveResource(String repositoryId, UUID artifactId);

  byte[] getData(String repositoryId, ArtifactVersion version);
}
