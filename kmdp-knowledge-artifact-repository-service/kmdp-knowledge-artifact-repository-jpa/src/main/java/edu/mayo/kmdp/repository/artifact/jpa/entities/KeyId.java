package edu.mayo.kmdp.repository.artifact.jpa.entities;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.Embeddable;
import org.hibernate.annotations.Type;

@Embeddable
public class KeyId implements Serializable {

  private String repositoryId;
  @Type(type = "uuid-char")
  private UUID artifactId;
  private String versionTag;

  public KeyId() {
    // empty constructor
  }

  public KeyId(String repositoryId, UUID artifactId, String versionTag) {
    this.repositoryId = repositoryId;
    this.artifactId = artifactId;
    this.versionTag = versionTag;
  }

  public String getRepositoryId() {
    return repositoryId;
  }

  public void setRepositoryId(String repositoryId) {
    this.repositoryId = repositoryId;
  }

  public UUID getArtifactId() {
    return artifactId;
  }

  public void setArtifactId(UUID uuid) {
    this.artifactId = uuid;
  }

  public String getVersionTag() {
    return versionTag;
  }

  public void setVersionTag(String versionTag) {
    this.versionTag = versionTag;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    KeyId keyId = (KeyId) o;
    return Objects.equals(repositoryId, keyId.repositoryId) && Objects
        .equals(artifactId, keyId.artifactId) && Objects
        .equals(versionTag, keyId.versionTag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(repositoryId, artifactId, versionTag);
  }

  @Override
  public String toString() {
    return "##" + artifactId + ":" + versionTag;
  }
}
