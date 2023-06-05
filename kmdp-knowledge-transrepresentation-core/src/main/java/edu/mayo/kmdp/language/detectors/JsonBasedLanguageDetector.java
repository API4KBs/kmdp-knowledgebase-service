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
package edu.mayo.kmdp.language.detectors;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;

import com.fasterxml.jackson.databind.JsonNode;
import edu.mayo.kmdp.util.JSonUtil;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;


public abstract class JsonBasedLanguageDetector<T>
    extends AbstractLanguageDetector {

  protected Class<T> root;

  @Override
  public List<SyntacticRepresentation> getSupportedRepresentations() {
    return Arrays.asList(
        rep(getSupportedLanguage()),
        rep(getSupportedLanguage(), JSON));
  }

  @Override
  protected Optional<SyntacticRepresentation> detectBinary(byte[] bytes) {
    if (JSonUtil.tryParseJson(new String(bytes), root).isPresent()) {
      return Optional.of(rep(getSupportedLanguage(), JSON, Charset.defaultCharset()));
    } else {
      return Optional.empty();
    }
  }

  @Override
  protected Optional<SyntacticRepresentation> detectString(String str) {
    if (JSonUtil.tryParseJson(str, root).isPresent()) {
      return Optional.of(rep(getSupportedLanguage(), JSON, Charset.defaultCharset()));
    } else {
      return Optional.empty();
    }
  }

  @Override
  protected Optional<SyntacticRepresentation> detectAST(Object node) {
    if (node instanceof JsonNode
        && JSonUtil.tryParseJson((JsonNode) node, root).isPresent()) {
      return Optional.of(rep(getSupportedLanguage(), JSON));
    } else {
      return Optional.empty();
    }
  }

  @Override
  protected Optional<SyntacticRepresentation> detectTree(Object ast) {
    if (root.isInstance(ast)) {
      return Optional.of(rep(getSupportedLanguage()));
    } else {
      return Optional.empty();
    }
  }

}
