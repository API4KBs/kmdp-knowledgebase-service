package edu.mayo.kmdp.repository.artifact.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerProperties;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerProperties.KnowledgeArtifactRepositoryOptions;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;


@AutoConfigureDataJpa
@EnableAutoConfiguration
@ContextConfiguration(classes = TestJPAConfiguration.class)
@SpringBootTest(properties = {
    "spring.jpa.show-sql=true",
    "spring.jpa.hibernate.ddl-auto = update"})
class JPARepositoryConstructionTest {

  @Autowired
  JPAArtifactDAO dao;

  @Autowired
  KnowledgeArtifactRepositoryServerProperties cfg;

  KnowledgeArtifactRepositoryService repo;
  String repoId;

  @BeforeEach
  void init() {
    repo = new JPAKnowledgeArtifactRepository(dao, cfg);
    repoId = cfg.getTyped(KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_ID);
  }

  @Test
  void testConstruction() {
    assertNotNull(repo);

    repo.setKnowledgeArtifactVersion(repoId, UUID.randomUUID(), "v1", "hi".getBytes());
    int n = repo.listKnowledgeArtifacts(repoId).map(List::size).orElseGet(Assertions::fail);
    assertEquals(1, n);
  }


}
