package edu.mayo.kmdp.repository.artifact.jpa.entities;

import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;

import edu.mayo.kmdp.repository.artifact.dao.Artifact;
import edu.mayo.kmdp.repository.artifact.dao.ArtifactVersion;
import edu.mayo.kmdp.repository.artifact.dao.DaoResult;
import edu.mayo.kmdp.repository.artifact.exceptions.DaoRuntimeException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Temporal;
import javax.persistence.Transient;
import javax.persistence.Version;
import javax.sql.rowset.serial.SerialBlob;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;

@Entity(name = "KnowledgeArtifacts")
public class ArtifactVersionEntity implements Artifact, ArtifactVersion,
    DaoResult<ArtifactVersionEntity> {

  @EmbeddedId
  private KeyId key;

  @Version
  private Long recordVersion;

  @Column(updatable = false)
  @Temporal(javax.persistence.TemporalType.TIMESTAMP)
  private Date created;

  private Boolean softDeleted;

  private Boolean series;

  @Lob
  private Blob binaryData;


  public ArtifactVersionEntity() {
    // empty constructor
  }

  public ArtifactVersionEntity(String repositoryId, UUID artifactId) {
    this.key = new KeyId(repositoryId, artifactId, artifactId.toString());
    this.softDeleted = false;
    this.series = true;
    this.created = new Date();
  }

  public ArtifactVersionEntity(String repositoryId, UUID artifactId, String versionTag) {
    this.key = new KeyId(repositoryId, artifactId, versionTag);
    this.softDeleted = false;
    this.series = false;
    this.created = new Date();
  }

  public static ArtifactVersionEntity pattern() {
    return new ArtifactVersionEntity();
  }

  public ArtifactVersionEntity withRepositoryId(String repositoryId) {
    if (key == null) {
      key = new KeyId();
    }
    key.setRepositoryId(repositoryId);
    return this;
  }

  public ArtifactVersionEntity withArtifactId(UUID artifactId) {
    if (key == null) {
      key = new KeyId();
    }
    key.setArtifactId(artifactId);
    return this;
  }

  public ArtifactVersionEntity withVersionTag(String versionTag) {
    if (key == null) {
      key = new KeyId();
    }
    key.setVersionTag(versionTag);
    return this;
  }

  public Boolean isSeries() {
    return series;
  }

  public void setSeries(Boolean series) {
    this.series = series;
  }

  public ArtifactVersionEntity withSeries(Boolean series) {
    setSeries(series);
    return this;
  }


  public Blob getBinaryData() {
    return binaryData;
  }

  public void setBinaryData(Blob binaryData) {
    this.binaryData = binaryData;
  }

  public ArtifactVersionEntity withBinaryData(Blob binaryData) {
    setBinaryData(binaryData);
    return this;
  }

  @Transient
  public void setBinaryData(byte[] binaryData) {
    try {
      this.binaryData = new SerialBlob(binaryData);
    } catch (SQLException sqle) {
      throw new DaoRuntimeException(sqle);
    }
  }

  @Transient
  public ArtifactVersionEntity withBinaryData(byte[] binaryData) {
    setBinaryData(binaryData);
    return this;
  }

  @Override
  public InputStream getDataStream() {
    try {
      return getBinaryData().getBinaryStream();
    } catch (SQLException sqle) {
      throw new DaoRuntimeException(sqle);
    }
  }

  public Boolean isSoftDeleted() {
    return softDeleted;
  }

  public void setSoftDeleted(Boolean deleted) {
    this.softDeleted = deleted;
  }

  public ArtifactVersionEntity withSoftDeleted(Boolean deleted) {
    this.softDeleted = deleted;
    return this;
  }

  @Transient
  public boolean isUnavailable() {
    return softDeleted;
  }

  @Transient
  public boolean isAvailable() {
    return !softDeleted;
  }

  @Override
  public ResourceIdentifier getResourceIdentifier() {
    return newId(key.getArtifactId(), key.getVersionTag());
  }

  @Override
  @Transient
  public UUID getArtifactId() {
    return key.getArtifactId();
  }

  public Date getCreated() {
    return created;
  }

  @Override
  public ArtifactVersionEntity getValue() {
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ArtifactVersionEntity that = (ArtifactVersionEntity) o;
    return key.equals(that.key);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key);
  }
}
