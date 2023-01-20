package edu.mayo.kmdp.knowledgebase.introspectors.owl2.internal;


import edu.mayo.kmdp.ConfigProperties;
import edu.mayo.kmdp.Opt;
import edu.mayo.kmdp.Option;
import edu.mayo.kmdp.knowledgebase.introspectors.owl2.internal.OWLIntrospectorConfiguration.OWLIntrospectorParams;
import edu.mayo.kmdp.registry.Registry;
import java.util.Properties;

/**
 * {@link ConfigProperties} for
 * {@link edu.mayo.kmdp.knowledgebase.introspectors.owl2.OWLMetadataIntrospector}
 */
public class OWLIntrospectorConfiguration
    extends
    ConfigProperties<OWLIntrospectorConfiguration, OWLIntrospectorParams> {

  private static final Properties DEFAULTS =
      defaulted(OWLIntrospectorConfiguration.OWLIntrospectorParams.class);

  public OWLIntrospectorConfiguration() {
    super(DEFAULTS);
  }

  public OWLIntrospectorConfiguration(Properties defaults) {
    super(defaults);
  }

  public OWLIntrospectorConfiguration(String cfg) {
    super(cfg);
  }

  @Override
  public OWLIntrospectorConfiguration.OWLIntrospectorParams[] properties() {
    return OWLIntrospectorConfiguration.OWLIntrospectorParams.values();
  }

  public enum OWLIntrospectorParams implements
      Option<OWLIntrospectorParams> {

    DEFAULT_VERSION(Opt.of(
        "defaultVersionTag",
        null,
        "Version tag to be used if unable to detect from the ontology version IRI",
        String.class,
        false)),
    VERSION_PATTERN(Opt.of(
        "versionPattern",
        "(https?:\\/\\/(?:\\w+\\.)*(?:[a-z][\\w\\d_-]+\\/)+)(?:(\\d{8}|SNAPSHOT)\\/)?(?:([a-zA-Z][\\w\\d_-]+)[\\/|\\#]?)",
        "A 2/3-part RegEx that determines the boundaries and order of a "
            + "(i) namespace, (ii) tag and (iii) version components of the URI, "
            + "where the version is optional",
        String.class,
        false)),
    NS_INDEX(Opt.of(
        "namespacePatternIndex",
        "1",
        "",
        Integer.class,
        false)),
    VER_INDEX(Opt.of(
        "versionPatternIndex",
        "2",
        "",
        Integer.class,
        false)),
    TAG_INDEX(Opt.of(
        "tagPatternIndex",
        "3",
        "",
        Integer.class,
        false)),
    IGNORES(Opt.of(
        "importIgnores",
        "",
        "List of IRIs of imported ontologis that will be ignored when extracting dependency relationships",
        String.class,
        false)),
    ASSET_NS(Opt.of(
        "assetNamespace",
        Registry.BASE_UUID_URN,
        "Enterprise Asset ID namespace",
        String.class,
        false)
    );

    private final
    Opt<OWLIntrospectorConfiguration.OWLIntrospectorParams> opt;

    OWLIntrospectorParams(
        Opt<OWLIntrospectorConfiguration.OWLIntrospectorParams> opt) {
      this.opt = opt;
    }

    @Override
    public Opt<OWLIntrospectorConfiguration.OWLIntrospectorParams>
    getOption() {
      return opt;
    }
  }
}
