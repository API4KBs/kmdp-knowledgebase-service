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
package edu.mayo.kmdp.repository.artifact.exceptions;

import static edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries.NoContent;
import static java.lang.String.format;

import edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries;
import java.util.UUID;
import org.omg.spec.api4kp._20200801.ServerSideException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NO_CONTENT)
public class ResourceNoContentException extends ServerSideException {

  public ResourceNoContentException() {
    this("No Content for this Resource");
  }

  public ResourceNoContentException(String message) {
    super(NoContent, message);
  }

  public ResourceNoContentException(UUID artifactId, String repositoryId) {
    this(format("No Artifacts associated to %s in Repository %s", artifactId, repositoryId));
  }
}
