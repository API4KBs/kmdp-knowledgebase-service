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
package edu.mayo.kmdp.language.common.csv;

import edu.mayo.kmdp.ConfigProperties;
import edu.mayo.kmdp.Opt;
import edu.mayo.kmdp.Option;
import edu.mayo.kmdp.language.common.csv.CSVExtractorConfig.CSVExtractorOptions;
import java.util.Properties;

@SuppressWarnings("unchecked")
public class CSVExtractorConfig extends
    ConfigProperties<CSVExtractorConfig, CSVExtractorOptions> {

  private static final Properties DEFAULTS = defaulted(CSVExtractorOptions.class);

  public CSVExtractorConfig() {
    super(DEFAULTS);
  }

  @Override
  public CSVExtractorOptions[] properties() {
    return CSVExtractorOptions.values();
  }

  public enum CSVExtractorOptions implements Option<CSVExtractorOptions> {

    AS_TEXT(Opt.of("asText", "false", "", Boolean.class, false)),
    SEPARATOR(Opt.of("separator", ",", "", String.class, false)),
    SHEET(Opt.of("sheet", "0", "", Integer.class, false)),
    COLUMNS(Opt.of("columns", "", "", String.class, false));

    private final Opt<CSVExtractorOptions> opt;

    CSVExtractorOptions(Opt<CSVExtractorOptions> opt) {
      this.opt = opt;
    }

    @Override
    public Opt<CSVExtractorOptions> getOption() {
      return opt;
    }

  }
}
