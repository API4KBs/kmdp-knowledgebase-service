package edu.mayo.kmdp.language;

import static edu.mayo.kmdp.language.translators.fhir.stu3.structdef.StructDefToDMNHelper.isFeelPrimitive;
import static org.hl7.fhir.dstu3.model.Enumerations.FHIRAllTypes.ATTACHMENT;
import static org.hl7.fhir.dstu3.model.Enumerations.FHIRAllTypes.CODEABLECONCEPT;
import static org.hl7.fhir.dstu3.model.Enumerations.FHIRAllTypes.CODING;
import static org.hl7.fhir.dstu3.model.Enumerations.FHIRAllTypes.IDENTIFIER;
import static org.hl7.fhir.dstu3.model.Enumerations.FHIRAllTypes.META;
import static org.hl7.fhir.dstu3.model.Enumerations.FHIRAllTypes.NARRATIVE;
import static org.hl7.fhir.dstu3.model.Enumerations.FHIRAllTypes.OBSERVATION;
import static org.hl7.fhir.dstu3.model.Enumerations.FHIRAllTypes.PERIOD;
import static org.hl7.fhir.dstu3.model.Enumerations.FHIRAllTypes.QUANTITY;
import static org.hl7.fhir.dstu3.model.Enumerations.FHIRAllTypes.RANGE;
import static org.hl7.fhir.dstu3.model.Enumerations.FHIRAllTypes.RATIO;
import static org.hl7.fhir.dstu3.model.Enumerations.FHIRAllTypes.REFERENCE;
import static org.hl7.fhir.dstu3.model.Enumerations.FHIRAllTypes.SAMPLEDDATA;
import static org.hl7.fhir.dstu3.model.Enumerations.FHIRAllTypes.SIMPLEQUANTITY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Serialized_Knowledge_Expression;

import edu.mayo.kmdp.language.parsers.dmn.v1_2.DMN12Parser;
import edu.mayo.kmdp.language.translators.fhir.stu3.structdef.StructDefToDMNProjectingTranslator;
import edu.mayo.kmdp.language.validators.dmn.v1_2.DMN12Validator;
import edu.mayo.kmdp.util.FileUtil;
import edu.mayo.kmdp.util.Util;
import edu.mayo.kmdp.util.fhir.fhir3.FHIR3JsonUtil;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.Observation.ObservationComponentComponent;
import org.hl7.fhir.dstu3.model.Observation.ObservationReferenceRangeComponent;
import org.hl7.fhir.dstu3.model.Observation.ObservationRelatedComponent;
import org.hl7.fhir.dstu3.model.Observation.ObservationRelationshipType;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.hl7.fhir.dstu3.model.codesystems.IdentifierUse;
import org.hl7.fhir.dstu3.model.codesystems.NarrativeStatus;
import org.hl7.fhir.dstu3.model.codesystems.ObservationStatus;
import org.hl7.fhir.dstu3.model.codesystems.QuantityComparator;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder;
import org.omg.spec.dmn._20180521.model.TDefinitions;
import org.omg.spec.dmn._20180521.model.TItemDefinition;

public class FhirProfileTranslatorTest {

  @Test
  void testTranslator() {
    KnowledgeCarrier kc = readProfile("FHIRObservationStructureDefinitionFullList.json");
    Answer<KnowledgeCarrier> ast = new StructDefToDMNProjectingTranslator()
        .applyTransrepresent(kc, ModelMIMECoder.encode(rep(DMN_1_2)), null);

    assertTrue(ast.isSuccess());
    validate(ast.get());

    TDefinitions dmn = ast
        .flatOpt(x -> x.as(TDefinitions.class))
        .orElse(new TDefinitions());

    printout(ast);

    test(dmn);
    testIntegrity(dmn);
  }


  private void validate(KnowledgeCarrier kc) {
    LanguageValidator validator = new LanguageValidator(
        Collections.singletonList(new DMN12Validator()));
    Answer<Void> validationResponse = validator.applyValidate(kc);
    assertTrue(validationResponse.isSuccess());
  }

