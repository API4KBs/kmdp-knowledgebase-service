package edu.mayo.kmdp.language.detectors.owl2;

import edu.mayo.kmdp.ConfigProperties;
import edu.mayo.kmdp.Opt;
import edu.mayo.kmdp.Option;
import edu.mayo.kmdp.language.detectors.owl2.OWLDetectorConfig.DetectorParams;
import java.util.Properties;

@SuppressWarnings("unchecked")
public class OWLDetectorConfig extends
    ConfigProperties<OWLDetectorConfig, DetectorParams> {

  private static final Properties DEFAULTS = defaulted(DetectorParams.class);

  public OWLDetectorConfig() {
    super(DEFAULTS);
  }

  @Override
  public DetectorParams[] properties() {
    return DetectorParams.values();
  }

  public enum DetectorParams implements Option<DetectorParams> {

    CATALOG(Opt.of(
        "catalog",
        null,
        "URL of an XML Catalog to resolve ontology IRIs",
        String.class,
        false));

    private Opt<DetectorParams> opt;

    DetectorParams(Opt<DetectorParams> opt) {
      this.opt = opt;
    }

    @Override
    public Opt<DetectorParams> getOption() {
      return opt;
    }

  }
}