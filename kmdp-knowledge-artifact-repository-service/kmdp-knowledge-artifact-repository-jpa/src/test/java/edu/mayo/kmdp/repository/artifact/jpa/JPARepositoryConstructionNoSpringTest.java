package edu.mayo.kmdp.repository.artifact.jpa;

import static edu.mayo.kmdp.repository.artifact.jpa.JPAKnowledgeArtifactRepositoryService.inMemoryArtifactRepository;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerProperties;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerProperties.KnowledgeArtifactRepositoryOptions;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryService;
import edu.mayo.kmdp.repository.artifact.jpa.entities.ArtifactVersionEntity;
import edu.mayo.kmdp.repository.artifact.jpa.entities.KeyId;
import edu.mayo.kmdp.repository.artifact.jpa.stores.simple.SimpleArtifactVersionRepository;
import edu.mayo.kmdp.util.Util;
import java.util.List;
import javax.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.id.Pointer;

class JPARepositoryConstructionNoSpringTest {

  KnowledgeArtifactRepositoryServerProperties cfg =
      new KnowledgeArtifactRepositoryServerProperties(
          JPADaoNoSpringTest.class.getResourceAsStream("/application.test.properties"));

  String repoId;

  @BeforeEach
  void init() {
    repoId = cfg.getTyped(KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_ID);
  }

  @Test
  void testConstruction() {
    KnowledgeArtifactRepositoryService svc =
        inMemoryArtifactRepository(cfg);

    Answer<List<Pointer>> ptrs = svc.listKnowledgeArtifacts(repoId);
    assertTrue(ptrs.isSuccess());
  }

  @Test
  void testEntityManager() {
    JPAKnowledgeArtifactRepository jpk =
        (JPAKnowledgeArtifactRepository) inMemoryArtifactRepository(cfg);
    SimpleArtifactVersionRepository repo = (SimpleArtifactVersionRepository) jpk.getPersistenceLayer();
    EntityManager emRef = repo.getEMRef();

    ArtifactVersionEntity x = new ArtifactVersionEntity(repoId, Util.uuid("xxx"),"1");
    repo.save(x);

    assertTrue(emRef.contains(x));
    ArtifactVersionEntity y = emRef.find(ArtifactVersionEntity.class, new KeyId(repoId, Util.uuid("xxx"),"1"));
    assertNotNull(y);
  }

}
