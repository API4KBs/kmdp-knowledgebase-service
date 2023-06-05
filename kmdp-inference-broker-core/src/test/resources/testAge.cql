library Age

using FHIR version '1.8'

context Patient

// 01aaaf22-ca7f-42d0-a1a2-f027fbf81fa6
define "CurrentChronologicalAge":
    Quantity {  value: FHIR.decimal { value: AgeInYears() },
                unit : FHIR.string { value: 'y' } }
