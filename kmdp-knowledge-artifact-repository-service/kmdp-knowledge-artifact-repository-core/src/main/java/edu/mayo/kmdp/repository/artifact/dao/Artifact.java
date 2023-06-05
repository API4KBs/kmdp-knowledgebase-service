package edu.mayo.kmdp.repository.artifact.dao;

import java.util.UUID;

public interface Artifact {

  default String getArtifactTag() {
    return getArtifactId().toString();
  }

  UUID getArtifactId();

  boolean isUnavailable();

  boolean isAvailable();

}
