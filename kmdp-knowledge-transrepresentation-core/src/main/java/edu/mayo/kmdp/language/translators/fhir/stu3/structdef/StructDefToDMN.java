package edu.mayo.kmdp.language.translators.fhir.stu3.structdef;

import static edu.mayo.kmdp.language.translators.fhir.stu3.structdef.StructDefToDMNHelper.PATH_SEPARATOR;
import static edu.mayo.kmdp.language.translators.fhir.stu3.structdef.StructDefToDMNHelper.add;
import static edu.mayo.kmdp.language.translators.fhir.stu3.structdef.StructDefToDMNHelper.findRootElement;
import static edu.mayo.kmdp.language.translators.fhir.stu3.structdef.StructDefToDMNHelper.isRoot;

import ca.uhn.fhir.context.BaseRuntimeChildDefinition;
import ca.uhn.fhir.context.BaseRuntimeElementDefinition;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeChildChoiceDefinition;
import ca.uhn.fhir.context.RuntimeChildCompositeDatatypeDefinition;
import ca.uhn.fhir.context.RuntimeChildContainedResources;
import ca.uhn.fhir.context.RuntimeChildPrimitiveDatatypeDefinition;
import ca.uhn.fhir.context.RuntimeChildResourceBlockDefinition;
import ca.uhn.fhir.context.RuntimeChildResourceDefinition;
import ca.uhn.fhir.context.RuntimeCompositeDatatypeDefinition;
import ca.uhn.fhir.context.RuntimeElemContainedResourceList;
import ca.uhn.fhir.context.RuntimePrimitiveDatatypeDefinition;
import ca.uhn.fhir.context.RuntimeResourceBlockDefinition;
import ca.uhn.fhir.context.RuntimeResourceDefinition;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.dstu3.model.Base64BinaryType;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.dstu3.model.DecimalType;
import org.hl7.fhir.dstu3.model.ElementDefinition;
import org.hl7.fhir.dstu3.model.Enumeration;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.InstantType;
import org.hl7.fhir.dstu3.model.PositiveIntType;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.hl7.fhir.dstu3.model.TimeType;
import org.hl7.fhir.dstu3.model.UnsignedIntType;
import org.hl7.fhir.dstu3.model.UriType;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;
import org.omg.spec.dmn._20180521.model.TDefinitions;
import org.omg.spec.dmn._20180521.model.TItemDefinition;
import org.omg.spec.dmn._20180521.model.TUnaryTests;

/**
 * Core implementation of the fhir:StructureDefiniton to dmn:ItemDefinition
 * translator {@link StructDefToDMNProjectingTranslator}
 *
 * The implementation is based on HAPI FHIR. It leverages the StructureDefinition
 * to resolve the HAPI 'Runtime' definitions, inferring data types and bindings
 */
public class StructDefToDMN {

  private static final FhirContext FHIR_CONTEXT = FhirContext.forDstu3();

  public TDefinitions transformRootElementToFrame(StructureDefinition profile) {
    TDefinitions dmnRoot = new TDefinitions();

    transformRootElementToFrame(findRootElement(profile), profile, "", dmnRoot);

    return dmnRoot;
  }

  private TItemDefinition transformRootElementToFrame(ElementDefinition element,
      StructureDefinition profile, String path, TDefinitions dmnRoot) {
    String relativePath = element.getPath().replace(path, "");

    TItemDefinition frame = new TItemDefinition()
        .withName(relativePath)
        .withLabel(element.getLabel())
        .withDescription(element.getDefinition())
        .withId(element.getId());

    Collection<TItemDefinition> slots = profile.getSnapshot().getElement().stream()
        .filter(el -> !isRoot(el))
        .filter(el -> !"0".equals(el.getMax()))
        .filter(x -> !StructDefToDMNHelper.isExtension(x))
        .flatMap(el -> transformNamedElementToSlot(el, frame.getName(), dmnRoot))
        .collect(Collectors.toList());

    return add(frame.withItemComponent(slots), dmnRoot);
  }

  private Stream<TItemDefinition> transformNamedElementToSlot(ElementDefinition elemDef,
      String path, TDefinitions dmnRoot) {
    String relativePath = elemDef.getPath().replace(path, "")
        .substring(1);

    if (relativePath.indexOf(PATH_SEPARATOR) > 0) {
      // TODO...
      return Stream.empty();
    }

    boolean isCollection = "*".equals(elemDef.getMax()) || Integer.parseInt(elemDef.getMax()) > 1;

    RuntimeResourceDefinition res = FHIR_CONTEXT.getResourceDefinition(path);
    BaseRuntimeChildDefinition childDef = res.getChildByName(relativePath);

    if (childDef instanceof RuntimeChildChoiceDefinition) {
      return childDef.getValidChildNames().stream()
          .map(childName -> {
            BaseRuntimeElementDefinition<?> choice = childDef.getChildByName(childName);
            TItemDefinition choiceItem = transformRuntimeElementToFrame(choice, dmnRoot);
            return new TItemDefinition()
                .withName(childName)
                .withLabel(childName)
                .withIsCollection(isCollection)
                .withTypeRef(choiceItem.getName());
          });

    } else {
      return transformDatatypeToSlot(childDef, dmnRoot);
    }

  }


