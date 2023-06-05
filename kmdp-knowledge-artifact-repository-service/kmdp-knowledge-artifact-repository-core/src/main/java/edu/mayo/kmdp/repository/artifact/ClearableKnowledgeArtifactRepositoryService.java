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
package edu.mayo.kmdp.repository.artifact;


/**
 * An extension of {@link KnowledgeArtifactRepositoryService} that allows for clearing all content.
 */
public interface ClearableKnowledgeArtifactRepositoryService extends KnowledgeArtifactRepositoryService {

  /**
   * Clears all content.
   *
   * !!NOTICE!! This clears all data and is irreversible. This should not be used as a
   * general-purpose API call, but limited to very specific scenarios (such as clearing
   * content after integration test).
   */
  void clear();

}


