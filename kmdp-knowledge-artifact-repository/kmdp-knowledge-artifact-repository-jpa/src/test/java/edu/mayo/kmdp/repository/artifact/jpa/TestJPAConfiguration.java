package edu.mayo.kmdp.repository.artifact.jpa;

import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerProperties;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerProperties.KnowledgeArtifactRepositoryOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ComponentScan(basePackageClasses = JPAArtifactDAO.class)
@PropertySource("classpath:application.test.properties")
public class TestJPAConfiguration {

  @Value("${edu.mayo.kmdp.repository.artifact.identifier:default}")
  private String repoId;
  @Value("${edu.mayo.kmdp.repository.artifact.name:Default Artifact Repository}")
  private String repoName;
  @Value("${edu.mayo.kmdp.repository.artifact.namespace}")
  private String namespace;

  @Bean
  KnowledgeArtifactRepositoryServerProperties testCfg() {
    return KnowledgeArtifactRepositoryServerProperties.emptyConfig()
        .with(KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_ID, repoId)
        .with(KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_NAME, repoName)
        .with(KnowledgeArtifactRepositoryOptions.BASE_NAMESPACE, namespace);
  }

}