  private void test(TDefinitions dmn) {
    TItemDefinition rootType = findFrame(dmn, OBSERVATION.toCode());

    assertEquals(OBSERVATION.toCode(), rootType.getName());
    assertEquals(OBSERVATION.toCode(), rootType.getId());

    checkSlot(rootType, "id", "string", false);
    checkSlot(rootType, "implicitRules", "string", false);
    checkSlot(rootType, "language", "string", false);

    checkSlot(rootType, "meta", META.toCode(), false);
    TItemDefinition metaType = findFrame(dmn, META.toCode());
    checkSlot(metaType, "versionId", "string", false);
    checkSlot(metaType, "lastUpdated", "dateTime", false);
    checkSlot(metaType, "profile", "string", true);
    checkSlot(metaType, "security", CODING.toCode(), true);
    checkSlot(metaType, "tag", CODING.toCode(), true);

    TItemDefinition codingType = findFrame(dmn, CODING.toCode());
    checkSlot(codingType, "system", "string", false);
    checkSlot(codingType, "version", "string", false);
    checkSlot(codingType, "code", "string", false);
    checkSlot(codingType, "display", "string", false);
    checkSlot(codingType, "userSelected", "boolean", false);

    checkSlot(rootType, "contained", "Any", true);

    checkSlot(rootType, "text", NARRATIVE.toCode(), false);
    TItemDefinition narrativeType = findFrame(dmn, NARRATIVE.toCode());
    checkSlot(narrativeType, "status", NarrativeStatus.class.getSimpleName(), false);
    checkSlot(narrativeType, "div", "string", false);
    checkEnumeration(dmn, NarrativeStatus.class.getSimpleName(),
        Arrays.asList(
            NarrativeStatus.GENERATED.toCode(),
            NarrativeStatus.EXTENSIONS.toCode(),
            NarrativeStatus.ADDITIONAL.toCode(),
            NarrativeStatus.EMPTY.toCode()));

    checkSlot(rootType, "identifier", IDENTIFIER.toCode(), true);
    TItemDefinition identifierType = findFrame(dmn, IDENTIFIER.toCode());
    checkSlot(identifierType, "use", IdentifierUse.class.getSimpleName(), false);
    checkSlot(identifierType, "type", CODEABLECONCEPT.toCode(), false);
    checkSlot(identifierType, "system", "string", false);
    checkSlot(identifierType, "value", "string", false);
    checkSlot(identifierType, "period", PERIOD.toCode(), false);
    checkSlot(identifierType, "assigner", REFERENCE.toCode(), false);
    checkEnumeration(dmn, IdentifierUse.class.getSimpleName(),
        Arrays.asList(
            IdentifierUse.USUAL.toCode(),
            IdentifierUse.OFFICIAL.toCode(),
            IdentifierUse.TEMP.toCode(),
            IdentifierUse.SECONDARY.toCode()));

    TItemDefinition codeableConceptType = findFrame(dmn, CODEABLECONCEPT.toCode());
    checkSlot(codeableConceptType, "coding", CODING.toCode(), true);
    checkSlot(codeableConceptType, "text", "string", false);

    // ruled out in a profile
    checkNotExistingSlot(rootType, "basedOn");

    checkSlot(rootType, "category", CODEABLECONCEPT.toCode(), true);
    checkSlot(rootType, "code", CODEABLECONCEPT.toCode(), false);
    checkSlot(rootType, "subject", REFERENCE.toCode(), false);
    checkSlot(rootType, "context", REFERENCE.toCode(), false);

    checkSlot(rootType, "status", ObservationStatus.class.getSimpleName(), false);
    checkEnumeration(dmn, ObservationStatus.class.getSimpleName(),
        Arrays.asList(
            ObservationStatus.REGISTERED.toCode(),
            ObservationStatus.PRELIMINARY.toCode(),
            ObservationStatus.FINAL.toCode(),
            ObservationStatus.AMENDED.toCode(),
            ObservationStatus.CORRECTED.toCode(),
            ObservationStatus.CANCELLED.toCode(),
            ObservationStatus.ENTEREDINERROR.toCode(),
            ObservationStatus.UNKNOWN.toCode()
        ));

    checkNotExistingSlot(rootType, "effective[x]");
    checkSlot(rootType, "effectivePeriod", PERIOD.toCode(), false);
    checkSlot(rootType, "effectiveDateTime", "dateTime", false);

    checkSlot(rootType, "issued", "dateTime", false);
    checkSlot(rootType, "performer", REFERENCE.toCode(), true);

    checkNotExistingSlot(rootType, "value[x]");
    checkSlot(rootType, "valueQuantity", QUANTITY.toCode(), false);
    checkSlot(rootType, "valueCodeableConcept", CODEABLECONCEPT.toCode(), false);
    checkSlot(rootType, "valueString", "string", false);
    checkSlot(rootType, "valueBoolean", "boolean", false);
    checkSlot(rootType, "valueRange", RANGE.toCode(), false);
    checkSlot(rootType, "valueRatio", RATIO.toCode(), false);
    checkSlot(rootType, "valueSampledData", SAMPLEDDATA.toCode(), false);
    checkSlot(rootType, "valueAttachment", ATTACHMENT.toCode(), false);
    checkSlot(rootType, "valueTime", "time", false);
    checkSlot(rootType, "valueDateTime", "dateTime", false);
    checkSlot(rootType, "valuePeriod", PERIOD.toCode(), false);

    TItemDefinition qtyType = findFrame(dmn, QUANTITY.toCode());
    checkSlot(qtyType, "value", "number", false);
    checkSlot(qtyType, "comparator", QuantityComparator.class.getSimpleName(), false);
    checkSlot(qtyType, "unit", "string", false);
    checkSlot(qtyType, "system", "string", false);
    checkSlot(qtyType, "code", "string", false);
    checkEnumeration(dmn, QuantityComparator.class.getSimpleName(),
        Arrays.asList(
            QuantityComparator.LESS_THAN.toCode(),
            QuantityComparator.LESS_OR_EQUAL.toCode(),
            QuantityComparator.GREATER_OR_EQUAL.toCode(),
            QuantityComparator.GREATER_THAN.toCode()));

    TItemDefinition rangeType = findFrame(dmn, RANGE.toCode());
    checkSlot(rangeType, "low", SIMPLEQUANTITY.toCode(), false);
    checkSlot(rangeType, "high", SIMPLEQUANTITY.toCode(), false);

    checkSlot(rootType, "dataAbsentReason", CODEABLECONCEPT.toCode(), false);
    checkSlot(rootType, "interpretation", CODEABLECONCEPT.toCode(), false);
    checkSlot(rootType, "comment", "string", false);

    checkSlot(rootType, "bodySite", CODEABLECONCEPT.toCode(), false);
    checkSlot(rootType, "method", CODEABLECONCEPT.toCode(), false);

    checkSlot(rootType, "specimen", REFERENCE.toCode(), false);
    checkSlot(rootType, "device", REFERENCE.toCode(), false);

    checkSlot(rootType, "referenceRange", ObservationReferenceRangeComponent.class.getSimpleName(),
        true);
    TItemDefinition refRangeType = findFrame(dmn,
        ObservationReferenceRangeComponent.class.getSimpleName());
    checkSlot(refRangeType, "low", SIMPLEQUANTITY.toCode(), false);
    checkSlot(refRangeType, "high", SIMPLEQUANTITY.toCode(), false);
    checkSlot(refRangeType, "type", CODEABLECONCEPT.toCode(), false);
    checkSlot(refRangeType, "appliesTo", CODEABLECONCEPT.toCode(), true);
    checkSlot(refRangeType, "age", RANGE.toCode(), false);
    checkSlot(refRangeType, "text", "string", false);

    checkSlot(rootType, "related", ObservationRelatedComponent.class.getSimpleName(), true);
    TItemDefinition relatedType = findFrame(dmn, ObservationRelatedComponent.class.getSimpleName());
    checkSlot(relatedType, "type", ObservationRelationshipType.class.getSimpleName(), false);
    checkSlot(relatedType, "target", REFERENCE.toCode(), false);
    checkEnumeration(dmn, ObservationRelationshipType.class.getSimpleName(),
        Arrays.asList(
            ObservationRelationshipType.HASMEMBER.toCode(),
            ObservationRelationshipType.DERIVEDFROM.toCode(),
            ObservationRelationshipType.SEQUELTO.toCode(),
            ObservationRelationshipType.REPLACES.toCode(),
            ObservationRelationshipType.QUALIFIEDBY.toCode(),
            ObservationRelationshipType.INTERFEREDBY.toCode()));

    checkSlot(rootType, "component", ObservationComponentComponent.class.getSimpleName(), true);
    TItemDefinition compType = findFrame(dmn, ObservationComponentComponent.class.getSimpleName());
    checkSlot(compType, "code", CODEABLECONCEPT.toCode(), false);
    checkSlot(compType, "valueString", "string", false);
    checkSlot(compType, "valueTime", "time", false);
    checkSlot(compType, "valueQuantity", QUANTITY.toCode(), false);
    checkSlot(compType, "dataAbsentReason", CODEABLECONCEPT.toCode(), false);
    checkSlot(compType, "interpretation", CODEABLECONCEPT.toCode(), false);
    checkSlot(compType, "referenceRange", ObservationReferenceRangeComponent.class.getSimpleName(),
        true);
  }