  private Stream<TItemDefinition> transformDatatypeToSlot(BaseRuntimeChildDefinition childDef,
      TDefinitions dmnRoot) {
    if (childDef instanceof RuntimeChildPrimitiveDatatypeDefinition) {
      RuntimeChildPrimitiveDatatypeDefinition basicDatatypeDef = (RuntimeChildPrimitiveDatatypeDefinition) childDef;
      if (basicDatatypeDef.getBoundEnumType() != null) {
        TItemDefinition enumType = add(
            mapEnumeratedValue(basicDatatypeDef.getDatatype(), basicDatatypeDef.getBoundEnumType()),
            dmnRoot);
        return Stream.of(new TItemDefinition()
            .withName(childDef.getElementName())
            .withLabel(childDef.getElementName())
            .withIsCollection(childDef.getMax() > 1 || childDef.getMax() < 0)
            .withTypeRef(enumType.getName()));
      } else {
        return Stream.of(new TItemDefinition()
            .withName(childDef.getElementName())
            .withLabel(childDef.getElementName())
            .withIsCollection(childDef.getMax() > 1 || childDef.getMax() < 0)
            .withTypeRef(mapNativeType(basicDatatypeDef.getDatatype())));
      }
    } else if (childDef instanceof RuntimeChildCompositeDatatypeDefinition) {
      RuntimeChildCompositeDatatypeDefinition compositeDatatypeDef = (RuntimeChildCompositeDatatypeDefinition) childDef;
      BaseRuntimeElementDefinition<?> dataTypeDef = FHIR_CONTEXT
          .getElementDefinition(compositeDatatypeDef.getDatatype());
      TItemDefinition sub = transformRuntimeElementToFrame(dataTypeDef, dmnRoot);
      return Stream.of(new TItemDefinition()
          .withName(childDef.getElementName())
          .withLabel(childDef.getElementName())
          .withIsCollection(childDef.getMax() > 1 || childDef.getMax() < 0)
          .withTypeRef(sub.getName()));
    } else if (childDef instanceof RuntimeChildResourceDefinition) {
      TItemDefinition reference = buildReference(dmnRoot);
      return Stream.of(new TItemDefinition()
          .withName(childDef.getElementName())
          .withLabel(childDef.getElementName())
          .withIsCollection(childDef.getMax() > 1 || childDef.getMax() < 0)
          .withTypeRef(reference.getName()));
    } else if (childDef instanceof RuntimeChildChoiceDefinition) {
      RuntimeChildChoiceDefinition choiceDef = (RuntimeChildChoiceDefinition) childDef;
      return choiceDef.getValidChildNames().stream()
          .map(choiceName -> {
            BaseRuntimeElementDefinition<?> choice = choiceDef.getChildByName(choiceName);
            return new TItemDefinition()
                .withName(choiceName)
                .withId(choiceName)
                .withTypeRef(transformRuntimeElementToFrame(choice, dmnRoot).getName());
          });
    } else if (childDef instanceof RuntimeChildResourceBlockDefinition) {
      RuntimeChildResourceBlockDefinition childBlockDef = (RuntimeChildResourceBlockDefinition) childDef;
      return childBlockDef.getValidChildNames().stream()
          .map(childBlockDef::getChildByName)
          .map(block -> transformRuntimeElementToFrame(block, dmnRoot))
          .map(frame -> new TItemDefinition()
              .withName(childBlockDef.getElementName())
              .withLabel(childBlockDef.getElementName())
              .withIsCollection(childBlockDef.getMax() > 1 || childDef.getMax() < 0)
              .withTypeRef(frame.getName()));
    } else if (childDef instanceof RuntimeChildContainedResources) {
      return Stream.of(new TItemDefinition()
          .withName("contained")
          .withLabel("contained")
          .withIsCollection(true)
          .withTypeRef("Any"));
    } else {
      throw new UnsupportedOperationException("Unable to map child class" + childDef.getClass());
    }
  }


