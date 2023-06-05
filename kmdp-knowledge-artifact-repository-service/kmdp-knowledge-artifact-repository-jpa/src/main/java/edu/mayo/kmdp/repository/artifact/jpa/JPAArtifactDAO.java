package edu.mayo.kmdp.repository.artifact.jpa;

import static edu.mayo.kmdp.repository.artifact.jpa.JPAResult.ofJPA;

import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerProperties;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerProperties.KnowledgeArtifactRepositoryOptions;
import edu.mayo.kmdp.repository.artifact.dao.Artifact;
import edu.mayo.kmdp.repository.artifact.dao.ArtifactDAO;
import edu.mayo.kmdp.repository.artifact.dao.ArtifactVersion;
import edu.mayo.kmdp.repository.artifact.dao.DaoResult;
import edu.mayo.kmdp.repository.artifact.exceptions.DaoRuntimeException;
import edu.mayo.kmdp.repository.artifact.exceptions.RepositoryNotFoundException;
import edu.mayo.kmdp.repository.artifact.exceptions.ResourceNoContentException;
import edu.mayo.kmdp.repository.artifact.exceptions.ResourceNotFoundException;
import edu.mayo.kmdp.repository.artifact.jpa.entities.ArtifactVersionEntity;
import edu.mayo.kmdp.repository.artifact.jpa.entities.KeyId;
import edu.mayo.kmdp.repository.artifact.jpa.stores.ArtifactVersionRepository;
import edu.mayo.kmdp.repository.artifact.jpa.stores.simple.SimpleArtifactVersionRepository;
import edu.mayo.kmdp.util.FileUtil;
import edu.mayo.kmdp.util.StreamUtil;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JPAArtifactDAO implements ArtifactDAO {

  @Autowired
  private DataSource dataSource;

  @Autowired
  private ArtifactVersionRepository versionRepo;

  @Autowired
  private KnowledgeArtifactRepositoryServerProperties cfg;

  private String defaultRepositoryId;

  public JPAArtifactDAO() {
    //
  }

  /**
   * Test constructor
   *
   * @param source
   * @param cfg
   */
  public JPAArtifactDAO(DataSource source, KnowledgeArtifactRepositoryServerProperties cfg) {
    this.dataSource = source;
    this.cfg = cfg;
    this.versionRepo = SimpleArtifactVersionRepository.simpleRepo(source, cfg);
    ensureInit();
  }

  @PostConstruct
  void ensureInit() {
    if (defaultRepositoryId == null) {
      defaultRepositoryId = cfg.getTyped(KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_ID);
    }
  }

  @Override
  public void shutdown() throws DaoRuntimeException {
    try {
      dataSource.getConnection().close();
      if (versionRepo instanceof Closeable) {
        ((Closeable) versionRepo).close();
      }
    } catch (SQLException | IOException e) {
      throw new DaoRuntimeException(e);
    }
  }

  @Override
  @Transactional
  public void clear() {
    versionRepo.deleteAll();
  }

  ArtifactVersionRepository getPersistenceAdapter() {
    return versionRepo;
  }

  /***********************************************************************************/


  @Override
  public DaoResult<List<Artifact>> listResources(String repositoryId, Boolean includeSoftDeleted,
      Map<String, String> config) {
    List<Artifact> artifacts;
    if (includeSoftDeleted) {
      artifacts =
          versionRepo.findAllByKey_RepositoryIdAndSeries(repositoryId, true);
    } else {
      artifacts =
          versionRepo.findAllByKey_RepositoryIdAndSeriesAndSoftDeleted(repositoryId, true, false);
    }
    if (artifacts.isEmpty()) {
      checkHasRepository(repositoryId);
    }
    return ofJPA(artifacts);
  }


  @Override
  public DaoResult<ArtifactVersion> getResourceVersion(String repositoryId, UUID artifactId,
      String versionTag, Boolean includeSoftDeleted) {
    return ofJPA(
        fetchArtifactVersion(repositoryId, artifactId, versionTag, includeSoftDeleted));
  }


  @Override
  public DaoResult<Boolean> hasResourceSeries(String repositoryId, UUID artifactId) {
    return ofJPA(
        versionRepo.existsByKey_RepositoryIdAndKey_ArtifactIdAndSeries(
            repositoryId, artifactId, true));
  }

  @Override
  public DaoResult<Artifact> getResourceSeries(String repositoryId, UUID artifactId) {
    return ofJPA(
        fetchArtifactSeries(repositoryId, artifactId));
  }

  @Override
  public DaoResult<List<ArtifactVersion>> getResourceVersions(String repositoryId, UUID artifactId,
      Boolean includeSoftDeleted) {
    return ofJPA(
        fetchAllArtifactVersions(repositoryId, artifactId, includeSoftDeleted));
  }

  @Override
  public DaoResult<Boolean> hasResourceVersions(String repositoryId, UUID artifactId,
      Boolean includeSoftDeleted) {
    return ofJPA(
        versionRepo.existsByKey_RepositoryIdAndKey_ArtifactIdAndSeriesAndSoftDeleted(
            repositoryId, artifactId, false, includeSoftDeleted));
  }

  @Override
  public DaoResult<ArtifactVersion> getLatestResourceVersion(String repositoryId, UUID artifactId,
      Boolean includeSoftDeleted) {
    return ofJPA(
        fetchLatestVersion(repositoryId, artifactId, includeSoftDeleted));
  }

  @Override
  @Transactional
  public void deleteResourceVersion(String repositoryId, UUID artifactId, String versionTag) {
    versionRepo.save(fetchArtifactVersion(repositoryId, artifactId, versionTag, true)
        .withSoftDeleted(true));
  }

  @Override
  @Transactional
  public void removeResourceVersion(String repositoryId, UUID artifactId, String versionTag) {
    if (versionRepo.existsByKey_RepositoryIdAndKey_ArtifactIdAndKey_VersionTag(
        repositoryId, artifactId, versionTag)) {
      versionRepo.deleteById(new KeyId(repositoryId, artifactId, versionTag));
    }
  }

  @Override
  @Transactional
  public void deleteResourceSeries(String repositoryId, UUID artifactId) {
    List<ArtifactVersionEntity> versions =
        fetchAllArtifactVersions(repositoryId, artifactId, true).stream()
            .flatMap(StreamUtil.filterAs(ArtifactVersionEntity.class))
            .collect(Collectors.toList());

    // soft-deleting a series...
    versionRepo.save(fetchArtifactSeries(repositoryId, artifactId).withSoftDeleted(true));
    // ...soft-deletes all versions
    versions.forEach(version -> versionRepo.save(version.withSoftDeleted(true)));
  }

  @Override
  @Transactional
  public void removeResourceSeries(String repositoryId, UUID artifactId) {
    if (versionRepo.existsByKey_RepositoryIdAndKey_ArtifactIdAndSeries(
        repositoryId, artifactId, true)) {
      versionRepo.deleteById(new KeyId(repositoryId, artifactId, artifactId.toString()));
    }
  }

  @Override
  @Transactional
  public void enableResourceVersion(String repositoryId, UUID artifactId, String versionTag) {
    ArtifactVersionEntity version = fetchArtifactVersion(repositoryId, artifactId, versionTag,
        true);
    if (version.isUnavailable()) {
      versionRepo.save(version.withSoftDeleted(false));
    }
  }

  @Override
  @Transactional
  public void enableResourceSeries(String repositoryId, UUID artifactId) {
    Optional<ArtifactVersionEntity> series = tryFetchArtifactSeries(repositoryId, artifactId);
    if (series.isPresent() && series.get().isUnavailable()) {
      versionRepo.save(series.get().withSoftDeleted(false));
    } else if (series.isEmpty()) {
      // save would activate the repository, but enable should not
      checkHasRepository(repositoryId);
      versionRepo.save(new ArtifactVersionEntity(repositoryId, artifactId));
    }

    List<ArtifactVersionEntity> disabledVersions =
        fetchAllArtifactVersions(repositoryId, artifactId, true).stream()
            .flatMap(StreamUtil.filterAs(ArtifactVersionEntity.class))
            .filter(ArtifactVersionEntity::isUnavailable)
            .collect(Collectors.toList());
    disabledVersions.forEach(version -> versionRepo.save(version.withSoftDeleted(false)));
  }

  @Override
  @Transactional
  public DaoResult<ArtifactVersion> saveResource(String repositoryId, UUID artifactId,
      String versionTag, byte[] document, Map<String, String> config) {

    saveResource(repositoryId, artifactId);

    ArtifactVersionEntity entity = tryFetchArtifactVersion(repositoryId, artifactId, versionTag,
        true)
        .orElseGet(() -> new ArtifactVersionEntity(repositoryId, artifactId, versionTag));

    entity.setBinaryData(document);
    entity.setSoftDeleted(false);

    return ofJPA(versionRepo.save(entity));
  }

  @Override
  @Transactional
  public DaoResult<Artifact> saveResource(String repositoryId, UUID artifactId) {

    Optional<ArtifactVersionEntity> seriesOpt = tryFetchArtifactSeries(repositoryId, artifactId);

    ArtifactVersionEntity series = seriesOpt
        .orElseGet(() -> new ArtifactVersionEntity(repositoryId, artifactId));
    if (seriesOpt.isEmpty() || series.isSoftDeleted()) {
      series.setSoftDeleted(false);
      versionRepo.save(series);
    }

    return ofJPA(series);
  }


  @Override
  public byte[] getData(String repositoryId, ArtifactVersion version) {

    InputStream is = ((ArtifactVersionEntity) version).getDataStream();
    return FileUtil.readBytes(is)
        .orElseThrow(() -> new ResourceNoContentException(
            "Unable to load binary for " + version));
  }

  /***********************************************************************************/


  public ArtifactVersionEntity fetchArtifactSeries(String repositoryId, UUID artifactId) {
    Optional<ArtifactVersionEntity> series =
        tryFetchArtifactSeries(repositoryId, artifactId);

    if (series.isEmpty()) {
      checkSeries(repositoryId, artifactId);
      checkHasRepository(repositoryId);
    }
    return series
        .orElseThrow(() -> new ResourceNotFoundException(artifactId, null, repositoryId));
  }


  public Optional<ArtifactVersionEntity> tryFetchArtifactSeries(String repositoryId,
      UUID artifactId) {
    // no point in pushing the filtering on the deleted flag downstream
    // there is at most one 'series' object per id
    return versionRepo
        .getFirstByKey_RepositoryIdAndKey_ArtifactIdAndSeries(
            repositoryId, artifactId, true);
  }

  public ArtifactVersionEntity fetchArtifactVersion(String repositoryId, UUID artifactId,
      String versionTag,
      Boolean includeSoftDeleted) {
    return tryFetchArtifactVersion(repositoryId, artifactId, versionTag, includeSoftDeleted)
        .orElseThrow(() -> new ResourceNotFoundException(artifactId, versionTag, repositoryId));
  }

  public Optional<ArtifactVersionEntity> tryFetchArtifactVersion(String repositoryId,
      UUID artifactId,
      String versionTag,
      Boolean includeSoftDeleted) {
    Optional<ArtifactVersionEntity> entity = versionRepo
        .findById(new KeyId(repositoryId, artifactId, versionTag))
        .filter(x -> includeSoftDeleted || !x.isSoftDeleted());
    if (entity.isEmpty()) {
      checkHasRepository(repositoryId);
    }
    return entity;
  }


  private List<ArtifactVersion> fetchAllArtifactVersions(String repositoryId, UUID artifactId,
      boolean includeSoftDeleted) {
    List<ArtifactVersion> a;
    if (includeSoftDeleted) {
      a = versionRepo
          .findAllByKey_RepositoryIdAndKey_ArtifactIdAndSeriesOrderByCreatedDesc(
              repositoryId, artifactId, false);
    } else {
      a = versionRepo
          .findAllByKey_RepositoryIdAndKey_ArtifactIdAndSeriesAndSoftDeletedOrderByCreatedDesc(
              repositoryId, artifactId, false, false);
    }
    if (a.isEmpty()) {
      checkSeries(repositoryId, artifactId);
      checkHasRepository(repositoryId);
    }
    return a;
  }


  private ArtifactVersion fetchLatestVersion(String repositoryId, UUID artifactId,
      Boolean includeSoftDeleted) {
    Optional<ArtifactVersion> version;
    if (includeSoftDeleted) {
      version = versionRepo
          .findFirstByKey_RepositoryIdAndKey_ArtifactIdAndSeriesOrderByCreatedDesc(
              repositoryId, artifactId, false);
    } else {
      version = versionRepo
          .findFirstByKey_RepositoryIdAndKey_ArtifactIdAndSeriesAndSoftDeletedOrderByCreatedDesc(
              repositoryId, artifactId, false, false);
    }
    if (version.isEmpty()) {
      checkAll(repositoryId, artifactId);
    }
    return version.orElseThrow(() -> new ResourceNotFoundException(artifactId, null, repositoryId));
  }


  private void checkAll(String repositoryId, UUID artifactId) {
    // throws when neither series nor version associated to the repository (unless default repository)
    checkHasRepository(repositoryId);
    // throws when (series or version) and not deleted exists
    // (version implies series)
    checkSeriesExist(repositoryId, artifactId);
    // throws when (series or version) exists
    checkSeriesHistory(repositoryId, artifactId);
    // throws when (no version that is not deleted) exists
    checkSeriesEmpty(repositoryId, artifactId);
  }

  private void checkSeries(String repositoryId, UUID artifactId) {
    checkSeriesExist(repositoryId, artifactId);
    checkSeriesHistory(repositoryId, artifactId);
  }

  private void checkSeriesHistory(String repositoryId, UUID artifactId) {
    if (!hasResourceSeries(repositoryId, artifactId, false)) {
      throw new ResourceNoContentException(artifactId, repositoryId);
    }
  }

  private void checkSeriesExist(String repositoryId, UUID artifactId) {
    if (!hasResourceSeries(repositoryId, artifactId, true)) {
      throw new ResourceNotFoundException(artifactId, repositoryId);
    }
  }

  private void checkSeriesEmpty(String repositoryId, UUID artifactId) {
    if (!hasVersions(repositoryId, artifactId, false)) {
      throw new ResourceNoContentException(artifactId, repositoryId);
    }
  }


  private void checkHasRepository(String repositoryId) {
    if (!hasRepository(repositoryId)) {
      throw new RepositoryNotFoundException("Unknown Artifact Repository " + repositoryId);
    }
  }

  public boolean hasRepository(String repositoryId) {
    if (repositoryId.equals(defaultRepositoryId)) {
      return true;
    }
    return versionRepo.existsByKey_RepositoryId(repositoryId);
  }


  private boolean hasVersions(String repositoryId, UUID artifactId,
      boolean includeSoftDeleted) {
    return versionRepo.existsByKey_RepositoryIdAndKey_ArtifactIdAndSeriesAndSoftDeleted(
        repositoryId, artifactId, false, includeSoftDeleted);
  }

  public boolean hasResourceSeries(String repositoryId, UUID artifactId,
      boolean includeSoftDeleted) {
    if (includeSoftDeleted) {
      return versionRepo.existsByKey_RepositoryIdAndKey_ArtifactId(
          repositoryId, artifactId);
    } else {
      return versionRepo
          .existsByKey_RepositoryIdAndKey_ArtifactIdAndSoftDeleted(
              repositoryId, artifactId, false);
    }
  }

}
