/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.mayo.kmdp.language.detectors;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;

import edu.mayo.kmdp.util.JaxbUtil;
import edu.mayo.kmdp.util.Util;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.w3c.dom.Document;


public abstract class XMLBasedLanguageDetector<T>
    extends AbstractLanguageDetector {

  protected Class<T> root;

  @Override
  public List<SyntacticRepresentation> getSupportedRepresentations() {
    return Arrays.asList(
        rep(getSupportedLanguage()),
        rep(getSupportedLanguage(), XML_1_1));
  }

  @Override
  protected Optional<SyntacticRepresentation> detectBinary(byte[] bytes) {
    if (Arrays.equals("<".getBytes(), Arrays.copyOfRange(bytes, 0, 1))
        && JaxbUtil.unmarshall(root, root, new ByteArrayInputStream(bytes)).isPresent()) {
      return Optional.of(rep(getSupportedLanguage(), XML_1_1, Charset.defaultCharset()));
    } else {
      return Optional.empty();
    }
  }

  @Override
  protected Optional<SyntacticRepresentation> detectString(String str) {
    if (!Util.isEmpty(str) && str.charAt(0) == '<'
        && JaxbUtil.unmarshall(root, root, str).isPresent()) {
      return Optional.of(rep(getSupportedLanguage(), XML_1_1, Charset.defaultCharset()));
    } else {
      return Optional.empty();
    }
  }

  @Override
  protected Optional<SyntacticRepresentation> detectAST(Object dox) {
    if (dox instanceof Document
        && JaxbUtil.unmarshall(root, root, (Document) dox).isPresent()) {
      return Optional.of(rep(getSupportedLanguage(), XML_1_1));
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