  private TItemDefinition transformRuntimeElementToFrame(
      BaseRuntimeElementDefinition<?> runtimeDef, TDefinitions dmnRoot) {
    if (runtimeDef instanceof RuntimePrimitiveDatatypeDefinition) {
      RuntimePrimitiveDatatypeDefinition basicElemDef = (RuntimePrimitiveDatatypeDefinition) runtimeDef;
      // anonymous built-in, don't add
      return new TItemDefinition()
          .withName(mapNativeType(basicElemDef.getImplementingClass()));
    } else if (runtimeDef instanceof RuntimeCompositeDatatypeDefinition) {
      RuntimeCompositeDatatypeDefinition compositeDDef = (RuntimeCompositeDatatypeDefinition) runtimeDef;
      return add(new TItemDefinition()
              .withName(compositeDDef.getName())
              .withId(compositeDDef.getName())
              .withItemComponent(compositeDDef.getChildren().stream()
                  .filter(x -> !StructDefToDMNHelper.isExtension(x))
                  .flatMap(x -> transformDatatypeToSlot(x, dmnRoot))
                  .collect(Collectors.toList())),
          dmnRoot);
    } else if (runtimeDef instanceof RuntimeResourceBlockDefinition) {
      RuntimeResourceBlockDefinition blockDef = (RuntimeResourceBlockDefinition) runtimeDef;
      String name = blockDef.getName()
          .substring(blockDef.getName().lastIndexOf(PATH_SEPARATOR) + 1);
      return add(new TItemDefinition()
              .withName(name)
              .withId(name)
              .withItemComponent(blockDef.getChildren().stream()
                  .filter(x -> !StructDefToDMNHelper.isExtension(x))
                  .flatMap(x -> this.transformDatatypeToSlot(x, dmnRoot))
                  .collect(Collectors.toList())),
          dmnRoot);
    } else if (runtimeDef instanceof RuntimeElemContainedResourceList) {
      return new TItemDefinition()
          .withName("Any")
          .withLabel("Any")
          .withIsCollection(true)
          .withTypeRef("Any");
    } else {
      throw new UnsupportedOperationException("Unable to map " + runtimeDef.getClass());
    }
  }


  private TItemDefinition buildReference(TDefinitions dmnRoot) {
    return add(new TItemDefinition()
            .withName("Reference")
            .withId("Reference")
            .withItemComponent(
                new TItemDefinition()
                    .withName("reference")
                    .withTypeRef("string"))
            .withItemComponent(
                new TItemDefinition()
                    .withName("identifier")
                    .withTypeRef("Identifier")  // assume this is built from a main resource
            ),
        dmnRoot);
  }

  private String mapNativeType(Class<?> implementingClass) {
    if (String.class.isAssignableFrom(implementingClass)
        || StringType.class.isAssignableFrom(implementingClass)
        || UriType.class.isAssignableFrom(implementingClass)
        || IdType.class.isAssignableFrom(implementingClass)
        || XhtmlNode.class.isAssignableFrom(implementingClass)
    ) {
      return "string";
    }
    if (InstantType.class.isAssignableFrom(implementingClass)
        || DateTimeType.class.isAssignableFrom(implementingClass)
    ) {
      return "dateTime";
    }
    if (BooleanType.class.isAssignableFrom(implementingClass)
        || Boolean.class.isAssignableFrom(implementingClass)) {
      return "boolean";
    }
    if (DecimalType.class.isAssignableFrom(implementingClass)
        || UnsignedIntType.class.isAssignableFrom(implementingClass)
        || PositiveIntType.class.isAssignableFrom(implementingClass)) {
      return "number";
    }
    if (Date.class.isAssignableFrom(implementingClass)
        || DateType.class.isAssignableFrom(implementingClass)) {
      return "date";
    }
    if (TimeType.class.isAssignableFrom(implementingClass)) {
      return "time";
    }
    if (Base64BinaryType.class.isAssignableFrom(implementingClass)) {
      return "Any";
    }
    if (Enumeration.class.isAssignableFrom(implementingClass)) {
      return "CodeableConcept";
    }
    throw new UnsupportedOperationException("Unable to map primitive class " + implementingClass);
  }


  private TItemDefinition mapEnumeratedValue(Class<?> implementingClass,
      Class<? extends Enum<?>> boundEnumType) {
    if (!Enumeration.class.isAssignableFrom(implementingClass)) {
      throw new IllegalArgumentException(
          "Defensive: An enum-bound class was specified, but the base type is not enumerated");
    }
    if (boundEnumType == null) {
      throw new IllegalArgumentException(
          "Defensive: Unable to map Enumerated type without a bound enumeration");
    }
    return new TItemDefinition()
        .withName(boundEnumType.getSimpleName())
        .withLabel(boundEnumType.getSimpleName())
        .withId(boundEnumType.getSimpleName())
        .withTypeRef("string")
        .withAllowedValues(new TUnaryTests().withText(
            Arrays.stream(boundEnumType.getEnumConstants())
                .map(this::enumToCode)
                .filter(str -> !"?".equals(str))
                .collect(Collectors.joining(","))
        ));
  }

  private String enumToCode(Enum<?> x) {
    try {
      return (String) x.getClass().getMethod("toCode").invoke(x);
    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      throw new IllegalStateException("Defensive: FHIR enum does not have a toCode method");
    }
  }


}