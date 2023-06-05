package edu.mayo.kmdp.repository.artifact.jpa.stores;

import edu.mayo.kmdp.repository.artifact.dao.Artifact;
import edu.mayo.kmdp.repository.artifact.dao.ArtifactVersion;
import edu.mayo.kmdp.repository.artifact.jpa.entities.ArtifactVersionEntity;
import edu.mayo.kmdp.repository.artifact.jpa.entities.KeyId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public interface ArtifactVersionRepository extends CrudRepository<ArtifactVersionEntity, KeyId> {

  List<Artifact> findAllByKey_RepositoryIdAndSeries(
      String repositoryId, boolean series);

  List<Artifact> findAllByKey_RepositoryIdAndSeriesAndSoftDeleted(
      String repositoryId, boolean series, boolean softDeleted);

  List<ArtifactVersion> findAllByKey_RepositoryIdAndKey_ArtifactIdAndSeriesAndSoftDeletedOrderByCreatedDesc(
      String repositoryId, UUID artifactId, boolean series, boolean softDeleted);

  List<ArtifactVersion> findAllByKey_RepositoryIdAndKey_ArtifactIdAndSeriesOrderByCreatedDesc(
      String repositoryId, UUID artifactId, boolean series);

  Optional<ArtifactVersion> findFirstByKey_RepositoryIdAndKey_ArtifactIdAndSeriesAndSoftDeletedOrderByCreatedDesc(
      String repositoryId, UUID artifactId, boolean series, boolean softDeleted);

  Optional<ArtifactVersion> findFirstByKey_RepositoryIdAndKey_ArtifactIdAndSeriesOrderByCreatedDesc(
      String repositoryId, UUID artifactId, boolean series);


  List<ArtifactVersionEntity> getArtifactVersionEntityByKey_RepositoryIdAndKey_ArtifactIdAndSeriesAndSoftDeleted(
      String repositoryId, UUID artifactId, boolean series, boolean softDeleted);

  List<ArtifactVersionEntity> getArtifactVersionEntityByKey_RepositoryIdAndKey_ArtifactIdAndSeries(
      String repositoryId, UUID artifactId, boolean series);

  Optional<ArtifactVersionEntity> getFirstByKey_RepositoryIdAndKey_ArtifactIdAndSeries(
      String repositoryId, UUID artifactId, boolean series);


  boolean existsByKey_RepositoryId(String repositoryId);

  boolean existsByKey_RepositoryIdAndKey_ArtifactIdAndSeries(
      String repositoryId, UUID artifactId, boolean isSeries);

  boolean existsByKey_RepositoryIdAndKey_ArtifactIdAndSeriesAndSoftDeleted(
      String repositoryId, UUID artifactId, boolean isSeries, boolean softDeleted);

  boolean existsByKey_RepositoryIdAndKey_ArtifactId(
      String repositoryId, UUID artifactId);

  boolean existsByKey_RepositoryIdAndKey_ArtifactIdAndSoftDeleted(
      String repositoryId, UUID artifactId, boolean softDeleted);

  boolean existsByKey_RepositoryIdAndKey_ArtifactIdAndKey_VersionTagAndSoftDeleted(
      String repositoryId, UUID artifactId, String versionTag, boolean softDeleted);

  boolean existsByKey_RepositoryIdAndKey_ArtifactIdAndKey_VersionTag(
      String repositoryId, UUID artifactId, String versionTag);
}
