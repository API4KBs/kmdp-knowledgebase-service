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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;


public abstract class MultiFormatLanguageDetector<T>
    extends AbstractLanguageDetector {

  protected Class<T> root;
  protected XMLBasedLanguageDetector<T> xmlDetector;
  protected JsonBasedLanguageDetector<T> jsonDetector;

  protected MultiFormatLanguageDetector(XMLBasedLanguageDetector<T> xmlDetector,
      JsonBasedLanguageDetector<T> jsonDetector) {
    this.xmlDetector = xmlDetector;
    this.jsonDetector = jsonDetector;
  }

  @Override
  public List<SyntacticRepresentation> getSupportedRepresentations() {
    return Stream.of(
        xmlDetector.getSupportedRepresentations(),
        jsonDetector.getSupportedRepresentations())
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  @Override
  protected Optional<SyntacticRepresentation> detectBinary(byte[] bytes) {
    return xmlDetector.detectBinary(bytes)
        .or(() -> jsonDetector.detectBinary(bytes));
  }

  @Override
  protected Optional<SyntacticRepresentation> detectString(String str) {
    return xmlDetector.detectString(str)
        .or(() -> jsonDetector.detectString(str));
  }

  @Override
  protected Optional<SyntacticRepresentation> detectTree(Object tree) {
    return xmlDetector.detectTree(tree)
        .or(() -> jsonDetector.detectTree(tree));
  }

  @Override
  protected Optional<SyntacticRepresentation> detectAST(Object ast) {
    return xmlDetector.detectAST(ast)
        .or(() -> jsonDetector.detectAST(ast));
  }

}
