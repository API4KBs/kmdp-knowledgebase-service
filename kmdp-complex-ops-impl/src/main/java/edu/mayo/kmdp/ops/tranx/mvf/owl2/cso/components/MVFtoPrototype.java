package edu.mayo.kmdp.ops.tranx.mvf.owl2.cso.components;

import static edu.mayo.kmdp.ops.tranx.mvf.owl2.cso.CSOFabricator.CSO_NS;
import static edu.mayo.kmdp.ops.tranx.mvf.owl2.cso.CSOFabricator.CSV_NS;
import static edu.mayo.kmdp.ops.tranx.mvf.owl2.cso.CSOFabricator.CSV_PATTERN;
import static org.omg.spec.api4kp._20200801.id.Term.newTerm;

import edu.mayo.kmdp.util.StreamUtil;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.omg.spec.api4kp._20200801.id.Term;
import org.omg.spec.mvf._20220702.mvf.MVFDictionary;
import org.omg.spec.mvf._20220702.mvf.MVFEntry;
import org.omg.spec.mvf._20220702.mvf.Vocabulary;
import org.omg.spec.mvf._20220702.mvf.VocabularyEntry;
import org.snomed.SCTHelper;

public class MVFtoPrototype {

  public Set<ConceptPrototype> getDefinitions(
      MVFDictionary dict,
      Vocabulary csv,
      Vocabulary sct) {
    return csv.getEntry().stream()
        .map(ve -> getDefinition(ve, dict, sct))
        .flatMap(StreamUtil::trimStream)
        .collect(Collectors.toSet());
  }

  private Optional<ConceptPrototype> getDefinition(
      VocabularyEntry ve,
      MVFDictionary dict,
      Vocabulary sct) {
    var self = toCSOClass(ve);
    var mvfConcept = resolve(ve.getMVFEntry(), dict);
    if (mvfConcept.isEmpty()) {
      return Optional.empty();
    }

    var parentClass = mvfConcept.map(this::getParentClass);

    var focusEntry = mvfConcept.flatMap(c -> getFocus(c, sct));
    var focusTerm = focusEntry.map(VocabularyEntry::getTerm);
    var focusDef = focusEntry.map(VocabularyEntry::getDefinition);
    var focusParent = focusEntry.flatMap(this::getFocusParent);

    var definition = ve.getDefinition();

    var proto = new ConceptPrototype(
        self,
        parentClass.orElse(null),
        focusTerm.orElse(null),
        focusDef.orElse(null),
        focusParent.orElse(null),
        definition,
        mvfConcept.get().getName(),
        dict.getUri()
    );
    return Optional.of(proto);
  }

  private Term toCSOClass(VocabularyEntry ve) {
    var uri = ve.getUri()
        .replace(CSV_NS + "#", CSO_NS.toString());
    return newTerm(URI.create(uri));
  }

  private Optional<Term> getFocusParent(VocabularyEntry focus) {
    return focus.getReference().stream()
        .filter(r -> r.startsWith(SCTHelper.SNOMED_NS))
        .map(URI::create)
        .map(Term::newTerm)
        .findFirst();
  }

  private Term getParentClass(MVFEntry mvfConcept) {
    return Optional.ofNullable(mvfConcept)
        .flatMap(c -> c.getBroader().stream().findFirst())
        .map(MVFEntry::getExternalReference)
        .map(uri -> uri.replace(CSV_PATTERN.toString(), CSO_NS.toString()))
        .map(URI::create)
        .map(Term::newTerm)
        .orElse(null);
  }

  private Optional<MVFEntry> resolve(MVFEntry ref, MVFDictionary dict) {
    return dict.getEntry().stream()
        .filter(e -> Objects.equals(e.getUri(), ref.getUri()))
        .findFirst();
  }


  private Optional<VocabularyEntry> getFocus(
      MVFEntry mvfConcept, Vocabulary sct) {
    return mvfConcept.getContext().stream()
        .findFirst()
        .flatMap(f -> resolveFocus(f, sct));
  }

  private Optional<VocabularyEntry> resolveFocus(
      MVFEntry focusRef, Vocabulary sct) {
    return sct.getEntry().stream()
        .filter(ve -> Objects.equals(focusRef.getUri(), ve.getMVFEntry().getUri()))
        .findFirst();
  }

}
