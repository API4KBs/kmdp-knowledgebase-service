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
package edu.mayo.kmdp.repository.artifact.exceptions;

import static edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries.NotFound;
import static java.lang.String.format;

import java.util.UUID;
import org.omg.spec.api4kp._20200801.ServerSideException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends ServerSideException {

  public ResourceNotFoundException() {
    this("Unable to find Artifact in Repository");
  }

  public ResourceNotFoundException(UUID artifactId, String versionTag, String repositoryId) {
    this(format("Unable to find %s:%s in Artifact repository %s", artifactId, versionTag,
        repositoryId));
  }

  public ResourceNotFoundException(UUID artifactId, String repositoryId) {
    this(format("Unable to find %s in Artifact repository %s", artifactId, repositoryId));
  }

  public ResourceNotFoundException(String message) {
    super(NotFound, message);
  }
}
