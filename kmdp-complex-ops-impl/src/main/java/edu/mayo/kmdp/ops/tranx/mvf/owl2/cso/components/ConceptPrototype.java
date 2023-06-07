package edu.mayo.kmdp.ops.tranx.mvf.owl2.cso.components;

import java.util.Objects;
import org.omg.spec.api4kp._20200801.id.Term;


public class ConceptPrototype {

  private String label;

  private String focusTerm;

  private String focusPostCoorDefinition;

  private Term focusParent;

  private String definition;

  private Term parentClass;

  private Term self;

  private String moduleUri;

  public ConceptPrototype() {
    // empty constructor
  }
  public ConceptPrototype(
      Term self, Term parentClass,
      String focus, String focusDef, Term focusParent,
      String definition, String label, String moduleUri) {
    this.label = label;
    this.focusTerm = focus;
    this.focusPostCoorDefinition = focusDef;
    this.focusParent = focusParent;
    this.definition = definition;
    this.parentClass = parentClass;
    this.self = self;
    this.moduleUri = moduleUri;
  }


  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getFocusTerm() {
    return focusTerm;
  }

  public void setFocusTerm(String focusTerm) {
    this.focusTerm = focusTerm;
  }

  public String getFocusPostCoorDefinition() {
    return focusPostCoorDefinition;
  }

  public void setFocusPostCoorDefinition(String focusPostCoorDefinition) {
    this.focusPostCoorDefinition = focusPostCoorDefinition;
  }

  public Term getFocusParent() {
    return focusParent;
  }

  public void setFocusParent(Term focusParent) {
    this.focusParent = focusParent;
  }

  public String getDefinition() {
    return definition;
  }

  public void setDefinition(String definition) {
    this.definition = definition;
  }

  public Term getParentClass() {
    return parentClass;
  }

  public void setParentClass(Term parentClass) {
    this.parentClass = parentClass;
  }

  public Term getSelf() {
    return self;
  }

  public void setSelf(Term self) {
    this.self = self;
  }

  public String getModuleUri() {
    return moduleUri;
  }

  public void setModuleUri(String moduleUri) {
    this.moduleUri = moduleUri;
  }

  @Override
  public String toString() {
    return "ConceptPrototype{" +
        "focalConcept=" + focusTerm +
        ", focalParent=" + focusParent +
        ", parentClass=" + parentClass +
        ", self=" + self +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConceptPrototype that = (ConceptPrototype) o;
    return Objects.equals(self.getConceptId(), that.self.getConceptId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(self.getConceptId());
  }


}
