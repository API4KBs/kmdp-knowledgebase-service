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
package edu.mayo.kmdp.repository.artifact.jpa;


import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryCore;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerProperties;
import edu.mayo.kmdp.repository.artifact.dao.ArtifactDAO;
import edu.mayo.kmdp.repository.artifact.jpa.stores.ArtifactVersionRepository;
import javax.sql.DataSource;
import org.omg.spec.api4kp._20200801.services.KPServer;

@KPServer
public class JPAKnowledgeArtifactRepository extends KnowledgeArtifactRepositoryCore
    implements JPAKnowledgeArtifactRepositoryService {

  public JPAKnowledgeArtifactRepository(ArtifactDAO dao,
      KnowledgeArtifactRepositoryServerProperties cfg) {
    super(dao, cfg);
  }

  /**
   * Test Constructor
   * @param dao the DataSource DAO
   * @param cfg the Config properties
   */
  public JPAKnowledgeArtifactRepository(DataSource dao,
      KnowledgeArtifactRepositoryServerProperties cfg) {
    super(new JPAArtifactDAO(dao, cfg), cfg);
  }

  ArtifactVersionRepository getPersistenceLayer() {
    return ((JPAArtifactDAO) dao).getPersistenceAdapter();
  }

}
