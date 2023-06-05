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

import edu.mayo.kmdp.ConfigProperties;
import edu.mayo.kmdp.Opt;
import edu.mayo.kmdp.Option;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class KnowledgeArtifactRepositoryServerProperties extends
    ConfigProperties<KnowledgeArtifactRepositoryServerProperties, KnowledgeArtifactRepositoryServerProperties.KnowledgeArtifactRepositoryOptions> {

  private KnowledgeArtifactRepositoryServerProperties() {
    super(new Properties());
  }

  public KnowledgeArtifactRepositoryServerProperties(Properties defaults) {
    super(defaults);
  }

  public KnowledgeArtifactRepositoryServerProperties(InputStream propertiesStream) {
    super(defaulted(KnowledgeArtifactRepositoryOptions.class));
    if (propertiesStream != null) {
      try {
        this.load(propertiesStream);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public static KnowledgeArtifactRepositoryServerProperties emptyConfig() {
    return new KnowledgeArtifactRepositoryServerProperties();
  }

  @Override
  public KnowledgeArtifactRepositoryOptions[] properties() {
    return KnowledgeArtifactRepositoryOptions.values();
  }

  public enum KnowledgeArtifactRepositoryOptions implements
      Option<KnowledgeArtifactRepositoryOptions> {

    DEFAULT_REPOSITORY_ID(
        Opt.of("edu.mayo.kmdp.repository.artifact.identifier",
            getDefaultRepositoryId(),
            "ID of the default artifact repository",
            String.class,
            false)),

    DEFAULT_REPOSITORY_NAME(
        Opt.of("edu.mayo.kmdp.repository.artifact.name",
            "Default Artifact Repository",
            "Name of the default artifact repository",
            String.class,
            false)),

    BASE_DIR(
        Opt.of("edu.mayo.kmdp.repository.artifact.filesystem.directory",
            null,
            "Root directory for filesystem-based repositories",
            File.class,
            false)),

    BASE_NAMESPACE(
        Opt.of("edu.mayo.kmdp.repository.artifact.namespace",
            null,
            "Base namespace",
            String.class,
            false));


    private static final String ID = "default";

    private final Opt<KnowledgeArtifactRepositoryOptions> opt;

    KnowledgeArtifactRepositoryOptions(Opt<KnowledgeArtifactRepositoryOptions> opt) {
      this.opt = opt;
    }

    @Override
    public Opt<KnowledgeArtifactRepositoryOptions> getOption() {
      return opt;
    }


    private static String getDefaultRepositoryId() {
      return ID;
    }

  }
}
