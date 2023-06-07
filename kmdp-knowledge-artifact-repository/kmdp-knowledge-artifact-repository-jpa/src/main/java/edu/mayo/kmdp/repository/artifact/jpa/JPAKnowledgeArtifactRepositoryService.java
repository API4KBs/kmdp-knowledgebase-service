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

import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerProperties;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryService;
import java.util.Properties;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;

public interface JPAKnowledgeArtifactRepositoryService extends KnowledgeArtifactRepositoryService {

  static DataSource inMemoryDataSource() {
    var dbName = UUID.randomUUID().toString();

    DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();
    dataSourceBuilder.driverClassName("org.h2.Driver");
    dataSourceBuilder.url("jdbc:h2:mem:" + dbName);
    dataSourceBuilder.username("SA");
    dataSourceBuilder.password("");

    return dataSourceBuilder.build();
  }


  static KnowledgeArtifactRepositoryService inMemoryArtifactRepository(
      Properties cfg) {
    return new JPAKnowledgeArtifactRepository(
        inMemoryDataSource(),
        new KnowledgeArtifactRepositoryServerProperties(cfg));
  }


}