  private void testIntegrity(TDefinitions dmn) {
    dmn.getItemDefinition()
        .forEach(def -> assertTrue(Character.isUpperCase(def.getName().charAt(0))));
    dmn.getItemDefinition()
        .forEach(def -> {
          def.getItemComponent().stream()
              .map(TItemDefinition::getTypeRef)
              .filter(type -> !isFeelPrimitive(type))
              .forEach(
                  slotType -> findFrame(dmn, slotType)
              );
        });
  }

  private void checkEnumeration(TDefinitions dmn, String enumName, List<String> values) {
    TItemDefinition enumType = findFrame(dmn, enumName);
    assertEquals(values.stream().collect(Collectors.joining(",")),
        enumType.getAllowedValues().getText());
  }

  private void checkSlot(TItemDefinition rootType,
      String slotName,
      String slotType,
      boolean isCollection) {
    TItemDefinition slot = findSlot(rootType, slotName)
        .orElseGet(() -> fail("Unable to find slot: " + slotName));
    assertEquals(slotType, slot.getTypeRef());
    assertEquals(isCollection, slot.isIsCollection());
  }

  private void checkNotExistingSlot(TItemDefinition rootType,
      String slotName) {
    assertFalse(findSlot(rootType, slotName).isPresent());
  }

  private TItemDefinition findFrame(TDefinitions dmn, String typeId) {
    return dmn.getItemDefinition().stream()
        .filter(def -> typeId.equals(def.getId()))
        .findFirst()
        .orElseGet(() -> fail("Unable to find type " + typeId));
  }

  private Optional<TItemDefinition> findSlot(TItemDefinition rootType, String slotName) {
    if (Util.isEmpty(slotName)) {
      return Optional.empty();
    }
    return rootType.getItemComponent().stream()
        .filter(slot -> slotName.equals(slot.getName()))
        .findFirst();
  }

  private void printout(Answer<KnowledgeCarrier> ast) {
    ast.flatMap(ans -> new DMN12Parser()
        .applyLower(ans, Serialized_Knowledge_Expression, null, null))
        .flatOpt(AbstractCarrier::asString)
        .ifPresent(System.out::print);
  }

  private KnowledgeCarrier readProfile(String srcFile) {
    return AbstractCarrier.ofAst(
        FileUtil.read(FhirProfileTranslatorTest.class
            .getResourceAsStream("/fhir.stu3/" + srcFile))
            .map(str -> FHIR3JsonUtil.instance.parse(str, StructureDefinition.class))
            .orElse(null)
    ).withRepresentation(rep(FHIR_STU3));
  }

}
