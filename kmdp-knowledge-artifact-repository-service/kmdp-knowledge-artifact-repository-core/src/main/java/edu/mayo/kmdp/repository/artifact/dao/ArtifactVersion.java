package edu.mayo.kmdp.repository.artifact.dao;

import edu.mayo.kmdp.repository.artifact.exceptions.DaoRuntimeException;
import java.io.InputStream;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;

public interface ArtifactVersion {

  ResourceIdentifier getResourceIdentifier();

  boolean isUnavailable();

  boolean isAvailable();

  InputStream getDataStream() throws DaoRuntimeException;
}
