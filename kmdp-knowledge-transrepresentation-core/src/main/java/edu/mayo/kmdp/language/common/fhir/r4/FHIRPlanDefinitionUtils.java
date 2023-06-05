package edu.mayo.kmdp.language.common.fhir.r4;

import static edu.mayo.kmdp.language.common.fhir.r4.FHIRVisitor.getContained;
import static edu.mayo.kmdp.util.StreamUtil.filterAs;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent;
import org.hl7.fhir.r4.model.Reference;
import org.omg.spec.api4kp._20200801.id.Term;

public final class FHIRPlanDefinitionUtils {

  private FHIRPlanDefinitionUtils() {
    // static functions only
  }

  public static CodeableConcept toCodeableConcept(Term trm) {
    Coding cd = toCoding(trm);
    return new CodeableConcept()
        .addCoding(cd)
        .setText(cd.getDisplay());
  }

  public static CodeableConcept toCodeableConcept(List<Term> trm) {
    CodeableConcept cc = new CodeableConcept();
    trm.forEach(t -> cc.addCoding(toCoding(t)));
    cc.setText(trm.stream().map(Term::getLabel).collect(Collectors.joining(" + ")));
    return cc;
  }


  public static CodeableConcept toCodeableConcept(Coding cd) {
    return new CodeableConcept()
        .addCoding(cd)
        .setText(cd.getDisplay());
  }

  public static Coding toCoding(Term trm) {
    return new Coding()
        .setCode(trm.getTag())
        .setDisplay(trm.getLabel())
        .setSystem(trm.getNamespaceUri().toString())
        .setVersion(trm.getVersionTag());
  }

  public static Term toTerm(Coding c) {
    return Term.newTerm(
        URI.create(c.getSystem()),
        c.getCode(),
        c.getVersion(),
        c.getDisplay());
  }

  public static List<Term> toTerm(CodeableConcept c) {
    return c.getCoding().stream()
        .map(FHIRPlanDefinitionUtils::toTerm)
        .collect(Collectors.toList());
  }

  public static String toDisplayTerms(CodeableConcept c) {
    return c.getCoding().stream()
        .map(FHIRPlanDefinitionUtils::toTerm)
        .map(Term::getLabel)
        .collect(Collectors.joining(","));
  }


  /**
   * Traverses a PlanDefintion, returning a Stream of the nested Action Components
   *
   * @param planDef the root PlanDefinition
   * @return a Stream of the nested Action Components
   */
  public static Stream<PlanDefinitionActionComponent> getSubActions(PlanDefinition planDef) {
    return planDef.getAction().stream()
        .flatMap(FHIRPlanDefinitionUtils::getSubActions);
  }

  /**
   * Traverses a PlanDefintion with nested (contained) PlanDefinitions
   *
   * @param planDef the root PlanDefinition
   * @return a Stream of the Action components, across all nested PlanDefinitions
   */
  public static Stream<PlanDefinitionActionComponent> getDeepNestedSubActions(
      PlanDefinition planDef) {
    return getNestedPlanDefs(planDef)
        .flatMap(FHIRPlanDefinitionUtils::getSubActions);
  }

  public static Stream<? extends PlanDefinitionActionComponent> getSubActions(
      PlanDefinitionActionComponent act) {
    return Stream.concat(
        Stream.of(act),
        act.getAction().stream().flatMap(FHIRPlanDefinitionUtils::getSubActions));
  }


  /**
   * Retrieves the action in a PlanDefinition that has the given title The search is recursive
   * within a PD, but does not extend to nested PlanDefinitions
   *
   * @param planDefinition
   * @param title
   * @return
   */
  public static Optional<PlanDefinitionActionComponent> getSubActionByTitle(
      PlanDefinition planDefinition, String title) {
    return getSubActions(planDefinition)
        .filter(act -> title.equalsIgnoreCase(act.getTitle()))
        .findFirst();
  }

  /**
   * Traverses a PlanDefintion with nested (contained) PlanDefinitions
   *
   * @param planDef the root PlanDefinition
   * @return a Stream of the nested PlanDefinitions
   */
  public static Stream<PlanDefinition> getNestedPlanDefs(PlanDefinition planDef) {
    return getContained(planDef, PlanDefinition.class);
  }

  /**
   * Traverses a PlanDefintion with nested (contained) PlanDefinitions and returns the one whose
   * name matches the given name
   *
   * @param planDef the root PlanDefinition
   * @param name    the name of the PlanDefinition to select
   * @return an Optional nested PlanDefinitions
   */
  public static Optional<PlanDefinition> getNestedPlanDefByName(
      PlanDefinition planDef, String name) {
    return getNestedPlanDefs(planDef)
        .filter(pd -> pd.getName().equalsIgnoreCase(name.trim()))
        .findFirst();
  }


  public static <T> Optional<T> resolveInternalReference(
      DomainResource root, Reference ref, Class<T> tgtClass) {
    return getContained(root, DomainResource.class)
        .filter(x -> ref.getReference().contains(x.getId()))
        .flatMap(filterAs(tgtClass))
        .findFirst();
  }

  public static boolean joins(String pk, String fkHref) {
    if (pk == null || fkHref == null) {
      return false;
    }
    return asId(pk).equals(refToId(fkHref));
  }

  public static String refToId(String href) {
    return href.startsWith("#")
        ? asId(href.substring(1))
        : asId(href);
  }

  public static String asId(String id) {
    return id.trim().replace("_", "");
  }

}
